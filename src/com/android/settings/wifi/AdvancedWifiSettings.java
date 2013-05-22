/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.wifi;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiWatchdogStateMachine;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import android.content.BroadcastReceiver;
import com.qrd.plugin.feature_query.FeatureQuery;

public class AdvancedWifiSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "AdvancedWifiSettings";
    private static final String KEY_MAC_ADDRESS = "mac_address";
    private static final String KEY_CURRENT_IP_ADDRESS = "current_ip_address";
    private static final String KEY_FREQUENCY_BAND = "frequency_band";
    private static final String KEY_NOTIFY_OPEN_NETWORKS = "notify_open_networks";
    private static final String KEY_SLEEP_POLICY = "sleep_policy";
    private static final String KEY_POOR_NETWORK_DETECTION = "wifi_poor_network_detection";
    private static final String KEY_SUSPEND_OPTIMIZATIONS = "suspend_optimizations";

//QUALCOMM_CMCC_START 
    private static final String KEY_CURRENT_GATEWAY = "current_gateway";
    private static final String KEY_CURRENT_NETMASK = "current_netmask";
    private static final String KEY_PRIORITY_TYPE = "wifi_priority_type";
    private static final String KEY_PRIORITY_SETTINGS = "wifi_priority_settings";
    //set whether settings will auto connect wifi 
    private static final String KEY_AUTO_CONNECT_TYPE = "auto_connect_type";
    private static final String KEY_SELECT_IN_SSIDS_TYPE = "select_in_ssids_type";
    private static final String KEY_WIFI_GSM_CONNECT_TYPE = "wifi_gsm_connect_type";
    private static final String KEY_GSM_WIFI_CONNECT_TYPE = "gsm_wifi_connect_type";
//QUALCOMM_CMCC_END 
    private WifiManager mWifiManager;
//QUALCOMM_CMCC_START
    private CheckBoxPreference mPriorityTypePref;
    private Preference mPrioritySettingPref;
    private CheckBoxPreference mAutoConnectTypePref;
    private CheckBoxPreference mWifiGsmConnectTypePref;
    private ListPreference mSelectInSsidsTypePref;
    private ListPreference mGsmWifiConnectTypePref;
//QUALCOMM_CMCC_END

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_advanced_settings);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreferences();
        refreshWifiInfo();
//QUALCOMM_CMCC_START         
        if (FeatureQuery.FEATURE_WLAN_CMCC_SUPPORT) {
            ContentResolver contentResolver = getContentResolver();
            if(mAutoConnectTypePref != null) {
                mAutoConnectTypePref.setChecked(Settings.System.getInt(contentResolver, 
                        Settings.System.WIFI_AUTO_CONNECT_TYPE, Settings.System.WIFI_AUTO_CONNECT_TYPE_AUTO) == Settings.System.WIFI_AUTO_CONNECT_TYPE_AUTO);
            }
			if(mWifiGsmConnectTypePref != null) {
                mWifiGsmConnectTypePref.setChecked(Settings.System.getInt(contentResolver, 
                        Settings.System.WIFI_GSM_CONNECT_TYPE, Settings.System.WIFI_GSM_CONNECT_TYPE_AUTO) == Settings.System.WIFI_GSM_CONNECT_TYPE_ASK);
            }
            if(mGsmWifiConnectTypePref!=null){
                int value = Settings.System.getInt(contentResolver,Settings.System.GSM_WIFI_CONNECT_TYPE, Settings.System.GSM_WIFI_CONNECT_TYPE_AUTO);
                mGsmWifiConnectTypePref.setValue(String.valueOf(value));
            }
            if (mSelectInSsidsTypePref != null) {
                int value = Settings.System.getInt(contentResolver, Settings.System.WIFI_SELECT_IN_SSIDS_TYPE, Settings.System.WIFI_SELECT_IN_SSIDS_ASK);
                mSelectInSsidsTypePref.setValue(String.valueOf(value));
            }
            if (mPriorityTypePref != null){
                mPriorityTypePref.setChecked(Settings.System.getInt(contentResolver, 
                        Settings.System.WIFI_PRIORITY_TYPE, Settings.System.WIFI_PRIORITY_TYPE_DEFAULT) == Settings.System.WIFI_PRIORITY_TYPE_MANUAL);
            }
        }
