/*
 * oasis_zp@hisense add agps settings for developer use only.
 * developer or tester can change pref agps mode and supl setting here
*/
package com.android.settings;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.EditTextPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.preference.CheckBoxPreference;
import android.preference.PreferenceCategory;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.widget.Toast;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

public class AgpsSettings extends PreferenceActivity
            implements SharedPreferences.OnSharedPreferenceChangeListener,
    Preference.OnPreferenceChangeListener {
    static final boolean DEBUG = true;
    static final String TAG = "AgpsSettings";
    private static final String MSB = "0";
    private static final String MSA = "1";

    private static final String HOME = "0";
    private static final String ALL = "1";

    private static final int MENU_SAVE = Menu.FIRST;
    private static final int MENU_RESTORE = MENU_SAVE + 1;

    private static final String PROPERTIES_FILE = "/etc/gps.conf";

    private static String sNotSet;
    private EditTextPreference mServer;
    private EditTextPreference mPort;
    private boolean mFirstTime;

    private String mAssistedType;
    private String mApnType;
    private String mResetType;
    private String mNetworkType;

    private ContentResolver mContentResolver;
    private static final String INTENT_BROADCAST_AGPS_PARAMETERS_CHANGED = "intent_agps_parms_changed";

    //private TelephonyManager mPhone;
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
	mContentResolver = getContentResolver();
        addPreferencesFromResource(R.xml.agps_settings);
        mFirstTime = icicle == null;
        sNotSet = getResources().getString(R.string.supl_not_set);
        mServer = (EditTextPreference) findPreference("server_addr");
        mPort = (EditTextPreference) findPreference("server_port");
	dump(null);
        fillUi(false);
    }
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
        	.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
       		 .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
              + ", newValue - " + newValue + ", newValue type - "
              + newValue.getClass());
        return true;
    }
    
    private void setPrefAgpsType() {
        final ListPreference pref = (ListPreference)findPreference("agps_pref");
        pref.setOnPreferenceChangeListener
        (new Preference.OnPreferenceChangeListener() {
             public boolean onPreferenceChange(Preference preference, Object newValue) {
                 final String value = newValue.toString();
                 pref.setValue(value);

                 int type = Integer.valueOf(value);
                 String[] types = getResources().getStringArray(R.array.agps_si_mode_entries);
                 if (type == 0) {
                     mAssistedType  = "MSB";
                 } else if (type == 1) {
                     mAssistedType  = "MSA";
                 }
                 pref.setSummary(types[type]);
                 return true;
             }
         }
        );

        String[] types = getResources().getStringArray(R.array.agps_si_mode_entries);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String defPref = getPrefAgpsType();
	Log.d(TAG, "getPrefAgpsType()=" + defPref);
        mAssistedType = defPref;
        if (defPref.equals("MSB")) {
            pref.setValue("0");
            pref.setSummary(types[0]);
        } else if (defPref.equals("MSA")){
            pref.setValue("1");
            pref.setSummary(types[1]);
        } else {
            pref.setValue("2");
            pref.setSummary(types[2]);
        }
    }

    private void setPrefAgpsNetwork() {
        final ListPreference pref = (ListPreference)findPreference("agps_network");
        pref.setOnPreferenceChangeListener
        (new Preference.OnPreferenceChangeListener() {
             public boolean onPreferenceChange(Preference preference, Object newValue) {
                 final String value = newValue.toString();
                 pref.setValue(value);

                 int type = Integer.valueOf(value);
                 String[] types = getResources().getStringArray(R.array.agps_network_entries);
                  if (type == 0) {
                     mNetworkType  = "HOME";
                 } else if (type == 1) {
                     mNetworkType  = "ALL";
                 }
                 pref.setSummary(types[type]);
                 if (type == 1) {
                     Toast.makeText(AgpsSettings.this, R.string.location_agps_roaming_help,
                        Toast.LENGTH_SHORT).show();  
                 }
                 return true;
             }
         }
        );

        String[] types = getResources().getStringArray(R.array.agps_network_entries);
        String defPref = getPrefNetwork();
	Log.d(TAG, "getPrefNetwork()=" + defPref);
        mNetworkType = defPref;
        if (defPref.equals("ALL")) {            
            pref.setValue("1");
            pref.setSummary(types[1]);
        } else {
            pref.setValue("0");
            pref.setSummary(types[0]);
        }
    }

    private void setPrefAgpsResetType() {
        final ListPreference pref = (ListPreference)findPreference("agps_reset_type");
        pref.setOnPreferenceChangeListener
        (new Preference.OnPreferenceChangeListener() {
             public boolean onPreferenceChange(Preference preference, Object newValue) {
                 final String value = newValue.toString();
                 pref.setValue(value);

                 int type = Integer.valueOf(value);
                 String[] types = getResources().getStringArray(R.array.agps_reset_type_entries);
                 if (type == 0) {
                    mResetType  = "HOT";
                 } else if (type == 1) {
                     mResetType  = "WARM";
                 } else {
                     mResetType  = "COLD";
                 }
                 pref.setSummary(types[type]);
                 return true;
             }
         }
        );

        String[] types = getResources().getStringArray(R.array.agps_reset_type_entries);
        String defPref = getPrefResetType();
	Log.d(TAG, "getPrefResetType()=" + defPref);
        mResetType = defPref;
        if (defPref.equals("COLD")) {            
            pref.setValue("2");
            pref.setSummary(types[2]);
        } else if (defPref.equals("WARM")){
            pref.setValue("1");
            pref.setSummary(types[1]);
        } else {
            pref.setValue("0");
            pref.setSummary(types[0]);
        }
    }

    private void fillUi(boolean restore) {
        if (mFirstTime || restore) {
            mFirstTime = false;
            // Fill in all the values from the db in both text editor and summary
            mServer.setText(getSuplServer());
            mPort.setText(getSuplPort());
        }

        Log.w(TAG, "fillUi");
        mServer.setSummary(checkNull(mServer.getText()));
        mPort.setSummary(checkNull(mPort.getText()));
        setPrefAgpsType();
        setPrefAgpsNetwork();
        setPrefAgpsResetType();
    }
	
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setSummary(checkNull(sharedPreferences.getString(key, "")));
        }
    }
	
    private String checkNull(String value) {
        if (value == null || value.length() == 0) {
            return sNotSet;
        } else {
            return value;
        }
    }

    private String checkNotSet(String value) {
        if (value == null || value.equals(sNotSet)) {
            return "";
        } else {
            return value;
        }
    }

    private String getSuplServer() {
	String supl_host = Settings.Global.getString(mContentResolver, Settings.Global.SUPL_HOST);
        return (null != supl_host) ? supl_host : "221.176.0.55";
    }	
	
    private String getSuplPort() {
	String supl_port = Settings.Global.getString(mContentResolver, Settings.Global.SUPL_PORT);
        return (null != supl_port) ? supl_port : "7275";
    }
	
    private String getPrefNetwork() {
	String agps_network = Settings.Global.getString(mContentResolver, Settings.Global.AGPS_NETWORK);
        return (null != agps_network) ? agps_network : "HOME";
    }
	
    private String getPrefResetType() {
	String agps_reset_type = Settings.Global.getString(mContentResolver, Settings.Global.AGPS_RESET_TYPE);
        return (null != agps_reset_type) ? agps_reset_type : "HOT";
    }
	
    private String getPrefAgpsType() {
	String agps_type = Settings.Global.getString(mContentResolver, Settings.Global.AGPS_PROVID);
        return (null != agps_type) ? agps_type : "MSB";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_SAVE, 0,
                getResources().getString(R.string.menu_save))
                .setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, MENU_RESTORE, 0,
                getResources().getString(R.string.menu_restore))
                .setIcon(android.R.drawable.ic_menu_upload);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SAVE:
            updateAgpsParams("save");
            return true;

        case MENU_RESTORE:
            updateAgpsParams("restore");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateAgpsParams(String flag){
	Intent intent = new Intent(INTENT_BROADCAST_AGPS_PARAMETERS_CHANGED);
	if(flag.equals("save")){
		intent.putExtra("host", checkNotSet(mServer.getText()));
		intent.putExtra("port", checkNotSet(mPort.getText()));
		intent.putExtra("providerid", mAssistedType);	
		intent.putExtra("network", mNetworkType);
		intent.putExtra("resettype", mResetType);
	}else if(flag.equals("restore")){
	        FileInputStream stream = null;
	        try {
	            Properties properties = new Properties();
	            File file = new File(PROPERTIES_FILE);
	            stream = new FileInputStream(file);
	            properties.load(stream);		
	            intent.putExtra("host", properties.getProperty("SUPL_HOST", null));
	            intent.putExtra("port", properties.getProperty("SUPL_PORT", null));

	        } catch (IOException e) {
	            Log.e(TAG, "Could not open GPS configuration file " + PROPERTIES_FILE + ", e=" + e);
	        } finally {
	            if (stream != null) {
	                try {
	                    stream.close();
	                } catch (Exception e) {
	                }
	            }
	        }
		mAssistedType  = "MSB";
		mNetworkType = "HOME";
		mResetType = "HOT";
		intent.putExtra("providerid", mAssistedType);	
		intent.putExtra("network", mNetworkType);
		intent.putExtra("resettype", mResetType);
	 		     
	}else{
	        Log.e(TAG, "INVALID flag when update Agps params.");
		return;
	}
	
	SetValue(intent);
	if(flag.equals("restore"))
		fillUi(true);
	
	dump(intent);
	this.sendBroadcast(intent);
    }
	
    private void SetValue(Intent intent)
    {
         String supl_host = intent.getStringExtra("host");
         String supl_port = intent.getStringExtra("port");
         String agps_provid = intent.getStringExtra("providerid");
	  String agps_network = intent.getStringExtra("network");
	  String agps_reset_type = intent.getStringExtra("resettype");
         if(null != supl_host && supl_host.length() > 0){
		Settings.Global.putString(mContentResolver, Settings.Global.SUPL_HOST, supl_host);
	}
	if(null != supl_port){
		Settings.Global.putString(mContentResolver, Settings.Global.SUPL_PORT, supl_port);
	}	
	if(null != agps_provid && agps_provid.length() > 0){
		Settings.Global.putString(mContentResolver, Settings.Global.AGPS_PROVID, agps_provid);
	}	
	if(null != agps_network && agps_network.length() > 0){
		Settings.Global.putString(mContentResolver, Settings.Global.AGPS_NETWORK, agps_network);
	}
	if(null != agps_reset_type && agps_reset_type.length() > 0){
		Settings.Global.putString(mContentResolver, Settings.Global.AGPS_RESET_TYPE, agps_reset_type);
	}
    }
	
    private void dump(Intent intent){
	if(DEBUG){
		if(null != intent){
			Log.d(TAG, "update from AGPS settings.");
			Log.d(TAG, "SUPL_HOST=" + intent.getStringExtra("host"));
			Log.d(TAG, "SUPL_PORT=" + intent.getStringExtra("port"));
			Log.d(TAG, "AGPS_PROVID=" + intent.getStringExtra("providerid"));
			Log.d(TAG, "AGPS_NETWORK=" + intent.getStringExtra("network"));
			Log.d(TAG, "AGPS_RESET_TYPE=" + intent.getStringExtra("resettype"));
		}else{
			Log.d(TAG, "dump all AGPS params.");
			Log.d(TAG, "SUPL_HOST=" + getSuplServer());
			Log.d(TAG, "SUPL_PORT=" + getSuplPort());
			Log.d(TAG, "AGPS_PROVID=" + getPrefAgpsType());
			Log.d(TAG, "AGPS_NETWORK=" + getPrefNetwork());
			Log.d(TAG, "AGPS_RESET_TYPE=" + getPrefResetType());
		}
	}
    }
}
