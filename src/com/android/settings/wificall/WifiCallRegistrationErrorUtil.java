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
package com.android.settings.wificall;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;

import com.android.settings.R;

public class WifiCallRegistrationErrorUtil {

    private static final String TAG = "WifiCallRegistrationErrorUtil";
    // Below is the registration error
    public static final int WIFI_CALLING_REGISTRATION_SUCCESS = 1;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_02 = 2;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_03 = 3;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_04 = 4;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_041 = 41;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_05 = 5;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_07 = 7;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_081 = 81;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_082 = 82;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_08 = 8;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_10 = 10;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_101 = 101;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_011 = 11;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_111 = 111;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_REG91 = 91;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_REG09 = 9;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_REG99 = 99;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_REG901 = 901;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_REG902 = 902;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_REG903 = 903;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_REG904 = 904;
    public static final int WIFI_CALLING_REGISTRATION_ERROR_REG905 = 905;

    public static int parserErrorCodeFromString(String value){
        if(value == null){
            return -1;
        }
        String regEx="[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(value);
        String errorcode = m.replaceAll("");
        if(!"".equals(errorcode)){
            return Integer.parseInt(errorcode);
        }else{
            return -1;
        }
    }

    public static boolean isWifiCallingRegistrationError(int errorcode){
        switch (errorcode) {
        case WIFI_CALLING_REGISTRATION_ERROR_02:
        case WIFI_CALLING_REGISTRATION_ERROR_03:
        case WIFI_CALLING_REGISTRATION_ERROR_04:
        case WIFI_CALLING_REGISTRATION_ERROR_041:
        case WIFI_CALLING_REGISTRATION_ERROR_05:
        case WIFI_CALLING_REGISTRATION_ERROR_07:
        case WIFI_CALLING_REGISTRATION_ERROR_081:
        case WIFI_CALLING_REGISTRATION_ERROR_082:
        case WIFI_CALLING_REGISTRATION_ERROR_08:
        case WIFI_CALLING_REGISTRATION_ERROR_10:
        case WIFI_CALLING_REGISTRATION_ERROR_101:
        case WIFI_CALLING_REGISTRATION_ERROR_011:
        case WIFI_CALLING_REGISTRATION_ERROR_111:
        case WIFI_CALLING_REGISTRATION_ERROR_REG91:
        case WIFI_CALLING_REGISTRATION_ERROR_REG09:
        case WIFI_CALLING_REGISTRATION_ERROR_REG99:
        case WIFI_CALLING_REGISTRATION_ERROR_REG901:
        case WIFI_CALLING_REGISTRATION_ERROR_REG902:
        case WIFI_CALLING_REGISTRATION_ERROR_REG903:
        case WIFI_CALLING_REGISTRATION_ERROR_REG904:
        case WIFI_CALLING_REGISTRATION_ERROR_REG905:
            return true;

        default:
            return false;
        }
    }

    public static int matchRegistrationError(int errcode, Context context){
        int summary = 0;
        int reason = 0;
        switch (errcode) {
        case WIFI_CALLING_REGISTRATION_SUCCESS:
            summary = R.string.wifi_calling_registration_success_for_user;
            reason = R.string.wifi_calling_registration_success_cause;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_02:
            summary = R.string.wifi_calling_registration_error02_for_user;
            reason = R.string.wifi_calling_registration_error_cause_02;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_03:
            summary = R.string.wifi_calling_registration_error03_for_user;
            reason = R.string.wifi_calling_registration_error_cause_03;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_04:
            summary = R.string.wifi_calling_registration_error04_for_user;
            reason = R.string.wifi_calling_registration_error_cause_04;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_041:
            summary = R.string.wifi_calling_registration_error041_for_user;
            reason = R.string.wifi_calling_registration_error_cause_041;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_05:
            summary = R.string.wifi_calling_registration_error05_for_user;
            reason = R.string.wifi_calling_registration_error_cause_05;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_07:
            summary = R.string.wifi_calling_registration_error07_for_user;
            reason = R.string.wifi_calling_registration_error_cause_07;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_08:
            summary = R.string.wifi_calling_registration_error08_for_user;
            reason = R.string.wifi_calling_registration_error_cause_08;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_081:
            summary = R.string.wifi_calling_registration_error081_for_user;
            reason = R.string.wifi_calling_registration_error_cause_081;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_082:
            summary = R.string.wifi_calling_registration_error082_for_user;
            reason = R.string.wifi_calling_registration_error_cause_082;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_10:
            summary = R.string.wifi_calling_registration_error10_for_user;
            reason = R.string.wifi_calling_registration_error_cause_10;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_011:
            summary = R.string.wifi_calling_registration_error011_for_user;
            reason = R.string.wifi_calling_registration_error_cause_011;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_101:
            summary = R.string.wifi_calling_registration_error101_for_user;
            reason = R.string.wifi_calling_registration_error_cause_101;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_111:
            summary = R.string.wifi_calling_registration_error111_for_user;
            reason = R.string.wifi_calling_registration_error_cause_111;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_REG09:
            summary = R.string.wifi_calling_registration_reg09_for_user;
            reason = R.string.wifi_calling_registration_reg09_cause;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_REG91:
            summary = R.string.wifi_calling_registration_reg91_for_user;
            reason = R.string.wifi_calling_registration_reg91_cause;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_REG99:
            summary = R.string.wifi_calling_registration_reg99_for_user;
            reason = R.string.wifi_calling_registration_reg99_cause;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_REG901:
            summary = R.string.wifi_calling_registration_reg901_for_user;
            reason = R.string.wifi_calling_registration_reg901_cause;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_REG902:
            summary = R.string.wifi_calling_registration_reg902_for_user;
            reason = R.string.wifi_calling_registration_reg902_cause;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_REG903:
            summary = R.string.wifi_calling_registration_reg903_for_user;
            reason = R.string.wifi_calling_registration_reg903_cause;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_REG904:
            summary = R.string.wifi_calling_registration_reg904_for_user;
            reason = R.string.wifi_calling_registration_reg904_cause;
            break;

        case WIFI_CALLING_REGISTRATION_ERROR_REG905:
            summary = R.string.wifi_calling_registration_reg905_for_user;
            reason = R.string.wifi_calling_registration_reg905_cause;
            break;

        default:
            summary = R.string.wifi_calling_registration_unknow_error_for_user;
            reason = R.string.wifi_calling_registration_unknow_cause;
            break;
        }
        if(reason != 0){
            String cause = context.getResources().getString(reason);
            Log.i(TAG, "Wifi calling registration error : " + cause);
        }
        return summary;
    }
}
