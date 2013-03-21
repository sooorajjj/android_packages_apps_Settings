//QUALCOMM_CMCC_START
package com.android.settings.wifi.cmcc;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.qrd.plugin.feature_query.FeatureQuery;

public class WifiGprsSettings extends SettingsPreferenceFragment {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
		addPreferencesFromResource(R.xml.wifi_access_points_and_gprs);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        return super.onPreferenceTreeClick(screen, preference);
    }
//QUALCOMM_CMCC_END
}
