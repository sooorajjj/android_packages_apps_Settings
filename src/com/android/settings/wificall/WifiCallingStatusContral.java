/*
* Copyright (c) 2015, The Linux Foundation. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are
* met:
*     * Redistributions of source code must retain the above copyright
*      notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above
*       copyright notice, this list of conditions and the following
*       disclaimer in the documentation and/or other materials provided
*      with the distribution.
*     * Neither the name of The Linux Foundation nor the names of its
*      contributors may be used to endorse or promote products derived
*       from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
* ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
* BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
* BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
* WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
* OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
* IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.android.settings.wificall;

import android.content.BroadcastReceiver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Parcelable;
import android.telephony.CellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.ims.ImsConfig;
import com.android.ims.ImsReasonInfo;
import java.util.List;

public class WifiCallingStatusContral extends BroadcastReceiver {

    private static final String TAG = WifiCallingStatusContral.class.getSimpleName();
    private static final boolean DEBUG = true;

    public static final String ACTION_WIFI_CALL_TURN_ON = "com.android.wificall.TURNON";
    public static final String ACTION_WIFI_CALL_TURN_OFF = "com.android.wificall.TURNOFF";
    public static final String ACTION_WIFI_CALL_ERROR_CODE = "com.android.wificall.ERRORCODE";
    public static final String ACTION_IMS_STATE_CHANGE = "com.android.imscontection.DISCONNECTED";
    public static final int WIFI_CALLING_ROVE_IN_THRESHOD = -75;
    public static final String ACTION_WIFI_CALL_READY_STATUS_CHANGE = "com.android.wificall.READY";
    public static final String ACTION_WIFI_CALL_READY_EXTRA = "com.android.wificall.ready.extra";

    private static Context mContext;
    private static int mWifiCallPreferred = -1;
    private static int mErrorCode = -1;
    private static PhoneStateListener mPhoneStateListener = new PhoneStateListener(){
        public void onCallStateChanged(int state, String incomingNumber) {
            WifiCallingNotification.updateWFCCallStateChange(mContext, state);
        };
    };

    private final int WIFI_CALLING_STATE_REGISTERED = 1;
    private final int WIFI_CALLING_STATE_NOT_REGISTERED = 2;

    private static boolean mWifiTurnOn = false;
    private static boolean mWifiConnected = false;
    private static boolean mWifiCallTurnOn = false;
    private static boolean mImsRegisted = false;
    private static boolean mIsWifiSignalWeak = false;
    private static boolean mWifiCallReady = false;
    private static NetworkInfo mWifiNetwork = null;

    private void savePreference(int iPreference, boolean status) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(
                   "MY_PERFS", mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("currentWifiCallingPrefernce", iPreference);
        editor.putBoolean("currentWifiCallingStatus", status );
        editor.commit();
    }

    private void readPreference() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(
                "MY_PERFS", mContext.MODE_PRIVATE);
        mWifiCallPreferred = sharedPreferences.getInt("currentWifiCallingPrefernce",
                ImsConfig.WifiCallingPreference.WIFI_PREFERRED);
        mWifiCallTurnOn = sharedPreferences.getBoolean("currentWifiCallingStatus", true);
        if (DEBUG) Log.d(TAG, "readPreference, mWifiCallPreferred = " + mWifiCallPreferred);
        if (DEBUG) Log.d(TAG, "readPreference, mWifiCallTurnOn = " + mWifiCallTurnOn);
    }

    private boolean cellularNetworkIsAbailable() {
        boolean cellularNetworkAvailable = false;

        TelephonyManager tm = (TelephonyManager)mContext.
                 getSystemService(Context.TELEPHONY_SERVICE);
        List<CellInfo> cellInfoList = tm.getAllCellInfo();

        if (cellInfoList != null) {
            for (CellInfo cellinfo : cellInfoList) {
                if (cellinfo.isRegistered()) {
                    cellularNetworkAvailable = true;
                }
            }
        }

        return cellularNetworkAvailable;
    }

    private void isWifiCallTurnOn(Intent intent, String action) {
        if (ACTION_WIFI_CALL_TURN_OFF.equals(action)
                || ACTION_WIFI_CALL_TURN_ON.equals(action)) {
            if (DEBUG) Log.d(TAG, "isWifiCallTurnOn");
            mWifiCallTurnOn = ACTION_WIFI_CALL_TURN_ON.equals(action);
            mWifiCallPreferred = intent.getIntExtra("preference",
                    ImsConfig.WifiCallingPreference.WIFI_PREFERRED);
            savePreference(mWifiCallPreferred, mWifiCallTurnOn);
        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            if (DEBUG) Log.d(TAG, "isWifiCallTurnOn - CONNECTIVITY_ACTION");
            SharedPreferences sharedPreferences = mContext.getSharedPreferences(
                    "MY_PERFS", mContext.MODE_PRIVATE);
            mWifiCallTurnOn = sharedPreferences.getBoolean("currentWifiCallingStatus", true);
            mWifiCallPreferred = sharedPreferences.getInt("currentWifiCallingPrefernce",
                    ImsConfig.WifiCallingPreference.WIFI_PREFERRED);
        }

        if (mWifiCallPreferred == ImsConfig.WifiCallingPreference.CELLULAR_PREFERRED) {
            if (cellularNetworkIsAbailable()) {
                mWifiCallTurnOn = false;
            } else {
                mWifiCallTurnOn = true;
            }
        }
        if (DEBUG) Log.d(TAG, "mWifiCallPreferred = " + mWifiCallPreferred);
        if (DEBUG) Log.d(TAG, "mWifiCallTurnOn = " + mWifiCallTurnOn);
    }

    private void isWifiStatusChange(Intent intent, String action) {
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            if (DEBUG) Log.d(TAG, "isWifiTurnOn");
            SharedPreferences sharedPreferences = mContext.getSharedPreferences(
                    WifiCallingWizardActivity.PRIVTE_PREFERENCE, Context.MODE_PRIVATE);
            boolean showWifiCallWizard = sharedPreferences.getBoolean(
                    WifiCallingWizardActivity.WIZARD_SHOW_PREFERENCE, true);
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
            if (DEBUG) Log.d(TAG, "showWifiCallingWizardActivity = " + showWifiCallWizard);

            if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                mWifiConnected = true;
                if (showWifiCallWizard) {
                    Intent start = new Intent(mContext, WifiCallingWizardActivity.class);
                    start.setAction("android.intent.action.MAIN");
                    start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(start);
                }
            } else {
                mWifiConnected = false;
            }

            WifiManager wifimgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            if (wifimgr != null && wifimgr.isWifiEnabled()) {
                mWifiTurnOn = true;
            } else {
                mWifiTurnOn = false;
            }

        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            if (DEBUG) Log.d (TAG, "Wifi RSSI change");

                WifiManager wifiManager = (WifiManager) mContext.getSystemService(
                        Context.WIFI_SERVICE);
                if ((wifiManager == null) || (wifiManager.getConnectionInfo() == null)) {
                    mIsWifiSignalWeak = false;
                } else {
                    if (DEBUG) Log.d (TAG, "Wifi RSSI = "+wifiManager.getConnectionInfo().getRssi() +"dbm");
                    mIsWifiSignalWeak = (wifiManager.getConnectionInfo().getRssi() < WIFI_CALLING_ROVE_IN_THRESHOD);
                }
        }

        //check wifi contivity state
        ConnectivityManager connect = (ConnectivityManager) mContext
               .getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiNetwork = connect.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWifiNetwork == null) {
            mWifiTurnOn = false;
        } else {
            if (mWifiNetwork.isConnected()) {
                mWifiTurnOn = true;
            } else {
                mWifiTurnOn = false;
            }
        }
        if (DEBUG) Log.d(TAG, "mWifiTurnOn = " + mWifiTurnOn);
    }

    private void isImsRegisted(Intent intent, String action) {
        if (ACTION_IMS_STATE_CHANGE.equals(action) ) {
            if (DEBUG) Log.d(TAG, "isImsRegisted");
            int regState = intent.getIntExtra("stateChanged", WIFI_CALLING_STATE_NOT_REGISTERED);
            if (regState == WIFI_CALLING_STATE_REGISTERED) {
                mImsRegisted = true;
            } else {
                mImsRegisted = false;
            }

            if (DEBUG) Log.d(TAG, "regState =" + regState + ", mImsRegisted = " + mImsRegisted);
        }
    }

    private void registerWFCInCallListener() {
        TelephonyManager tm = (TelephonyManager)mContext.
                getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void unregisterWFCInCallListener() {
        TelephonyManager tm = (TelephonyManager)mContext.
                getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    private void updateWFCReadyIcon() {
        WifiCallingNotification.getIntance().updateWFCStatusChange(mContext, mWifiCallReady);
    }

    private void updateWFCInCallIcon() {
        if (mWifiCallReady) {
            registerWFCInCallListener();
        } else {
            unregisterWFCInCallListener();
        }
    }

    private void broadcastWifiCallReadyStatus() {
        Intent intent = new Intent(ACTION_WIFI_CALL_READY_STATUS_CHANGE);
        intent.putExtra(ACTION_WIFI_CALL_READY_EXTRA, mWifiCallReady);
        mContext.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!WifiCallingNotification.getWifiCallingNotifiEnable(context)) {
            if (DEBUG) Log.d(TAG, "getIntent : " + intent.getAction() + " flag : false");
            return;
        }

        mContext = context;
        if (mWifiCallPreferred == -1) {
            readPreference();
        }

        String action = intent.getAction();
        if (DEBUG) Log.d(TAG, "WifiCallingStatusContral, onReceive, action = " + action);

        isWifiCallTurnOn(intent, action);
        isWifiStatusChange(intent, action);
        isImsRegisted(intent, action);

        if (DEBUG) Log.d(TAG, "mWifiCallTurnOn = " + mWifiCallTurnOn
                + ", mWifiConnected = " + mWifiConnected + ", mImsRegisted = " + mImsRegisted);

        if (mWifiCallReady != (mWifiCallTurnOn &&
                    mWifiConnected && mImsRegisted && !mIsWifiSignalWeak)) {
            mWifiCallReady = mWifiCallTurnOn && mWifiConnected && mImsRegisted && !mIsWifiSignalWeak;
            updateWFCReadyIcon();
            updateWFCInCallIcon();
            broadcastWifiCallReadyStatus();
        }
    }
}
