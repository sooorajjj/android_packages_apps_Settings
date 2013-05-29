//QUALCOMM_CMCC_START	
package com.android.settings.wifi.cmcc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.provider.Settings;
import android.os.Handler;
import android.os.Message;
import com.android.internal.util.AsyncChannel;
import android.util.Log;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import com.qrd.plugin.feature_query.FeatureQuery;
import android.telephony.TelephonyManager;
import android.content.pm.PackageManager;
import android.content.ComponentName;

public class CmccMainReceiver extends BroadcastReceiver {
	
    static boolean lastConnected = false;
    static boolean currentConnected = false;
    static boolean isInSSIDs = false;
    static String lastSsid;
	/** These values are matched in string arrays -- changes must be kept in sync */
	static final int SECURITY_NONE = 0;
	static final int SECURITY_WEP = 1;
	static final int SECURITY_PSK = 2;
	static final int SECURITY_EAP = 3;
	static final int SECURITY_WAPI_PSK = 4;
	static final int SECURITY_WAPI_CERT = 5;

    private static final boolean DBG = true;
    private static final String TAG = "CmccMainReceiver";
    private static final String CMCC_SERVICE_ACTION = "com.android.action.wifi.cmccservice";

    private static final int KEY_SELECT_IN_SSIDS_AUTO = 0;
    private static final int KEY_SELECT_IN_SSIDS_MANUL = 1;
    private static final int KEY_SELECT_IN_SSIDS_ASK = 2;
	
