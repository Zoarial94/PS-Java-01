package com.zoarial.iot.threads.udp;

import com.zoarial.PrintBaseClass;

import java.io.*;
import java.net.DatagramPacket;
import java.util.UUID;

public class DatagramPacketHelper extends PrintBaseClass {
    public final BufferedInputStream rawIn;
    public final DataInputStream in;

    public DatagramPacketHelper(DatagramPacket dp) {
        super("Datagram Packet Helper");
        rawIn = new BufferedInputStream(new ByteArrayInputStream(dp.getData()));
        in = new DataInputStream(rawIn);
    }

    public String readString() throws IOException {
        StringBuilder str = new StringBuilder();
        byte b = in.readByte();
        while(b != 0) {
            str.append((char)b);
            b = in.readByte();
        }
        return str.toString();
    }

    public UUID readUUID() throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }

    public int readInt() throws IOException {
        return in.readInt();
    }

    public long readLong() throws IOException {
        return in.readLong();
    }

    public byte readByte() throws IOException {
        return in.readByte();
    }

    public byte[] readBytes(int i) throws IOException {
        return in.readNBytes(i);
    }
}

