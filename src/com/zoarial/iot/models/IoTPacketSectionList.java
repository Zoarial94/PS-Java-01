package com.zoarial.iot.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IoTPacketSectionList extends ArrayList<IoTPacketSection> {

    public IoTPacketSectionList() {
        super();
    }

    public IoTPacketSectionList(int initialLength) {
        super(initialLength);
    }

    public void add(String str) {
        super.add(new IoTPacketSection(str));
    }

    public void add(int i) {
        super.add(new IoTPacketSection(i));
    }

    public void add(byte b) {
        super.add(new IoTPacketSection(b));
    }

    public void add(long l) {
        super.add(new IoTPacketSection(l));
    }

    public void add(UUID uuid) {
        super.add(new IoTPacketSection(uuid));
    }

    public byte[] getNetworkResponse() {
        byte[] fullBytesArr;
        int len = 0;

        for(IoTPacketSection section : this) {
            len += section.getByteList().length;
        }

        fullBytesArr = new byte[len];
        len = 0;
        for(IoTPacketSection section : this) {
            System.arraycopy(section.getByteList(), 0, fullBytesArr, len, section.getByteList().length);
            len += section.getByteList().length;
        }

        return fullBytesArr;
    }
}
