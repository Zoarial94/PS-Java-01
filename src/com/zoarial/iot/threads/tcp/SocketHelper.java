package com.zoarial.iot.threads.tcp;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.models.IoTSession;
import com.zoarial.iot.models.actions.IoTAction;
import com.zoarial.iot.models.actions.IoTActionArgument;
import com.zoarial.iot.models.actions.IoTActionArgumentList;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.UUID;

class SocketHelper extends PrintBaseClass {
    public final Socket inSocket;
    public final BufferedOutputStream rawOut;
    public final DataOutputStream out;
    public final BufferedInputStream rawIn;
    public final DataInputStream in;
    private final boolean encrypted;
    private final boolean local;

    public SocketHelper(Socket socket) {
        super("Socket Helper");
        inSocket = socket;
        DataOutputStream tempOut = null;
        DataInputStream tempIn = null;
        BufferedInputStream tempRawIn = null;
        BufferedOutputStream tempRawOut = null;

        if(socket.getRemoteSocketAddress().toString().startsWith("/127.")) {
            encrypted = true; // Its a local connection. Same effect as encryption.
            local = true;
        } else {
            encrypted = false;
            local = false;
        }

        try {
            tempRawOut = new BufferedOutputStream(inSocket.getOutputStream());
            tempOut = new DataOutputStream(tempRawOut);
            tempRawIn = new BufferedInputStream(inSocket.getInputStream());
            tempIn = new DataInputStream(tempRawIn);
        } catch (IOException ex) {
            System.out.println("Something happened while creating inSocketWrapper. Exiting.");
            System.exit(-1);
        }
        out = tempOut;
        in = tempIn;
        rawOut = tempRawOut;
        rawIn = tempRawIn;
    }

    public String readKey() throws IOException {
        StringBuilder str = new StringBuilder();
        byte b = in.readByte();
        while(b != ':') {
            str.append((char)b);
            b = in.readByte();
        }
        return str.toString();
    }

    public String readValue() throws  IOException {
        StringBuilder str = new StringBuilder();
        byte b = in.readByte();
        while(b != ',' && b != '.') {
            str.append((char)b);
            b = in.readByte();
        }
        return str.toString();
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

    public IoTActionArgumentList readArgumentList(IoTAction action) throws IOException {

        IoTActionArgumentList argumentList = new IoTActionArgumentList();

        byte b = in.readByte();
        while(b != '.') {
            StringBuilder str = new StringBuilder();
            while (b != ',' && b != '.') {
                str.append((char) b);
                b = in.readByte();
            }
            argumentList.add(new IoTActionArgument(str.toString()));
        }
        return argumentList;
    }


    public boolean isEncrypted() {
        return encrypted;
    }

    public boolean isLocal() {
        return local;
    }

    public boolean isClosed() {
        return inSocket.isClosed();
    }

    public void close() throws IOException {
        out.close();
        in.close();
        inSocket.close();
    }

}