//QUALCOMM_CMCC_END 
    }

    private void initPreferences() {
        CheckBoxPreference notifyOpenNetworks =
            (CheckBoxPreference) findPreference(KEY_NOTIFY_OPEN_NETWORKS);
        notifyOpenNetworks.setChecked(Settings.Global.getInt(getContentResolver(),
                Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0) == 1);
        notifyOpenNetworks.setEnabled(mWifiManager.isWifiEnabled());

        CheckBoxPreference poorNetworkDetection =
            (CheckBoxPreference) findPreference(KEY_POOR_NETWORK_DETECTION);
        if (poorNetworkDetection != null) {
            if (Utils.isWifiOnly(getActivity())) {
                getPreferenceScreen().removePreference(poorNetworkDetection);
            } else {
                poorNetworkDetection.setChecked(Global.getInt(getContentResolver(),
                        Global.WIFI_WATCHDOG_POOR_NETWORK_TEST_ENABLED,
                        WifiWatchdogStateMachine.DEFAULT_POOR_NETWORK_AVOIDANCE_ENABLED ?
                        1 : 0) == 1);
            }
        }

        CheckBoxPreference suspendOptimizations =
            (CheckBoxPreference) findPreference(KEY_SUSPEND_OPTIMIZATIONS);
        suspendOptimizations.setChecked(Global.getInt(getContentResolver(),
                Global.WIFI_SUSPEND_OPTIMIZATIONS_ENABLED, 1) == 1);

        ListPreference frequencyPref = (ListPreference) findPreference(KEY_FREQUENCY_BAND);

        if (mWifiManager.isDualBandSupported()) {
            frequencyPref.setOnPreferenceChangeListener(this);
            int value = mWifiManager.getFrequencyBand();
            if (value != -1) {
                frequencyPref.setValue(String.valueOf(value));
            } else {
                Log.e(TAG, "Failed to fetch frequency band");
            }
        } else {
            if (frequencyPref != null) {
                // null if it has already been removed before resume
                getPreferenceScreen().removePreference(frequencyPref);
            }
        }

        ListPreference sleepPolicyPref = (ListPreference) findPreference(KEY_SLEEP_POLICY);
        if (sleepPolicyPref != null) {
            if (Utils.isWifiOnly(getActivity())) {
                sleepPolicyPref.setEntries(R.array.wifi_sleep_policy_entries_wifi_only);
            }
            sleepPolicyPref.setOnPreferenceChangeListener(this);
            int value = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.WIFI_SLEEP_POLICY,
                    Settings.Global.WIFI_SLEEP_POLICY_NEVER);
            String stringValue = String.valueOf(value);
            sleepPolicyPref.setValue(stringValue);
            updateSleepPolicySummary(sleepPolicyPref, stringValue);
        }
//QUALCOMM_CMCC_START
        mPriorityTypePref = (CheckBoxPreference)findPreference(KEY_PRIORITY_TYPE);
        mPrioritySettingPref = findPreference(KEY_PRIORITY_SETTINGS);
        mAutoConnectTypePref = (CheckBoxPreference)findPreference(KEY_AUTO_CONNECT_TYPE);
        mSelectInSsidsTypePref = (ListPreference)findPreference(KEY_SELECT_IN_SSIDS_TYPE);
        mGsmWifiConnectTypePref = (ListPreference)findPreference(KEY_GSM_WIFI_CONNECT_TYPE);
        mWifiGsmConnectTypePref = (CheckBoxPreference)findPreference(KEY_WIFI_GSM_CONNECT_TYPE);		
        if(mPriorityTypePref != null && mPrioritySettingPref != null && mAutoConnectTypePref != null && 
			    mSelectInSsidsTypePref != null && mGsmWifiConnectTypePref != null && mWifiGsmConnectTypePref != null) {
            if (FeatureQuery.FEATURE_WLAN_CMCC_SUPPORT) {
                mPriorityTypePref.setOnPreferenceChangeListener(this);
                mAutoConnectTypePref.setOnPreferenceChangeListener(this);
                mSelectInSsidsTypePref.setOnPreferenceChangeListener(this);
				mWifiGsmConnectTypePref.setOnPreferenceChangeListener(this);
				mGsmWifiConnectTypePref.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(mAutoConnectTypePref);
                getPreferenceScreen().removePreference(mPriorityTypePref);
                getPreferenceScreen().removePreference(mPrioritySettingPref);
                getPreferenceScreen().removePreference(mSelectInSsidsTypePref);
				getPreferenceScreen().removePreference(mWifiGsmConnectTypePref);
				getPreferenceScreen().removePreference(mGsmWifiConnectTypePref);
            }
        } else {
            Log.d(TAG, "Fail to get CMCC Pref");
        }
