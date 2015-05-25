package com.android.settings.wificall;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.settings.ImsDisconnectedReceiver;

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
import android.widget.SearchView.OnSuggestionListener;
import android.widget.Switch;
import com.android.ims.ImsReasonInfo;

public class WifiCallSwitchPreference extends SwitchPreference {

    private static final String TAG = "WifiCallSwitchPreference";

    private boolean mCheckedStatus;

    public WifiCallSwitchPreference(Context context) {
        super(context);
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
                        }
                    }
                }
            };
            context.registerReceiver(receiver, filter);
        }
    }

    public WifiCallSwitchPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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

    private void onSwitchClicked(){
        // FIX ME : when the wifi-calling switch is turn/on, do something
        Log.d(TAG, "onSwitchClicked "+isChecked());
    }

}
