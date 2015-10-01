/*
     Copyright (c) 2015, The Linux Foundation. All Rights Reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.android.settings.wificall;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.app.Activity;
import com.android.settings.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.SearchView.OnSuggestionListener;
import android.widget.Switch;
import android.telephony.SubscriptionManager;
import com.android.ims.ImsConfig;
import com.android.ims.ImsManager;
import com.android.ims.ImsException;
import com.android.ims.ImsConfigListener;

public class WifiCallSwitchPreference extends SwitchPreference {

    private static final String TAG = "WifiCallSwitchPreference";
    private int mState = ImsConfig.WifiCallingValueConstants.ON;
    private int mPreference = ImsConfig.WifiCallingPreference.WIFI_PREFERRED;
    private Activity mParent = null;
    private static BroadcastReceiver mReceiver = null;
    private ImsConfig mImsConfig;
    private boolean mSwitchClicked = false;
    private String mWFCStatusMsgDisplay = "";

    public WifiCallSwitchPreference(Context context) {
        super(context);
        initImsConfig();
        mSwitchClicked = false;
        Log.d(TAG, "WifiCallSwitchPreference constructor 1");
    }

    public WifiCallSwitchPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initImsConfig();
        mSwitchClicked = false;
        Log.d(TAG, "WifiCallSwitchPreference constructor 2");
    }

    public WifiCallSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WifiCallSwitchPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.switchPreferenceStyle);
    }

    private void initImsConfig() {
        try {
            ImsManager imsManager = ImsManager.getInstance(getContext(),
                    SubscriptionManager.getDefaultVoiceSubId());
            mImsConfig = imsManager.getConfigInterface();
            Log.d(TAG, "mImsConfig:"+mImsConfig);
        } catch (ImsException e) {
            mImsConfig = null;
            Log.e(TAG, "ImsService is not running");
        }
    }

    public void getWifiCallingPreference() {
        Log.d(TAG, "getWifiCallingPreference called");
        try {
            if (mImsConfig != null) {
                mImsConfig.getWifiCallingPreference(imsConfigListener);
            } else {
                mState = isChecked()? ImsConfig.WifiCallingValueConstants.ON:
                        ImsConfig.WifiCallingValueConstants.OFF;
                mPreference = 1;
                Log.e(TAG, "getWifiCallingPreference failed. mImsConfig is null");
            }
        } catch (ImsException e) {
            Log.e(TAG, "getWifiCallingPreference failed. Exception = " + e);
        }
    }

    public void setParentActivity(Activity act) {
        mParent = act;
    }

    public void registerReciever() {
        Log.d(TAG, "registerReciever");
        unRegisterReciever();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiCallingStatusContral.ACTION_WIFI_CALL_READY_STATUS_CHANGE);
        filter.addAction(WifiCallingStatusContral.ACTION_WIFI_CALL_ERROR_CODE);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ((mState == ImsConfig.WifiCallingValueConstants.OFF) ||
                        (mState == ImsConfig.WifiCallingValueConstants.NOT_SUPPORTED)){
                    Log.d(TAG, "do not handle any intent when wificall turned off");
                    return;
                }

                if (WifiCallingStatusContral.ACTION_WIFI_CALL_ERROR_CODE.
                            equals(intent.getAction()) ||
                    WifiCallingStatusContral.ACTION_WIFI_CALL_READY_STATUS_CHANGE.
                            equals(intent.getAction())) {
                    updateWFCStatusFromIntent(intent);
                    refreshSwitchSummary(mWFCStatusMsgDisplay);
                }
            } //end onReceive
        };
        getContext().registerReceiver(mReceiver, filter);
    }

    public void unRegisterReciever() {
        if (mReceiver != null) {
            getContext().unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    private void updateWFCStatusFromIntent(Intent intent) {
        if (intent == null) {
            Log.e(TAG, "updateWFCStatusDisplay: empty intent!");
        } else if (WifiCallingStatusContral.ACTION_WIFI_CALL_ERROR_CODE.
                equals(intent.getAction())) {
            mWFCStatusMsgDisplay = intent.getStringExtra(
                    WifiCallingStatusContral.ACTION_WIFI_CALL_ERROR_CODE_EXTRA);
        } else {
            Log.e(TAG, "unexpected intent handled.");
        }
        Log.d(TAG, "updateWFCStatusFromIntent called. mWFCStatusMsgDisplay:" + mWFCStatusMsgDisplay);
    }

    private void updateWFCStatusFromProp() {
        if ((mState == ImsConfig.WifiCallingValueConstants.OFF) ||
                (mState == ImsConfig.WifiCallingValueConstants.NOT_SUPPORTED)) {
            mWFCStatusMsgDisplay = getContext().getString(R.string.wifi_call_status_disabled);
        } else {
            if (mSwitchClicked) {
                //Display "enabling..." if user just turns on WFC.
                //This message will be updated upon other intents.
                mWFCStatusMsgDisplay = getContext().getString(R.string.wifi_call_status_enabling);
            } else {
                String msg = SystemProperties.get(
                        WifiCallingStatusContral.SYSTEM_PROPERTY_WIFI_CALL_STATUS_MSG, "");
                if (!TextUtils.isEmpty(msg)) {
                    mWFCStatusMsgDisplay = msg;
                } else {
                    mWFCStatusMsgDisplay = getContext().getString(R.string.wifi_call_status_error_unknown);
                }
            }
        }

        Log.d(TAG, "updateWFCStatusFromProp called. mWFCStatusMsgDisplay:" + mWFCStatusMsgDisplay);
    }

    @Override
    protected void onClick() {
        super.onClick();
        Log.i(TAG, "onClik CheckedStatus : " + isChecked());
        // The switchpreference turn on/off must keep as the same as the interal switch
        setChecked(!isChecked());
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        View checkableView = view
                .findViewById(com.android.internal.R.id.switchWidget);
        if (checkableView != null && checkableView instanceof Checkable) {
            if (checkableView instanceof Switch) {
                Log.i(TAG, "start setOnCheckedChangeListener");
                ((Switch) checkableView).setClickable(true);
                final Switch switchView = (Switch) checkableView;
                // Add the switch checkedChangeListener for that
                // when user press the switch to turn on/off the wifi calling
                // press the text part for the wifi calling preference interface.
                switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(
                            CompoundButton buttonView, boolean isChecked) {
                        Log.i(TAG, "start onCheckedChanged isChecked : " + isChecked);
                        if (!callChangeListener(isChecked)) {
                            Log.e(TAG, "onCheckedChanged does not like it. Change it back.");
                            buttonView.setChecked(!isChecked);
                            return;
                        }
                        Log.i(TAG, "start onCheckedChanged isChecked : " + isChecked);

                        WifiCallSwitchPreference.this.setChecked(isChecked);
                        onSwitchClicked();
                    }
                });
            }
        }
    }

    public void onSwitchClicked() {
        Log.d(TAG, "onSwitchClicked " + isChecked());
        mSwitchClicked = true;
        getWifiCallingPreference();
        updateWFCStatusFromProp();
        refreshSwitchSummary(mWFCStatusMsgDisplay);
    }

    private boolean setWifiCallingPreference(int state, int preference) {
        Log.d(TAG, "setWifiCallingPreference:");
        boolean result = false;
        try {
            if (mImsConfig != null) {
                mImsConfig.setWifiCallingPreference(state,
                        preference, imsConfigListener);
                Log.d(TAG, "setWifiCallingPreference:");
                result = true;
            } else {
                Log.e(TAG, "setWifiCallingPreference failed. mImsConfig is null");
            }
        } catch (ImsException e) {
            Log.e(TAG, "setWifiCallingPreference failed. Exception = " + e);
        }
        return result;
    }

    private void checkWifiCallingCapability(int state) {
        Log.d(TAG, "checkWifiCallingCapability:");
        if (state == ImsConfig.WifiCallingValueConstants.NOT_SUPPORTED) {
            refreshSwitchEnabled(true);
            Log.e(TAG, "checkWifiCallingCapability: wificalling not supported.");
        }
    }

    private void syncUserSetting2Modem(int state, int pref) {
        Log.d(TAG, "sync user setting to modem: state=" + state + " Preference=" + pref);
        setWifiCallingPreference(state, pref);
        Intent intent = new Intent(state == ImsConfig.WifiCallingValueConstants.ON ?
                WifiCallingStatusContral.ACTION_WIFI_CALL_TURN_ON :
                WifiCallingStatusContral.ACTION_WIFI_CALL_TURN_OFF);
        intent.putExtra("preference", pref);
        getContext().sendBroadcast(intent);
    }

    private void syncUserSettingFromModem(int state, int pref) {
        Log.d(TAG, "sync user setting from modem: state=" + state + " Preference=" + pref);
        mPreference = pref;
        if (state != mState) {
            mState = state;
            updateWFCStatusFromProp();
            refreshSwitchState(mState);
            refreshSwitchSummary(mWFCStatusMsgDisplay);
        }
    }

    private void refreshSwitchState(final int state) {
        Log.d(TAG, "refreshSwitchState");
        if (mParent == null) {
            Log.e(TAG, "refreshSwitchState: mParent = null!");
        } else {
            mParent.runOnUiThread(new Runnable() {
                public void run() {
                    Log.d (TAG, "new UI thread.");
                    setChecked((state == ImsConfig.WifiCallingValueConstants.ON));
                }
            });
        }
    }

    private void refreshSwitchEnabled(final boolean isGreyOut) {
        Log.d(TAG, "refreshSwitchEnabled");
        if (mParent == null) {
            Log.e(TAG, "refreshSwitchEnabled: mParent = null!");
        } else {
            mParent.runOnUiThread(new Runnable() {
                public void run() {
                    Log.d (TAG, "new UI thread.");
                    setEnabled(!isGreyOut);
                }
            });
        }
    }

    private void refreshSwitchSummary(final String msg) {
        Log.d(TAG, "refreshSwitchSummary");
        if ((mParent == null) && (msg == null)) {
            Log.e(TAG, "refreshSwitchSummary: mParent = null or message = null");
        } else {
            mParent.runOnUiThread(new Runnable() {
                public void run() {
                    Log.d (TAG, "new UI thread.");
                    setSummary(msg);
                }
            });
        }
    }

    private ImsConfigListener imsConfigListener = new ImsConfigListener.Stub() {
        public void onGetVideoQuality(int status, int quality) {
            //TODO not required as of now
        }

        public void onSetVideoQuality(int status) {
            //TODO not required as of now
        }

        public void onGetFeatureResponse(int feature, int network, int value, int status) {
            //TODO not required as of now
        }

        public void onSetFeatureResponse(int feature, int network, int value, int status) {
            //TODO not required as of now
        }

        public void onGetPacketCount(int status, long packetCount) {
            //TODO not required as of now
        }

        public void onGetPacketErrorCount(int status, long packetErrorCount) {
            //TODO not required as of now
        }

        public void onSetWifiCallingPreference(int status) {
            Log.d(TAG, "onSetWifiCallingPreference");
            if (hasRequestFailed(status)) {
                Log.e(TAG, "onSetWifiCallingPreference : set failed. errorCode = " + status);

                //load default value as per isChecked
                mState = isChecked()? ImsConfig.WifiCallingValueConstants.ON:
                        ImsConfig.WifiCallingValueConstants.OFF;
                mPreference = 1;
            } else {
                Log.d(TAG, "onSetWifiCallingPreference: set succeeded.");
            }
        }

        public void onGetWifiCallingPreference(int status, final int wifiCallingStatus,
                final int wifiCallingPreference) {
            if (hasRequestFailed(status)) {
                Log.e(TAG, "onGetWifiCallingPreference: failed. errorCode = " + status);
                //load default value as per isChecked
                mState = isChecked()? ImsConfig.WifiCallingValueConstants.ON:
                        ImsConfig.WifiCallingValueConstants.OFF;
                mPreference = ImsConfig.WifiCallingPreference.WIFI_PREFERRED;
            } else {
                Log.d(TAG, "onGetWifiCallingPreference");
                checkWifiCallingCapability(wifiCallingStatus);
                if (mSwitchClicked) {
                    Log.d (TAG, "mSwitchClicked is true.");
                    int state = isChecked() ? ImsConfig.WifiCallingValueConstants.ON:
                            ImsConfig.WifiCallingValueConstants.OFF;
                    //add check if change happen
                    syncUserSetting2Modem(state, mPreference);
                    mSwitchClicked = false;
                } else {
                    syncUserSettingFromModem(wifiCallingStatus, wifiCallingPreference);
                    updateWFCStatusFromProp();
                    refreshSwitchState(wifiCallingStatus);
                    refreshSwitchSummary(mWFCStatusMsgDisplay);
                }
            }

        }

        private boolean hasRequestFailed(int result) {
            return (result != ImsConfig.OperationStatusConstants.SUCCESS);
        }
    };

}
