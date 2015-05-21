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

import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TetherSettingsAccountHandler extends Handler {

    private Context mContext;
    private CharSequence mOriginalSummary;
    private TetherSettings mTetherSettings;

    public TetherSettingsAccountHandler(TetherSettings tetherSettings) {
        mTetherSettings = tetherSettings;
        mOriginalSummary = tetherSettings.getEnableWifiApSwitch().getSummary();
    }

    private void turnOnTethering() {
       String[] provisionApp = mTetherSettings.getProvisionApp();

       if (mTetherSettings.isProvisioningNeeded(provisionApp)) {
           Intent intent = new Intent(Intent.ACTION_MAIN);
           intent.setClassName(provisionApp[0], provisionApp[1]);
           intent.putExtra(TetherSettings.TETHER_CHOICE, mTetherSettings.getTetherChoice());
           mTetherSettings.startActivityForResult(intent, TetherSettings.PROVISION_REQUEST);
       } else {
           mTetherSettings.startTethering();
       }
    }

    private void setTetheringOff() {
        if(mTetherSettings.getTetherChoice() == TetherSettings.USB_TETHERING) {
            mTetherSettings.setUsbTethering(false);
        } else if(mTetherSettings.getTetherChoice() == TetherSettings.WIFI_TETHERING) {
            mTetherSettings.getEnableWifiApSwitch().setChecked(false);
            mTetherSettings.getEnableWifiApSwitch().setSummary(mOriginalSummary);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(Message message) {
        Log.d("AccountCheck","message.arg1 in AccountHandler:"+message.arg1);
        if(message.arg1 == 1) {
            turnOnTethering();
        } else {
            setTetheringOff();
        }
    }
}
