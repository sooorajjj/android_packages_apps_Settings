/*
Copyright (c) 2015, The Linux Foundation. All rights reserved.

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
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;

public class UsbSecurityReceiver extends BroadcastReceiver {
    private static final String TAG = "UsbSecurity";
    private static boolean mIsLastConnected = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_STATE.equals(action)) {
            boolean isConnected = intent.getExtras().getBoolean(UsbManager.USB_CONNECTED, false);
            boolean isSimCardInserted = SystemProperties.getBoolean(
                    "persist.sys.sim.activate", false);
            boolean isUsbSecurityEnable = SystemProperties.getBoolean(
                    "persist.sys.usb.security", false);
            Log.d(TAG, "ACTION_USB_STATE" + !isSimCardInserted + isUsbSecurityEnable + isConnected
                    + mIsLastConnected);
            if ((!isSimCardInserted) && isUsbSecurityEnable && isConnected && !mIsLastConnected) {
                Intent UsbSecurityActivityIntent = new Intent();
                UsbSecurityActivityIntent.setClass(context, UsbSecurityActivity.class);
                UsbSecurityActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(UsbSecurityActivityIntent);
            }
            mIsLastConnected = isConnected;
        } else if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
            String stateExtra = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
            if (IccCardConstants.INTENT_VALUE_ICC_READY.equals(stateExtra)
                    || IccCardConstants.INTENT_VALUE_ICC_IMSI.equals(stateExtra)
                    || IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(stateExtra)) {
                SystemProperties.set("persist.sys.sim.activate", "true");
            }
        }
    }
}
