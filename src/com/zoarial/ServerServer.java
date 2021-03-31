package com.zoarial;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

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

    public void print(String str) {
        System.out.println("DatagramSocketHelper: " + str);
    }

    public void run() {
        print("Starting thread...");
        while(!close) {
            DatagramPacket dp = new DatagramPacket(new byte[BUF_SIZE], BUF_SIZE);
            try {
                _ds.receive(dp);
            } catch (IOException e) {
                e.printStackTrace();
            }
            print("Received packet, adding to queue.");
            synchronized (queue) {
                queue.add(dp);
            }
        }
        print("Finished thread.");
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
    final InetAddress _serverIP;

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

    private static <T> void println(T var) {
        System.out.println("ServerServer: " + var);
    }

    private static void println() {
        System.out.println();
    }


    public ServerServer(String hostname, int nodeType, Boolean isVolatile, int serverPort, int messageTimeout, int pingTimeout, boolean isHeadCapable) {
        println("Initializing...");

        _hostname = hostname;
        _nodeType = nodeType;
        _isVolatile = isVolatile;
        _serverPort = serverPort;
        _isHeadCapable = isHeadCapable;

        _messageTimeout = messageTimeout;
        _pingTimeout = pingTimeout;

        Enumeration<NetworkInterface> n = null;
        try {
            n = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        boolean found = false;
        InetAddress tmp = null;
        for (; n.hasMoreElements() && !found;)
        {
            NetworkInterface e = n.nextElement();
            if(!e.getName().equals("eth0")) {
                continue;
            }
            Enumeration<InetAddress> a = e.getInetAddresses();
            for (; a.hasMoreElements() && !found;)
            {

                InetAddress addr = a.nextElement();
                System.out.println(e.getName() + ":  " + addr.getHostAddress());
                try {
                    if(!addr.equals(InetAddress.getByAddress(new byte[]{127, 0, 0, 1})) && (addr instanceof Inet4Address)) {
                        tmp = addr;
                        found = true;
                    }
                } catch (UnknownHostException unknownHostException) {
                    unknownHostException.printStackTrace();
                }
            }
        }

        _serverIP = tmp;
    }

    public void run() {

        if(_isHeadCapable) {
            runHeadCapable();
        } else {
            runNotHeadCapable();
        }
        
    }

    private void runHeadCapable() {
        /*
         *
         * SETUP
         *
         */
        try {
            println("Starting a server on port " + _serverPort + ".");
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

        /*
         *
         * LOOP
         *
         */
        int sleepTime = 5000;
        DatagramPacket dp;
        while(!close) {
            try {
                /*
                 *
                 * UDP Handler Loop
                 *
                 */
                while(!_datagramSocketHelper.isNextDataEmpty()) {
                    dp = _datagramSocketHelper.getNextData();
                    String str = buildString(dp.getData()).toString();
                    println("Datagram Packet: " + str);
                    HeadUDPHandler(dp);
                }

                /*
                 *
                 * TCP Handler Loop
                 *
                 */
                while(!_serverSocketHelper.isNextSocketEmpty()) {
                    _inSockets.add(new inSocketWrapper(_serverSocketHelper.getNextSocket()));
                }

                for(inSocketWrapper socketWrapper : _inSockets) {
                    println("Socket Packet: " + socketWrapper.in.readUTF());
                }

                println("Sleeping for " + sleepTime + ".");
                sleep(sleepTime);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void runNotHeadCapable() {

        try {
            _ds = new DatagramSocket(_serverPort);
        } catch (SocketException e) {
            e.printStackTrace();
        }

       //Find head, either by reading from a save file, or a broadcast.
        while(!close && true) {

            byte[] buf = new byte[32];
            System.arraycopy("ZIoT".getBytes(), 0, buf, 0, 4);
            buf[7] = (byte)_nodeType;
            System.arraycopy(_hostname.getBytes(), 0, buf, 8, Math.min(24, _hostname.length()));

            //printArray(buf);

            byte[] addr = {10, 94, 50, (byte) 146};
            println("Sending packet...");
            DatagramPacket dp;
            try {
                dp = new DatagramPacket(buf, buf.length, InetAddress.getByAddress(addr), _serverPort);
                _ds.send(dp);
            } catch (IOException e) {
                e.printStackTrace();
            }

            dp = new DatagramPacket(new byte[65535], 65535);
            try {
                println("Waiting for response...");
                _ds.setSoTimeout(30000);
                _ds.receive(dp);
                byte[] data = dp.getData();
                if(new String(data, 0, 4).equals("ZIoT")) {
                    println("Packet is Z-IoT");
                }
                println("Length: " + dp.getLength());

                InetAddress[] headAddrs = { InetAddress.getByAddress(Arrays.copyOfRange(data, 4, 8)),
                                            InetAddress.getByAddress(Arrays.copyOfRange(data, 8, 12)),
                                            InetAddress.getByAddress(Arrays.copyOfRange(data, 12, 16))};

                printArray(data, dp.getLength());

                for(InetAddress i : headAddrs) {
                    printArray(i.getAddress());
                }



            } catch (IOException e) {
                //e.printStackTrace();
            }

        }

        //Do logic
        while(!close) {

            try {
                sleep(30000);
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

    static void printArray(byte[] arr) {
        printArray(arr, arr.length);
    }

    static void printArray(byte[] arr, int len) {
        String prefix = "Array: ";
        for(int i = 0; i < len; i++) {
            if(i == 0) {
                System.out.print(prefix);
            } else if (i % 8 == 0) {
                System.out.print("\n" + prefix);
            }

            System.out.print((char)arr[i]);
            System.out.print("(" + Integer.toUnsignedString(arr[i]) + ") ");
        }
        System.out.println();
    }

    void HeadUDPHandler(DatagramPacket dp) {

        byte[] data = dp.getData();
        if(new String(data, 0, 4).equals("ZIoT")) {
            println("Is Z-IoT Packet.");
        } else {
            println("Is not Z-IoT packet.");
            return;
        }

        int node = data[7];
        String hostname = new String(data, 8, 24);

        println("Node type is: " + node);
        println("Hostname is: " + hostname);

        byte[] response = new byte[16];
        System.arraycopy("ZIoT".getBytes(), 0, response, 0, 4);

        System.arraycopy(_serverIP.getAddress(), 0, response, 4, 4);


        DatagramPacket dpResponse = new DatagramPacket(response, response.length, dp.getAddress(), _serverPort);


        try {
            _ds.send(dpResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void NonHeadUDPHandler(DatagramPacket dp) {

    }

    void TCPHandler() {

    }

}
