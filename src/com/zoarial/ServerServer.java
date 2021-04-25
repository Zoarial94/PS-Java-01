package com.zoarial;

import com.zoarial.threads.*;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class ServerServer extends PrintBaseClass implements Runnable {

    final private String _hostname;
    final private int _nodeType;
    final private boolean _isVolatile;
    final private int _serverPort;
    final private boolean _isHeadCapable;
    final private InetAddress _serverIP;
    final private UUID _uuid = UUID.randomUUID();

    final private AtomicBoolean _started = new AtomicBoolean(false);
    final private AtomicBoolean _close = new AtomicBoolean(false);

    final private ArrayList<IoTAction> listOfActions = new ArrayList<>(10);
    private long startTime;

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

        // Generate the IoTActions list so we can know what we can do
        generateIoTActions();

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
            startTime = System.currentTimeMillis();
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
        new Thread(new HeadTCPAcceptingThread(this, _serverSocketHelper)).start();

    }

    private void runNotHeadCapable() {

        try {
            _ds = new DatagramSocket(_serverPort);
            _datagramSocketHelper = new DatagramSocketHelper(this, _ds);
            new Thread(_datagramSocketHelper).start();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        new Thread(new NonHeadUDPThread(this, _datagramSocketHelper)).start();

    }

    private void generateIoTActions() {

        listOfActions.add(new JavaIoTAction("Stop", UUID.randomUUID(), (byte)4, 0, ()-> {
            System.exit(0);
            return "";
        }));
        listOfActions.add(new JavaIoTAction("Shutdown", UUID.randomUUID(), (byte)4, 0, ()-> {
            System.exit(0);
            return "";
        }));
        listOfActions.add(new JavaIoTAction("GetUptime", UUID.randomUUID(), (byte)4, 0, ()-> {
            return String.valueOf(System.currentTimeMillis() - startTime);
        }));

        // For now, assume we are always on linux.
        Path rootDir = Paths.get("/etc/ZoarialIoT");
        Path scriptDir = Paths.get("/etc/ZoarialIoT/scripts");
        Stream<Path> listOfScripts;

        // Check for things
        if(!Files.isDirectory(rootDir)) {
            println("Root directory does not exist.");
            return;
        }
        if(!Files.isDirectory(scriptDir)) {
            println("Script directory does not exist.");
            // If there is no script dir, then we can't create any actions.
            return;
        }

        // Try to generate the list
        try {
            listOfScripts = Files.list(scriptDir);
            println("Found files: ");
            listOfScripts.filter(file -> {
                try {
                    boolean b = Files.getOwner(file).getName().equals("hwhite") &&
                                Files.isRegularFile(file) &&
                                Files.isReadable(file) &&
                                Files.isExecutable(file) &&
                                !Files.isSymbolicLink(file);
                    if(!b) {
                        println("Something is wrong with the permissions/file.");
                    }
                    return b;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }).forEach(file->{
                String fileName = file.getFileName().toString();
                println("Adding " + fileName);
                // By default use highest security level
                // Security level should be changed manually by user though other means. (Saved to database)
                ScriptIoTAction action = new ScriptIoTAction(fileName, UUID.randomUUID(), (byte) 4, 0, file);
                println(action);
                listOfActions.add(action);
            });
        } catch (IOException e) {
            e.printStackTrace();
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

    public static void printArray(byte[] arr) {
        printArray(arr, arr.length);
    }

    public static void printArray(byte[] arr, int len) {
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

    public ArrayList<IoTAction> getListOfActions() {
        return listOfActions;
    }

    public static Character[] getNetworkResponse(List<IoTPacketSection> sections) {
        ArrayList<Character> byteList = new ArrayList<>(sections.size() * 32);

        for(IoTPacketSection section : sections) {
            byteList.addAll(section.getByteList());
            byteList.add((char)0);
        }

        return (Character[]) byteList.toArray();
    }

    public InetAddress getIP() {
        return _serverIP;
    }

    public int getPort() {
        return _serverPort;
    }

    public int getNodeType() {
        return _nodeType;
    }

    public String getHostname() {
        return _hostname;
    }

    public void close() {
        _close.setOpaque(true);
    }

    public boolean isClosed() {
        return _close.getOpaque();
    }

}
