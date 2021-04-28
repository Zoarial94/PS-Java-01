package com.zoarial.iot.models;

import java.nio.ByteBuffer;
import java.util.UUID;

public class IoTPacketSection {
    enum PacketSectionType {
        STRING,
        INTEGER,
        BYTE,
        LONG,
        UUID,
        HEADER,
        BOOLEAN,
    }
    final private PacketSectionType type;
    final private String str;
    final private long num;
    final private long uuid_low;

    public IoTPacketSection(String str) {
        if (str.equals("ZIoT")) {
            type = PacketSectionType.HEADER;
        } else {
            type = PacketSectionType.STRING;
        }
        this.str = str;
        num = 0;
        uuid_low = 0;
    }

    public IoTPacketSection(byte b) {
        type = PacketSectionType.BYTE;
        num = (int)b & 0xFF;
        uuid_low = 0;
        str = "";
    }

    public IoTPacketSection(int i) {
        type = PacketSectionType.INTEGER;
        num = i;
        uuid_low = 0;
        str = "";
    }

    public IoTPacketSection(long i) {
        type = PacketSectionType.LONG;
        num = i;
        uuid_low = 0;
        str = "";
    }

    public IoTPacketSection(UUID uuid) {
        type = PacketSectionType.UUID;
        num = uuid.getMostSignificantBits();
        uuid_low = uuid.getLeastSignificantBits();
        str = "";
    }

    public IoTPacketSection(boolean b) {
        type = PacketSectionType.BOOLEAN;
        num = 0;
        uuid_low = 0;
        str = b ? "true" : "false";
    }

    public byte[] getByteList() {
        return switch (type) {
            case BYTE -> new byte[]{(byte) num};
            case LONG -> ByteBuffer.allocate(8).putLong(num).array();
            case UUID -> ByteBuffer.allocate(16).putLong(num).putLong(uuid_low).array();
            case BOOLEAN, STRING -> {
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

    public PacketSectionType getType() {
        return type;
    }

}
