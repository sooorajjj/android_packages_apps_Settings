//QUALCOMM_CMCC_START
package com.android.settings.wifi.cmcc;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import android.provider.Settings;

/**
 * This class is the main controller for CMCC. Now it's responsible for the following things:

 * 1) Show the PresetNetworkInfo Activity to represent the preset networks.
 */
public class CmccService extends Service {
    private static final String TAG = "CmccService";

    private static final String KEY_ALREADY_IMPORTED = "already_imported";
    private static final String KEY_ALREADY_SAVE_PRESET_CMCC_NETWORK = "already_save_preset_cmcc_network";
    private static final String NOT_YET = "not_yet";
    private static final String ALREADY = "already";
	
    private WifiManager mWifiManager;
    private Handler mHandler = new CmccEventHandler();
	
    private static final int EVENT_CHECK_AND_SAVE_PRESET_NETWORK = 109;

    private WifiManager.ActionListener mConnectListener;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        registerReceiver(mReceiver, filter);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //mWifiManager.asyncConnect(this, new Handler());

        mConnectListener = new WifiManager.ActionListener() {
                                   public void onSuccess() {
                                   }
                                   public void onFailure(int reason) {
                                       Log.i(TAG, "Faild to connect to another AP, reason: " + reason);
                                   }
                               };
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "receive action: " + action);
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN);
                if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                    mHandler.sendEmptyMessage(EVENT_CHECK_AND_SAVE_PRESET_NETWORK);
                }
            }
        }
    };

    private class CmccEventHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
               
                case EVENT_CHECK_AND_SAVE_PRESET_NETWORK:
                    try {
                        checkAndSavePresetNetwork(getApplicationContext());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
         * Check if Wi-Fi needs to be enabled and start
         * if needed
         *
         * This function is used only at first time when Wi-Fi is enabled
         */
    public void checkAndSavePresetNetwork(Context context) {
	    String flag = Settings.System.getString(context.getContentResolver(), KEY_ALREADY_SAVE_PRESET_CMCC_NETWORK);
        if (isNullString(flag)) {
            flag = NOT_YET;
        }

        Log.d(TAG, "checkAndSavePresetNetwork, "
                + KEY_ALREADY_SAVE_PRESET_CMCC_NETWORK + ": " + flag);
        if (ALREADY.equals(flag)) {
            return;
        }

        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        WifiManager.ActionListener mSaveListener = new WifiManager.ActionListener() {
            public void onSuccess() {
            }

            public void onFailure(int reason) {
                Log.d(TAG, "failed to save network, reason:" + reason);
            }
        };

        String ssids[] = new String[] { WifiManager.PRESER_NETWORK_CMCC,
                WifiManager.PRESER_NETWORK_CMCC_EDU };

        for (int i = 0; i < ssids.length; i++) {

            WifiConfiguration config = new WifiConfiguration();
            config.priority = i + 1;
            config.SSID = convertToQuotedString(ssids[i]);
            config.allowedKeyManagement.set(KeyMgmt.NONE);

            Log.d(TAG, "save network: " + config.SSID);
            wifiManager.save(config, mSaveListener);
            wifiManager.startScan();
        }
		Settings.System.putString(context.getContentResolver(), KEY_ALREADY_SAVE_PRESET_CMCC_NETWORK, ALREADY);
    }

    private String convertToQuotedString(String string) {
	    return "\"" + string + "\"";
    }

    private boolean isNullString(String value) {
	    if (value == null || value.length() == 0) {
		    return true;
	    }
    	return false;
    }

}
//QUALCOMM_CMCC_END
