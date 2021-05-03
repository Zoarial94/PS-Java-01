package com.zoarial.iot.models.actions;

import com.zoarial.iot.models.IoTBasicType;

public class IoTActionArgument {
    private String str = "";
    private long l = 0;
    private boolean b = false;

    public IoTActionArgument(String str) {
        this.str = str;
    }

    public IoTActionArgument(int i) {
        l = i;
    }

    public IoTActionArgument(long l) {
        this.l = l;
    }

    public IoTActionArgument(boolean b) {
        this.b = b;
    }

    public IoTActionArgument(byte b) {
        l = (long)b & 0xFF;
    }

    public String getString() {
        return str;
    }

    public int getInt() {
        return (int) l;
    }

    public long getLong() {
        return l;
    }

    public byte getByte() {
        return (byte) l;
    }

    public boolean getBool() {
        return b;
    }


}
