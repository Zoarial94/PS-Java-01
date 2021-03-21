package com.zoarial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

class inSocketHelper {
    public Socket inSocket;
    public PrintWriter out;
    public BufferedReader in;

    public inSocketHelper(Socket socket) throws IOException {
        inSocket = socket;
        try {
            out = new PrintWriter(inSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(inSocket.getInputStream()));
        } catch(IOException ex) {
            throw ex;
        }
    }
}

public class ServerServer {

    final String _hostname;
    final int _nodeType;
    final boolean _isVolatile;
    final int _serverPort;

    //Server can update these at runtime
    int _messageTimeout;
    int _pingTimeout;

    ServerSocket _outSocket;
    ArrayList<Socket> _inSockets;

    private void print(String str) {
        System.out.println("ServerServer: " + str);
    }

    public ServerServer(String hostname, int nodeType, Boolean isVolatile, int serverPort, int messageTimeout, int pingTimeout) {
        print("Initializing...");

        _hostname = hostname;
        _nodeType = nodeType;
        _isVolatile = isVolatile;
        _serverPort = serverPort;

        _messageTimeout = messageTimeout;
        _pingTimeout = pingTimeout;
    }

    public void start() throws IOException {
        try {
            _outSocket = new ServerSocket(_serverPort);
        } catch(IOException ex) {
            throw ex;
        }
    }


}
