package com.zoarial.iot.network;

import com.zoarial.iot.model.IoTBasicType;

import java.nio.ByteBuffer;
import java.util.UUID;

public class IoTPacketSection {
    final private IoTBasicType type;
    final private String str;
    final private long num;
    final private long uuid_low;

    public IoTPacketSection(String str) {
        if (str.equals("ZIoT")) {
            type = IoTBasicType.HEADER;
        } else {
            type = IoTBasicType.STRING;
        }
        this.str = str;
        num = 0;
        uuid_low = 0;
    }

    public IoTPacketSection(byte b) {
        type = IoTBasicType.BYTE;
        num = (int)b & 0xFF;
        uuid_low = 0;
        str = "";
    }

    public IoTPacketSection(int i) {
        type = IoTBasicType.INTEGER;
        num = i;
        uuid_low = 0;
        str = "";
    }

    public IoTPacketSection(long i) {
        type = IoTBasicType.LONG;
        num = i;
        uuid_low = 0;
        str = "";
    }

    public IoTPacketSection(UUID uuid) {
        type = IoTBasicType.UUID;
        num = uuid.getMostSignificantBits();
        uuid_low = uuid.getLeastSignificantBits();
        str = "";
    }

    public IoTPacketSection(boolean b) {
        type = IoTBasicType.BOOLEAN;
        num = 0;
        uuid_low = 0;
        str = b ? "true" : "false";
    }

    public byte[] getByteList() {
        return switch (type) {
            case BYTE -> new byte[]{(byte) num};
            case LONG -> ByteBuffer.allocate(8).putLong(num).array();
            case UUID -> ByteBuffer.allocate(16).putLong(num).putLong(uuid_low).array();
            //TODO: Decided if i want to add JSONObject to hold the JSON data
            case BOOLEAN, STRING, JSON -> {
                byte[] strArr = str.getBytes();
                int len = strArr.length;
                byte[] byteArr = new byte[strArr.length + 1];
                System.arraycopy(strArr, 0, byteArr, 0, len);
                byteArr[len] = 0; // Set null byte
                yield byteArr;
            }
            case INTEGER -> ByteBuffer.allocate(4).putInt((int) num).array();
            case HEADER -> str.getBytes();
        };
    }

    public IoTBasicType getType() {
        return type;
    }

}
