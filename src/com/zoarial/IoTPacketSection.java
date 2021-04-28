package com.zoarial;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IoTPacketSection {
    enum PacketSectionType {
        STRING,
        INTEGER,
        BYTE,
        LONG,
        UUID,
        HEADER,
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

    public byte[] getByteList() {
        byte[] byteArr;
        switch (type) {
            case BYTE:
                byteArr =  new byte[] {(byte)num};
                break;
            case LONG:
                byteArr =  ByteBuffer.allocate(8).putLong(num).array();
                break;
            case UUID:
                byteArr =  ByteBuffer.allocate(16).putLong(num).putLong(uuid_low).array();
                break;
            case STRING:
                byte[] strArr = str.getBytes();
                int len = strArr.length;
                byteArr = new byte[strArr.length+1];
                System.arraycopy(strArr, 0, byteArr, 0, len);
                byteArr[len] = 0; // Set null byte
                break;
            case INTEGER:
                byteArr =  ByteBuffer.allocate(4).putInt((int)num).array();
                break;
            case HEADER:
                byteArr = str.getBytes();
                break;
            default:
                byteArr = new byte[0];
        }
        return byteArr;
    }

    public PacketSectionType getType() {
        return type;
    }

}
