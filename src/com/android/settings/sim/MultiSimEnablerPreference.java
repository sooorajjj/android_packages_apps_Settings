/**
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
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
 *
 */


package com.android.settings.sim;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.internal.telephony.PhoneConstants;
import com.android.settings.SelectSubscription;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;
import com.android.settings.Utils;

import java.util.List;


/**
 * SimEnabler is a helper to manage the slot on/off checkbox preference. It is
 * turns on/off slot and ensures the summary of the preference reflects the current state.
 */
public class MultiSimEnablerPreference extends Preference implements OnCheckedChangeListener {
    private final Context mContext;

    private String TAG = "MultiSimEnablerPreference";
    private static final boolean DBG = true;

    private static final int EVT_UPDATE = 1;
    private static final int EVT_SHOW_RESULT_DLG = 2;
    private static final int EVT_SHOW_PROGRESS_DLG = 3;
    private static final int EVT_PROGRESS_DLG_TIME_OUT = 4;

    private static final int CONFIRM_ALERT_DLG_ID = 1;
    private static final int ERROR_ALERT_DLG_ID = 2;
    private static final int RESULT_ALERT_DLG_ID = 3;

    private static final String DISPLAY_NUMBERS_TYPE = "display_numbers_type";

    private int mSlotId;
    private SubscriptionInfo mSir;
    private boolean mCurrentState;
    private boolean mRequest;

    private int[] mTintArr;
    private String[] mColorStrings;
    private int mTintSelectorPos;

    private boolean mCmdInProgress = false;
    private int mSwitchVisibility = View.VISIBLE;
    private CompoundButton mSwitch;
    private Handler mParentHandler = null;
    private static AlertDialog sAlertDialog = null;
    private static ProgressDialog sProgressDialog = null;
    //Delay for progress dialog to dismiss
    private static final int PROGRESS_DLG_TIME_OUT = 30000;
    private static final int MSG_DELAY_TIME = 2000;

    private static Object mSyncLock = new Object();

    private IntentFilter mIntentFilter = new IntentFilter(
            TelephonyIntents.ACTION_SUBSCRIPTION_SET_UICC_RESULT);