	private WifiManager mWifiManager;
	private List<WifiConfiguration> mConfigs;
    private int mRssi;
    private WifiManager.ActionListener mConnectListener;
	
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (DBG) Log.d(TAG, "receive action: " + action);
		if (FeatureQuery.FEATURE_WLAN_CMCC_SUPPORT) {  
	        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {

	            DetailedState state = ((NetworkInfo) intent
	                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).getDetailedState();

	            if (state == DetailedState.CONNECTED) {
	                if (DBG) Log.i(TAG, "WifiManager.NETWORK_STATE_CHANGED_ACTION");
	                startCmccService(context);
	            }
	        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                if (!TelephonyManager.isMultiSimEnabled())
                {
                   PackageManager pm = context.getPackageManager();
                   pm.setComponentEnabledSetting(new ComponentName("com.android.phone",
                                                 "com.android.phone.MSimMobileNetworkSettings"),
                                                 PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
                }
	            startCmccService(context);
				return;
	        } 

            mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			if (mWifiManager == null) {
				return;
			}

            if (!mWifiManager.isWifiEnabled()) {
                return;
            }
		    mConnectListener = new WifiManager.ActionListener() {
                       public void onSuccess() {
                       }
                       public void onFailure(int reason) {
                            Log.e(TAG,"mConnectListener onFailure"); 
                       }
                   };

			if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                    WifiManager.EXTRA_NETWORK_INFO);
                if (info != null && currentConnected != info.isConnected()) {
                    lastConnected = currentConnected;
                    currentConnected = info.isConnected();
                }
                if (lastConnected && !currentConnected) {             
                    mWifiManager.startScanActive();
                }
                if (!currentConnected || lastConnected) {
                    isInSSIDs = true;
                }
                //Log.d(TAG, "---------- isInSSIDs ----------:" + isInSSIDs);
                Log.d(TAG,"NETWORK_STATE_CHANGED_ACTION, lastConnected=" + lastConnected+", currentConnected=" + currentConnected); 
                return;
            } else if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)){
                Log.d(TAG, "isInSSIDs:" + isInSSIDs);
                if (isInSSIDs) {
                    isInSSIDs = false;
                    return;
                }
                if(!lastConnected || currentConnected || lastSsid == null) return;
                int currentRssi = getNetworkRssi(lastSsid);
                lastConnected = currentConnected;
                currentConnected = false;
                if(currentRssi != Integer.MAX_VALUE && WifiManager.compareSignalLevel(currentRssi, -79) > 0){
                    Log.d(TAG,"SCAN_RESULTS_AVAILABLE_ACTION, rssi is better than -79, return");
                    return;
                } 
                Log.d(TAG,"SCAN_RESULTS_AVAILABLE_ACTION, lastConnected=" + lastConnected + ", currentConnected=" + currentConnected);   
            }


			WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                Log.d(TAG,"wifiInfo == null, then return .");
                return;
            }
            mRssi = wifiInfo.getRssi();
            mConfigs = mWifiManager.getConfiguredNetworks();
            Log.d(TAG,"wifiInfo.getSsid() = " + wifiInfo.getSSID() + " , mRssi=" + mRssi + " ,NetworkId =" + wifiInfo.getNetworkId());
            if (wifiInfo.getSSID()!=null) {
                lastSsid = wifiInfo.getSSID();
            }
            int mConfiguredApCount = mConfigs == null ? 0 : mConfigs.size();
            if (mConfiguredApCount < 2 ) {
                Log.d(TAG," mConfiguredApCount < 2");
                return;
            }
			
            int bestConfigSignal = getBestSignalNetworkId();  
            if (WifiManager.compareSignalLevel(mRssi, -85) > 0 || bestConfigSignal == -1) {
                Log.d(TAG, "RSSI > -85, finish");
                return;
            }else{
                //not show while the best signal is itself
                if(wifiInfo.getNetworkId() == bestConfigSignal) {
                    Log.d(TAG, " not show dialog while the best signal is itself");
                    return;   
                }
                
                Log.d(TAG, "RSSI < -85");
            }
			
            int value = Settings.System.getInt(context.getContentResolver(),Settings.System.WIFI_SELECT_IN_SSIDS_TYPE, 
                        Settings.System.WIFI_SELECT_IN_SSIDS_ASK);
            Log.d(TAG, "select in ssids type is: " + value);
            switch(value){
                case KEY_SELECT_IN_SSIDS_AUTO:
                    //connect to which AP
                    if(bestConfigSignal != -1){ 
						mWifiManager.connect(bestConfigSignal, mConnectListener);
                        //mWifiManager.connectNetwork(bestConfigSignal);
                        Log.d(TAG, "select in ssids type : auto connect");
                    }
                    return;
                case KEY_SELECT_IN_SSIDS_MANUL:
                    //do nothing
                    Log.d(TAG, "select in ssids type : manul connect");
                    return;
                case KEY_SELECT_IN_SSIDS_ASK:
                    Log.d(TAG, "select in ssids type : always ask,create dialog");
                    Intent alterIntent = new Intent();
                    alterIntent.setAction("android.net.wifi.cmcc.WIFI_SELECTION_SSIDS");
                    alterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(alterIntent);
                    break;
            }
		}
    }

    private void startCmccService(Context context) {
        Intent service = new Intent(CMCC_SERVICE_ACTION);
        context.startService(service);
    }

    private int getNetworkRssi(String SSID){
        int rssi = Integer.MAX_VALUE;
        List<ScanResult> results = mWifiManager.getScanResults();
        if (SSID != null && results != null) {
            for (ScanResult result : results) {
                if (SSID.equals("\"" + result.SSID + "\"")) {
				    rssi = result.level;
                    Log.d(TAG, "getNetworkRssi, SSID:" + result.SSID + ", rssi:" + rssi);
                    break;
                }
            }
        }
        return rssi;
    }	
	private int getBestSignalNetworkId(){
		int networkId = -1;
		int rssi = -200;
		mConfigs = mWifiManager.getConfiguredNetworks();
		List<ScanResult> results = mWifiManager.getScanResults();
		if (mConfigs != null && results != null) {
			for (WifiConfiguration config : mConfigs) {
				for (ScanResult result : results) {
					if (config!=null && config.SSID!=null && removeDoubleQuotes(config.SSID).equals(result.SSID) 
							&& getSecurity(config) == getSecurity(result)) {
						if (WifiManager.compareSignalLevel(result.level, rssi) > 0) {
							networkId=config.networkId;
							rssi=result.level;
							Log.d(TAG, "getBestSignalId,config.SSID:" + config.SSID + ", networkId:" + networkId);
						}
					}
				}
			}
		}
		if (WifiManager.compareSignalLevel(rssi, -79) > 0) {
			Log.d(TAG, "there is ap's signal is better than -79.networkId=" + networkId);
			return networkId;
		} else {
			Log.d(TAG, "there is no ap's signal is better than -79.");
			return -1;
		}
	}

	static String removeDoubleQuotes(String string) {
		int length = string.length();
		if ((length > 1) && (string.charAt(0) == '"')
				&& (string.charAt(length - 1) == '"')) {
			return string.substring(1, length - 1);
		}
		return string;
	}

    static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
            return SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
            return SECURITY_EAP;
        }
// WAPI+++
        if (config.allowedKeyManagement.get(KeyMgmt.WAPI_PSK)) {
            return SECURITY_WAPI_PSK;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.WAPI_CERT)) {
            return SECURITY_WAPI_CERT;
        }
// WAPI--
        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

    private static int getSecurity(ScanResult result) {
	if (result.capabilities.contains("WAPI-PSK")) {
           return SECURITY_WAPI_PSK;
        } else if (result.capabilities.contains("WEP")) { 
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (result.capabilities.contains("EAP")) {
            return SECURITY_EAP;
// WAPI++ // WAPI-PSK .. here PSK contain matches with PSK security causing prob
        } else if (result.capabilities.contains("WAPI-CERT")) {
            return SECURITY_WAPI_CERT;
         }
// WAPI-
             Log.w(TAG, "private getSecurity: " + result.capabilities);
        return SECURITY_NONE;
    }
}
//QUALCOMM_CMCC_END	
