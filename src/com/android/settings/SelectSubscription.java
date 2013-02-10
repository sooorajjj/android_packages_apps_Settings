/* Copyright (c) 2010-12, The Linux Foundation. All rights reserved.
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

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.MSimTelephonyManager;

public class SelectSubscription extends PreferenceActivity {

    private static final String PREF_PARENT_KEY = "parent_pref";
    public static final String SUBSCRIPTION_KEY = "subscription";
    public static final String PACKAGE = "PACKAGE";
    public static final String TARGET_CLASS = "TARGET_CLASS";
    private int[] resourceIndex = {R.string.subscription_01_title, R.string.subscription_02_title,
            R.string.subscription_03_title};

    private Preference subscriptionPref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.select_subscription);
        PreferenceScreen prefParent = (PreferenceScreen) getPreferenceScreen().
                findPreference(PREF_PARENT_KEY);

        Intent intent =  getIntent();
        String pkg = intent.getStringExtra(PACKAGE);
        String targetClass = intent.getStringExtra(TARGET_CLASS);

        int numPhones = MSimTelephonyManager.getDefault().getPhoneCount();
        Intent selectIntent;


        for (int i = 0; i < numPhones; i++) {
            selectIntent = new Intent();
            subscriptionPref = new Preference(getApplicationContext());
            // Set the package and target class.
            selectIntent.setClassName(pkg, targetClass);
            selectIntent.putExtra(SUBSCRIPTION_KEY, i);
            subscriptionPref.setIntent(selectIntent);
            subscriptionPref.setTitle(resourceIndex[i]);
            subscriptionPref.setOnPreferenceClickListener(mPreferenceClickListener);
            prefParent.addPreference(subscriptionPref);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    Preference.OnPreferenceClickListener mPreferenceClickListener =
            new Preference.OnPreferenceClickListener() {
       public boolean onPreferenceClick(Preference preference) {
           startActivity(preference.getIntent());
           return true;
       }
    };
}