    public MultiSimEnablerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        if (mContext.getResources().getBoolean(R.bool.config_custom_multi_sim_checkbox)) {
            setWidgetLayoutResource(R.layout.custom_checkbox_multisim);
        } else {
            setWidgetLayoutResource(R.layout.custom_checkbox);
        }
        setSwitchVisibility(View.VISIBLE);
    }

    public MultiSimEnablerPreference(Context context, SubscriptionInfo sir, Handler handler,
            int slotId) {
        this(context, null, com.android.internal.R.attr.checkBoxPreferenceStyle);
        logd("Contructor..Enter" + sir);
        mSlotId = slotId;
        mSir = sir;
        mParentHandler = handler;
        mTintArr = context.getResources().getIntArray(com.android.internal.R.array.sim_colors);
        mColorStrings = context.getResources().getStringArray(R.array.color_picker);
        mTintSelectorPos = 0;
    }

    private void sendMessage(int event, Handler handler, int delay) {
        Message message = handler.obtainMessage(event);
        handler.sendMessageDelayed(message, delay);
    }

    private void sendMessage(int event, Handler handler, int delay, int arg1, int arg2) {
        Message message = handler.obtainMessage(event, arg1, arg2);
        handler.sendMessageDelayed(message, delay);
    }

    private boolean hasCard() {
        return TelephonyManager.getDefault().hasIccCard(mSlotId);
    }

    private boolean isAirplaneModeOn() {
        return (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mSwitch = (CompoundButton) view.findViewById(R.id.sub_switch_widget);
        mSwitch.setOnCheckedChangeListener(this);
        update();
        // now use other config screen to active/deactive sim card\
        mSwitch.setVisibility(mSwitchVisibility);
        IntentFilter intentFilter = new IntentFilter(
                TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
        mContext.registerReceiver(mStateChanegReceiver, intentFilter);

    }

    public void update() {
        final Resources res = mContext.getResources();
        logd("update()" + mSir);

        boolean isSubValid = isCurrentSubValid();
        setEnabled(isSubValid);

        logd("update() isSubValid "  + isSubValid);
        setTitle(res.getString(R.string.sim_card_number_title, mSlotId + 1));
        if (isSubValid) {
            updateSummary();
            setIcon(new BitmapDrawable(res, (mSir.createIconBitmap(mContext))));
        } else {
            setSummary(res.getString(R.string.sim_slot_empty));
        }
    }

    private boolean isCurrentSubValid() {
        boolean isSubValid = false;
        if (!isAirplaneModeOn() && hasCard()) {
            List<SubscriptionInfo> sirList =
                    SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();
            if (sirList != null ) {
                for (SubscriptionInfo sir : sirList) {
                    if (sir != null && mSlotId == sir.getSimSlotIndex()) {
                        mSir = sir;
                        break;
                    }
                }
                if (mSir != null && mSir.getSubscriptionId() > 0 && mSir.getSimSlotIndex() >= 0 &&
                        mSir.getStatus() != SubscriptionManager.SUB_CONFIGURATION_IN_PROGRESS) {
                    isSubValid = true;
                }
            }
        }
        return isSubValid;
    }

    public void setSwitchVisibility (int visibility) {
        mSwitchVisibility = visibility;
    }

    private void setChecked(boolean state) {
        logd("setChecked: state " + state + "sir:" + mSir);
        if (mSwitch != null) {
            mSwitch.setOnCheckedChangeListener(null);
            // Do not update update checkstatus again in progress
            if (!mCmdInProgress) {
                mSwitch.setChecked(state);
            }
            mSwitch.setOnCheckedChangeListener(this);
            mCurrentState = state;
        }
    }

    private String getNetOperatorName () {
        String netOperatorName = TelephonyManager.getDefault().getNetworkOperatorName(
                mSir.getSubscriptionId());

        return android.util.NativeTextHelper.getInternalLocalString(mContext,
                netOperatorName,
                R.array.origin_carrier_names,
                R.array.locale_carrier_names);
    }

    private void updateSummary() {
        Resources res = mContext.getResources();
        String summary;
        boolean isActivated = (mSir.getStatus() == SubscriptionManager.ACTIVE);
        logd("updateSummary: subId " + mSir.getSubscriptionId() + " isActivated = " + isActivated +
                " slot id = " + mSlotId);

        String displayName = mSir == null ? "SIM" : (String)mSir.getDisplayName();
        if (isActivated) {
            summary = displayName;
            if (!TextUtils.isEmpty(mSir.getNumber())) {
                summary = displayName + " - " + mSir.getNumber();
            }
        } else {
            summary = displayName + mContext.getString(R.string.sim_enabler_summary,
                    res.getString(hasCard() ? R.string.sim_disabled : R.string.sim_missing));
        }

        setSummary(summary);
        setChecked(isActivated);
    }


    /**
     * get count of active SubInfo on the device
     * @param context
     * @return
     */
    public static int getActivatedSubInfoCount(Context context) {
        int activeSubInfoCount = 0;
        List<SubscriptionInfo> subInfoLists =
                SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if (subInfoLists != null) {
            for (SubscriptionInfo subInfo : subInfoLists) {
                if (subInfo.getStatus() == SubscriptionManager.ACTIVE) activeSubInfoCount++;
            }
        }
        return activeSubInfoCount;
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mRequest = isChecked;
        logd("onClick: " + isChecked);

        synchronized (mSyncLock) {
            configureSubscription();
        }
    }

    private void configureSubscription() {
        if (isAirplaneModeOn()) {
            // do nothing but warning
            logd("APM is on, EXIT!");
            showAlertDialog(ERROR_ALERT_DLG_ID, R.string.sim_enabler_airplane_on);
            return;
        }
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            int[] subId = SubscriptionManager.getSubId(i);
            if (TelephonyManager.getDefault().getCallState(subId[0])
                != TelephonyManager.CALL_STATE_IDLE) {
                logd("Call state for phoneId: " + i + " is not idle, EXIT!");
                showAlertDialog(ERROR_ALERT_DLG_ID, R.string.sim_enabler_in_call);
                return;
            }
        }

        if (!mRequest) {
            if (getActivatedSubInfoCount(mContext) > 1) {
                logd("More than one sub is active, Deactivation possible.");
                showAlertDialog(CONFIRM_ALERT_DLG_ID, 0);
            } else {
                logd("Only one sub is active. Deactivation not possible.");
                showAlertDialog(ERROR_ALERT_DLG_ID, R.string.sim_enabler_both_inactive);
                return;
            }
        } else {
            logd("Activate the sub");
            sendSubConfigurationRequest();
        }

    }

    private void sendSubConfigurationRequest() {
        if (mParentHandler == null || !mSwitch.isEnabled()) {
            return;
        }
        mCmdInProgress = true;

        showProgressDialog();
        setEnabled(false);
        if (mRequest) {
            SubscriptionManager.activateSubId(mSir.getSubscriptionId());
        } else {
            SubscriptionManager.deactivateSubId(mSir.getSubscriptionId());
        }

        mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    private void processSetUiccDone(int result, int newSubState) {
        sendMessage(EVT_UPDATE, mParentHandler, MSG_DELAY_TIME);
        sendMessage(EVT_SHOW_RESULT_DLG, mHandler, MSG_DELAY_TIME, result, newSubState);
        mCmdInProgress = false;
        unregisterReceiver();
    }

    private void showAlertDialog(int dialogId, int msgId) {
        String title = mSir == null ? "SUB" : mSir.getDisplayName().toString();
        // Confirm only one AlertDialog instance to show.
        dismissDialog(sAlertDialog);
        dismissDialog(sProgressDialog);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title);
        switch(dialogId) {
            case CONFIRM_ALERT_DLG_ID:
                builder.setMessage(mContext.getString(R.string.sim_enabler_need_disable_sim));
                builder.setPositiveButton(android.R.string.ok, mDialogClickListener);
                builder.setNegativeButton(android.R.string.no, mDialogClickListener);
                builder.setOnCancelListener(mDialogCanceListener);
                break;
            case ERROR_ALERT_DLG_ID:
                builder.setMessage(mContext.getString(msgId));
                builder.setNeutralButton(android.R.string.ok, mDialogClickListener);
                builder.setCancelable(false);
                break;
            case RESULT_ALERT_DLG_ID:
                String msg = mCurrentState ? mContext.getString(R.string.sub_activate_success) :
                        mContext.getString(R.string.sub_deactivate_success);
                builder.setMessage(msg);
                builder.setNeutralButton(android.R.string.ok, null);
                break;
           default:
           break;
        }

        sAlertDialog = builder.create();
        sAlertDialog.setCanceledOnTouchOutside(false);
        sAlertDialog.show();
    }

    private void showProgressDialog() {
        String title = mSir == null ? "SUB" : mSir.getDisplayName().toString();

        String msg = mContext.getString(mRequest ? R.string.sim_enabler_enabling
                : R.string.sim_enabler_disabling);
        dismissDialog(sProgressDialog);
        sProgressDialog = new ProgressDialog(mContext);
        sProgressDialog.setIndeterminate(true);
        sProgressDialog.setTitle(title);
        sProgressDialog.setMessage(msg);
        sProgressDialog.setCancelable(false);
        sProgressDialog.setCanceledOnTouchOutside(false);
        sProgressDialog.show();

        sendMessage(EVT_PROGRESS_DLG_TIME_OUT, mHandler, PROGRESS_DLG_TIME_OUT);
    }

    private void dismissDialog(Dialog dialog) {
        if(dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    public void cleanUp() {
        unregisterReceiver();
        dismissDialog(sProgressDialog);
        dismissDialog(sAlertDialog);
    }

    private void unregisterReceiver() {
        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (Exception ex) {}
    }

    private DialogInterface.OnClickListener mDialogClickListener = new DialogInterface
            .OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        sendSubConfigurationRequest();
                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                        update();
                    } else if (which == DialogInterface.BUTTON_NEUTRAL) {
                        update();
                    }
                }
            };

    private DialogInterface.OnCancelListener mDialogCanceListener = new DialogInterface
            .OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    update();
                }
            };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_SUBSCRIPTION_SET_UICC_RESULT.equals(action)) {
                int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                int result = intent.getIntExtra(TelephonyIntents.EXTRA_RESULT,
                        PhoneConstants.FAILURE);
                int newSubState = intent.getIntExtra(TelephonyIntents.EXTRA_NEW_SUB_STATE,
                        SubscriptionManager.INACTIVE);

                logd("Received ACTION_SUBSCRIPTION_SET_UICC_RESULT on subId: " + subId
                        + "result " + result + " new sub state " + newSubState);
                if (mCmdInProgress && mSir.getSubscriptionId() == subId) {
                    processSetUiccDone(result, newSubState);
                }
            }
        }
    };

    private final BroadcastReceiver mStateChanegReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logd("Intent received: " + action);
            if (TelephonyIntents.ACTION_SERVICE_STATE_CHANGED.equals(action)) {
                ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());
                if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
                    if(isCurrentSubValid()) {
                       updateSummary();
                    }
                }
            }
        }
    };

    private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case EVT_SHOW_RESULT_DLG:
                        int result = msg.arg1;
                        int newSubState = msg.arg2;
                        logd("EVT_SHOW_RESULT_DLG result: " + result +
                                " new sub state " + newSubState);
                        update();
                        if (result == PhoneConstants.FAILURE) {
                            int msgId = (newSubState == SubscriptionManager.ACTIVE) ?
                                    R.string.sub_activate_failed : R.string.sub_deactivate_failed;
                            showAlertDialog(ERROR_ALERT_DLG_ID, msgId);
                        } else {
                            showAlertDialog(RESULT_ALERT_DLG_ID, 0);
                        }
                        mHandler.removeMessages(EVT_PROGRESS_DLG_TIME_OUT);
                        break;
                    case EVT_SHOW_PROGRESS_DLG:
                        logd("EVT_SHOW_PROGRESS_DLG");
                        showProgressDialog();
                        break;
                    case EVT_PROGRESS_DLG_TIME_OUT:
                        logd("EVT_PROGRESS_DLG_TIME_OUT");
                        dismissDialog(sProgressDialog);
                        // Must update UI when time out
                        update();
                        break;
                    default:
                    break;
                }
            }
        };

    public void destroy() {
        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            // May receive Receiver not registered error
            logd(e.getMessage());
        }
        try {
            mContext.unregisterReceiver(mStateChanegReceiver);
        } catch (IllegalArgumentException e) {
            // May receive Receiver not registered error
            logd(e.getMessage());
        }
    }

    //keep this function the same as SimSettings
    public void createEditDialog() {
        Activity activity = (Activity) mContext;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final View dialogLayout = activity.getLayoutInflater().inflate(
                R.layout.multi_sim_dialog, null);
        builder.setView(dialogLayout);

        EditText nameText = (EditText)dialogLayout.findViewById(R.id.sim_name);
        nameText.setText(mSir.getDisplayName());

        final Spinner tintSpinner = (Spinner) dialogLayout.findViewById(R.id.spinner);
        SelectColorAdapter adapter = new SelectColorAdapter(getContext(),
                 R.layout.settings_color_picker_item, mColorStrings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tintSpinner.setAdapter(adapter);

        for (int i = 0; i < mTintArr.length; i++) {
            if (mTintArr[i] == mSir.getIconTint()) {
                tintSpinner.setSelection(i);
                mTintSelectorPos = i;
                break;
            }
        }

        tintSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                int pos, long id){
                tintSpinner.setSelection(pos);
                mTintSelectorPos = pos;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        TextView numberView = (TextView)dialogLayout.findViewById(R.id.number);
        numberView.setText(mSir.getNumber());

        TextView carrierView = (TextView)dialogLayout.findViewById(R.id.carrier);
        carrierView.setText(getNetOperatorName());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final Editor editor = prefs.edit();
        Spinner displayNumbers = (Spinner) dialogLayout.findViewById(R.id.display_numbers);
        displayNumbers.setSelection(prefs.getInt(DISPLAY_NUMBERS_TYPE, 0));
        displayNumbers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position,
                    long id) {
                editor.putInt(DISPLAY_NUMBERS_TYPE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // do nothing
            }
        });

        final Resources res = mContext.getResources();
        builder.setTitle(res.getString(R.string.sim_editor_title, mSlotId + 1));

        builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                final EditText nameText = (EditText)dialogLayout.findViewById(R.id.sim_name);
                mSir.setDisplayName(nameText.getText());
                SubscriptionManager.from(mContext).setDisplayName(mSir.getDisplayName().toString(),
                        mSir.getSubscriptionId(), SubscriptionManager.NAME_SOURCE_USER_INPUT);

                final int tintSelected = tintSpinner.getSelectedItemPosition();
                int subscriptionId = mSir.getSubscriptionId();
                int tint = mTintArr[tintSelected];
                mSir.setIconTint(tint);
                SubscriptionManager.from(mContext).setIconTint(tint, subscriptionId);
                Utils.findRecordBySubId(mContext, subscriptionId).setIconTint(tint);

                update();
                editor.commit();
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    private class SelectColorAdapter extends ArrayAdapter<CharSequence> {
        private Context mContext;
        private int mResId;

        public SelectColorAdapter(
            Context context, int resource, String[] arr) {
            super(context, resource, arr);
            mContext = context;
            mResId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView;
            final ViewHolder holder;
            Resources res = mContext.getResources();
            int iconSize = res.getDimensionPixelSize(R.dimen.color_swatch_size);
            int strokeWidth = res.getDimensionPixelSize(R.dimen.color_swatch_stroke_width);

            if (convertView == null) {
                // Cache views for faster scrolling
                rowView = inflater.inflate(mResId, null);
                holder = new ViewHolder();
                ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
                drawable.setIntrinsicHeight(iconSize);
                drawable.setIntrinsicWidth(iconSize);
                drawable.getPaint().setStrokeWidth(strokeWidth);
                holder.label = (TextView) rowView.findViewById(R.id.color_text);
                holder.icon = (ImageView) rowView.findViewById(R.id.color_icon);
                holder.swatch = drawable;
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }

            holder.label.setText(getItem(position));
            holder.swatch.getPaint().setColor(mTintArr[position]);
            holder.swatch.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
            holder.icon.setVisibility(View.VISIBLE);
            holder.icon.setImageDrawable(holder.swatch);
            return rowView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View rowView = getView(position, convertView, parent);
            final ViewHolder holder = (ViewHolder) rowView.getTag();

            if (mTintSelectorPos == position) {
                holder.swatch.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
            } else {
                holder.swatch.getPaint().setStyle(Paint.Style.STROKE);
            }
            holder.icon.setVisibility(View.VISIBLE);
            return rowView;
        }

        private class ViewHolder {
            TextView label;
            ImageView icon;
            ShapeDrawable swatch;
        }
    }

    private void logd(String msg) {
        if (DBG) Log.d(TAG + "(" + mSlotId + ")", msg);
    }

    private void loge(String msg) {
        if (DBG) Log.e(TAG + "(" + mSlotId + ")", msg);
    }

}
