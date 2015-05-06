package com.android.settings.wificall;

import java.util.ArrayList;

import android.app.ActionBar;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

import com.android.settings.R;

public class WifiCallPreferenceActivity extends PreferenceActivity
    implements OnPreferenceClickListener{

    private static final String TAG = "WifiCallPreferenceActivity";

    // Below is the prefence key as the same as the CheckBoxPreference title.
    public static final String KEY_WIFI_CALLING_PREFERRED = "Wi-Fi Preferred";
    public static final String KEY_CELLULAR_NETWORK_PREFERRED = "Cellular Network Preferred";
    public static final String KEY_NEVER_USE_CELLULAR_NETWORK_PREFERRED = "Never use Cellular Network";

    // This is the wifi calling switch on the actionbar
    private Switch mSwitch;
    private int mSelection;
    private ArrayList<CheckBoxPreference> mCheckboxPref = new ArrayList<CheckBoxPreference>();
    private Resources mRes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_call_prefenecs);
        mRes = getResources();
        initWifiCallSettings();
    }

    private void initWifiCallSettings() {
        // init action bar at first
        ActionBar actionBar = getActionBar();
        // FIX ME : init the wificallswitch status
        mSwitch = new Switch(this);
        mSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // FIX ME When this checked something should be done e.x set Wifi call
                Log.i(TAG, "onCheckedChanged isChecked : " + isChecked);
            }
        });
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(mSwitch, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL
                        | Gravity.RIGHT));

        PreferenceScreen rootPre = getPreferenceScreen();
        PreferenceCategory prefCate = new PreferenceCategory(this);
        prefCate.setTitle(mRes.getString(R.string.wifi_call_preferrd_setting_title));
        rootPre.addPreference(prefCate);
        // FIX ME : The current wifi-calling Preferred
        int current = 0;// this value should read by devices setting.
        String[] titleArray = mRes.getStringArray(R.array.wifi_call_preferences_entries_title);
        String[] summaryArray = mRes.getStringArray(R.array.wifi_call_preferences_entries_summary);
        for (int i=0;i<titleArray.length;i++) {
            CheckBoxPreference pref = new CheckBoxPreference(this);
            pref.setKey(titleArray[i]);
            pref.setOnPreferenceClickListener(this);
            pref.setChecked(i == current ? true : false);
            pref.setTitle(titleArray[i]);
            pref.setSummary(summaryArray[i]);
            prefCate.addPreference(pref);
            mCheckboxPref.add(pref);
            if (pref.isChecked()) mSelection = i;
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // FIX ME : when the Preferred is changed, some thing should do
        Log.i(TAG, "The preference have click with : " + preference.getKey());
        updateSelection(preference.getKey());
        return true;
    }

    // Control the three checkbox: only one should be selected.
    private void updateSelection(String preferenceKey){
        if(preferenceKey == null){
            Log.i(TAG, "updateSelection is null");
            return;
        }
        for(int index = 0; index < mCheckboxPref.size(); index ++){
            CheckBoxPreference checkbox = mCheckboxPref.get(index);
            if(preferenceKey.equals(checkbox.getKey())){
                checkbox.setChecked(true);
                mSelection = index;
            }else{
                checkbox.setChecked(false);
            }
            Log.i(TAG, "updateSelection with mSelect : " + mSelection + " Checkbox : " + checkbox.getKey());
        }
    }
}
