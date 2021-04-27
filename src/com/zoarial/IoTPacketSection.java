package com.zoarial;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

enum PacketSectionType {
    STRING,
    INTEGER,
    CHAR,
    LONG,
    UUID,
}
public class IoTPacketSection {
    final private PacketSectionType type;
    final private String str;
    final private long num;
    final private long uuid_low;

    public IoTPacketSection(String str) {
        type = PacketSectionType.STRING;
        this.str = str;
        num = 0;
        uuid_low = 0;
    }

    public IoTPacketSection(char c) {
        type = PacketSectionType.CHAR;
        num = ((int)c) & 0xFF;
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
            case CHAR:
                byteArr =  new byte[] {(byte)num};
                break;
            case LONG:
                byteArr =  ByteBuffer.allocate(8).putLong(num).array();
                break;
            case UUID:
                byteArr =  ByteBuffer.allocate(16).putLong(num).putLong(uuid_low).array();
                break;
            case STRING:
                byteArr =  str.getBytes();
                break;
            case INTEGER:
                byteArr =  ByteBuffer.allocate(4).putInt((int)num).array();
                break;
            default:
                byteArr = new byte[0];
        }
        return byteArr;
    }

}
