package com.android.settings.adddevicesinfo.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AddDevicesInfoDBHelper extends SQLiteOpenHelper{
    private static final String DB_NAME = "alloweddevicesinfo.db";
    private static final int DB_VERSION = 1;

    public AddDevicesInfoDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL("CREATE TABLE IF NOT EXISTS devices" +
            "(_id INTEGER PRIMARY KEY AUTOINCREMENT, mac TEXT, dev_name TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
    }

}
