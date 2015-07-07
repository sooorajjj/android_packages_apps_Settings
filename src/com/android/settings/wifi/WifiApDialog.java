/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.R;

/**
 * Dialog to configure the SSID and security settings
 * for Access Point operation
 */
public class WifiApDialog extends AlertDialog implements View.OnClickListener,
        TextWatcher, AdapterView.OnItemSelectedListener {

    static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;

    private final DialogInterface.OnClickListener mListener;

    public static final int OPEN_INDEX = 0;
    public static final int WPA2_INDEX = 1;

    private View mView;
    private View mshowAgainView;
    private TextView mSsid;
    private int mSecurityTypeIndex = OPEN_INDEX;
    private EditText mPassword;
    private CheckBox mCheckBox;

    private CheckBox mShowAgain;
    private CheckBox mBroadcastSsid;
    WifiConfiguration mWifiConfig;

    public WifiApDialog(Context context, DialogInterface.OnClickListener listener,
            WifiConfiguration wifiConfig) {
        super(context);
        mListener = listener;
        mWifiConfig = wifiConfig;
        if (wifiConfig != null) {
            mSecurityTypeIndex = getSecurityTypeIndex(wifiConfig);
        }
    }

    public static int getSecurityTypeIndex(WifiConfiguration wifiConfig) {
        if (wifiConfig.allowedKeyManagement.get(KeyMgmt.WPA2_PSK)) {
            return WPA2_INDEX;
        }
        return OPEN_INDEX;
    }

    public WifiConfiguration getConfig() {

        WifiConfiguration config = new WifiConfiguration();

        /**
         * TODO: SSID in WifiConfiguration for soft ap
         * is being stored as a raw string without quotes.
         * This is not the case on the client side. We need to
         * make things consistent and clean it up
         */
        config.SSID = mSsid.getText().toString();

        Context context = getContext();
        if (context.getResources().getBoolean(
                com.android.internal.R.bool.config_regional_hotspot_show_broadcast_ssid_checkbox)) {
            config.hiddenSSID = !(mBroadcastSsid.isChecked());
        }
        switch (mSecurityTypeIndex) {
            case OPEN_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                return config;

            case WPA2_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.WPA2_PSK);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                if (mPassword.length() != 0) {
                    String password = mPassword.getText().toString();
                    config.preSharedKey = password;
                }
                return config;
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mView = getLayoutInflater().inflate(R.layout.wifi_ap_dialog, null);
        Spinner mSecurity = ((Spinner) mView.findViewById(R.id.security));

        setView(mView);
        setInverseBackgroundForced(true);

        Context context = getContext();

        setTitle(R.string.wifi_tether_configure_ap_text);
        mView.findViewById(R.id.type).setVisibility(View.VISIBLE);
        mSsid = (TextView) mView.findViewById(R.id.ssid);
        mPassword = (EditText) mView.findViewById(R.id.password);
        mCheckBox = (CheckBox) mView.findViewById(R.id.show_password);

        setButton(BUTTON_SUBMIT, context.getString(R.string.wifi_save), mListener);
        setButton(DialogInterface.BUTTON_NEGATIVE,
        context.getString(R.string.wifi_cancel), mListener);

        if (mWifiConfig != null) {
            mSsid.setText(mWifiConfig.SSID);
            if (context.getResources().getBoolean(
                    com.android.internal.R.bool
                    .config_regional_hotspot_show_broadcast_ssid_checkbox)) {
                mBroadcastSsid = (CheckBox) mView.findViewById(R.id.broadcast_ssid);
                mBroadcastSsid.setChecked(!mWifiConfig.hiddenSSID);
            }
            mSecurity.setSelection(mSecurityTypeIndex);
            if (mSecurityTypeIndex == WPA2_INDEX) {
                  mPassword.setText(mWifiConfig.preSharedKey);
                if (context.getResources().getBoolean(
                        com.android.internal.R.bool.
                        config_regional_use_empty_password_default)) {
                    if (TextUtils.isEmpty(mWifiConfig.preSharedKey)) {
                        mCheckBox.setChecked(true);
                    }
                }
            }
        }

        EditText PassWordEdit = (EditText) mView.findViewById(R.id.password);
        PassWordEdit.requestFocus();
        mSsid.addTextChangedListener(this);
        mPassword.addTextChangedListener(this);
        mCheckBox.setOnClickListener(this);
        mSecurity.setOnItemSelectedListener(this);

        super.onCreate(savedInstanceState);

        showSecurityFields();
        validate();
        if (context.getResources().getBoolean(
                com.android.internal.R.bool.config_regional_hotspot_show_broadcast_ssid_checkbox)) {
            mBroadcastSsid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (!isChecked) {
                        final Context context = getContext();
                        mshowAgainView = getLayoutInflater().inflate(R.layout.not_show_again, null);
                        mShowAgain = (CheckBox)mshowAgainView.findViewById(R.id.check);
                        SharedPreferences sharedpreferences = context.getSharedPreferences(
                                "ShowAgain", Context.MODE_PRIVATE);
                        boolean showagain = sharedpreferences.getBoolean("SsidBroadcast", true);
                        if (showagain) {
                            BroadcastSsidDialog();
                        }
                    }
                }
            });
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        final Context context = getContext();
        if (context.getResources().getBoolean(
                com.android.internal.R.bool.config_regional_hotspot_show_help)) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("MY_PERFS",
                    context.MODE_PRIVATE);
            boolean WifiApBackKeyEnable = sharedPreferences.getBoolean(
                    "WifiApBackKeyEnable", true);
            if (keyCode == KeyEvent.KEYCODE_BACK && (!WifiApBackKeyEnable)) {
                return false;
            }
        }
        return super.onKeyDown(keyCode,event);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mPassword != null && mCheckBox != null) {
            mPassword.setInputType(
                InputType.TYPE_CLASS_TEXT | (mCheckBox.isChecked() ?
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                InputType.TYPE_TEXT_VARIATION_PASSWORD));
        }
    }

    private void validate() {
        if ((mSsid != null && mSsid.length() == 0) ||
                   ((mSecurityTypeIndex == WPA2_INDEX)&& mPassword.length() < 8) ||
                        (mSsid != null && mSsid.getText().toString().getBytes().length > 32)) {
            getButton(BUTTON_SUBMIT).setEnabled(false);
        } else {
            getButton(BUTTON_SUBMIT).setEnabled(true);
        }
    }

    public void onClick(View view) {
        mPassword.setInputType(
                InputType.TYPE_CLASS_TEXT | (((CheckBox) view).isChecked() ?
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void afterTextChanged(Editable editable) {
        validate();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mSecurityTypeIndex = position;
        final Context context = getContext();
        if (context.getResources().getBoolean(
                com.android.internal.R.bool.config_regional_hotspot_show_open_security_dialog)) {
            if (mSecurityTypeIndex == OPEN_INDEX) {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setMessage(R.string.open_security_settings_note);
                alert.setPositiveButton(R.string.okay, null);
                alert.show();
            }
        }
        showSecurityFields();
        validate();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void showSecurityFields() {
        final Context context = getContext();
        if ((!context.getResources().getBoolean(
                    com.android.internal.R.bool
                    .config_regional_hotspot_show_broadcast_ssid_checkbox))) {
            mView.findViewById(R.id.checkbox).setVisibility(View.GONE);
        } else {
            mView.findViewById(R.id.checkbox).setVisibility(View.VISIBLE);
        }
        if (mSecurityTypeIndex == OPEN_INDEX) {
            mView.findViewById(R.id.fields).setVisibility(View.GONE);
            return;
        }
        mView.findViewById(R.id.fields).setVisibility(View.VISIBLE);
    }

    private void BroadcastSsidDialog() {
        final Context context = getContext();
        AlertDialog.Builder alertdialog =  new AlertDialog.Builder(context);
        alertdialog.setTitle(R.string.ssid_broadcast_dialog_title);
        alertdialog.setMessage(R.string.ssid_broadcast_dialog_text);
        alertdialog.setView(mshowAgainView);
        alertdialog.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick( DialogInterface dialog, int which) {
                SharedPreferences sharedpreferences = context.getSharedPreferences(
                        "ShowAgain", Context.MODE_PRIVATE);
                Editor editor = sharedpreferences.edit();
                if (mShowAgain.isChecked()) {
                    editor.putBoolean("SsidBroadcast", false);
                } else {
                    editor.putBoolean("SsidBroadcast", true);
                }
                editor.commit();
            }
        });
        alertdialog.show();
    }
}
