/*
     Copyright (c) 2014, The Linux Foundation. All Rights Reserved.
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

package com.android.settings.wifi;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.SharedPreferences;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiDevice;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.provider.Settings.System;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import android.widget.NumberPicker;
import java.util.ArrayList;
import java.util.List;
import com.android.settings.adddevicesinfo.db.AddDevicesInfoDBManager;

/*
 * Displays preferences for Tethering.
 */
public class HotspotSettings extends SettingsPreferenceFragment implements
        DialogInterface.OnClickListener {

    public static final String TAG = "HotspotSettings";
    private static final boolean DEBUG = true;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private int niSelectedMaximum = 8;
    private String maximumConnectionsDialogTitle = null;
    private String maximumConnectionsPositive = null;
    private String maximumConnectionsNegative = null;
    private WifiConfiguration mWifiConfig = null;

    private static final String MAXIMUM_CONNECTIONS_BUTTON = "maximum_connections";
    private static final String WIFI_HOTSPOT_MAX_CLIENT_NUM = "WIFI_HOTSPOT_MAX_CLIENT_NUM";
    private static final int PROVISION_REQUEST = 0;
    private static final int DIALOG_AP_SETTINGS = 1;
    private static final int CONFIG_SUBTEXT = R.string.wifi_tether_configure_subtext;
    private static final int MENU_HELP = Menu.FIRST;
    private static final String ALLOWED_DEVICES_PREFERENCE = "allowed_devices";
    private static final String WIFI_AP_SSID_AND_SECURITY = "wifi_ap_ssid_and_security";
    private static final String AP_CONNECTED_STATE_CHANGED_ACTION =
            "android.net.conn.TETHER_CONNECT_STATE_CHANGED";
    private static final String KEY_AP_DEVICE_LIST = "ap_device_list";
    private static final String CONN_STATE = "ConnectedState";
    private static final String CONN_ADDR = "ConnectedAddress";
    private static final String CONN_NAME = "ConnectedName";
    private static final String CONN_COUNT = "ConnectedCount";

    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;
    private List<WifiDevice> mTetherConnectedStaList = new ArrayList<WifiDevice>();
    private List<String> mConnectedNameList = new ArrayList<String>();
    private List<String> mConnectedAddressList = new ArrayList<String>();
    private PreferenceCategory mListPref;
    private TextView mEmptyView;
    private WifiApSwitch mWifiApSwitch;
    private ConnectivityManager mCm;
    private WifiApDialog mDialog;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiApConfig = null;
    private String[] mProvisionApp;
    private Preference mCreateNetworkPref;
    private PreferenceScreen mAllowedDevice;
    private AddDevicesInfoDBManager mdbManager;
    private PreferenceScreen mMaximumConnections;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.hotspot_settings_pre);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiApConfig = mWifiManager.getWifiApConfiguration();
        mProvisionApp = getActivity().getResources().getStringArray(
                com.android.internal.R.array.config_mobile_hotspot_provision_app);

        mFilter = new IntentFilter();
        mFilter.addAction(AP_CONNECTED_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(context, intent);
            }
        };

        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        setHasOptionsMenu(true);
        if (getResources().getBoolean(
                com.android.internal.R.bool.config_regional_hotspot_accout_check_enable)) {
            final Activity activity = getActivity();
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        if (getResources().getBoolean(
                com.android.internal.R.bool.config_regional_hotspot_show_allowed_devices_enable)) {
            mAllowedDevice = (PreferenceScreen)findPreference(ALLOWED_DEVICES_PREFERENCE);
            mdbManager = new AddDevicesInfoDBManager(getActivity());
            mAllowedDevice.setSummary(mdbManager.getDevicesCount() + " "
                    + getActivity().getResources().getString(R.string.add_devices_summary));
        } else {
            getPreferenceScreen().removePreference(findPreference(ALLOWED_DEVICES_PREFERENCE));
        }
        if (getResources().getBoolean(
                com.android.internal.R.bool
                .config_regional_hotspot_show_maximum_connection_enable)) {
            final Activity activity = getActivity();
            sharedPreferences = activity.getSharedPreferences("MY_PERFS",Activity.MODE_PRIVATE);
            editor = sharedPreferences.edit();
            mMaximumConnections = (PreferenceScreen) findPreference(MAXIMUM_CONNECTIONS_BUTTON);
            maximumConnectionsDialogTitle = activity.getResources().getString(R.string
                    .maximum_connections_dialog_title_text);
            maximumConnectionsPositive = activity.getResources().getString(R.string
                    .maximum_connections_dialog_positive_text);
            maximumConnectionsNegative = activity.getResources().getString(R.string
                    .maximum_connections_dialog_negative_text);
            mMaximumConnections.setSummary(""+(sharedPreferences.getInt("maximum",8)));
        } else {
            getPreferenceScreen().removePreference(findPreference(MAXIMUM_CONNECTIONS_BUTTON));
        }
        initWifiTethering();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wifi_ap_deveice_list, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PreferenceScreen root = getPreferenceScreen();
        mListPref = (PreferenceCategory) root.findPreference(KEY_AP_DEVICE_LIST);
        Activity activity = getActivity();

        Switch actionBarSwitch = new Switch(activity);
        final int padding = activity.getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        actionBarSwitch.setPaddingRelative(0, 0, padding, 0);
        activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(actionBarSwitch, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL | Gravity.END));
        activity.getActionBar().setTitle(R.string.wifi_tether_checkbox_text);

        mWifiApSwitch = new WifiApSwitch(activity, this, actionBarSwitch);
        mCm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);

        mEmptyView = (TextView) getView().findViewById(R.id.empty);
    }

    @Override
    public void onResume() {
        super.onResume();
        log("onResume..");
        mWifiApSwitch.resume();
        if (getResources().getBoolean(
                com.android.internal.R.bool.config_regional_hotspot_show_allowed_devices_enable)) {
            mAllowedDevice.setSummary(mdbManager.getDevicesCount() + " "
                + getActivity().getResources().getString(R.string.add_devices_summary));
        }
        getActivity().registerReceiver(mReceiver, mFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
        mWifiApSwitch.pause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add(0, MENU_HELP, 0, R.string.hotspot_settings_menu_help);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_HELP:
                Intent intent = new Intent();
                intent.setClass(getActivity(), HotspotSettingsHelp.class);
                getActivity().startActivity(intent);
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    private String getSecurityTypeIndexToString() {
        String[] securityList = getResources().getStringArray(R.array.wifi_ap_security);
        if (mWifiApConfig == null) {
            return securityList[WifiApDialog.OPEN_INDEX];
        }
        return securityList[WifiApDialog.getSecurityTypeIndex(mWifiApConfig)];
    }

    private String getApSsid() {
        return mWifiApConfig.SSID;
    }

    private void initWifiTethering() {
        mCreateNetworkPref = findPreference(WIFI_AP_SSID_AND_SECURITY);
        if (mWifiApConfig == null) {
            mCreateNetworkPref.setSummary(String.format(getString(CONFIG_SUBTEXT),
                    getString(com.android.internal.R.string.wifi_tether_configure_ssid_default),
                    getSecurityTypeIndexToString()));
        } else {
            mCreateNetworkPref.setSummary(String.format(getString(CONFIG_SUBTEXT),
                    getApSsid(), getSecurityTypeIndexToString()));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (preference == mCreateNetworkPref) {
            showDialog(DIALOG_AP_SETTINGS);
        } else if (getResources().getBoolean(
                com.android.internal.R.bool
                .config_regional_hotspot_show_maximum_connection_enable)) {
            if (preference == mMaximumConnections) {
                showMaximumConnectionsDialog();
            }
        }

        return super.onPreferenceTreeClick(screen, preference);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_AP_SETTINGS) {
            mDialog = new WifiApDialog(getActivity(), this, mWifiApConfig);
            return mDialog;
        }
        return null;
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button != DialogInterface.BUTTON_POSITIVE) {
            return;
        }
        mWifiApConfig = mDialog.getConfig();
        if (mWifiApConfig != null) {
            /**
             * if soft AP is stopped, bring up else restart with new config
             * TODO: update config on a running access point when framework
             * support is added
             */
            if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                mWifiManager.setWifiApEnabled(null, false);
                mWifiManager.setWifiApEnabled(mWifiApConfig, true);
            } else {
                mWifiManager.setWifiApConfiguration(mWifiApConfig);
            }
            mCreateNetworkPref.setSummary(String.format(
                    getActivity().getString(CONFIG_SUBTEXT),
                    getApSsid(),
                    getSecurityTypeIndexToString()));
        }

    }

    private void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        log("action::" + action);
        if (AP_CONNECTED_STATE_CHANGED_ACTION.equals(action)) {
            constructConnectedDevices();
        }
        if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE,
                    WifiManager.WIFI_AP_STATE_FAILED);
            switch (state) {
                case WifiManager.WIFI_AP_STATE_DISABLED:
                    clearApList();
                    mEmptyView.setVisibility(View.VISIBLE);
                    break;
                case WifiManager.WIFI_AP_STATE_ENABLED:
                case WifiManager.WIFI_AP_STATE_ENABLING:
                case WifiManager.WIFI_AP_STATE_DISABLING:
                    mEmptyView.setVisibility(View.GONE);
                default:
            }
            return;
        }
    }

    private void clearApList() {
        if (mListPref != null) {
            mListPref.removeAll();
        }
    }

    private void constructConnectedDevices() {
        log("constructConnectedDevices..");
        mTetherConnectedStaList = mCm.getTetherConnectedSta();
        if (mTetherConnectedStaList == null || mTetherConnectedStaList.size() == 0) {
            log("ConnectedCount = 0");
            mListPref.removeAll();
            return;
        }
        mConnectedNameList.clear();
        mConnectedAddressList.clear();
        for (int i = 0; i < mTetherConnectedStaList.size(); i++) {
            WifiDevice device = (WifiDevice) mTetherConnectedStaList.get(i);
            if (device.deviceState == WifiDevice.CONNECTED) {
                mConnectedNameList.add(device.deviceName);
                mConnectedAddressList.add(device.deviceAddress);
            }
        }

        mListPref.removeAll();
        for (int index = 0; index < mConnectedAddressList.size(); ++index) {
            log("in construct pref addr = " + mConnectedAddressList.get(index));
            Preference pref = new Preference(getActivity());
            if (mConnectedNameList != null && !mConnectedNameList.get(index).isEmpty()) {
                log("in construct pref.name = " + mConnectedNameList.get(index));
                pref.setTitle(mConnectedNameList.get(index));
            } else {
                pref.setTitle(R.string.ap_device_name_default);
            }
            pref.setSummary(mConnectedAddressList.get(index));
            mListPref.addPreference(pref);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == PROVISION_REQUEST && resultCode == Activity.RESULT_OK) {
            mWifiApSwitch.setSoftapEnabled(true);
        }
    }

    public void startProvisioningIfNecessary(int choice) {
        if (isProvisioningNeeded()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(mProvisionApp[0], mProvisionApp[1]);
            getActivity().startActivityForResult(intent, PROVISION_REQUEST);
        } else {
            if (getResources().getBoolean(
                    com.android.internal.R.bool.config_regional_hotspot_show_help)
                    && needShowHelp()) {
                showHelpDialog();
            } else {
                mWifiApSwitch.setSoftapEnabled(true);
            }
        }
    }

    boolean isProvisioningNeeded() {
        if (SystemProperties.getBoolean("net.tethering.noprovisioning", false)) {
            return false;
        }
        return mProvisionApp.length == 2;
    }

    private void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
    public void showMaximumConnectionsDialog() {
        niSelectedMaximum = sharedPreferences.getInt("maximum", 8);

        final Activity activity = getActivity();
        final AlertDialog dialog = new AlertDialog.Builder(activity).create();
        dialog.setTitle(maximumConnectionsDialogTitle);
        final NumberPicker catalogPick = new NumberPicker(activity);
        dialog.setButton(maximumConnectionsPositive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.putInt(getContentResolver(),
                    WIFI_HOTSPOT_MAX_CLIENT_NUM, (catalogPick.getValue()));
                if (mWifiApConfig != null&&mWifiManager.isWifiApEnabled()) {
                    mWifiManager.setWifiApEnabled(null, false);
                    mWifiManager.setWifiApEnabled(mWifiApConfig, true);
                }
                dialog.dismiss();
                Log.i("sysout","item:"+catalogPick.getValue());
                editor.putInt("maximum",catalogPick.getValue());
                editor.commit();
                mMaximumConnections.setSummary(""+(catalogPick.getValue()));
            }
        });

        dialog.setButton2(maximumConnectionsNegative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        catalogPick.setMaxValue(8);
        catalogPick.setMinValue(1);
        catalogPick.setValue(niSelectedMaximum);// default set 8
        catalogPick.setDescendantFocusability(catalogPick.FOCUS_BLOCK_DESCENDANTS);
        dialog.setView(catalogPick);
        dialog.show();
    }

    private void showHelpDialog() {
        final AlertDialog.Builder build = new AlertDialog.Builder(getActivity());
        build.setTitle(getActivity().getResources().getString(
            R.string.tether_settings_launch_title));
        build.setMessage(getActivity().getResources().getString(
            R.string.wifi_tether_first_use_message));
        build.setNegativeButton(R.string.later,null);
        build.setPositiveButton(R.string.add_device_btn_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        showDialog(DIALOG_AP_SETTINGS);
                    }
                });
        build.show();
    }

    private boolean needShowHelp() {
        boolean isFirstUse = sharedPreferences.getBoolean("FirstLaunchWifiHotspot",true);
        if(isFirstUse) {
           editor = sharedPreferences.edit();
           editor.putBoolean("FirstLaunchWifiHotspot", false);
           editor.commit();
        }
        return isFirstUse;
    }
}
