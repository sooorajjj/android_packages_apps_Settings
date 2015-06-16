package com.android.settings.wificall;

import java.util.regex.Matcher;

import java.util.regex.Pattern;

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

public class WifiCallSwitchPreference extends SwitchPreference {

    private static final String TAG = "WifiCallSwitchPreference";

    private boolean mCheckedStatus;
    private ImsConfig mImsConfig;
    private int mState;
    private int mPreference = 1;

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
        if (status == ImsConfig.WifiCallingValueConstants.NOT_SUPPORTED) {
            this.setChecked(false);
            this.setEnabled(false);
        } else {
            boolean turnOn = getWifiCallingSettingFromStatus(status);
            this.setChecked(turnOn);
            this.setEnabled(true);
            setSummary(getSummary(turnOn));
            Intent intent = new Intent(turnOn ? WifiCallingStatusContral.ACTION_WIFI_CALL_TURN_ON
                    : WifiCallingStatusContral.ACTION_WIFI_CALL_TURN_OFF);
            intent.putExtra("preference", mPreference);
            getContext().sendBroadcast(intent);
        }
        mState = status;
        mPreference = preference;
    }

    private int getSummary(boolean turnOn) {
        if (!turnOn) {
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
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo.getRssi() < -85) {
            return R.string.wifi_call_status_poor_wifi_signal;
        }
        return R.string.wifi_call_status_ready;
    }

    public WifiCallSwitchPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (context.getResources().getBoolean(
                com.android.internal.R.bool.config_regional_wifi_calling_registration_errorcode)) {
            IntentFilter filter = new IntentFilter(
                    ImsDisconnectedReceiver.IMSDICONNECTED_ACTION);
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Parcelable bundle = intent.getParcelableExtra("result");
                    if (bundle != null && bundle instanceof ImsReasonInfo) {
                        int errorCode = ((ImsReasonInfo) bundle).getExtraCode();
                        Log.i(TAG, "get errorcode : " + errorCode);
                        if (WifiCallRegistrationErrorUtil
                                .isWifiCallingRegistrationError(errorCode)) {
                            setSummaryOn(WifiCallRegistrationErrorUtil
                                    .matchRegistrationError(errorCode, getContext()));
                            Intent broadcast = new Intent(WifiCallingStatusContral.
                                    ACTION_WIFI_CALL_ERROR_CODE);
                            broadcast.putExtra("result", errorCode);
                            WifiCallSwitchPreference.this.getContext().sendBroadcast(broadcast);
                        }
                    }
                }
            };
            context.registerReceiver(receiver, filter);
        }
        try {
            ImsManager imsManager = ImsManager.getInstance(getContext(),
                    SubscriptionManager.getDefaultVoiceSubId());
            mImsConfig = imsManager.getConfigInterface();
        } catch (ImsException e) {
            mImsConfig = null;
            Log.e(TAG, "ImsService is not running");
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

    private void onSwitchClicked(){
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

        public void onGetWifiCallingPreference(int status, int wifiCallingStatus,
                int wifiCallingPreference) {
            if (hasRequestFailed(status)) {
                mState = ImsConfig.WifiCallingValueConstants.OFF;
                mPreference = ImsConfig.WifiCallingPreference.WIFI_PREF_NONE;
                Log.e(TAG, "onGetWifiCallingPreference: failed. errorCode = " + status);
            }
            Log.d(TAG, "onGetWifiCallingPreference: status = " + wifiCallingStatus +
                    " preference = " + wifiCallingPreference);
            loadWifiCallingPreference(wifiCallingStatus, wifiCallingPreference);
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