//QUALCOMM_CMCC_END

        replaceWifiToWlan(frequencyPref);
        replaceWifiToWlan(sleepPolicyPref);
        String poorNetwok = WifiManager.replaceAllWiFi(poorNetworkDetection.getSummary().toString());
        poorNetworkDetection.setSummary(poorNetwok);

		suspendOptimizations.setTitle(WifiManager.replaceAllWiFi(suspendOptimizations.getTitle().toString()));		
		suspendOptimizations.setSummary(WifiManager.replaceAllWiFi(suspendOptimizations.getSummary().toString()));
    }

    private void replaceWifiToWlan(ListPreference preference) {
        if(null == preference) return ;
        String prefString = WifiManager.replaceAllWiFi(preference.getTitle().toString());
        preference.setTitle(prefString);
        preference.setDialogTitle(prefString);
    }
	
    private void updateSleepPolicySummary(Preference sleepPolicyPref, String value) {
        if (value != null) {
            String[] values = getResources().getStringArray(R.array.wifi_sleep_policy_values);
            final int summaryArrayResId = Utils.isWifiOnly(getActivity()) ?
                    R.array.wifi_sleep_policy_entries_wifi_only : R.array.wifi_sleep_policy_entries;
            String[] summaries = getResources().getStringArray(summaryArrayResId);
            for (int i = 0; i < values.length; i++) {
                if (value.equals(values[i])) {
                    if (i < summaries.length) {
                        sleepPolicyPref.setSummary(summaries[i]);
                        return;
                    }
                }
            }
        }

        sleepPolicyPref.setSummary("");
        Log.e(TAG, "Invalid sleep policy value: " + value);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        String key = preference.getKey();

        if (KEY_NOTIFY_OPEN_NETWORKS.equals(key)) {
            Global.putInt(getContentResolver(),
                    Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
        } else if (KEY_POOR_NETWORK_DETECTION.equals(key)) {
            Global.putInt(getContentResolver(),
                    Global.WIFI_WATCHDOG_POOR_NETWORK_TEST_ENABLED,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
        } else if (KEY_SUSPEND_OPTIMIZATIONS.equals(key)) {
            Global.putInt(getContentResolver(),
                    Global.WIFI_SUSPEND_OPTIMIZATIONS_ENABLED,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(screen, preference);
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        if (KEY_FREQUENCY_BAND.equals(key)) {
            try {
                mWifiManager.setFrequencyBand(Integer.parseInt((String) newValue), true);
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), R.string.wifi_setting_frequency_band_error,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        if (KEY_SLEEP_POLICY.equals(key)) {
            try {
                String stringValue = (String) newValue;
                Settings.Global.putInt(getContentResolver(), Settings.Global.WIFI_SLEEP_POLICY,
                        Integer.parseInt(stringValue));
                updateSleepPolicySummary(preference, stringValue);
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), R.string.wifi_setting_sleep_policy_error,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }
//QUALCOMM_CMCC_START
		else if (key.equals(KEY_AUTO_CONNECT_TYPE)) {
            boolean checked = ((Boolean) newValue).booleanValue();
            Settings.System.putInt(getContentResolver(), Settings.System.WIFI_AUTO_CONNECT_TYPE, 
                    checked ? Settings.System.WIFI_AUTO_CONNECT_TYPE_AUTO : Settings.System.WIFI_AUTO_CONNECT_TYPE_MANUAL);
        } else if(key.equals(KEY_SELECT_IN_SSIDS_TYPE)){
            try{
                Settings.System.putInt(getContentResolver(),
                        Settings.System.WIFI_SELECT_IN_SSIDS_TYPE, Integer.parseInt(((String) newValue)));
            }catch (NumberFormatException e) {
                return false;
            }
        } else if (key.equals(KEY_PRIORITY_TYPE)) {
            boolean checked = ((Boolean) newValue).booleanValue();
            Settings.System.putInt(getContentResolver(), Settings.System.WIFI_PRIORITY_TYPE, 
                    checked ? Settings.System.WIFI_PRIORITY_TYPE_MANUAL : Settings.System.WIFI_PRIORITY_TYPE_DEFAULT);
        } else if (key.equals(KEY_GSM_WIFI_CONNECT_TYPE)) {
            Log.d(TAG, "Gsm to Wifi connect type is " + newValue);
            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.GSM_WIFI_CONNECT_TYPE, Integer.parseInt(((String) newValue)));
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), R.string.wifi_setting_connect_type_error,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        } else if (key.equals(KEY_WIFI_GSM_CONNECT_TYPE)) {
            Log.d(TAG, "Wifi to Gsm connect type is " + newValue);
            boolean checked = ((Boolean) newValue).booleanValue();
            Settings.System.putInt(getContentResolver(), Settings.System.WIFI_GSM_CONNECT_TYPE, 
                    checked ? Settings.System.WIFI_GSM_CONNECT_TYPE_ASK : Settings.System.WIFI_GSM_CONNECT_TYPE_AUTO);
        }
//QUALCOMM_CMCC_END

        return true;
    }

    private void refreshWifiInfo() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        Preference wifiMacAddressPref = findPreference(KEY_MAC_ADDRESS);
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        wifiMacAddressPref.setSummary(!TextUtils.isEmpty(macAddress) ? macAddress
                : getActivity().getString(R.string.status_unavailable));

        Preference wifiIpAddressPref = findPreference(KEY_CURRENT_IP_ADDRESS);
        String ipAddress = Utils.getWifiIpAddresses(getActivity());
        wifiIpAddressPref.setSummary(ipAddress == null ?
                getActivity().getString(R.string.status_unavailable) : ipAddress);

//QUALCOMM_CMCC_START
        Preference wifiGatewayPref = findPreference(KEY_CURRENT_GATEWAY);
        String gateway = null;
        Preference wifiNetmaskPref = findPreference(KEY_CURRENT_NETMASK);
        String netmask = null;
        if (FeatureQuery.FEATURE_WLAN_CMCC_SUPPORT) {  
            DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
            if (wifiInfo != null) {
                if (dhcpInfo != null) {
                    gateway = ipTransfer(dhcpInfo.gateway);
                    netmask = ipTransfer(dhcpInfo.netmask);
                }
            }
            if (wifiGatewayPref != null) {
                wifiGatewayPref.setSummary(gateway == null ?
                        getString(R.string.status_unavailable) : gateway);
            }
            if (wifiNetmaskPref != null) {
                wifiNetmaskPref.setSummary(netmask == null ?
                        getString(R.string.status_unavailable) : netmask);
            }
        } else {
            PreferenceScreen screen = getPreferenceScreen();
            if (screen!=null){
                if (wifiGatewayPref!=null) {
                    screen.removePreference(wifiGatewayPref);
                }
                if (wifiNetmaskPref!=null) {
                    screen.removePreference(wifiNetmaskPref);
                }
            }
        }
//QUALCOMM_CMCC_END
    }


//QUALCOMM_CMCC_START	
    private String ipTransfer(int value){
        String result = null;
        if (value!=0) {
            if (value < 0) value += 0x100000000L;
            result = String.format("%d.%d.%d.%d",
                    value & 0xFF, (value >> 8) & 0xFF, (value >> 16) & 0xFF, (value >> 24) & 0xFF);
        }
        return result;
    }
//QUALCOMM_CMCC_END
}
