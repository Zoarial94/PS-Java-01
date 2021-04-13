package com.zoarial;

import com.zoarial.threads.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerServer extends PrintBaseClass implements Runnable {

    final private String _hostname;
    final private int _nodeType;
    final private boolean _isVolatile;
    final private int _serverPort;
    final private boolean _isHeadCapable;
    final private InetAddress _serverIP;

    final private AtomicBoolean _started = new AtomicBoolean(false);
    final private AtomicBoolean _close = new AtomicBoolean(false);

    //Server can update these at runtime
    int _messageTimeout;
    int _pingTimeout;

    /*
     *
     *  I use socket helpers which only receive packets/sockets so I can separate the logic to use the packets/sockets elsewhere.
     *  For example, a head node needs to respond to UDP packets, but a non-head may not want to response, or may want to respond differently.
     *  For sockets, once they are accepted, they can be assigned to a thread which will handle it accordingly.
     *
     */
    ServerSocket _outSocket;
    ServerSocketHelper _serverSocketHelper;

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

    /*
     *
     * I'm not certain how I want this program to be structured.
     * For now, I will have ServerServer run as its own thread.
     * The ServerServer thread will start the Head or Non-Head threads and then stop.
     * In the future, this behavior may change depending on the needs of the program.
     *
     */
    public void run() {
        // Try to get the atomic flag. If compareAndSet returns true, then we have the flag and we can start.
        if (_started.compareAndSet(false, true)) {
            println("Starting...");
            if (_isHeadCapable) {
                runHeadCapable();
            } else {
                runNotHeadCapable();
            }
        } else {
            println("Server Already started.");
        }

    }

    private void runHeadCapable() {
        /*
         * SETUP
         * Create socket helpers, then assign the helpers to the threads.
         * The socket helpers are thread-safe and can block while the thread waits.
         */
        try {
            println("Starting a server on port " + _serverPort + ".");
            _outSocket = new ServerSocket(_serverPort);
            _serverSocketHelper = new ServerSocketHelper(this, _outSocket);
            new Thread(_serverSocketHelper).start();

            _ds = new DatagramSocket(_serverPort);
            _datagramSocketHelper = new DatagramSocketHelper(this, _ds);
            new Thread(_datagramSocketHelper).start();

        } catch(IOException ex) {
            System.out.println("Something happened when starting the server. Exiting.");
            System.exit(-1);
        }

        /*
         * Start Threads
         */

        new Thread(new HeadUDPThread(this, _datagramSocketHelper)).start();

    }

    private void runNotHeadCapable() {

        try {
            _ds = new DatagramSocket(_serverPort);
        } catch (SocketException e) {
            e.printStackTrace();
        }

       //Find head, either by reading from a save file, or a broadcast.
        while(!_close.getOpaque()) {

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
        while(!_close.getOpaque()) {

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

    public InetAddress getIP() {
        return _serverIP;
    }

    public int getPort() {
        return _serverPort;
    }

    public void close() {
        _close.setOpaque(true);
    }

    public boolean isClosed() {
        return _close.getOpaque();
    }

}
