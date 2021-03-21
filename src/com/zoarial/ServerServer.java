package com.zoarial;

import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

class inSocketHelper implements Runnable {
    Thread t;
    Socket inSocket;
    PrintWriter out;
    BufferedReader in;
    String threadName;

    public inSocketHelper(Socket socket)  throws IOException {
        inSocket = socket;
        try {
            out = new PrintWriter(inSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(inSocket.getInputStream()));
        } catch(IOException ex) {
            throw ex;
        }
        this.start();
    }

    private void start() {
        if(t == null) {
            t = new Thread(this);
            t.start();
        }
    }

    @Override
    public void run() {

    }
}

/*
 *
 * Just accepts new connections in a non-blocking fashion
 *
 */
class ServerSocketHelper extends Thread {

    ServerSocket servSocket;
    DataInputStream in;
    boolean _running = true;
    boolean close = false;

    ArrayList<Socket> _inSockets = new ArrayList<>();

    void print(String str) {
        System.out.println("ServerSocketHelper: " + str);
    }

    public ServerSocketHelper(ServerSocket socket) {
        this.servSocket = socket;
    }

    public void run() {
        while(!close) {
            try {
                Socket tmp = servSocket.accept();
                synchronized (_inSockets) {
                    print("Accepting new socket.");
                    _inSockets.add(tmp);
                }
            } catch (IOException ex) {

            }
        }
    }

    public Socket getNextSocket() {
        Socket tmp;
        synchronized (_inSockets) {
            if (_inSockets.isEmpty()) {
                return null;
            }
            tmp = _inSockets.get(0);
            _inSockets.remove(0);
        }
        return tmp;
    }

    public boolean isNextSocketEmpty() {
        return _inSockets.isEmpty();
    }

    public void close() {
        close = true;
    }

}

class inSocketWrapper {
    public Socket inSocket;
    public PrintWriter out;
    public DataInputStream in;

    public inSocketWrapper(Socket socket)  {
        inSocket = socket;
        try {
            out = new PrintWriter(inSocket.getOutputStream(), true);
            in = new DataInputStream(new BufferedInputStream(inSocket.getInputStream()));
        } catch (IOException ex) {
            System.out.println("Something happened while creating inSocketWrapper. Exiting.");
            System.exit(-1);
        }
    }
}

public class ServerServer extends Thread{

    boolean close = false;
    
    final String _hostname;
    final int _nodeType;
    final boolean _isVolatile;
    final int _serverPort;

    //Server can update these at runtime
    int _messageTimeout;
    int _pingTimeout;

    ServerSocket _outSocket;
    ServerSocketHelper _serverSocketHelper;
    ArrayList<inSocketWrapper> _inSockets = new ArrayList<>();

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

    public void run() {
        try {
            print("Starting a server on port " + _serverPort + ".");
            _outSocket = new ServerSocket(_serverPort);
            _serverSocketHelper = new ServerSocketHelper(_outSocket);
            _serverSocketHelper.start();
        } catch(IOException ex) {
            System.out.println("Something happened when starting the server. Exiting.");
            System.exit(-1);
        }

        int sleepTime = 5000;
        while(!close) {
            try {
                while(!_serverSocketHelper.isNextSocketEmpty()) {
                    _inSockets.add(new inSocketWrapper(_serverSocketHelper.getNextSocket()));
                }

                for(inSocketWrapper socketWrapper : _inSockets) {
                    print(socketWrapper.in.readUTF());
                }

                print("Sleeping for " + sleepTime + ".");
                sleep(sleepTime);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
        
    }


}
