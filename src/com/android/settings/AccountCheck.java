/*
     Copyright (c) 2015, The Linux Foundation. All Rights Reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.android.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Message;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import com.android.settings.R;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import static android.content.Context.TELEPHONY_SERVICE;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

public class AccountCheck  {
    public static final String TAG = "AccountCheck";
    private static final boolean DEBUG = true;
    private static final int INVALID             = -1;
    private static final int WIFI_TETHERING      = 0;
    private static final int USB_TETHERING       = 1;
    private static AccountCheck thisInstance = null;
    private static View mshowAgainView;
    private static CheckBox mShowAgain;

    public String mServerUrl = "";
    public String upsellUrl = "";
    public ProgressDialog accoutCheckDialog;

    public static AccountCheck getInstance() {
        if(thisInstance == null) {
            thisInstance = new AccountCheck();
        }
        return thisInstance;
    }

    public AccountCheck() {
    }

    private void initAccoutCheckDialog(Context ctx) {
        mServerUrl = ctx.getResources().getString(
                com.android.internal.R.string.config_regional_hotspot_accout_check_server_url);
        upsellUrl =  ctx.getResources().getString(
                com.android.internal.R.string.config_regional_hotspot_accout_up_sell_url);
        accoutCheckDialog = ProgressDialog.show(ctx,
            "", ctx.getString(R.string.account_check_msg), true, false);
    }
    private void dismissAccoutCheckDialog()  {
        if(accoutCheckDialog != null && accoutCheckDialog.isShowing()) {
            accoutCheckDialog.dismiss();
        }
    }

    private boolean getHttpResponse() {
        if(TextUtils.isEmpty(mServerUrl)) {
            return true;
        }
        HttpClient client = new DefaultHttpClient();
        try {
            HttpGet httpRequest = new HttpGet(mServerUrl);
            HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
            HttpConnectionParams.setSoTimeout(client.getParams(), 10000);
            HttpResponse response = client.execute(httpRequest);
            int statusCode = response.getStatusLine().getStatusCode();
            if(DEBUG) {
                Log.d(TAG,"statusCode:"+statusCode);
            }
            if(statusCode == 200) {
                String strResult = EntityUtils.toString(response.getEntity());
                if(DEBUG) {
                    Log.d(TAG,"strResult:"+strResult);
                }
                if(null != strResult) {
                     if(strResult.equalsIgnoreCase("Yes")) {
                        return true;
                    } else if(strResult.equalsIgnoreCase("No")) {
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            } else if(statusCode / 100 == 4 || statusCode / 100 == 5) {
                return true;
            }
        } catch (ClientProtocolException err) {
            Log.w(TAG,"ClientProtocolException error!");
            return true;
        } catch (IOException err) {
            Log.w(TAG, "IOException error!");
            return true;
        } catch (Exception e) {
            Log.w(TAG,"Exception occur!");
            return true;
        } finally {
            client.getConnectionManager().shutdown();
        }
        return true;
    }

    public void checkAccount(Context ctx, Message message) {
        AccoutCheckTask accCheckTask = new AccoutCheckTask(ctx,message);
        accCheckTask.execute();
    }

    private void initServiceUpgradeDialog(final Context ctx) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(R.string.service_upgrade_title);
        builder.setMessage(R.string.service_msg);
        builder.setPositiveButton(R.string.service_upgrade,
            new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(upsellUrl));
                    ctx.startActivity(intent);
                    dialog.dismiss();
                } catch (Exception err) {
                    Log.w(TAG,"can not open url:"+upsellUrl);
                    err.printStackTrace();
                }
            }
        });
        builder.setNegativeButton(R.string.later,
            new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    public static boolean showNoSimCardDialog(Context ctx) {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(TELEPHONY_SERVICE);
        if (!isSimCardReady(tm)) {
            AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
            alert.setTitle(R.string.tethering_no_sim_alert_title);
            alert.setMessage(R.string.tethering_no_sim_card_text);
            alert.setPositiveButton("Ok", null);
            alert.show();
            return true;
        }
        return false;
    }

    private static boolean isSimCardReady(
        TelephonyManager telephonyManager) {
        return (telephonyManager.getSimState()
            == TelephonyManager.SIM_STATE_READY);
    }
    public static boolean isCarrierSimCard(Context ctx) {
        boolean isCarrierSimCard = false;
        String[] carrierMccMncs = ctx.getResources().getStringArray(
                com.android.internal.R.array.config_regional_carrier_operator_list);
        TelephonyManager tm = (TelephonyManager)ctx.getSystemService(
                Context.TELEPHONY_SERVICE);
        String simOperator = tm.getSimOperator();
        if (DEBUG) Log.d(TAG,
            "carrier sim card check: sim operator is " + simOperator);
        if (simOperator != null) {
            if (Arrays.asList(carrierMccMncs).contains(simOperator)) {
                isCarrierSimCard = true;
            }
            else {
                for(String s: Arrays.asList(carrierMccMncs)) {
                    if (simOperator.indexOf(s) >= 0) {
                        isCarrierSimCard = true;
                        break;
                    }
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG,"is home Carrier SIM Card? " + isCarrierSimCard);
        }
        return isCarrierSimCard;
    }
    public static void showTurnOffWifiDialog(final Context ctx) {
        LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mshowAgainView = inflater.inflate(R.layout.not_show_again, null);
        mShowAgain = (CheckBox)mshowAgainView.findViewById(R.id.check);
        SharedPreferences sharedpreferences = ctx.getSharedPreferences(
                "ShowAgain", Context.MODE_PRIVATE);
        boolean showagain = sharedpreferences.getBoolean("SsidBroadcast", true);
        if (showagain) {
            AlertDialog.Builder alertdialog =  new AlertDialog.Builder(ctx);
            alertdialog.setTitle(R.string.turn_off_wifi_dialog_title);
            alertdialog.setMessage(R.string.turn_off_wifi_dialog_text);
            alertdialog.setView(mshowAgainView);
            alertdialog.setPositiveButton("close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick( DialogInterface dialog, int which) {
                    SharedPreferences sharedpreferences = ctx.getSharedPreferences(
                        "TurnOffWifiShowAgain", Context.MODE_PRIVATE);
                    Editor editor = sharedpreferences.edit();
                    if (mShowAgain.isChecked()) {
                        editor.putBoolean("SsidBroadcast", false);
                    } else {
                        editor.putBoolean("SsidBroadcast", true);
                    }
                    editor.commit();
                }
            });
            alertdialog.show();
        }
    }

    public static boolean isNeedShowHelp(final Context ctx, int type) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(
                "MY_PERFS", Activity.MODE_PRIVATE);
        String strKey = type == 0? "FirstLaunchHotspotTethering":"FirstLaunchUsbTethering";
        Editor editor = sharedPreferences.edit();
        boolean isFirstUse = sharedPreferences.getBoolean(strKey, true);
        if (isFirstUse) {
            editor.putBoolean(strKey, false);
            editor.commit();
        }
        return isFirstUse;
    }

    public static void needShowHelpLater(final Context ctx) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(
                "MY_PERFS", Activity.MODE_PRIVATE);
        String strKey = "FirstLaunchHotspotTethering";
        Editor editor = sharedPreferences.edit();
        editor.putBoolean(strKey, true);
        editor.commit();
    }

    public static void showHelpDialog(final Context ctx,
            DialogInterface.OnClickListener okListener,
            DialogInterface.OnClickListener laterListener) {
        final AlertDialog.Builder build = new AlertDialog.Builder(ctx);
        build.setTitle(R.string.tether_settings_launch_title);
        build.setMessage(R.string.wifi_tether_first_use_message);
        build.setNegativeButton(R.string.later, laterListener);
        build.setPositiveButton(R.string.okay, okListener);
        build.show();
    }

    public static void showPasswordEmptyDialog(final Context ctx) {
        final AlertDialog.Builder build = new AlertDialog.Builder(ctx);
        build.setTitle(R.string.hotspot_password_empty_title);
        build.setMessage(R.string.hotspot_password_empty_text);
        build.setPositiveButton(R.string.okay, null);
        build.show();
    }

    public static boolean isNeedShowActivated (Context ctx, final int type) {
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(
                "MY_PERFS", Activity.MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        String strKey = type == 0? "FirstHotspotActivated":"FirstUSBActivated";
        boolean isFirstUse = sharedPreferences.getBoolean(strKey,true);
        if (isFirstUse) {
            editor = sharedPreferences.edit();
            editor.putBoolean(strKey, false);
            editor.commit();
        }
        return isFirstUse;
    }

    public static void showActivatedDialog(final Context ctx, final int type) {
        if (!isNeedShowActivated(ctx, type)) {
            return;
        }
        final AlertDialog.Builder build = new AlertDialog.Builder(ctx);
        if (USB_TETHERING == type) {
            build.setTitle(R.string.learn_usb_dialog_title);
            build.setMessage(R.string.learn_usb_dialog_text);
        } else {
            build.setTitle(R.string.learn_hotspot_dialog_title);
            build.setMessage(R.string.learn_hotspot_dialog_text);
        }
        build.setPositiveButton("yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        showMoreAboutActivation(ctx,type);
                    }
                });
        build.setNegativeButton("skip",null);
        build.show();

    }

    public static void showMoreAboutActivation(final Context ctx,int type) {
        final AlertDialog.Builder build = new AlertDialog.Builder(ctx);
        if (USB_TETHERING == type) {
            build.setTitle(R.string.mobile_tether_help_dialog_title);
            build.setMessage(R.string.mobile_usb_help_dialog_text);
        } else {
            build.setTitle(R.string.mobile_tether_help_dialog_title);
            build.setMessage(R.string.mobile_hotspot_help_dialog_text);
        }
        build.setPositiveButton("ok",null);
        build.show();
    }

    public static boolean isPasswordEmpty(WifiManager wifiManager) {
        if (wifiManager == null) {
            return false;
        }
        WifiConfiguration wifiAPConfig = wifiManager.getWifiApConfiguration();
        if (wifiAPConfig == null) {
            return false;
        }
        if(TextUtils.isEmpty(wifiAPConfig.preSharedKey)) {
            return true;
        }
        return false;
    }

    private class AccoutCheckTask extends AsyncTask<Void, Integer, Boolean>{
        private final Context mContext;
        private Message mCurrentMsg = null;
        public AccoutCheckTask(Context context,Message currentMsg) {
            mContext = context;
            mCurrentMsg = currentMsg;
        }
         @Override
         protected void onPreExecute() {
             super.onPreExecute();
             initAccoutCheckDialog(mContext);
         }

         @Override
         protected void onPostExecute(Boolean result) {
             super.onPostExecute(result);
             dismissAccoutCheckDialog();
             mCurrentMsg.arg1 =result?1:0;
             mCurrentMsg.sendToTarget();
             if(!result) {
                 initServiceUpgradeDialog(mContext);
             }
         }

         @Override
         protected void onProgressUpdate(Integer... values) {
             super.onProgressUpdate(values);
         }

         @Override
         protected void onCancelled(Boolean result) {
             super.onCancelled(result);
         }

         @Override
         protected void onCancelled() {
             super.onCancelled();
         }

         @Override
         protected Boolean doInBackground(Void... params) {
            return getHttpResponse();
        }
    }

    public static boolean isHotspotAutoTurnOffEnabled(Context ctx) {
        boolean isHotspotAutoTurnOffEnabled = false;
        if (ctx != null) {
            isHotspotAutoTurnOffEnabled = ctx.getResources().getBoolean(
                    R.bool.def_wifi_hotspot_enable);
        }
        return isHotspotAutoTurnOffEnabled;
    }

    public static boolean isServiceRunning(Context context, String className) {
        boolean isRunning = false;
        if (context != null && (!TextUtils.isEmpty(className))) {
            ActivityManager activityManager = (ActivityManager)
                    context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                    .getRunningServices(100);
            if (serviceList != null) {
                for (int i = 0; i < serviceList.size(); i++) {
                    if (serviceList.get(i).service.getClassName().equals(className)) {
                        isRunning = true;
                        break;
                    }
                }
            }
        }
        return isRunning;
    }
}
