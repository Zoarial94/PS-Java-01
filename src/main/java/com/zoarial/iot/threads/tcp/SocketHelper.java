package com.zoarial.iot.threads.tcp;

import com.zoarial.PrintBaseClass;
import me.zoarial.networkArbiter.ZoarialNetworkArbiter;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

public class SocketHelper extends PrintBaseClass {
    public final Socket inSocket;
    public final BufferedOutputStream rawOut;
    public final DataOutputStream out;
    public final BufferedInputStream rawIn;
    public final DataInputStream in;
    private final boolean encrypted;
    private final boolean local;
    private final ZoarialNetworkArbiter networkArbiter;

    public SocketHelper(Socket socket) {
        super("Socket Helper");
        inSocket = socket;
        networkArbiter = ZoarialNetworkArbiter.getInstance();
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

    public boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    public String readJson() throws IOException {
        StringBuilder json = new StringBuilder();

        byte b = in.readByte();
        if(b != '{') {
            return "";
        }
        json.append((char)b);
        int num = 1;
        while(num != 0) {
            b = in.readByte();
            if(b == '{') {
                num++;
            } else if (b == '}') {
                num--;
            }
            json.append((char)b);
        }

        // Read the 0 appended to the end
        if(in.readByte() != 0) {
            throw new RuntimeException("Expected null byte termination");
        }
        return json.toString();
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
