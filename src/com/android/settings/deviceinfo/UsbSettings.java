/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;


/**
 * USB storage settings.
 */
public class UsbSettings extends SettingsPreferenceFragment {

    private static final boolean FEATURE_ADD_USB_MODE = true;

    private static final boolean DEBUG = true;
    private static boolean SD_REMOVAL = true;
	
    private static final String TAG = "UsbSettings";
	
    private static String KEY_CHOSEN = "usb_charging";
    private static final String KEY_MTP = "usb_mtp";
    private static final String KEY_PTP = "usb_ptp";
    private static final String KEY_SDCARD = "usb_sdcard";
    private static final String KEY_CHARGING = "usb_charging";

    private UsbManager mUsbManager;
    private CheckBoxPreference mMtp;
    private CheckBoxPreference mPtp;
    private CheckBoxPreference mSDCard;
    private CheckBoxPreference mCharging;

    private StorageManager mStorageManager = null;
    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context content, Intent intent) {
             String action = intent.getAction();
             if (action.equals(UsbManager.ACTION_USB_STATE)) {
                boolean connected = intent.getExtras().getBoolean(UsbManager.USB_CONNECTED);
                if (!connected) {
                    finish();
                    return;
                 } else {
                    updateUsbFunctioState();
                }
             }
        }
    };

    private void updateUsbFunctioState() {
        //if (mUsbManager.isChargeOnlyEnabled()) {
        //  updateToggles(null);
        // } else {
    createPreferenceHierarchy();
   if((KEY_CHOSEN==KEY_SDCARD) && (SD_REMOVAL))
   {
        try
             {
            Thread.sleep(1500);
	}
           catch (Exception e){}

         KEY_CHOSEN= KEY_CHARGING;
         mUsbManager.setCurrentFunction(
                    "diag,serial_smd,serial_tty,rmnet_bam,adb", true);
            updateToggles(null);
         
   }
   else{
	 //   if(SD_REMOVAL)
	//	updateToggles(null);	
	//else	
            updateToggles(mUsbManager.getDefaultFunction());
       // }
        getPreferenceScreen().setEnabled(true);
      }
    }
	
   private PreferenceScreen createPreferenceHierarchy() {
 	PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.usb_settings);
        root = getPreferenceScreen();

        mMtp = (CheckBoxPreference)root.findPreference(KEY_MTP);
        mPtp = (CheckBoxPreference)root.findPreference(KEY_PTP);
        mSDCard = (CheckBoxPreference)root.findPreference(KEY_SDCARD);
        mCharging = (CheckBoxPreference)root.findPreference(KEY_CHARGING);
 
        if(!FEATURE_ADD_USB_MODE){
           root.removePreference(mSDCard);
            root.removePreference(mCharging);
        }
	 if(SD_REMOVAL== true)
	 {
              root.removePreference(mSDCard);
	 }
        return root;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        if (mStorageManager == null) {
            Log.w(TAG, "Failed to get StorageManager");
        }
	String sdState = Environment.getExternalStorageState();
	//NOT point to the /Storage/sdcard1
	String exSdState = Environment.getSdCardState();
	if (exSdState.equals(Environment.MEDIA_REMOVED) ||exSdState.equals(Environment.MEDIA_BAD_REMOVAL) ) 
		SD_REMOVAL=true; 
	else
			 SD_REMOVAL=false;
	
      Log.i(TAG, " On Create:  SD_REMOVAL = " + SD_REMOVAL +",  exSdState = "+ exSdState);
    }

    private StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState,
                String newState) {
            if (DEBUG)
                Log.i(TAG, "onStorageStateChanged path= " + path
                        + " oldState = " + oldState + " newState= " + newState);
            final boolean isExternalPath = (Environment.getExternalStorageDirectory().getPath()
                    .equals(path));
            if (newState.equals(Environment.MEDIA_SHARED)) {
                if (isExternalPath) {
                    Toast.makeText(getActivity(), R.string.external_storage_turn_on,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), R.string.internal_storage_turn_on,
                            Toast.LENGTH_SHORT).show();
                }
            } else if (oldState.equals(Environment.MEDIA_SHARED)
                    && newState.equals(Environment.MEDIA_UNMOUNTED)) {
                if (isExternalPath) {
                   Toast.makeText(getActivity(), R.string.external_storage_turn_off,
                            Toast.LENGTH_SHORT).show();
                } else {
                   Toast.makeText(getActivity(), R.string.internal_storage_turn_off,
                            Toast.LENGTH_SHORT).show();
                }
            }

	    if(newState.equals(Environment.MEDIA_BAD_REMOVAL) ||newState.equals(Environment.MEDIA_REMOVED) ||newState.equals(Environment.MEDIA_MOUNTED) )
	    {
                  Log.i(TAG, " oldState = " + oldState + " newState= " + newState);
				 if(newState.equals(Environment.MEDIA_BAD_REMOVAL) ||newState.equals(Environment.MEDIA_REMOVED) )
				 	SD_REMOVAL=true;	
				 else
				 	SD_REMOVAL=false;

			 updateUsbFunctioState();
				
	    }
	    else	
            updateUsbFunctioState();
        }
	 
    };

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mStateReceiver);
        if (mStorageManager != null) {
            mStorageManager.unregisterListener(mStorageListener);
        }
	  
	
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        // ACTION_USB_STATE is sticky so this will call updateToggles
        getActivity().registerReceiver(mStateReceiver,
                new IntentFilter(UsbManager.ACTION_USB_STATE));
        if (mStorageManager != null) {
            mStorageManager.registerListener(mStorageListener);
        }
		
	String exSdState = Environment.getSdCardState();
			 
	if (exSdState.equals(Environment.MEDIA_REMOVED) ||exSdState.equals(Environment.MEDIA_BAD_REMOVAL) ) 
		SD_REMOVAL=true; 
	else
		SD_REMOVAL=false;
			 
        updateUsbFunctioState();
    }

    private void updateToggles(String function) {
        if (UsbManager.USB_FUNCTION_MTP.equals(function)) {
            mMtp.setChecked(true);
            mPtp.setChecked(false);
            mSDCard.setChecked(false);
            mCharging.setChecked(false);
        } else if (UsbManager.USB_FUNCTION_PTP.equals(function)) {
            mMtp.setChecked(false);
            mPtp.setChecked(true);
            mSDCard.setChecked(false);
            mCharging.setChecked(false);
        } else if (UsbManager.USB_FUNCTION_MASS_STORAGE.equals(function)) {
            mMtp.setChecked(false);
            mPtp.setChecked(false);
            mSDCard.setChecked(true);
            mCharging.setChecked(false);
        } else  {
            mMtp.setChecked(false);
            mPtp.setChecked(false);
	 if(!SD_REMOVAL)		
            mSDCard.setChecked(false);
            mCharging.setChecked(true);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        // Don't allow any changes to take effect as the USB host will be disconnected, killing
        // the monkeys
        if (Utils.isMonkeyRunning()) {
            return true;
        }
        // temporary hack - using check boxes as radio buttons
        // don't allow unchecking them
        if (preference instanceof CheckBoxPreference) {
            CheckBoxPreference checkBox = (CheckBoxPreference)preference;
            if (!checkBox.isChecked()) {
                checkBox.setChecked(true);
                return true;
            }
        }
	 KEY_CHOSEN = KEY_CHARGING;
        if (preference == mMtp) {
            mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MTP, true);
            updateToggles(UsbManager.USB_FUNCTION_MTP);
        } else if (preference == mPtp) {
            mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_PTP, true);
            updateToggles(UsbManager.USB_FUNCTION_PTP);
        } else if (preference == mSDCard) {
        KEY_CHOSEN = KEY_SDCARD;
            mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MASS_STORAGE, true);
            updateToggles(UsbManager.USB_FUNCTION_MASS_STORAGE);
        } else if (preference == mCharging) {
            mUsbManager.setCurrentFunction(
                    "diag,serial_smd,serial_tty,rmnet_bam,adb", true);
            updateToggles(null);
        }
        getPreferenceScreen().setEnabled(false);
        return true;
    }
}
