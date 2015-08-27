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
import com.android.ims.ImsConfigListener;
import com.android.settings.ImsDisconnectedReceiver;
import com.android.ims.ImsReasonInfo;
import com.android.ims.ImsManager;
import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.settings.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.SearchView.OnSuggestionListener;
import android.widget.Switch;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;


public class WifiCallSwitchPreference extends SwitchPreference {

    private static final String TAG = "WifiCallSwitchPreference";

    // Below is the registration state
    private final int WIFI_CALLING_STATE_REGISTERED = 1;
    private final int WIFI_CALLING_STATE_NOT_REGISTERED = 2;
    private final int WIFI_CALLING_STATE_REGISTERING = 3;

    private boolean mCheckedStatus;
    private ImsConfig mImsConfig;
    private int mState;
    private int mPreference = 1;
    private Activity mParent = null;
    private static BroadcastReceiver mReceiver = null;
    private int oldErrorCode = -1;
    private int mRegState = WIFI_CALLING_STATE_NOT_REGISTERED; //NOT_REGISTERED
    private String oldErrorMessage = "";

    public WifiCallSwitchPreference(Context context) {
        super(context);
    }

    public void getWifiCallingPreference(){
        try {
            if (mImsConfig != null) {
                mImsConfig.getWifiCallingPreference(imsConfigListener);
            } else {
                loadWifiCallingPreference(ImsConfig.WifiCallingValueConstants.OFF,
                        ImsConfig.WifiCallingPreference.WIFI_PREF_NONE);
                Log.e(TAG, "getWifiCallingPreference failed. mImsConfig is null");
            }
        } catch (ImsException e) {
            Log.e(TAG, "getWifiCallingPreference failed. Exception = " + e);
        }
    }

    private boolean getWifiCallingSettingFromStatus(int status) {
        switch (status) {
            case ImsConfig.WifiCallingValueConstants.ON:
                return true;
            case ImsConfig.WifiCallingValueConstants.OFF:
            case ImsConfig.WifiCallingValueConstants.NOT_SUPPORTED:
            default:
                return false;
        }
    }

    private void loadWifiCallingPreference(int status, int preference){
        mState = status;
        mPreference = preference;
        if (status == ImsConfig.WifiCallingValueConstants.NOT_SUPPORTED) {
            this.setChecked(false);
            this.setEnabled(false);
        } else {
            boolean turnOn = getWifiCallingSettingFromStatus(status);

            this.setChecked(turnOn);
            this.setEnabled(true);
            setSummary(getStringIDForSummary());
            Intent intent = new Intent(turnOn ? WifiCallingStatusContral.ACTION_WIFI_CALL_TURN_ON
                    : WifiCallingStatusContral.ACTION_WIFI_CALL_TURN_OFF);
            intent.putExtra("preference", mPreference);
            getContext().sendBroadcast(intent);
        }
    }

    private int getStringIDForSummary() {
        if (!isChecked()) {
            return R.string.wifi_call_status_disabled;
        }

        if (mPreference == ImsConfig.WifiCallingPreference.CELLULAR_PREFERRED) {
            return R.string.wifi_call_status_cellular_preferred;
        }
        WifiManager wifiManager = (WifiManager) getContext().getSystemService(
                Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            return R.string.wifi_call_status_wifi_off;
        }
        ConnectivityManager connManager = (ConnectivityManager) getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiManager.isWifiEnabled() && !wifi.isConnected()) {
            return R.string.wifi_call_status_not_connected_wifi;
        }

