package com.android.settings.wifi.cmcc;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.android.settings.R;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

public class ChangeDataConnectionDialog extends AlertActivity implements
        DialogInterface.OnClickListener {

	private Context mContext;
    private CheckBox mCb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        // Set up the "dialog"
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = android.R.drawable.ic_dialog_alert;
        p.mTitle = getString(R.string.wifi_failover_gprs_title);
        p.mView = createView();
        p.mPositiveButtonText = getString(android.R.string.ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(android.R.string.cancel);
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    private View createView() {
        View view = getLayoutInflater().inflate(R.layout.change_data_connection_dialog, null);
        TextView contentView = (TextView)view.findViewById(R.id.setContent);
        contentView.setText(getString(R.string.wifi_failover_gprs_summary));
        mCb = (CheckBox)view.findViewById(R.id.setPrimary);
        return view;
    }

    public void onClick(DialogInterface dialog, int which) {
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:

                if (cm != null) {
                    cm.setMobileDataEnabled(true);
                }
                
                if(mCb.isChecked() == true){
                    Settings.System.putInt(mContext.getContentResolver(),
                            Settings.System.WIFI_BROWSER_INTERACTION_REMIND_TYPE, Settings.System.WIFI_BROWSER_INTERACTION_REMIND_TYPE_CANCEL);
                }
                
                finish();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                if (cm != null) {
                    cm.setMobileDataEnabled(false);
                }
            	if(mCb.isChecked() == true){
                    Settings.System.putInt(mContext.getContentResolver(),
                            Settings.System.WIFI_BROWSER_INTERACTION_REMIND_TYPE, Settings.System.WIFI_BROWSER_INTERACTION_REMIND_TYPE_CANCEL);
                }
            	
                finish();
                break;
        }
    }
}
