package com.zoarial.iot.threads.tcp;

import java.io.*;
import java.net.Socket;

class SocketHelper {
    public final Socket inSocket;
    public final DataOutputStream out;
    public final DataInputStream in;

    public SocketHelper(Socket socket) {
        inSocket = socket;
        DataOutputStream tempOut = null;
        DataInputStream tempIn = null;
        try {
            tempOut = new DataOutputStream(new BufferedOutputStream(inSocket.getOutputStream()));
            tempIn = new DataInputStream(new BufferedInputStream(inSocket.getInputStream()));
        } catch (IOException ex) {
            System.out.println("Something happened while creating inSocketWrapper. Exiting.");
            System.exit(-1);
        }
        out = tempOut;
        in = tempIn;
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
