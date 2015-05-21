package com.android.settings.adddevicesinfo.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class AddDevicesInfoDBManager {
    private AddDevicesInfoDBHelper helper;
    private SQLiteDatabase db;
    private static final String DEVICES_TABLE = "devices";
    private static final String[] COLUMN_NAME = { "_id", "mac", "dev_name" };
    private static final String COLUMN_KEY = "mac";
    private static final String COLUMN_DEVNAME = "dev_name";

    public AddDevicesInfoDBManager(Context context) {
        helper = new AddDevicesInfoDBHelper(context);
        db = helper.getWritableDatabase();
    }

    /**
     * Insert mac addr
     *
     * @param mac
     */

    public void addDeviceInfo(String mac, String name) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_KEY, mac);
        cv.put(COLUMN_DEVNAME, name);
        db.insert(DEVICES_TABLE, null, cv);
    }

    /**
     * Get all the allowed devices
     *
     * @return
     */
    public List<DevicesInfo> queryDevicesInfo() {
        ArrayList<DevicesInfo> devices = new ArrayList<DevicesInfo>();
        Cursor c = db.query(DEVICES_TABLE,
            COLUMN_NAME, null, null, null, null, null);
        if (c != null){
            while (c.moveToNext()) {
                DevicesInfo d = new DevicesInfo();
                d.mId = c.getInt(c.getColumnIndex("_id"));
                d.mMac = c.getString(c.getColumnIndex(COLUMN_KEY));
                d.mName = c.getString(c.getColumnIndex(COLUMN_DEVNAME));
                devices.add(d);
            }
            c.close();
        }
        return devices;
    }

    public String queryName(String mac) {
        String ret = null;
        Cursor c = db.query(DEVICES_TABLE,
            COLUMN_NAME, null, null, null, null, null);
        if (c != null){
            while (c.moveToNext()) {
                if (mac != null && mac.compareTo(
                    c.getString(c.getColumnIndex(COLUMN_KEY))) == 0) {
                    ret = c.getString(c.getColumnIndex(COLUMN_DEVNAME));
                    break;
                }
            }
            c.close();
        }
        return ret;
    }

    /**
     * Get all the allowed devices count.
     *
     * @return
     */
    public int getDevicesCount() {
        Cursor c = db.query(DEVICES_TABLE,
            COLUMN_NAME, null, null, null, null, null);
        int i = 0;
        if (c != null){
            i = c.getCount();
            c.close();
        }
        return i;
    }

    /**
     * Delete the device
     *
     * @param id
     */
    public void deleteDeviceInfo(int id) {
        db.delete(DEVICES_TABLE, "_id = ?", new String[] { String.valueOf(id) });
    }

    /**
     * Update the device
     *
     * @param id
     * @param mac
     */
    public void updateDeviceInfo(int id, String mac, String name) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_KEY, mac);
        cv.put(COLUMN_DEVNAME, name);
        db.update(DEVICES_TABLE, cv, "_id = ?",
            new String[] { String.valueOf(id) });
    }

    /**
     * closeDB
     */
    public void closeDB() {
        db.close();
    }
}
