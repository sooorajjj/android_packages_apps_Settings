/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.Dialog;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;

import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.util.Log;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import android.content.ContentResolver;

public class AutoPowerOnOffSettings  extends SettingsPreferenceFragment 
    implements Preference.OnPreferenceChangeListener{
       
    final static String M12 = "h:mm aa";
    final static String M24 = "kk:mm";
    final static String AUTO_ON_FLAG = "auto_on_flag";
    final static String AUTO_ON_HOUR = "auto_on_hour";
    final static String AUTO_ON_MINUTE = "auto_on_minute";
    final static String AUTO_OFF_FLAG = "auto_off_flag";
    final static String AUTO_OFF_HOUR = "auto_off_hour";
    final static String AUTO_OFF_MINUTE = "auto_off_minute";

    private boolean bAutoOnEnabled;
    private boolean bAutoOffEnabled;
    private int mAutoOnHour =0;
    private int mAutoOnMinute =0;
    private int mAutoOffHour =0;
    private int mAutoOffMinute =0;
	
    private CheckBoxPreference mAutoOnEnabledPref;
    private Preference mAutoOnTimePref;
    private CheckBoxPreference mAutoOffEnabledPref;
    private Preference mAutoOffTimePref;
    private Activity mActivity;
    private ContentResolver mContentResolver; 

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mActivity = getActivity();
        mContentResolver = mActivity.getContentResolver(); 
        addPreferencesFromResource(R.xml.auto_power_on_off_prefs);     
        initUI();        
    }
    
    public void initUI() {
        mAutoOnEnabledPref = (CheckBoxPreference)findPreference("auto_on");
        mAutoOnTimePref =  findPreference("auto_on_time");
        mAutoOffEnabledPref = (CheckBoxPreference)findPreference("auto_off");
        mAutoOffTimePref =  findPreference("auto_off_time");
        mAutoOnEnabledPref.setOnPreferenceChangeListener(this);
        mAutoOnTimePref.setOnPreferenceChangeListener(this);
        mAutoOffEnabledPref.setOnPreferenceChangeListener(this);
        mAutoOffTimePref.setOnPreferenceChangeListener(this);	  
    }

    @Override
    public void onResume() {
        super.onResume();

        if(1 == Settings.System.getInt(mContentResolver, AUTO_ON_FLAG, 0)) {
            bAutoOnEnabled  = true;
        } else {
            bAutoOnEnabled  = false;
        }
        mAutoOnEnabledPref.setChecked(bAutoOnEnabled);

        if(1 == Settings.System.getInt(mContentResolver, AUTO_OFF_FLAG, 0)){
            bAutoOffEnabled = true;
        } else {
            bAutoOffEnabled = false;
        }

        mAutoOffEnabledPref.setChecked(bAutoOffEnabled);
        mAutoOnHour = Settings.System.getInt(mContentResolver, AUTO_ON_HOUR,7);		 
        mAutoOnMinute = Settings.System.getInt(mContentResolver, AUTO_ON_MINUTE,0);
        mAutoOnTimePref.setSummary(formatTime(mAutoOnHour,mAutoOnMinute));
        mAutoOffHour = Settings.System.getInt(mContentResolver, AUTO_OFF_HOUR,23);
        mAutoOffMinute = Settings.System.getInt(mContentResolver, AUTO_OFF_MINUTE,0);
        mAutoOffTimePref.setSummary(formatTime(mAutoOffHour,mAutoOffMinute));
        mAutoOnTimePref.setEnabled(bAutoOnEnabled);
        mAutoOffTimePref.setEnabled(bAutoOffEnabled);
    }

    @Override 
    public void onPause() {
        super.onPause();    
    }
     
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mAutoOnTimePref){
            showTimePicker(true);
        }else  if (preference == mAutoOffTimePref){
            showTimePicker(false);
        }
        return false;
    }

    public boolean onPreferenceChange(final Preference p, Object newValue) {
        if(p == mAutoOnEnabledPref){
            bAutoOnEnabled = !bAutoOnEnabled;
            Settings.System.putInt(mContentResolver, AUTO_ON_FLAG, 
                                              bAutoOnEnabled ? 1 : 0);
            SendAutoOnOffSetIntent();
            mAutoOnTimePref.setEnabled(bAutoOnEnabled);
        }else if(p == mAutoOffEnabledPref){
            bAutoOffEnabled = !bAutoOffEnabled;
            Settings.System.putInt(mContentResolver, AUTO_OFF_FLAG, 
                                              bAutoOffEnabled ? 1 : 0);
            SendAutoOnOffSetIntent();
            mAutoOffTimePref.setEnabled(bAutoOffEnabled);
        }
        return true;
    }

    private void showTimePicker(boolean bPowerOn) {
        if(bPowerOn){
            TimePickerDialog timepicker1 =  new TimePickerDialog(mActivity, 

            new TimePickerDialog.OnTimeSetListener() {					
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    mAutoOnHour = hourOfDay;
                    mAutoOnMinute = minute;
                    Calendar c = Calendar.getInstance();
                    c.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    c.set(Calendar.MINUTE, minute);                                 	 
                    mAutoOnTimePref.setSummary(formatTime(mAutoOnHour,mAutoOnMinute));
                    Settings.System.putInt(mContentResolver, AUTO_ON_HOUR, mAutoOnHour);
                    Settings.System.putInt(mContentResolver, AUTO_ON_MINUTE, mAutoOnMinute);
                    SendAutoOnOffSetIntent();
                }
            }, 
            mAutoOnHour, 
            mAutoOnMinute,
            DateFormat.is24HourFormat(mActivity));			
            timepicker1.show();
        }else{

            TimePickerDialog timepicker2=  new TimePickerDialog(mActivity, 

            new TimePickerDialog.OnTimeSetListener() {					
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                mAutoOffHour = hourOfDay;
                mAutoOffMinute = minute;
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, hourOfDay);
                c.set(Calendar.MINUTE, minute);                                 	 
                mAutoOffTimePref.setSummary(formatTime(mAutoOffHour,mAutoOffMinute));
                Settings.System.putInt(mContentResolver, AUTO_OFF_HOUR, mAutoOffHour);
                Settings.System.putInt(mContentResolver, AUTO_OFF_MINUTE, mAutoOffMinute);
                SendAutoOnOffSetIntent();
                }
            }, 
            mAutoOffHour, 
            mAutoOffMinute,
            DateFormat.is24HourFormat(mActivity));			
            timepicker2.show();
        }

    }

    public String formatTime(int hour, int minute)
    {
        String strFormat = DateFormat.is24HourFormat(mActivity) ? M24 : M12;
        Calendar c = Calendar.getInstance();
        Date newDate = new Date();
        newDate.setHours(hour);
        newDate.setMinutes(minute);
        c.setTime(newDate);
        String strShowTime = (String)DateFormat.format(strFormat, c);
        return strShowTime;
    }

    public void SendAutoOnOffSetIntent(){
        Intent intent = new Intent("com.android.deskclock.AUTO_POWER_ON_OFF");
        mActivity.sendBroadcast(intent);	
    }

}