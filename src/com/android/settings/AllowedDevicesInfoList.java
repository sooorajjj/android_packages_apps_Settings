package com.android.settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.adddevicesinfo.db.AddDevicesInfoDBManager;
import com.android.settings.adddevicesinfo.db.DevicesInfo;

public class AllowedDevicesInfoList extends Fragment {

    private ListView mAllowedDevicesInfoList;
    private AddDevicesInfoDBManager dbManager;
    private AllowedDevicesAdapter mAdapter;
    private WifiManager mWifiManager;

    private static final int ADD_FLAG = 0;
    private static final int UPDATE_FLAG = 1;
    private final static String HOSTAPD_ACCEPT_FILE = "/data/hostapd/hostapd.accept";

    private final static String TAG = "AllowedDevicesInfoList";
    private final static boolean DBG = false;

    @Override
    public void onCreate(Bundle icicle) {
        // TODO Auto-generated method stub
        super.onCreate(icicle);
        dbManager = new AddDevicesInfoDBManager(getActivity());
        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        View view = inflater.inflate(R.layout.wifi_allowed_devices_info_list, null);
        mAllowedDevicesInfoList = (ListView) view
                .findViewById(R.id.allowed_devices_info_list);
        mAdapter = new AllowedDevicesAdapter();
        mAllowedDevicesInfoList.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.allowed_devices_info_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        switch (item.getItemId()) {
        case R.id.allowed_devices_info_menu_add:
            AddDeviceInfoDialog mDialog = new AddDeviceInfoDialog(getActivity(),
                ADD_FLAG, -1, -1);
            mDialog.setTitle(R.string.add_device_dialog_title);
            mDialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final class ViewHolder {
        public TextView mAllowedDeviceText;
    }

    private class AllowedDevicesAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return dbManager.getDevicesCount();
        }

        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(final int position, View view, ViewGroup arg2) {
            // TODO Auto-generated method stub
            ViewHolder holder = null;
            if (view == null) {
                holder = new ViewHolder();
                mInflater = LayoutInflater.from(getActivity());
                view = mInflater.inflate(R.layout.wifi_allowed_devices_info_item,
                        null);
                holder.mAllowedDeviceText = (TextView) view
                        .findViewById(R.id.wifi_allowed_device_info);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            List<DevicesInfo> devices = dbManager.queryDevicesInfo();
            final DevicesInfo device = devices.get(position);
            holder.mAllowedDeviceText.setText(device.toShowString());
            holder.mAllowedDeviceText.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    // TODO Auto-generated method stub
                    AddDeviceInfoDialog mDialog = new AddDeviceInfoDialog(
                            getActivity(), UPDATE_FLAG, position,
                            device.mId);
                    mDialog.setTitle(R.string.add_device_dialog_title);
                    mDialog.show();
                }
            });
            holder.mAllowedDeviceText
                .setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View arg0) {
                    // TODO Auto-generated method stub
                    DeleteDialog mDialog = new DeleteDialog(
                            getActivity(), device.mId, device.mMac);
                    mDialog.setTitle(getActivity().getResources()
                            .getString(R.string.delete_device)
                            + " "
                            + device);
                    mDialog.show();
                    return true;
                }
            });
            return view;
        }
    }

    private class AddDeviceInfoDialog extends Dialog {

        private Button mCancel;
        private Button mOk;
        private EditText mac1Edit;
        private EditText mac2Edit;
        private EditText mac3Edit;
        private EditText mac4Edit;
        private EditText mac5Edit;
        private EditText mac6Edit;
        private EditText mEditDevName;
        private String mac1;
        private String mac2;
        private String mac3;
        private String mac4;
        private String mac5;
        private String mac6;
        private int mFlag;
        private int mId;
        private int mPosition;
        private String oldMac;

        public AddDeviceInfoDialog(Context context, int flag, int position, int id) {
            super(context);
            // TODO Auto-generated constructor stub
            mFlag = flag;
            mId = id;
            mPosition = position;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            // TODO Auto-generated method stub
            super.onCreate(savedInstanceState);
            setContentView(R.layout.add_device_info_dialog);
            mac1Edit = (EditText) findViewById(R.id.mac1);
            mac2Edit = (EditText) findViewById(R.id.mac2);
            mac3Edit = (EditText) findViewById(R.id.mac3);
            mac4Edit = (EditText) findViewById(R.id.mac4);
            mac5Edit = (EditText) findViewById(R.id.mac5);
            mac6Edit = (EditText) findViewById(R.id.mac6);
            mEditDevName = (EditText) findViewById(R.id.dev_name);
            if (mFlag == UPDATE_FLAG) {
                DevicesInfo dev = dbManager.queryDevicesInfo().get(mPosition);
                oldMac = dev.mMac;
                String[] singleMac = oldMac.split(":");
                mac1Edit.setText(singleMac[0]);
                mac2Edit.setText(singleMac[1]);
                mac3Edit.setText(singleMac[2]);
                mac4Edit.setText(singleMac[3]);
                mac5Edit.setText(singleMac[4]);
                mac6Edit.setText(singleMac[5]);
                mEditDevName.setText(dev.mName);
            }

            mac1Edit.addTextChangedListener(new MyWatcher(mac2Edit));
            mac2Edit.addTextChangedListener(new MyWatcher(mac3Edit));
            mac3Edit.addTextChangedListener(new MyWatcher(mac4Edit));
            mac4Edit.addTextChangedListener(new MyWatcher(mac5Edit));
            mac5Edit.addTextChangedListener(new MyWatcher(mac6Edit));

            mCancel = (Button) findViewById(R.id.add_device_cancel);
            mOk = (Button) findViewById(R.id.add_device_ok);
            mCancel.setOnClickListener(new android.view.View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    // TODO Auto-generated method stub
                    AddDeviceInfoDialog.this.dismiss();
                }
            });
            mOk.setOnClickListener(new android.view.View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    // TODO Auto-generated method stub
                    if (isValidMac()) {
                        if (mFlag == UPDATE_FLAG) {
                            dbManager.updateDeviceInfo(mId,
                                getMacString(), mEditDevName.getText().toString());
                            //update to hostapd.accept
                            updateHostAdpAccept(oldMac, getMacString());
                        } else {
                            dbManager.addDeviceInfo(getMacString(), mEditDevName.getText().toString());
                            //Add to hostapd.accept
                            addHostAdpAccept(getMacString());
                        }
                        setWifiAp();
                        mAdapter.notifyDataSetChanged();
                    } else {
                        // notice user it is not a valid mac.
                    }
                    AddDeviceInfoDialog.this.dismiss();
                }
            });
        }

        private class MyWatcher implements TextWatcher {
            public MyWatcher(EditText e) {
                mNext = e;
            }
            public void afterTextChanged(Editable s) {
            }
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(
                CharSequence s, int start, int before, int count) {
                if (isHex(s.toString())) {
                    mNext.setSelectAllOnFocus(true);
                    mNext.requestFocus();
                }
            }
            private EditText mNext;
        }

        private String getMacString() {
            mac1 = mac1Edit.getText().toString().toLowerCase();
            mac2 = mac2Edit.getText().toString().toLowerCase();
            mac3 = mac3Edit.getText().toString().toLowerCase();
            mac4 = mac4Edit.getText().toString().toLowerCase();
            mac5 = mac5Edit.getText().toString().toLowerCase();
            mac6 = mac6Edit.getText().toString().toLowerCase();
            return mac1 + ":" + mac2 + ":" + mac3 + ":" + mac4 + ":" + mac5
                    + ":" + mac6;
        }

        private boolean isValidMac() {
            getMacString();
            return (isHex(mac1) && isHex(mac2) && isHex(mac3)
                && isHex(mac4) && isHex(mac5) && isHex(mac6));
        }

        private boolean isHex(String s) {
            return ((s.length() == 2)
                && isHex(s.charAt(0)) && isHex(s.charAt(1)));
        }

        private boolean isHex(char c) {
            return ((c >= '0' && c <= '9') ||
                (c >= 'a' && c <= 'f'));
        }
    }

    private class DeleteDialog extends Dialog {

        private int mId;
        private Button mCancel;
        private Button mOk;
        private String mMac;

        public DeleteDialog(Context context, int id, String mac) {
            super(context);
            // TODO Auto-generated constructor stub
            mId = id;
            mMac = mac;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            // TODO Auto-generated method stub
            super.onCreate(savedInstanceState);
            setContentView(R.layout.delete_device_info_dialog);
            mCancel = (Button) findViewById(R.id.delete_device_cancel);
            mOk = (Button) findViewById(R.id.delete_device_ok);
            mCancel.setOnClickListener(new android.view.View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    // TODO Auto-generated method stub
                    DeleteDialog.this.dismiss();
                }
            });
            mOk.setOnClickListener(new android.view.View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    // TODO Auto-generated method stub
                    dbManager.deleteDeviceInfo(mId);
                    //Delete from hostapd.accept
                    deleteHostAdpAccept(mMac);
                    DeleteDialog.this.dismiss();
                    setWifiAp();
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private void addHostAdpAccept(String mac){
        if (DBG) Log.v(TAG, "addHostAdpAccept: " + mac);
        String content = "";
        File file = new File(HOSTAPD_ACCEPT_FILE);
        if (file.isFile() && file.exists()){
            try {
                InputStreamReader in = new InputStreamReader(new FileInputStream(file));
                BufferedReader reader = new BufferedReader(in);
                String line;
                while ((line = reader.readLine()) != null){
                    content += (line + "\n") ;
                }
                if (DBG) Log.v(TAG, "content: " + content);
                in.close();
                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file));
                BufferedWriter writer = new BufferedWriter(out);
                content = content + mac;
                writer.write(content);
                writer.close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void updateHostAdpAccept(String oldMac, String newMac){
        if (DBG) Log.v(TAG, "updateHostAdpAccept: " + oldMac + ", " + newMac);
        String content = "";
        File file = new File(HOSTAPD_ACCEPT_FILE);
        if (file.isFile() && file.exists()){
            try {
                InputStreamReader in = new InputStreamReader(new FileInputStream(file));
                BufferedReader reader = new BufferedReader(in);
                String line;
                while ((line = reader.readLine()) != null){
                    content += (line + "\n") ;
                }
                if (DBG) Log.v(TAG, "content: " + content);
                in.close();
                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file));
                BufferedWriter writer = new BufferedWriter(out);
                content = content.replace(oldMac, newMac);
                writer.write(content);
                writer.close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void deleteHostAdpAccept(String mac){
        if (DBG) Log.v(TAG, "deleteHostAdpAccept: " + mac);
        String content = "";
        File file = new File(HOSTAPD_ACCEPT_FILE);
        if (file.isFile() && file.exists()){
            try {
                InputStreamReader in = new InputStreamReader(new FileInputStream(file));
                BufferedReader reader = new BufferedReader(in);
                String line;
                while ((line = reader.readLine()) != null){
                    content += (line + "\n") ;
                }
                if (DBG) Log.v(TAG, "content: " + content);
                in.close();
                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file));
                BufferedWriter writer = new BufferedWriter(out);
                content = content.replace(mac+"\n", "");
                writer.write(content);
                writer.close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void setWifiAp(){
        if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED){
            mWifiManager.setWifiApEnabled(null, false);
            mWifiManager.setWifiApEnabled(null, true);
        }
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        dbManager.closeDB();
    }

}
