/*
* Copyright (c) 2015, The Linux Foundation. All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are
* met:
*     * Redistributions of source code must retain the above copyright
*      notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above
*       copyright notice, this list of conditions and the following
*       disclaimer in the documentation and/or other materials provided
*      with the distribution.
*     * Neither the name of The Linux Foundation nor the names of its
*      contributors may be used to endorse or promote products derived
*       from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
* ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
* BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
* BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
* WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
* OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
* IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.android.settings.wificall;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.settings.R;

public class WifiCallingNotification {

    private static final String TAG = "WifiCallingNotification";
    private static final boolean DEBUG = true;
    private static final int WIFI_CALLING_NOTIFICAION_ID = 1;
    private static final int UPDATE_NOTIFICAION_TURN_ON = 1;
    private static final int UPDATE_NOTIFICAION_TURN_OFF = 2;
    private static final int UPDATE_NOTIFICAION_ERROR_CODE = 3;
    private static final int UPDATE_NOTIFICAION_CALL_STATE = 4;

    private static WifiCallingNotification mWifiCallNoti;
    private static Builder mBuilder;
    private static int mCallState = TelephonyManager.CALL_STATE_IDLE;

    private WifiCallingNotification(){
    }

    public static WifiCallingNotification getIntance(){
        if (mWifiCallNoti == null) {
            mWifiCallNoti = new WifiCallingNotification();
        }
        return mWifiCallNoti;
    }

    public static boolean getWifiCallingNotifiEnable(Context context){
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_regional_wifi_calling_notificaion_enable);
    }

    public static void updateWFCStatusChange(Context context, boolean turnon){
        if (!getWifiCallingNotifiEnable(context)) {
            return;
        }
        NotificationManager notiManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (!turnon) {
            cancelNotification(context, WIFI_CALLING_NOTIFICAION_ID);
            return;
        }
        Builder builder = buildNotication(context, UPDATE_NOTIFICAION_TURN_ON, null);
        if (builder != null) {
            notiManager.notify(WIFI_CALLING_NOTIFICAION_ID, builder.build());
        }
    }

    public static void updateWFCCallStateChange(Context context, int callState){
        if (!getWifiCallingNotifiEnable(context)) {
            return;
        }
        if (mCallState != callState) {
            mCallState = callState;
        }
        Builder builder = buildNotication(context, UPDATE_NOTIFICAION_CALL_STATE, null);
        NotificationManager notiManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notiManager.notify(WIFI_CALLING_NOTIFICAION_ID, builder.build());
    }

    public static void updateRegistrationError(Context context, String extraMsg){
        if (!getWifiCallingNotifiEnable(context)) {
            return;
        }

        Builder builder = buildNotication(context, UPDATE_NOTIFICAION_ERROR_CODE, extraMsg);
        NotificationManager notiManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notiManager.notify(WIFI_CALLING_NOTIFICAION_ID, builder.build());
    }

    private static void cancelNotification(Context context, int id){
        if (!getWifiCallingNotifiEnable(context)) {
            return;
        }
        NotificationManager notiManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notiManager.cancel(id);
    }

    private static Notification.Builder buildNotication(Context context, int flag, String strNotification){
        if (mBuilder == null) {
            mBuilder = new Builder(context);
            Intent activityIntent = new Intent();
            mBuilder.setContentTitle(context.getResources().getString(
                    R.string.wifi_calling_notification_title));
            activityIntent.setAction(Intent.ACTION_MAIN);
            activityIntent.setClassName("com.android.phone",
                    "com.android.phone.WifiCallingSettings");
            PendingIntent pendingIntent = PendingIntent.getActivity(context,
                    WIFI_CALLING_NOTIFICAION_ID, activityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setOngoing(true);
            mBuilder.setContentIntent(pendingIntent);
        }
        switch (flag) {
        case UPDATE_NOTIFICAION_TURN_ON:
            mBuilder.setSmallIcon(R.drawable.wifi_calling_on_notification);
            mBuilder.setContentText(
                    context.getResources().getString(R.string.wifi_calling_notification_subtitle));
            break;
        case UPDATE_NOTIFICAION_ERROR_CODE:
            mBuilder.setSmallIcon(R.drawable.wifi_calling_noti_error);
            mBuilder.setContentText(strNotification);
            break;
        case UPDATE_NOTIFICAION_CALL_STATE:
            int icon = mCallState == TelephonyManager.CALL_STATE_IDLE ?
                    R.drawable.wifi_calling_on_notification : R.drawable.wifi_calling_incall_noti;
            mBuilder.setSmallIcon(icon);
            mBuilder.setContentText(
                    context.getResources().getString(R.string.wifi_calling_notification_subtitle));
        default:
            break;
        }
        return mBuilder;
    }
}
