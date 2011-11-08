/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.bluetooth;

import com.android.settings.R;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.util.Log;
import android.os.SystemProperties;

/**
 * BluetoothDiscoverableEnabler is a helper to manage the "Discoverable"
 * checkbox. It sets/unsets discoverability and keeps track of how much time
 * until the the discoverability is automatically turned off.
 */
public class BluetoothDiscoverableEnabler implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "BluetoothDiscoverableEnabler";

    private static final String SYSTEM_PROPERTY_DISCOVERABLE_TIMEOUT =
            "debug.bt.discoverable_time";

    static final int DISCOVERABLE_TIMEOUT_TWO_MINUTES = 120;
    static final int DISCOVERABLE_TIMEOUT_FIVE_MINUTES = 300;
    static final int DISCOVERABLE_TIMEOUT_ONE_HOUR = 3600;
    static final int DISCOVERABLE_TIMEOUT_NEVER = 0;

    public static final String SHARED_PREFERENCES_KEY_DISCOVERABLE_END_TIMESTAMP =
        "discoverable_end_timestamp";

    private static final String VALUE_DISCOVERABLE_TIMEOUT_TWO_MINUTES = "twomin";
    private static final String VALUE_DISCOVERABLE_TIMEOUT_FIVE_MINUTES = "fivemin";
    private static final String VALUE_DISCOVERABLE_TIMEOUT_ONE_HOUR = "onehour";
    private static final String VALUE_DISCOVERABLE_TIMEOUT_NEVER = "never";

    static final int DEFAULT_DISCOVERABLE_TIMEOUT = DISCOVERABLE_TIMEOUT_TWO_MINUTES;

    /* Variables to synchronize UI timer updates for discoverability period. */
    private static boolean mWasDiscoverable = false;

    private final Context mContext;
    private final Handler mUiHandler;
    private final CheckBoxPreference mCheckBoxPreference;
    private final ListPreference mTimeoutListPreference;

    private final LocalBluetoothManager mLocalManager;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(intent.getAction())) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
                        BluetoothAdapter.ERROR);
                if (mode != BluetoothAdapter.ERROR) {
                    Log.v(TAG, "onReceive called for scan mode change");
                    handleModeChanged(mode);
                }
            }
            else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF) {
                    if (isDiscoverable()) {
                        Log.v(TAG, "onReceive called for bt off: " +
                            SystemProperties.get("bluetooth.prev.discoverable"));
                        SystemProperties.set("bluetooth.prev.discoverable","true");
                    } else {
                        Log.v(TAG, "onReceive called for bt off: " +
                            SystemProperties.get("bluetooth.prev.discoverable"));
                        SystemProperties.set("bluetooth.prev.discoverable","false");
                    }

                }
            }
        }
    };

    private final Runnable mUpdateCountdownSummaryRunnable = new Runnable() {
        public void run() {
            updateCountdownSummary();
        }
    };

    public BluetoothDiscoverableEnabler(Context context,
            CheckBoxPreference checkBoxPreference, ListPreference timeoutListPreference) {
        mContext = context;
        mUiHandler = new Handler();
        mCheckBoxPreference = checkBoxPreference;
        mTimeoutListPreference = timeoutListPreference;

        checkBoxPreference.setPersistent(false);
        // we actually want to persist this since can't infer from BT device state
        mTimeoutListPreference.setPersistent(true);

        mLocalManager = LocalBluetoothManager.getInstance(context);
        if (mLocalManager == null) {
            // Bluetooth not supported
            checkBoxPreference.setEnabled(false);
        }
    }

    public void resume() {
        if (mLocalManager == null) {
            return;
        }

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
        mCheckBoxPreference.setOnPreferenceChangeListener(this);
        mTimeoutListPreference.setOnPreferenceChangeListener(this);
        if ("true".equals(SystemProperties.get("bluetooth.prev.discoverable"))) {
            mWasDiscoverable = true;
        }
        Log.v(TAG, "resume called: previous discoverable state: " + mWasDiscoverable);
        Log.v(TAG, "resume called: bluetooth.onboot.discov: " +
              SystemProperties.get("bluetooth.onboot.discov"));
        if ("true".equals(SystemProperties.get("bluetooth.onboot.discov"))) {
            Log.v(TAG, "resume called: reset system property and make connectable only");
            /* Reset the property and make the device connectable only for BT on at
             * boot irrespective of previous scan state */
            SystemProperties.set("bluetooth.onboot.discov","false");
            setEnabled(false);
        } else {
            handleModeChanged(mLocalManager.getBluetoothAdapter().getScanMode());
        }
    }

    public void pause() {
        if (mLocalManager == null) {
            return;
        }

        mUiHandler.removeCallbacks(mUpdateCountdownSummaryRunnable);
        mCheckBoxPreference.setOnPreferenceChangeListener(null);
        mTimeoutListPreference.setOnPreferenceChangeListener(null);
        mContext.unregisterReceiver(mReceiver);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mCheckBoxPreference) {
            // Turn on/off BT discoverability
            setEnabled((Boolean) value);
        } else if (preference == mTimeoutListPreference) {
            mTimeoutListPreference.setValue((String) value);
            setEnabled(true);
        }

        return true;
    }

    private void setEnabled(final boolean enable) {
        BluetoothAdapter manager = mLocalManager.getBluetoothAdapter();

        if (enable) {
            int timeout = getDiscoverableTimeout();
            manager.setDiscoverableTimeout(timeout);

            if (timeout > 0) {
                mCheckBoxPreference.setSummaryOn(
                        mContext.getResources().getString(R.string.bluetooth_is_discoverable,
                                                          timeout));

                long endTimestamp = System.currentTimeMillis() + timeout * 1000L;
                persistDiscoverableEndTimestamp(endTimestamp);
                updateCountdownSummary();
            }

            manager.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, timeout);
        } else {
            manager.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
        }
    }

    private void updateTimerDisplay(int timeout) {
        if (getDiscoverableTimeout() == DISCOVERABLE_TIMEOUT_NEVER) {
            mCheckBoxPreference.setSummaryOn(
                mContext.getResources()
                .getString(R.string.bluetooth_is_discoverable_always));
        } else {
            mCheckBoxPreference.setSummaryOn(
                mContext.getResources()
                .getString(R.string.bluetooth_is_discoverable, timeout));
        }
    }

    private int getDiscoverableTimeout() {
        int timeout = SystemProperties.getInt(SYSTEM_PROPERTY_DISCOVERABLE_TIMEOUT, -1);
        if (timeout < 0) {
            String timeoutValue = null;
            if (mTimeoutListPreference != null && mTimeoutListPreference.getValue() != null) {
                timeoutValue = mTimeoutListPreference.getValue().toString();
            } else {
                mTimeoutListPreference.setValue(VALUE_DISCOVERABLE_TIMEOUT_TWO_MINUTES);
                return DISCOVERABLE_TIMEOUT_TWO_MINUTES;
            }

            if (timeoutValue.equals(VALUE_DISCOVERABLE_TIMEOUT_NEVER)) {
                timeout = DISCOVERABLE_TIMEOUT_NEVER;
            } else if (timeoutValue.equals(VALUE_DISCOVERABLE_TIMEOUT_ONE_HOUR)) {
                timeout = DISCOVERABLE_TIMEOUT_ONE_HOUR;
            } else if (timeoutValue.equals(VALUE_DISCOVERABLE_TIMEOUT_FIVE_MINUTES)) {
                timeout = DISCOVERABLE_TIMEOUT_FIVE_MINUTES;
            } else {
                timeout = DISCOVERABLE_TIMEOUT_TWO_MINUTES;
            }
        }

        return timeout;
    }

    private void persistDiscoverableEndTimestamp(long endTimestamp) {
        SharedPreferences.Editor editor = mLocalManager.getSharedPreferences().edit();
        editor.putLong(SHARED_PREFERENCES_KEY_DISCOVERABLE_END_TIMESTAMP, endTimestamp);
        editor.apply();
    }

    private void handleModeChanged(int mode) {
        if (mode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            mCheckBoxPreference.setChecked(true);
            int timeout = getDiscoverableTimeout();
            Log.v(TAG, "handleModeChanged called: previous discoverable state: " + mWasDiscoverable
                       + ", " + SystemProperties.get("bluetooth.prev.discoverable"));
            Log.v(TAG, "handleModeChanged called: Discoverable Timeout: " + timeout +
                       " seconds, previous discoverable state: " + (mWasDiscoverable ||
                       "true".equals(SystemProperties.get("bluetooth.prev.discoverable"))));
            if (timeout > 0 && (mWasDiscoverable ||
                              "true".equals(SystemProperties.get("bluetooth.prev.discoverable")))) {
                /* If discoverable period was earlier cancelled by BT being turned off,
                 * either from BluetoothSettings or WirelessSettings view then
                 * local BT is made discoverable with previously selected discoverable
                 * period in successive BT on iterations from either of these views */
                mWasDiscoverable = false;
                SystemProperties.set("bluetooth.prev.discoverable","false");
                long endTimestamp = System.currentTimeMillis() + timeout * 1000;
                persistDiscoverableEndTimestamp(endTimestamp);
            }
            updateCountdownSummary();
        } else {
            mCheckBoxPreference.setChecked(false);
            Log.v(TAG, "handleModeChanged called: previous discoverable state: " + mWasDiscoverable
                       + ", " + SystemProperties.get("bluetooth.prev.discoverable"));
            Log.v(TAG, "handleModeChanged called: Bluetooth State: " +
                        mLocalManager.getBluetoothAdapter().getState());
            if (mLocalManager.getBluetoothAdapter().getState() != BluetoothAdapter.STATE_OFF) {
                /* After BT discoverable period has completely elapsed then reset the
                 * variables used to restart the discoverable period if cancelled
                 * because of BT off operation */
                mWasDiscoverable = false;
                SystemProperties.set("bluetooth.prev.discoverable","false");
                SystemProperties.set("bluetooth.onboot.discov","false");
            }
        }
    }

    private boolean isDiscoverable() {
        long currentTimestamp = System.currentTimeMillis();
        long endTimestamp = mLocalManager.getSharedPreferences().getLong(
                SHARED_PREFERENCES_KEY_DISCOVERABLE_END_TIMESTAMP, 0);

        Log.v(TAG, "isDiscoverable: currentTimestamp: " + currentTimestamp
                    + ", endTimestamp: " + endTimestamp);
        if (currentTimestamp >= endTimestamp) {
            Log.v(TAG, "isDiscoverable: Not discoverable");
            return false;
        } else {
            Log.v(TAG, "isDiscoverable: Discoverable");
            return true;
        }

    }

    private void updateCountdownSummary() {
        int mode = mLocalManager.getBluetoothAdapter().getScanMode();
        if (mode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            return;
        }

        if ( 0 == getDiscoverableTimeout()) {
            mCheckBoxPreference.setSummaryOn(null);
            return;
        }

        long currentTimestamp = System.currentTimeMillis();
        long endTimestamp = mLocalManager.getSharedPreferences().getLong(
                SHARED_PREFERENCES_KEY_DISCOVERABLE_END_TIMESTAMP, 0);

        if (currentTimestamp > endTimestamp) {
            // We're still in discoverable mode, but maybe there isn't a timeout.
            mCheckBoxPreference.setSummaryOn(null);
            return;
        }

        int timeLeft = (int) ((endTimestamp - currentTimestamp) / 1000L);
        updateTimerDisplay(timeLeft);

        synchronized (this) {
            mUiHandler.removeCallbacks(mUpdateCountdownSummaryRunnable);
            mUiHandler.postDelayed(mUpdateCountdownSummaryRunnable, 1000);
        }
    }


}
