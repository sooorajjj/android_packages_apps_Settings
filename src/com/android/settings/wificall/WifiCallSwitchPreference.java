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
import com.android.ims.ImsConfig;
import com.android.ims.ImsManager;
import com.android.ims.ImsException;
import com.android.ims.ImsConfigListener;

public class WifiCallSwitchPreference extends SwitchPreference {

    private static final String TAG = "WifiCallSwitchPreference";
    private int mState = ImsConfig.WifiCallingValueConstants.ON;
    private int mPreference = ImsConfig.WifiCallingPreference.WIFI_PREFERRED;
    private Activity mParent = null;
    private ImsConfig mImsConfig;

    public WifiCallSwitchPreference(Context context) {
        super(context);
        initImsConfig();
        Log.d(TAG, "WifiCallSwitchPreference constructor 1");
    }

    public WifiCallSwitchPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initImsConfig();
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
        getWifiCallingPreference();
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
            this.setEnabled(false);
            Log.e(TAG, "checkWifiCallingCapability: wificalling not supported.");
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
            }

        }

        private boolean hasRequestFailed(int result) {
            return (result != ImsConfig.OperationStatusConstants.SUCCESS);
        }
    };

}
