/* Copyright (c) 2011-12, Code Aurora Forum. All rights reserved.
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
 *     * Neither the name of Code Aurora Forum, Inc. nor the names of its
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
import android.content.DialogInterface;
import android.preference.PreferenceScreen;
import android.telephony.MSimTelephonyManager;

import com.android.internal.telephony.msim.Subscription.SubscriptionStatus;
import com.android.internal.telephony.msim.SubscriptionManager;
import com.android.internal.telephony.msim.MSimPhoneFactory;

import com.android.qualcomm.qcrilhook.QcRilHook;

import com.android.settings.R;

public class MultiSimSettings extends PreferenceActivity implements DialogInterface.
        OnDismissListener, DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener  {
    private static final String TAG = "MultiSimSettings";

    private static final String KEY_VOICE = "voice";
    private static final String KEY_DATA = "data";
    private static final String KEY_SMS = "sms";
    private static final String KEY_CONFIG_SUB = "config_sub";
    private static final String TUNE_AWAY = "tune_away";
    private static final String PRIORITY_SUB = "priority_subscription";

    private static final String CONFIG_SUB = "CONFIG_SUB";

    private static final int DIALOG_SET_DATA_SUBSCRIPTION_IN_PROGRESS = 100;

    static final int EVENT_SET_DATA_SUBSCRIPTION_DONE = 1;
    static final int EVENT_SUBSCRIPTION_ACTIVATED = 2;
    static final int EVENT_SUBSCRIPTION_DEACTIVATED = 3;
    static final int EVENT_SET_VOICE_SUBSCRIPTION = 4;
    static final int EVENT_SET_SMS_SUBSCRIPTION = 5;
    static final int EVENT_SET_PRIORITY_SUBSCRIPTION = 6;
    static final int EVENT_SET_TUNE_AWAY = 7;
    static final int EVENT_SET_TUNE_AWAY_DONE = 8;
    protected boolean mIsForeground = false;
    static final int SUBSCRIPTION_ID_0 = 0;
    static final int SUBSCRIPTION_ID_1 = 1;
    static final int SUBSCRIPTION_ID_2 = 2;
    static final int SUBSCRIPTION_ID_INVALID = -1;
    static final int SUBSCRIPTION_DUAL_STANDBY = 2;

    private ListPreference mVoice;
    private ListPreference mData;
    private ListPreference mSms;
    private PreferenceScreen mConfigSub;
    private int mNumPhones = MSimTelephonyManager.getDefault().getPhoneCount();
    CharSequence[] entries; // Used for entries like Subscription1, Subscription2 ...
    CharSequence[] entryValues; // Used for entryValues like 0, 1 ,2 ...
    CharSequence[] summaries; // Used for Summaries like Aubscription1, Subscription2....
    CharSequence[] entriesPrompt; // Used in case of prompt option is required.
    CharSequence[] entryValuesPrompt; // Used in case of prompt option is required.
    CharSequence[] summariesPrompt; // Used in case of prompt option is required.

    private QcRilHook mQcRilHook;

    /* tune away initial/old state */
    private boolean mTuneAwayValue = false;

    /* Priority subscription initial/old state */
    private int mPrioritySubValue = 0;

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

        mQcRilHook = new QcRilHook();
        mTuneAway = (CheckBoxPreference) findPreference(TUNE_AWAY);
        mTuneAway.setOnPreferenceChangeListener(this);
        mPrioritySub = (ListPreference) findPreference(PRIORITY_SUB);
        mPrioritySub.setOnPreferenceChangeListener(this);

        // Create and Intialize the strings required for MultiSIM
        // Dynamic creation of entries instead of using static array vlues.
        // entries are Subscription1, Subscription2, Subscription3 ....
        // EntryValues are 0, 1 ,2 ....
        // Summaries are Subscription1, Subscription2, Subscription3 ....
        entries = new CharSequence[mNumPhones];
        entryValues = new CharSequence[mNumPhones];
        summaries = new CharSequence[mNumPhones];
        entriesPrompt = new CharSequence[mNumPhones + 1];
        entryValuesPrompt = new CharSequence[mNumPhones + 1];
        summariesPrompt = new CharSequence[mNumPhones + 1];
        CharSequence[] subString = getResources().getTextArray(R.array.multi_sim_entries);
        int i = 0;
        for (i = 0; i < mNumPhones; i++) {
            entries[i] = subString[i];
            summaries[i] = subString[i];
            summariesPrompt[i] = subString[i];
            entriesPrompt[i] = subString[i];
            entryValues[i] = Integer.toString(i);
            entryValuesPrompt[i] = Integer.toString(i);
        }
        entryValuesPrompt[i] = Integer.toString(i);
        entriesPrompt[i] = getResources().getString(R.string.prompt);
        summariesPrompt[i] = getResources().getString(R.string.prompt_user);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMultiSimEntriesForVoice();
        updateMultiSimEntriesForData();
        updateMultiSimEntriesForSms();
        mIsForeground = true;
        resumeTuneAwayStatus();
        updateMultiSimEntriesForPrioritySub();
        resumePrioritySub();
        updateState();
    }

    protected void updateMultiSimEntriesForPrioritySub() {
        mPrioritySub.setEntries(entries);
        mPrioritySub.setEntryValues(entryValues);
        //Setting default values
        mPrioritySub.setValue("0");
        mPrioritySub.setSummary(summaries[0]);
    }

    protected void updateMultiSimEntriesForData() {
        mData.setEntries(entries);
        mData.setEntryValues(entryValues);
    }

    protected void updateMultiSimEntriesForSms() {
        mSms.setEntries(entries);
        mSms.setEntryValues(entryValues);
    }

    protected void updateMultiSimEntriesForVoice() {
        int count = subManager.getActiveSubscriptionsCount();
        if (count >= SUBSCRIPTION_DUAL_STANDBY) {
            mVoice.setEntries(entriesPrompt);
            mVoice.setEntryValues(entryValuesPrompt);
        } else  {
            mVoice.setEntries(entries);
            mVoice.setEntryValues(entryValues);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsForeground = false;
    }

    private boolean isAirplaneModeOn() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void updateState() {
        updateVoiceSummary();
        updateDataSummary();
        updateSmsSummary();
    }

    private void updateVoiceSummary() {

        int voiceSub = MSimPhoneFactory.getVoiceSubscription();
        boolean promptEnabled  = MSimPhoneFactory.isPromptEnabled();
        int count = subManager.getActiveSubscriptionsCount();

        Log.d(TAG, "updateVoiceSummary: voiceSub =  " + voiceSub
                + " promptEnabled = " + promptEnabled
                + " number of active SUBs = " + count);

        if (promptEnabled && count >= SUBSCRIPTION_DUAL_STANDBY) {

            Log.d(TAG, "prompt is enabled: setting value to : " + mNumPhones);
            mVoice.setValue(Integer.toString(mNumPhones));
            mVoice.setSummary(summariesPrompt[mNumPhones]);
        } else {

            String sub = Integer.toString(voiceSub);
            Log.d(TAG, "setting value to : " + sub);
            mVoice.setValue(sub);
            mVoice.setSummary(summaries[voiceSub]);
        }
    }

    private void updateDataSummary() {
        int dataSub = MSimPhoneFactory.getDataSubscription();

        Log.d(TAG, "updateDataSummary: Data Subscription : = " + dataSub);
        mData.setValue(Integer.toString(dataSub));
        mData.setSummary(summaries[dataSub]);
    }

    private void updateSmsSummary() {
        int smsSub = MSimPhoneFactory.getSMSSubscription();

        Log.d(TAG, "updateSmsSummary: Sms Subscripiton : = " + smsSub);
        mSms.setValue(Integer.toString(smsSub));
        mSms.setSummary(summaries[smsSub]);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        String status;
        Log.d(TAG, "onPreferenceChange:::: " );

        if (KEY_VOICE.equals(key)) {

            int voiceSub = Integer.parseInt((String) objValue);

            if (voiceSub == mNumPhones) { //mNumPhones is the maximum index of the UI options.
                                         //This will be the Prompt option.
                MSimPhoneFactory.setPromptEnabled(true);
                mVoice.setSummary(summariesPrompt[voiceSub]);
                Log.d(TAG, "prompt is enabled " + voiceSub);
            } else if (subManager.getCurrentSubscription(voiceSub).subStatus
                    == SubscriptionStatus.SUB_ACTIVATED) {
                Log.d(TAG, "setVoiceSubscription " + voiceSub);
                MSimPhoneFactory.setPromptEnabled(false);
                MSimPhoneFactory.setVoiceSubscription(voiceSub);
                mVoice.setSummary(summaries[voiceSub]);
            } else {
                status = getResources().getString(R.string.set_voice_error);
                displayAlertDialog(status);
            }
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_SET_VOICE_SUBSCRIPTION));
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
            updatePrioritySub(objValue);
        }
        return true;
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
                    updateMultiSimEntriesForVoice();
                    break;
                case EVENT_SET_VOICE_SUBSCRIPTION:
                    updateVoiceSummary();
                    break;
                case EVENT_SET_SMS_SUBSCRIPTION:
                    updateSmsSummary();
                    break;
                case EVENT_SET_TUNE_AWAY:
                    updateTuneAwayStatus();
                    break;
                case EVENT_SET_TUNE_AWAY_DONE:
                    mTuneAway.setChecked(mTuneAwayValue);
                    mTuneAway.setSummary(mTuneAwayValue ? "Enable" : "Disable");
                    break;
                case EVENT_SET_PRIORITY_SUBSCRIPTION:
                    mPrioritySub.setValue(Integer.toString(mPrioritySubValue));
                    mPrioritySub.setSummary(summaries[mPrioritySubValue]);
                    break;
            }
        }
    };

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

    private void resumeTuneAwayStatus() {
        new Thread(new Runnable() {
            public void run() {
                boolean tuneAwayValue = mQcRilHook.qcRilGetTuneAway();
                mTuneAwayValue = tuneAwayValue;
                Log.e(TAG, "resumeTuneAwayStatus qcRilGetTuneAway value is: " + mTuneAwayValue);
                mTuneAway.setChecked(tuneAwayValue);
                mTuneAway.setSummary(tuneAwayValue ? "Enable" : "Disable");
            }
        }).start();
    }

    private void updateTuneAwayStatus() {
        new Thread(new Runnable() {
            public void run() {
                boolean tuneAwayValue = mTuneAway.isChecked();
                Log.e(TAG," updateTuneAwayStatus change tuneAwayValue to: " + tuneAwayValue);
                boolean result = mQcRilHook.qcRilSetTuneAway(tuneAwayValue);
                if (result) {
                    mTuneAwayValue = tuneAwayValue;
                } else {
                    Log.e(TAG,"setTuneAway is failed, revert to old value: " + mTuneAwayValue);
                }
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_SET_TUNE_AWAY_DONE));
            }
        }).start();
    }

    private void resumePrioritySub() {
        new Thread(new Runnable() {
            public void run() {
                int prioritySubIndex = mQcRilHook.qcRilGetPrioritySub();
                Log.e(TAG, "resumePrioritySub qcRilGetPrioritySub value is: " + prioritySubIndex);
                if (prioritySubIndex < mNumPhones) {
                    mPrioritySubValue = prioritySubIndex;
                } else {
                    // Update old value to user
                }
                mPrioritySub.setValue(Integer.toString(mPrioritySubValue));
                mPrioritySub.setSummary(summaries[mPrioritySubValue]);
            }
        }).start();
    }

    private void updatePrioritySub(final Object presentPriority) {
        new Thread(new Runnable() {
            public void run() {
                int prioritySubIndex = Integer.parseInt((String) presentPriority);
                Log.e(TAG, "updatePrioritySub change priority sub to: " + prioritySubIndex);
                boolean result = mQcRilHook.qcRilSetPrioritySub(prioritySubIndex);
                if (result) {
                    mPrioritySubValue = prioritySubIndex;
                } else {
                    Log.e(TAG,"Set priority sub failed, revert to old value: " + mPrioritySubValue);
                }
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_SET_PRIORITY_SUBSCRIPTION));
            }
        }).start();
    }
}

