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

    public void run() {

    }
}

/*
 *
 * Just accepts new connections in a non-blocking fashion
 *
 */
class ServerSocketHelper extends PrintBaseClass implements Runnable {

    ServerSocket servSocket;
    boolean close = false;

    //  Final doesn't mean const
    //  Final means it can't be reassigned and makes for a good concurrency lock.
    final ArrayList<Socket> _inSockets = new ArrayList<>();

    public ServerSocketHelper(ServerSocket socket) {
        super("ServerSocketHelper");
        this.servSocket = socket;
    }

    public void run() {
        try {
            //  Set socket timeout so that it can stop blocking and be shutdown, eventually.
            servSocket.setSoTimeout(10000);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while(!close) {
            try {
                Socket tmp = servSocket.accept();
                synchronized (_inSockets) {
                    println("Accepting new socket.");
                    _inSockets.add(tmp);
                }
            } catch (SocketTimeoutException e) {
                println("Socked Timeout");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        println("Finished Thread");
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

class DatagramSocketHelper extends PrintBaseClass implements Runnable {

    DatagramSocket _ds;
    final ArrayList<DatagramPacket> queue = new ArrayList<>();
    boolean close = false;
    final int BUF_SIZE = 65535;

    DatagramSocketHelper(DatagramSocket ds) {
        super("DatagramSocketHelper");
        _ds = ds;
    }

    public void close() {
        close = true;
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

        println("Starting thread...");
        try {
            //  Set socket timeout so the socket will eventually close
            _ds.setSoTimeout(10000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while(!close) {
            DatagramPacket dp = new DatagramPacket(new byte[BUF_SIZE], BUF_SIZE);
            try {
                _ds.receive(dp);
                println("Received packet, adding to queue.");
                synchronized (queue) {
                    queue.add(dp);
                }
            } catch (SocketTimeoutException e) {
               println("Socket timeout");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        println("Finished thread.");
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

public class ServerServer extends PrintBaseClass implements Runnable {

    boolean _close = false;
    
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
    DatagramSocketHelper _datagramSocketHelper;


    public ServerServer(String hostname, int nodeType, Boolean isVolatile, int serverPort, int messageTimeout, int pingTimeout, boolean isHeadCapable) throws Exception {
        super("ServerServer");

        println("Initializing...");

        _hostname = hostname;
        _nodeType = nodeType;
        _isVolatile = isVolatile;
        _serverPort = serverPort;
        _isHeadCapable = isHeadCapable;

        _messageTimeout = messageTimeout;
        _pingTimeout = pingTimeout;

        Enumeration<NetworkInterface> n;
        try {
            n = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
            throw new Exception("Failed to get network interfaces.");
        }
        boolean found = false;
        InetAddress tmp = null;
        while (n.hasMoreElements() && !found) {
            NetworkInterface e = n.nextElement();
            if(!e.getName().equals("eth0")) {
                continue;
            }
            Enumeration<InetAddress> a = e.getInetAddresses();
            while (a.hasMoreElements() && !found) {

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

        if(tmp == null) {
            throw new Exception("Unable to find IP address.");
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
            new Thread(_serverSocketHelper).start();

            _ds = new DatagramSocket(_serverPort);
            _datagramSocketHelper = new DatagramSocketHelper(_ds);
            new Thread(_datagramSocketHelper).start();

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
        while(!_close) {
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
                Thread.sleep(sleepTime);
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
        while(!_close) {

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
        while(!_close) {

            try {
                Thread.sleep(30000);
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

            System.out.print("\t");

            if(Character.isLetterOrDigit(arr[i])) {
                System.out.print((char) arr[i]);
            } else {
                System.out.print("?");
            }
            // Java doesn't have unsigned bytes
            // Java will sign-extend the byte if >= 128
            // So binary-AND the first byte
            System.out.print("(" + ((int)arr[i] & 0xFF) + ") ");
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

    public void close() {
        _close = true;
        _serverSocketHelper.close();
        _datagramSocketHelper.close();
    }

}
