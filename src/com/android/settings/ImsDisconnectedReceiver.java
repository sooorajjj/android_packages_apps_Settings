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

package com.android.settings;

import android.content.BroadcastReceiver;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import com.android.ims.ImsReasonInfo;
import com.android.settings.wificall.WifiCallRegistrationErrorUtil;
import com.android.settings.wificall.WifiCallingNotification;

public class ImsDisconnectedReceiver extends BroadcastReceiver {

    private static final String TAG = "ImsDisconnectedReceiver";
    private static final boolean DUBG = true;
    public static final String IMSDICONNECTED_ACTION = "com.android.imscontection.DISCONNECTED";
    private static int EVENT_UPDATE_IMS_DISCONNECTED_STATE = 0;
    private static ImsReasonInfo mImsReasonInfo;
    private static Context mContext;

    private static Handler mHandlerUpdate = new Handler(){
        public void handleMessage(android.os.Message msg) {
            int error = mImsReasonInfo.getExtraCode();
            boolean isWifiCallError =
                    WifiCallRegistrationErrorUtil.isWifiCallingRegistrationError(error);
            if(isWifiCallError) {
                // Fix me: here need to judge wifi calling state ON/OFF
                if(true) {
                    WifiCallingNotification.updateRegistrationError(mContext, error);
                }
                return;
            } else {
                if (DUBG) Log.i(TAG, "ImsDisconnected extracode is " +
                            "not wifi calling Registration Error");
                if (DUBG) Log.i(TAG, "get ImsDisconnected extracode : " + error);
                if (DUBG) Log.i(TAG, "get ImsDisconnected getExtraMessage : "
                            + mImsReasonInfo.getExtraMessage());
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        if (DUBG) Log.i(TAG, "Receive action :" + intent.getAction());
        if(IMSDICONNECTED_ACTION.equals(intent.getAction())){
            Parcelable bundle = intent.getParcelableExtra("result");
            if(bundle == null){
                if (DUBG) Log.i(TAG, "intent didn't contain not parcelable");
                return ;
            }
            if(bundle instanceof ImsReasonInfo) {
                mImsReasonInfo = (ImsReasonInfo)bundle;
                if (DUBG) Log.i(TAG, "intent get ImsReasonInfo : " + mImsReasonInfo);
            } else {
                if (DUBG) Log.i(TAG, "intent didn't contain ImsReasonInfo");
                return ;
            }
            mHandlerUpdate.sendEmptyMessage(EVENT_UPDATE_IMS_DISCONNECTED_STATE);
        }
    }

}
