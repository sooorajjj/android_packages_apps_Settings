package com.android.settings.adddevicesinfo.db;

import java.lang.StringBuffer;

public class DevicesInfo {

    public int mId;
    public String mMac;
    public String mName;

    public DevicesInfo() {}

    public DevicesInfo(int id, String mac, String name) {
        this.mId = id;
        this.mMac = mac;
        this.mName = name;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer(mName);
        stringBuffer.append(" [");
        stringBuffer.append(mMac);
        stringBuffer.append("]");
        return stringBuffer.toString();
    }

    public String toShowString() {
        StringBuffer stringBuffer = new StringBuffer(mName);
        stringBuffer.append("  [");
        stringBuffer.append(mMac);
        stringBuffer.append("]");
        return stringBuffer.toString();
    }
}
