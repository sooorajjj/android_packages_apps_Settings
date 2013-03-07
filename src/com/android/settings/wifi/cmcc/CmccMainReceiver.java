//QUALCOMM_CMCC_START	
package com.android.settings.wifi.cmcc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;
import com.qrd.plugin.feature_query.FeatureQuery;

public class CmccMainReceiver extends BroadcastReceiver {

    private static final boolean DBG = true;
    private static final String TAG = "CmccMainReceiver";
    private static final String CMCC_SERVICE_ACTION = "com.android.action.wifi.cmccservice";
	
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (DBG) Log.d(TAG, "receive action: " + action);
		if(FeatureQuery.FEATURE_WLAN_CMCC_SUPPORT){  
	        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {

	            DetailedState state = ((NetworkInfo) intent
	                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).getDetailedState();

	            if (state == DetailedState.CONNECTED) {
	                if (DBG) Log.i(TAG, "WifiManager.NETWORK_STATE_CHANGED_ACTION");
	                startCmccService(context);
	            }
	        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
	            startCmccService(context);
				return;
	        } 
		}
    }

    private void startCmccService(Context context) {
        Intent service = new Intent(CMCC_SERVICE_ACTION);
        context.startService(service);
    }
}
//QUALCOMM_CMCC_END