/* Copyright (c) 2011-13, The Linux Foundation. All rights reserved.
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

package com.android.settings;


import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Message;
import android.os.Handler;
import android.os.AsyncResult;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.PreferenceScreen;
import android.telephony.MSimTelephonyManager;

import com.qualcomm.internal.telephony.Subscription.SubscriptionStatus;
import com.qualcomm.internal.telephony.SubscriptionManager;
import com.qualcomm.internal.telephony.MSimPhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.RIL;

import com.android.settings.R;
import java.lang.Object;

public class MultiSimSettings extends PreferenceActivity implements DialogInterface.
        OnDismissListener, DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener  {
    private static final String TAG = "MultiSimSettings";

    private static final String KEY_VOICE = "voice";
    private static final String KEY_DATA = "data";
    private static final String KEY_SMS = "sms";
    private static final String KEY_CONFIG_SUB = "config_sub";

    private static final String CONFIG_SUB = "CONFIG_SUB";
    private static final String TUNE_AWAY = "tune_away";
    private static final String PRIORITY_SUB = "priority_subscription";

    private static final int DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS = 100;

    static final int EVENT_SET_DATA_SUBSCRIPTION_DONE = 1;
    static final int EVENT_SUBSCRIPTION_ACTIVATED = 2;
    static final int EVENT_SUBSCRIPTION_DEACTIVATED = 3;
    static final int EVENT_SET_VOICE_SUBSCRIPTION = 4;
    static final int EVENT_SET_SMS_SUBSCRIPTION = 5;
    static final int EVENT_SET_TUNE_AWAY = 6;
    static final int EVENT_SET_TUNE_AWAY_DONE = 7;
    static final int EVENT_SET_PRIORITY_SUBSCRIPTION = 8;
    static final int EVENT_SET_PRIORITY_SUBSCRIPTION_DONE = 9;
    static final int EVENT_SET_VOICE_SUBSCRIPTION_DONE = 10;

    protected boolean mIsForeground = false;
    static final int SUBSCRIPTION_ID_0 = 0;
    static final int SUBSCRIPTION_ID_1 = 1;
    static final int SUBSCRIPTION_ID_INVALID = -1;
    static final int PROMPT_OPTION = 2;
    static final int SUBSCRIPTION_DUAL_STANDBY = 2;

    private ListPreference mVoice;
    private ListPreference mData;
    private ListPreference mSms;
    private PreferenceScreen mConfigSub;
    CharSequence[] entries;      // Used for entries like Subscription1, Subscription2 ...
    CharSequence[] entryValues;  // Used for entryValues like 0, 1 ,2 ...
    CharSequence[] summaries;    // Used for Summaries like Aubscription1, Subscription2....

    /* tune away initial/old state */
    private boolean mTuneAwayValue = false;
    /* Priority subscription initial/old state */
    private int mPrioritySubValue = 0;
    /* Default voice subscription initial/old state */
    private int mVoiceSub = 0;
    private int numPhones = 0;
    private PhoneProxy mPhoneProxy = null;
    private PhoneBase mPhoneBase = null;
    private RIL mCi = null;

    private CheckBoxPreference mTuneAway;
    private ListPreference mPrioritySub;

    SubscriptionManager subManager = SubscriptionManager.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.multi_sim_settings);

        mVoice = (ListPreference) findPreference(KEY_VOICE);
        mVoice.setOnPreferenceChangeListener(this);
        mData = (ListPreference) findPreference(KEY_DATA);
        mData.setOnPreferenceChangeListener(this);
        mSms = (ListPreference) findPreference(KEY_SMS);
        mSms.setOnPreferenceChangeListener(this);
        mConfigSub = (PreferenceScreen) findPreference(KEY_CONFIG_SUB);
        mConfigSub.getIntent().putExtra(CONFIG_SUB, true);
        mTuneAway = (CheckBoxPreference) findPreference(TUNE_AWAY);
        mTuneAway.setOnPreferenceChangeListener(this);
        mPrioritySub = (ListPreference) findPreference(PRIORITY_SUB);
        mPrioritySub.setOnPreferenceChangeListener(this);
        mPhoneProxy = (PhoneProxy)MSimPhoneFactory.getPhone(SUBSCRIPTION_ID_0);
        mPhoneBase = (PhoneBase)mPhoneProxy.getActivePhone();
        mCi = (RIL)mPhoneBase.mCM;
        numPhones = MSimTelephonyManager.getDefault().getPhoneCount();
        entries = new CharSequence[numPhones];
        entryValues = new CharSequence[numPhones];
        summaries = new CharSequence[numPhones];
        CharSequence[] subString = getResources().getTextArray(R.array.multi_sim_entries);
        for (int subIndex = 0; subIndex < numPhones; subIndex++) {
            entries[subIndex] = subString[subIndex];
            summaries[subIndex] = subString[subIndex];
            entryValues[subIndex] = Integer.toString(subIndex);
        }

        if (isAirplaneModeOn()) {
            Log.d(TAG, "Airplane mode is ON, grayout the config subscription menu!!!");
            mConfigSub.setEnabled(false);
        }
        for (int subId = 0; subId < SubscriptionManager.NUM_SUBSCRIPTIONS; subId++) {
            subManager.registerForSubscriptionActivated(subId,
                    mHandler, EVENT_SUBSCRIPTION_ACTIVATED, null);
            subManager.registerForSubscriptionDeactivated(subId,
                    mHandler, EVENT_SUBSCRIPTION_DEACTIVATED, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        int count = subManager.getActiveSubscriptionsCount();
        if (count == SUBSCRIPTION_DUAL_STANDBY) {
            mVoice.setEntries(R.array.multi_sim_entries_voice);
            mVoice.setEntryValues(R.array.multi_sim_values_voice);
        } else  {
            mVoice.setEntries(R.array.multi_sim_entries_voice_without_prompt);
            mVoice.setEntryValues(R.array.multi_sim_values_voice_without_prompt);
        }
        mIsForeground = true;
        updateState();
        updateTuneAwayState();
        updatePrioritySubState();
    }

    private void updateTuneAwayState() {
        boolean tuneawayVal = (Settings.Global.getInt(getContentResolver(),
                                  Settings.Global.TUNE_AWAY_STATUS,  0) == 1);
        mTuneAway.setChecked(tuneawayVal);
        mTuneAway.setSummary(tuneawayVal ? "Enable" : "Disable");
    }

    private void updatePrioritySubState() {
        mPrioritySub.setEntries(entries);
        mPrioritySub.setEntryValues(entryValues);

        try {
            int priorityValue = Settings.Global.getInt(getContentResolver(),
                           Settings.Global.MULTI_SIM_PRIORITY_SUBSCRIPTION);
            mPrioritySub.setValue(Integer.toString(priorityValue));
            mPrioritySub.setSummary(summaries[priorityValue]);
            mPrioritySubValue = priorityValue;


        } catch (SettingNotFoundException snfe) {
            Log.d(TAG, "Settings Exception Reading Dual Sim Priority Subscription Values");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;
    }

    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void updateState() {
        updateVoiceSummary();
        updateDataSummary();
        updateSmsSummary();
    }

    private void updateVoiceSummary() {
        CharSequence[] summaries = getResources().getTextArray(R.array.multi_sim_summaries_voice);
        int voiceSub = MSimPhoneFactory.getVoiceSubscription();
        boolean promptEnabled  = MSimPhoneFactory.isPromptEnabled();
        int count = subManager.getActiveSubscriptionsCount();

        Log.d(TAG, "updateVoiceSummary: voiceSub =  " + voiceSub
                + " promptEnabled = " + promptEnabled
                + " number of active SUBs = " + count);

        if (promptEnabled && count == SUBSCRIPTION_DUAL_STANDBY) {
            Log.d(TAG, "prompt is enabled: setting value to : 2");
            mVoice.setValue("2");
            mVoice.setSummary(summaries[2]);
        } else {
            String sub = Integer.toString(voiceSub);
            Log.d(TAG, "setting value to : " + sub);
            mVoice.setValue(sub);
            mVoice.setSummary(summaries[voiceSub]);
        }
    }

    private void updateDataSummary() {
        int Data_val = SUBSCRIPTION_ID_INVALID;
        CharSequence[] summaries = getResources().getTextArray(R.array.multi_sim_summaries);

        try {
            Data_val = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.MULTI_SIM_DATA_CALL_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, "Settings Exception Reading Multi Sim Data Subscription Value.");
        }

        Log.d(TAG, "updateDataSummary: Data_val = " + Data_val);
        if (Data_val == SUBSCRIPTION_ID_0) {
            mData.setValue("0");
            mData.setSummary(summaries[0]);
        } else if (Data_val == SUBSCRIPTION_ID_1) {
            mData.setValue("1");
            mData.setSummary(summaries[1]);
        } else {
            mData.setValue("0");
            mData.setSummary(summaries[0]);
        }
    }

    private void updateSmsSummary() {
        int Sms_val = SUBSCRIPTION_ID_INVALID;
        CharSequence[] summaries = getResources().getTextArray(R.array.multi_sim_summaries);

        try {
            Sms_val = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.MULTI_SIM_SMS_SUBSCRIPTION);
        } catch (SettingNotFoundException snfe) {
            Log.e(TAG, "Settings Exception Reading Multi Sim SMS Call Values.");
        }

        Log.d(TAG, "updateSmsSummary: Sms_val = " + Sms_val);
        if (Sms_val == SUBSCRIPTION_ID_0) {
            mSms.setValue("0");
            mSms.setSummary(summaries[0]);
        } else if (Sms_val == SUBSCRIPTION_ID_1) {
            mSms.setValue("1");
            mSms.setSummary(summaries[1]);
        } else {
            mSms.setValue("0");
            mSms.setSummary(summaries[0]);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        String status;
        CharSequence[] summaries = getResources().getTextArray(R.array.multi_sim_summaries);

        if (KEY_VOICE.equals(key)) {
            summaries = getResources().getTextArray(R.array.multi_sim_summaries_voice);
            mVoiceSub = Integer.parseInt((String) objValue);
            if (mVoiceSub == PROMPT_OPTION) {
                MSimPhoneFactory.setPromptEnabled(true);
                mVoice.setSummary(summaries[mVoiceSub]);
                Log.d(TAG, "prompt is enabled " + mVoiceSub);
            } else if (subManager.getCurrentSubscription(mVoiceSub).subStatus
                   == SubscriptionStatus.SUB_ACTIVATED) {
                Log.d(TAG, "setVoiceSubscription " + mVoiceSub);
                MSimPhoneFactory.setPromptEnabled(false);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_SET_VOICE_SUBSCRIPTION,
                        mVoiceSub));
            } else {
                status = getResources().getString(R.string.set_voice_error);
                displayAlertDialog(status);
            }
        }

        if (KEY_DATA.equals(key)) {
            int dataSub = Integer.parseInt((String) objValue);
            Log.d(TAG, "setDataSubscription " + dataSub);
            if (mIsForeground) {
                showDialog(DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS);
            }
            SubscriptionManager mSubscriptionManager = SubscriptionManager.getInstance();
            Message setDdsMsg = Message.obtain(mHandler, EVENT_SET_DATA_SUBSCRIPTION_DONE, null);
            mSubscriptionManager.setDataSubscription(dataSub, setDdsMsg);
        }

        if (KEY_SMS.equals(key)) {
            int smsSub = Integer.parseInt((String) objValue);
            Log.d(TAG, "setSMSSubscription " + smsSub);
            if (subManager.getCurrentSubscription(smsSub).subStatus
                    == SubscriptionStatus.SUB_ACTIVATED) {
                MSimPhoneFactory.setSMSSubscription(smsSub);
                mSms.setSummary(summaries[smsSub]);
            } else {
                status = getResources().getString(R.string.set_sms_error);
                displayAlertDialog(status);
            }
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_SET_SMS_SUBSCRIPTION));
        }

        if (TUNE_AWAY.equals(key)) {
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_SET_TUNE_AWAY));
        }

        if (PRIORITY_SUB.equals(key)) {
            int prioritySubIndex = Integer.parseInt((String) objValue);
            if (subManager.getCurrentSubscription(prioritySubIndex).subStatus
                    == SubscriptionStatus.SUB_ACTIVATED) {
                mPrioritySubValue = prioritySubIndex;
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_SET_PRIORITY_SUBSCRIPTION,
                        prioritySubIndex));
            } else {
                status = getResources().getString(R.string.set_priority_sub_error);
                displayAlertDialog(status);
            }
        }

        return true;
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS) {
            ProgressDialog dialog = new ProgressDialog(this);

            dialog.setMessage(getResources().getString(R.string.set_data_subscription_progress));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);

            return dialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS) {
            // when the dialogs come up, we'll need to indicate that
            // we're in a busy state to disallow further input.
            getPreferenceScreen().setEnabled(false);
        }
    }

    // This is a method implemented for DialogInterface.OnDismissListener
    public void onDismiss(DialogInterface dialog) {
        Log.d(TAG, "onDismiss!");
    }

    // This is a method implemented for DialogInterface.OnClickListener.
    public void onClick(DialogInterface dialog, int which) {
        Log.d(TAG, "onClick!");
    }

    void displayAlertDialog(String msg) {
        if (!mIsForeground) {
            Log.d(TAG, "The activitiy is not in foreground. Do not display dialog!!!");
            return;
        }
        Log.d(TAG, "displayErrorDialog!" + msg);
        new AlertDialog.Builder(this).setMessage(msg)
               .setTitle(android.R.string.dialog_alert_title)
               .setIcon(android.R.drawable.ic_dialog_alert)
               .setPositiveButton(android.R.string.yes, this)
               .show()
               .setOnDismissListener(this);
        }

        private void updateTuneAwayStatus() {
            boolean tuneAwayValue = mTuneAway.isChecked();
            mTuneAwayValue = tuneAwayValue;
            Log.d(TAG," updateTuneAwayStatus change tuneAwayValue to: " + tuneAwayValue);
            Message setTuneAwayMsg = Message.obtain(mHandler, EVENT_SET_TUNE_AWAY_DONE, null);
            mCi.setTuneAway(tuneAwayValue, setTuneAwayMsg);
        }

        private void updatePrioritySub(int priorityIndex) {
            Log.d(TAG, "updatePrioritySub change priority sub to: " + priorityIndex);
            Message setPrioritySubMsg = Message.obtain(mHandler,
                    EVENT_SET_PRIORITY_SUBSCRIPTION_DONE, null);
            mCi.setPrioritySub(priorityIndex, setPrioritySubMsg);
        }

        private void updateVoiceSub(int subIndex) {
            Log.d(TAG, "updateVoiceSub change voice sub to: " + subIndex);
            Message setVoiceSubMsg = Message.obtain(mHandler,
                    EVENT_SET_VOICE_SUBSCRIPTION_DONE, null);
            mCi.setDefaultVoiceSub(subIndex, setVoiceSubMsg);
        }


        private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;

            switch(msg.what) {
                case EVENT_SET_DATA_SUBSCRIPTION_DONE:
                    Log.d(TAG, "EVENT_SET_DATA_SUBSCRIPTION_DONE");
                    if (mIsForeground) {
                        dismissDialog(DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS);
                    }
                    getPreferenceScreen().setEnabled(true);
                    updateDataSummary();

                    ar = (AsyncResult) msg.obj;

                    String status;

                    if (ar.exception != null) {
                        status = getResources().getString(R.string.set_dds_error)
                                           + " " + ar.exception.getMessage();
                        displayAlertDialog(status);
                        break;
                    }

                    boolean result = (Boolean)ar.result;

                    Log.d(TAG, "SET_DATA_SUBSCRIPTION_DONE: result = " + result);

                    if (result == true) {
                        status = getResources().getString(R.string.set_dds_success);
                        Toast toast = Toast.makeText(getApplicationContext(), status,
                                                                Toast.LENGTH_LONG);
                        toast.show();
                    } else {
                        status = getResources().getString(R.string.set_dds_failed);
                        displayAlertDialog(status);
                    }

                    break;
                case EVENT_SUBSCRIPTION_ACTIVATED:
                case EVENT_SUBSCRIPTION_DEACTIVATED:
                    int count = subManager.getActiveSubscriptionsCount();
                    if (count == SUBSCRIPTION_DUAL_STANDBY) {
                        mVoice.setEntries(R.array.multi_sim_entries_voice);
                        mVoice.setEntryValues(R.array.multi_sim_values_voice);
                    } else  {
                        mVoice.setEntries(R.array.multi_sim_entries_voice_without_prompt);
                        mVoice.setEntryValues(R.array.multi_sim_values_voice_without_prompt);
                    }
                    break;

                case EVENT_SET_VOICE_SUBSCRIPTION:
                    updateVoiceSub(msg.arg1);
                    break;
                case EVENT_SET_VOICE_SUBSCRIPTION_DONE:
                    Log.d(TAG, "EVENT_SET_VOICE_SUBSCRIPTION_DONE");
                    ar = (AsyncResult) msg.obj;
                    String sub;
                    if (ar.exception != null) {
                        Log.e(TAG, "SET_VOICE_SUBSCRIPTION_DONE: returned Exception: "
                                                                             + ar.exception);
                        int voiceSub = MSimPhoneFactory.getVoiceSubscription();
                        sub = Integer.toString(voiceSub);
                        mVoice.setValue(sub);
                        mVoice.setSummary(summaries[voiceSub]);
                        mVoiceSub = voiceSub;
                        break;
                    }
                    sub = Integer.toString(mVoiceSub);
                    mVoice.setValue(sub);
                    mVoice.setSummary(summaries[mVoiceSub]);
                    MSimPhoneFactory.setVoiceSubscription(mVoiceSub);
                    break;
                case EVENT_SET_SMS_SUBSCRIPTION:
                    updateSmsSummary();
                    break;
                case EVENT_SET_TUNE_AWAY:
                    updateTuneAwayStatus();
                    break;
                case EVENT_SET_TUNE_AWAY_DONE:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Log.e(TAG, "SET_TUNE_AWAY_DONE: returned Exception: " + ar.exception);
                        updateTuneAwayState();
                        break;
                    }
                    Log.d(TAG, "SET_TUNE_AWAY_DONE: mTuneAwayValue = " + mTuneAwayValue);
                    mTuneAway.setChecked(mTuneAwayValue);
                    mTuneAway.setSummary(mTuneAwayValue ? "Enable" : "Disable");
                    Settings.Global.putInt(getContentResolver(), Settings.Global.TUNE_AWAY_STATUS,
                                                              mTuneAwayValue ? 1 : 0);
                    break;
                case EVENT_SET_PRIORITY_SUBSCRIPTION:
                    updatePrioritySub(msg.arg1);
                    break;
                case EVENT_SET_PRIORITY_SUBSCRIPTION_DONE:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Log.e(TAG, "EVENT_SET_PRIORITY_SUBSCRIPTION_DONE: returned Exception: "
                                                                             + ar.exception);
                        updatePrioritySubState();
                        break;
                    }
                    Log.d(TAG, "EVENT_SET_PRIORITY_SUBSCRIPTION_DONE : mPrioritySubValue "
                                                                        + mPrioritySubValue);
                    mPrioritySub.setValue(Integer.toString(mPrioritySubValue));
                    mPrioritySub.setSummary(summaries[mPrioritySubValue]);
                    MSimPhoneFactory.setPrioritySubscription(mPrioritySubValue);
                    break;
            }
        }
    };
}
