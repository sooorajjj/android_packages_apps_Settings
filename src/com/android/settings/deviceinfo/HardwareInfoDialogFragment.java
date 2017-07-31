/*
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright (C) 2020 Fairphone B.V.
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

package com.android.settings.deviceinfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HardwareInfoDialogFragment extends InstrumentedDialogFragment {

    public static final String TAG = "HardwareInfo";

    private static final String ASSEMBLY_NUMBER_FILE = "/persist/phoneid.bin";
    private static final String FILENAME_PROC_CPUINFO = "/proc/cpuinfo";

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DIALOG_SETTINGS_HARDWARE_INFO;
    }

    public static HardwareInfoDialogFragment newInstance() {
        final HardwareInfoDialogFragment fragment = new HardwareInfoDialogFragment();
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.hardware_info)
                .setPositiveButton(android.R.string.ok, null);
        final View content = LayoutInflater.from(builder.getContext())
                .inflate(R.layout.dialog_hardware_info, null /* parent */);
        // Model
        setText(content, R.id.model_label, R.id.model_value,
                DeviceModelPreferenceController.getDeviceModel());

        // Serial number
        setText(content, R.id.serial_number_label, R.id.serial_number_value, getSerialNumber());

        // Hardware rev
        setText(content, R.id.hardware_rev_label, R.id.hardware_rev_value,
                SystemProperties.get("ro.boot.hardware.revision"));

        // Assembly number
        setText(content, R.id.assembly_number_label, R.id.assembly_number_value,
                getAssemblyNumber());

        // Processor
        setText(content, R.id.processor_label, R.id.processor_value, getProcessorInfo());

        return builder.setView(content).create();
    }

    @VisibleForTesting
    void setText(View content, int labelViewId, int valueViewId, String value) {
        if (content == null) {
            return;
        }
        final View labelView = content.findViewById(labelViewId);
        final TextView valueView = content.findViewById(valueViewId);
        if (!TextUtils.isEmpty(value)) {
            labelView.setVisibility(View.VISIBLE);
            valueView.setVisibility(View.VISIBLE);
            valueView.setText(value);
        } else {
            labelView.setVisibility(View.GONE);
            valueView.setVisibility(View.GONE);
        }
    }

    @VisibleForTesting
    String getSerialNumber() {
        return Build.getSerial();
    }

    private static String getAssemblyNumber() {
        String assemblyNumber = null;
        FileInputStream input = null;

        try {
            input = new FileInputStream(new File(ASSEMBLY_NUMBER_FILE));
            final byte[] bytes = new byte[input.available()];

            input.read(bytes);
            assemblyNumber = new String(bytes, "ASCII");
        } catch (FileNotFoundException e) {
            Log.wtf(TAG, "Assembly number file not found", e);
        } catch(IOException e) {
            Log.wtf(TAG, "Could not read the assembly number file", e);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (Exception e) {
                Log.wtf(TAG, e);
            }
        }

        return assemblyNumber;
    }

    /**
     * Returns the Hardware value in /proc/cpuinfo, else returns "Unknown".
     * @return a string that describes the processor
     */
    private static String getProcessorInfo() {
        // Hardware : XYZ
        final String PROC_HARDWARE_REGEX = "Hardware\\s*:\\s*(.*)$"; /* hardware string */

        try {
            BufferedReader reader = new BufferedReader(new FileReader(FILENAME_PROC_CPUINFO));
            String cpuinfo;

            try {
                while (null != (cpuinfo = reader.readLine())) {
                    if (cpuinfo.startsWith("Hardware")) {
                        Matcher m = Pattern.compile(PROC_HARDWARE_REGEX).matcher(cpuinfo);
                        if (m.matches()) {
                            return m.group(1);
                        }
                    }
                }
                return "Unknown";
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            Log.e(TAG,
                "IO Exception when getting cpuinfo for Device Info screen",
                e);

            return "Unknown";
        }
    }
}
