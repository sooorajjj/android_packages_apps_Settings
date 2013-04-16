//QUALCOMM_CMCC_START
package com.android.settings.wifi.cmcc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;

public class WifiSelectInSsidsDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private static final String TAG = "WifiSelectInSsidsDialog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"WifiSelectInSsidsDialog onCreate");
        createDialog();
    }

    private void createDialog() {
        final AlertController.AlertParams mParams = mAlertParams;
        mParams.mIconId = android.R.drawable.ic_dialog_info;
        mParams.mTitle = getString(R.string.confirm_title);
        mParams.mView = createView();
        mParams.mViewSpacingSpecified=true;
        mParams.mViewSpacingLeft=15;
        mParams.mViewSpacingRight=15;
        mParams.mViewSpacingTop=5;
        mParams.mViewSpacingBottom=5;
        mParams.mPositiveButtonText = getString(android.R.string.yes);
        mParams.mPositiveButtonListener = this;
        mParams.mNegativeButtonText = getString(android.R.string.no);
        mParams.mNegativeButtonListener = this;
        setupAlert();
    }

    private View createView() {
        TextView messageView = new TextView(this);
        messageView.setText(R.string.wifi_signal_found_msg);
        return messageView;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void onPositive() {
        Intent intent = new Intent();
        intent.setAction("android.settings.WIFI_SETTINGS");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void onNegative() {    
        finish();
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                onPositive();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                onNegative();
                break;
        }
    }
}
//QUALCOMM_CMCC_END
