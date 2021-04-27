package com.zoarial.threads;

import java.io.*;
import java.net.Socket;

class inSocketWrapper {
    public final Socket inSocket;
    public final DataOutputStream out;
    public final DataInputStream in;

    public inSocketWrapper(Socket socket) {
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
}