        if (mRegState == WIFI_CALLING_STATE_REGISTERED) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo.getRssi() < -85) {
                return R.string.wifi_call_status_poor_wifi_signal;
            }
            return R.string.wifi_call_status_ready;
        }

        // for unknown state or unknow errors
        return R.string.wifi_call_status_error_unknown;
    }

    public void setParentActivity(Activity act) {
        mParent = act;
    }

    public WifiCallSwitchPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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

    public void registerReciever() {
       unRegisterReciever();
       if (getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_regional_wifi_calling_registration_errorcode)) {
            IntentFilter filter = new IntentFilter(
                    ImsDisconnectedReceiver.IMSDICONNECTED_ACTION);
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Parcelable bundle = intent.getParcelableExtra("result");
                    if (bundle != null && bundle instanceof ImsReasonInfo) {
                        ImsReasonInfo imsReasonInfo = (ImsReasonInfo)bundle;
                        int errorCode = imsReasonInfo.getExtraCode();
                        String extraMsg = imsReasonInfo.getExtraMessage();
                        mRegState = intent.getIntExtra("stateChanged", WIFI_CALLING_STATE_NOT_REGISTERED);
                        Log.i(TAG, "mRegState =" + mRegState);
                        Log.i(TAG, "get ImsDisconnected extracode : " + errorCode);
                        Log.i(TAG, "get ImsDisconnected getExtraMessage :" + extraMsg);
                        if (mRegState == WIFI_CALLING_STATE_REGISTERING) {
                            Log.i(TAG, "Do nothing for Registering state. Directly return.");
                            return;
                        } else if (mRegState == WIFI_CALLING_STATE_REGISTERED) {
                            Log.i(TAG, "Re-initialized for Registered state.");
                            oldErrorCode = 0;
                            oldErrorMessage = "";
                            setSummary(getStringIDForSummary());
                        } else if (mRegState == WIFI_CALLING_STATE_NOT_REGISTERED) {
                            if (extraMsg != null && !TextUtils.isEmpty(extraMsg)) {
                                Log.i(TAG, "valid error message received");
                            } else {
                                Log.i(TAG, "get null error message from low layer. Still use original one");
                                extraMsg = oldErrorMessage;
                                if (errorCode != oldErrorCode) {
                                    extraMsg = "";
                                }
                            }

                            if ((extraMsg == null) || (TextUtils.isEmpty(extraMsg))) {
                                Log.i(TAG, "display status message");
                                setSummary(getStringIDForSummary());
                            } else {
                                Log.i(TAG, "display extra error message if any");
                                setSummaryOn(extraMsg);
                            }

                            oldErrorCode = errorCode;
                            oldErrorMessage = extraMsg;
                        } else {
                            // also display error message for unexpected state
                            setSummary(getStringIDForSummary());
                        }
                    }
                }
            };
            getContext().registerReceiver(mReceiver, filter);
        }
    }
    public void unRegisterReciever() {
        if (mReceiver != null) {
            getContext().unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    public WifiCallSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WifiCallSwitchPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.switchPreferenceStyle);
    }

    @Override
    protected void onClick() {
        super.onClick();
        Log.i(TAG, "onClik mCheckedStatus : " + isChecked());
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
                            buttonView.setChecked(!isChecked);
                            return;
                        }
                        WifiCallSwitchPreference.this.setChecked(isChecked);
                        onSwitchClicked();
                    }
                });
            }
        }
    }

    private boolean setWifiCallingPreference(int wifiCallingStatus, int wifiCallingPreference) {
        try {
            if (mImsConfig != null) {
                mImsConfig.setWifiCallingPreference(wifiCallingStatus,
                        wifiCallingPreference, imsConfigListener);
            } else {
                Log.e(TAG, "setWifiCallingPreference failed. mImsConfig is null");
                return false;
            }
        } catch (ImsException e) {
            Log.e(TAG, "setWifiCallingPreference failed. Exception = " + e);
            return false;
        }
        this.setEnabled(false);
        setSummary(R.string.wifi_call_status_enabling);
        mState = wifiCallingStatus;
        mPreference = wifiCallingPreference;
        return true;
    }

    public void onSwitchClicked(){
        Log.d(TAG, "onSwitchClicked "+isChecked());
        final int status =  isChecked() ?
                ImsConfig.WifiCallingValueConstants.ON :
                ImsConfig.WifiCallingValueConstants.OFF;
        Log.d(TAG, "onPreferenceChange user selected status : wifiStatus " + status +
                " wifiPreference: " + mPreference);
        boolean result = setWifiCallingPreference(status, mPreference);
        if (result) {
            loadWifiCallingPreference(status, mPreference);
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

        public void onGetWifiCallingPreference(int status, final int wifiCallingStatus,
                final int wifiCallingPreference) {
            if (hasRequestFailed(status)) {
                mState = ImsConfig.WifiCallingValueConstants.OFF;
                mPreference = ImsConfig.WifiCallingPreference.WIFI_PREF_NONE;
                Log.e(TAG, "onGetWifiCallingPreference: failed. errorCode = " + status);
            }
            Log.d(TAG, "onGetWifiCallingPreference: status = " + wifiCallingStatus +
                    " preference = " + wifiCallingPreference);
            mParent.runOnUiThread(new Runnable() {
                public void run() {
                    //here WifiCallSwitchPreference will be recreated, so it needs
                    //unregisterReceiver and after UI refresh finish to registerReciever
                    unRegisterReciever();
                    loadWifiCallingPreference(wifiCallingStatus, wifiCallingPreference);
                    registerReciever();
                }
            });
        }

        public void onSetWifiCallingPreference(int status) {
            if (hasRequestFailed(status)) {
                Log.e(TAG, "onSetWifiCallingPreference : set failed. errorCode = " + status);
                getWifiCallingPreference();
            } else {
                Log.d(TAG, "onSetWifiCallingPreference: set succeeded.");
            }
        }
    };

    private boolean hasRequestFailed(int result) {
        return (result != ImsConfig.OperationStatusConstants.SUCCESS);
    }

}
