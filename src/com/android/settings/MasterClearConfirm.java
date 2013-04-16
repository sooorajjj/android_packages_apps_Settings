/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import com.android.internal.os.storage.ExternalStorageFormatter;
import com.android.internal.widget.LockPatternUtils;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import android.widget.Toast;
import android.content.DialogInterface;
import android.widget.EditText;
import java.io.RandomAccessFile;
import android.os.SystemProperties;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.app.AlertDialog;
import android.content.Context;

/**
 * Confirm and execute a reset of the device to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a final strongly-worded "THIS WILL ERASE EVERYTHING
 * ON THE PHONE" prompt.  If at any time the phone is allowed to go to sleep, is
 * locked, et cetera, then the confirmation sequence is abandoned.
 *
 * This is the confirmation screen.
 */
public class MasterClearConfirm extends Fragment {

    private View mContentView;
    private boolean mEraseSdCard;
    private boolean mErasePhoneStorage;
    private Button mFinalButton;
    private static final String ACTION_SET_NETWORKTYP_MASTER_CLEAR = "android.intent.action.SET_NETWORKTYPE_MASTER_CLEAR";

    private LockPatternUtils mLockPatternUtils;
    private String password ="000000";
    private boolean pwdChk = false;
    private View localView = null;
    private EditText localEditText = null;
    private static final String LOCK_PASSWORD_FILE = "/system/password.key";
    private boolean ctsTestFlg = false;
    
    /**
     * The user has gone through the multiple confirmation, so now we go ahead
     * and invoke the Checkin Service to reset the device to its factory-default
     * state (rebooting in the process).
     */
    private Button.OnClickListener mFinalClickListener = new Button.OnClickListener() {

        public void onClick(View v) {
            if (Utils.isMonkeyRunning()) {
                return;
            }
            if(ctsTestFlg){
                    localView = View.inflate(getActivity(), R.layout.mydiglog, null);
                    localEditText = (EditText)localView.findViewById(R.id.edittext);

                    AlertDialog.Builder localBuilder = new AlertDialog.Builder(getActivity());
                    localBuilder.setTitle(R.string.unlock_master_clear);
                    localBuilder.setView(localView);
                    localBuilder.setNegativeButton(R.string.dlg_cancel, null);
                    localBuilder.setPositiveButton(R.string.dlg_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    password = localEditText.getText().toString();
                                    String sLockPasswordFilename = android.os.Environment.getDataDirectory()
                                            .getAbsolutePath() + LOCK_PASSWORD_FILE;

                                    try {
                                        RandomAccessFile raf = new RandomAccessFile(sLockPasswordFilename, "r");
                                        final byte[] stored = new byte[(int) raf.length()];
                                        int got = raf.read(stored, 0, stored.length);
                                        raf.close();
                        /*
                                        if (got <= 0) {
                                            if (password.equals("000000")) {
                                                pwdChk = true;
                                            } else {
                                                pwdChk = false;
                                            }
                                        } else {
                                            pwdChk = mLockPatternUtils.checkPassword(password);
                                        }
                                         *///delete the adjust , use the default password-000000, need to modify later
                                    if (password.equals("000000")) {
                                        pwdChk = true;
                                    }else{
                                        pwdChk = false;
                                    }
                                } catch (FileNotFoundException ex) {
                                    if (password.equals("000000")) {
                                        pwdChk = true;
                                    } else {
                                        pwdChk = false;
                                    }
                                } catch (IOException ex) {
                                    if (password.equals("000000")) {
                                        pwdChk = true;
                                    } else {
                                        pwdChk = false;
                                    }
                                } catch (Exception ex) {
                                    if (password.equals("000000")) {
                                        pwdChk = true;
                                    } else {
                                        pwdChk = false;
                                    }
                                }

                                if (pwdChk) {
                                    getActivity().sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
                                } else {
                                    Toast.makeText(getActivity(), R.string.unlock_master_clear_error,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                        ).show();
            }else{
                if (mEraseSdCard && !mErasePhoneStorage) {
                    Intent intent = new Intent(ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET);
                    intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
                    getActivity().startService(intent);
                }else if (mEraseSdCard && mErasePhoneStorage) {
                    Intent intent = new Intent(PhoneStorageFormatter.FORMAT_ALL_AND_FACTORY_RESET);
                    getActivity().startService(intent);
                }else if (mErasePhoneStorage && !mEraseSdCard) {
                    Intent intent = new Intent(PhoneStorageFormatter.FORMAT_AND_FACTORY_RESET);
                    getActivity().startService(intent);

                } else {
                    getActivity().sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
                    // Intent handling is asynchronous -- assume it will happen soon.
                }
            }
        }
    };

    /**
     * Configure the UI for the final confirmation interaction
     */
    private void establishFinalConfirmationState() {
        mFinalButton = (Button) mContentView.findViewById(R.id.execute_master_clear);
        mFinalButton.setOnClickListener(mFinalClickListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.master_clear_confirm, null);
        establishFinalConfirmationState();
        return mContentView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mEraseSdCard = args != null ? args.getBoolean(MasterClear.ERASE_EXTERNAL_EXTRA) : false;
        mErasePhoneStorage = args != null ? args.getBoolean(MasterClear.ERASE_INTERNAL_EXTRA) : false;

        mLockPatternUtils = new LockPatternUtils(getActivity());
        if ( SystemProperties.get("ro.cta.test", "").equals("1")) {
            ctsTestFlg = true;
        } else {
            ctsTestFlg = false;
        }
    }
}
