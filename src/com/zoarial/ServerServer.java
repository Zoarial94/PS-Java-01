package com.zoarial;

import javax.xml.crypto.Data;
import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.charset.StandardCharsets;
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

class DatagramSocketHelper extends Thread {

    DatagramSocket _ds;
    ArrayList<DatagramPacket> queue = new ArrayList<>();
    boolean newData = false;
    boolean close = false;
    final int BUF_SIZE = 65535;

    DatagramSocketHelper(DatagramSocket ds) {
        _ds = ds;
    }

    public void close() {
        close = true;
    }

    public boolean isNewData() {
        return newData;
    }

    public boolean isNextDataEmpty() {
        return queue.isEmpty();
    }

    public DatagramPacket getNextData() {
        DatagramPacket tmp;
        synchronized (queue) {
            if (queue.isEmpty()) {
                return null;
            }

            tmp = queue.get(0);
            queue.remove(0);
        }
        return tmp;
    }

    public void run() {
        while(!close) {
            DatagramPacket dp = new DatagramPacket(new byte[BUF_SIZE], BUF_SIZE);
            try {
                _ds.receive(dp);
            } catch (IOException e) {
                e.printStackTrace();
            }

            synchronized (queue) {
                queue.add(dp);
            }
        }
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

public class ServerServer extends Thread {

    boolean close = false;
    
    final String _hostname;
    final int _nodeType;
    final boolean _isVolatile;
    final int _serverPort;
    final boolean _isHeadCapable;

    //Server can update these at runtime
    int _messageTimeout;
    int _pingTimeout;

    ServerSocket _outSocket;
    ServerSocketHelper _serverSocketHelper;
    ArrayList<inSocketWrapper> _inSockets = new ArrayList<>();

    DatagramSocket _ds;
    DatagramPacket _dp;
    byte[] _dpBuf = new byte[65535];
    DatagramSocketHelper _datagramSocketHelper;

    private void print(String str) {
        System.out.println("ServerServer: " + str);
    }

    public ServerServer(String hostname, int nodeType, Boolean isVolatile, int serverPort, int messageTimeout, int pingTimeout, boolean isHeadCapable) {
        print("Initializing...");

        _hostname = hostname;
        _nodeType = nodeType;
        _isVolatile = isVolatile;
        _serverPort = serverPort;
        _isHeadCapable = isHeadCapable;

        _messageTimeout = messageTimeout;
        _pingTimeout = pingTimeout;
    }

    public void run() {

        if(_isHeadCapable) {
            runHeadCapable();
        } else {
            runHeadIncapable();
        }
        
    }

    private void runHeadCapable() {
        try {
            print("Starting a server on port " + _serverPort + ".");
            _outSocket = new ServerSocket(_serverPort);
            _serverSocketHelper = new ServerSocketHelper(_outSocket);
            _serverSocketHelper.start();

            _ds = new DatagramSocket(_serverPort);
            _datagramSocketHelper = new DatagramSocketHelper(_ds);
            _datagramSocketHelper.start();

        } catch(IOException ex) {
            System.out.println("Something happened when starting the server. Exiting.");
            System.exit(-1);
        }

        int sleepTime = 5000;
        DatagramPacket dp;
        while(!close) {
            try {

                while(!_datagramSocketHelper.isNextDataEmpty()) {
                    dp = _datagramSocketHelper.getNextData();
                    String str = buildString(dp.getData()).toString();
                    print("Datagram Packet: " + str);
                }

                while(!_serverSocketHelper.isNextSocketEmpty()) {
                    _inSockets.add(new inSocketWrapper(_serverSocketHelper.getNextSocket()));
                }

                for(inSocketWrapper socketWrapper : _inSockets) {
                    print("Socket Packet: " + socketWrapper.in.readUTF());
                }

                print("Sleeping for " + sleepTime + ".");
                sleep(sleepTime);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void runHeadIncapable() {

        try {
            _ds = new DatagramSocket(_serverPort);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        _datagramSocketHelper = new DatagramSocketHelper(_ds);
        _datagramSocketHelper.start();

        while(!close) {
            byte[] buf = "Testing".getBytes();
            byte[] addr = {10, 94, 50, (byte) 146};
            print("Sending packet...");
            try {
                DatagramPacket dp = new DatagramPacket(buf, buf.length, InetAddress.getByAddress(addr), _serverPort);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            try {
                sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static StringBuilder buildString(byte[] a)
    {
        if (a == null)
            return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (a[i] != 0)
        {
            ret.append((char) a[i]);
            i++;
        }
        return ret;
    }

}
