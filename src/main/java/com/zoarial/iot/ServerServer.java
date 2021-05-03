package com.zoarial.iot;

import com.zoarial.*;
import com.zoarial.iot.models.actions.*;
import com.zoarial.iot.threads.tcp.HeadTCPAcceptingThread;
import com.zoarial.iot.threads.tcp.ServerSocketHelper;
import com.zoarial.iot.threads.udp.DatagramSocketHelper;
import com.zoarial.iot.threads.udp.HeadUDPThread;
import com.zoarial.iot.threads.udp.NonHeadUDPThread;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    final private List<Thread> threads = Collections.synchronizedList(new ArrayList<>(8));
    final private IoTActionList listOfActions = new IoTActionList();
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
    ServerSocketHelper _serverSocketHelper;

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

            _serverSocketHelper = new ServerSocketHelper(this, new ServerSocket(_serverPort));
            createAndStartNewThread(_serverSocketHelper);

            _datagramSocketHelper = new DatagramSocketHelper(this, new DatagramSocket(_serverPort));
            createAndStartNewThread(_datagramSocketHelper);

        } catch(IOException ex) {
            System.out.println("Something happened when starting the server. Exiting.");
            System.exit(-1);
        }

        /*
         * Start Threads
         */

        createAndStartNewThread(new HeadUDPThread(this, _datagramSocketHelper));
        createAndStartNewThread(new HeadTCPAcceptingThread(this, _serverSocketHelper));

    }

    private void runNotHeadCapable() {

        try {
            _datagramSocketHelper = new DatagramSocketHelper(this, new DatagramSocket(_serverPort));
            createAndStartNewThread(_datagramSocketHelper);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        createAndStartNewThread(new NonHeadUDPThread(this, _datagramSocketHelper));
        //new Thread(new NonHeadTCPThread(this, ))

    }

    private void generateIoTActions() {

        listOfActions.add(new JavaIoTAction("Stop", UUID.randomUUID(), (byte)4, true, false, (byte)0, (list)-> {
            System.exit(0);
            return "";
        }));
        listOfActions.add(new JavaIoTAction("Shutdown", UUID.randomUUID(), (byte)4, true, true, (byte)0, (list)-> {
            System.exit(0);
            return "";
        }));
        listOfActions.add(new JavaIoTAction("GetUptime", UUID.fromString("8ed2e6fb-7311-45e1-853f-d7b1c36684ac"), (byte)4, true, false, (byte)0, (list)-> {
            return String.valueOf(System.currentTimeMillis() - startTime);
        }));
        listOfActions.add(new JavaIoTAction("Print", UUID.fromString("6ed5edb1-1757-4953-96f4-89b89a0140e8"), (byte)4, true, false, (byte)1, (list) -> {
            println(list.get(0).getString());
            return "printed";
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
                ScriptIoTAction action = new ScriptIoTAction(fileName, UUID.randomUUID(), (byte) 4, true, false, (byte) 0, file);
                println(action);
                listOfActions.add(action);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void createAndStartNewThread(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.start();
        threads.add(t);
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

    public IoTActionList getListOfActions() {
        return listOfActions;
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

    public UUID getUUID() {
        return _uuid;
    }

    public void close() {
        close(false);
    }
    public void close(boolean join) {
        // Mark the server as closed
        _close.setOpaque(true);

        // Close original sockets
        if(!_datagramSocketHelper.isClosed()) {
            _datagramSocketHelper.close();
        }
        if(!_serverSocketHelper.isClosed()) {
            _serverSocketHelper.close();
        }

        // Close all daughter threads. (Those started with the helper function at least)
        for(Thread t : threads) {
            if(t.isAlive() && !t.isInterrupted()) {
                t.interrupt();
            }
        }

        if(join) {
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isClosed() {
        return _close.getOpaque();
    }

    public boolean isVolatile() {
        return _isVolatile;
    }

    public boolean isHeadCapable() {
        return _isHeadCapable;
    }

}
