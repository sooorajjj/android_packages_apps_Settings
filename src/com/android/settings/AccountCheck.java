
package com.android.settings;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
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

public class AccountCheck  {
    public static final String TAG = "AccountCheck";
    private static final boolean DEBUG = true;
    private static final int INVALID             = -1;
    private static final int WIFI_TETHERING      = 0;
    private static final int USB_TETHERING       = 1;
    private static AccountCheck thisInstance = null;

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
