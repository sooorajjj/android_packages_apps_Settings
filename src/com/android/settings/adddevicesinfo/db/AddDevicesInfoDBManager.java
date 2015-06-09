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
