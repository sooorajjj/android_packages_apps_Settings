//QUALCOMM_CMCC_START
package com.android.settings.wifi.cmcc;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.content.ContentResolver;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.internal.util.AsyncChannel;
import com.android.settings.R;
import android.util.Log;

public class WifiGsmDialog extends AlertActivity implements DialogInterface.OnClickListener {
    private static final String TAG = "WifiGsmDialog";

    private WifiManager mWm;
    private String mSsid;
    private int mNetworkId;

    private boolean mConnectApType;
    private WifiManager.ActionListener mConnectListener = new WifiManager.ActionListener() {
            public void onSuccess() {
            }
            public void onFailure(int reason) {
            }
        };

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWm = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        //mWm.asyncConnect(this, new WifiServiceHandler());
        Intent intent = getIntent();
        String action = intent.getAction();
        mSsid = intent.getStringExtra(WifiManager.EXTRA_NOTIFICATION_SSID);
        mNetworkId = intent.getIntExtra(WifiManager.EXTRA_NOTIFICATION_NETWORKID, -1);
        log("WifiNotifyDialog onCreate " + action);
        if (!action.equals(WifiManager.WIFI_NOTIFICATION_ACTION) || mNetworkId == -1) {
            Log.e(TAG, "Error: this activity may be started only with intent WIFI_NOTIFICATION_ACTION");
            finish();
        }
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
        log("createView mSsid="+mSsid);
        TextView messageView = new TextView(this);
        messageView.setText(getString(R.string.msg_wifi_signal_found, mSsid));
        return messageView;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void onPositive() {
        log("onOK mNetworkId=" + mNetworkId);
        mConnectApType = Settings.System.getInt(getContentResolver(), Settings.System.WIFI_AUTO_CONNECT_TYPE, Settings.System.WIFI_AUTO_CONNECT_TYPE_AUTO)==0;
        if(mConnectApType){
//            mWm.enableNetwork(mNetworkId, true);
//            mWm.reconnect();
            log("onOK auto connect AP");
            mWm.connect(mNetworkId, mConnectListener);
        }else{
            Intent intent = new Intent();
            intent.setAction("android.settings.WIFI_SETTINGS");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
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
    private class WifiServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AsyncChannel.CMD_CHANNEL_HALF_CONNECTED:
                    if (msg.arg1 == AsyncChannel.STATUS_SUCCESSFUL) {
                        //AsyncChannel in msg.obj
                    } else {
                        //AsyncChannel set up failure, ignore
                        log("Failed to establish AsyncChannel connection");
                    }
                    break;
                default:
                    //Ignore
                    break;
            }
        }
    }
}
//QUALCOMM_CMCC_END
