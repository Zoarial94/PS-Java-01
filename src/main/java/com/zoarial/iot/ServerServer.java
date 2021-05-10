package com.zoarial.iot;

import com.zoarial.*;
import com.zoarial.iot.dao.DAOHelper;
import com.zoarial.iot.dao.IoTActionDAO;
import com.zoarial.iot.models.IoTPacketSectionList;
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
    final private UUID _uuid;

    final private AtomicBoolean _started = new AtomicBoolean(false);
    final private AtomicBoolean _close = new AtomicBoolean(false);

    final private List<Thread> threads = Collections.synchronizedList(new ArrayList<>(8));
    final private IoTActionList listOfActions = new IoTActionList();
    final private IoTActionList actionsInQuestion = new IoTActionList();
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


    public ServerServer(String hostname, UUID uuid, int nodeType, Boolean isVolatile, int serverPort, int messageTimeout, int pingTimeout, boolean isHeadCapable) throws Exception {
        super("ServerServer");

        println("Initializing...");

        _hostname = hostname;
        _uuid = uuid;

        _nodeType = nodeType;
        _isVolatile = isVolatile;
        _serverPort = serverPort;
        _isHeadCapable = isHeadCapable;

        _messageTimeout = messageTimeout;
        _pingTimeout = pingTimeout;

        // I need to use a different database when running the non-head
        DAOHelper.setEntityManagerFactory("ZIoTNonHead");

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

    }

    private void generateIoTActions() {

        IoTActionDAO ioTActionDAO = new IoTActionDAO();

        // List all internal actions
        // We check later whether they are in the database (by name)
        // Maybe later, have a seed in the config file to generate random, but constant ids for JavaIoTActions
        // This would help with finding the actions by an id, rather than by name
        ArrayList<JavaIoTAction> javaIoTActions = new ArrayList<>(4);
        javaIoTActions.add(new JavaIoTAction("Stop", (byte)4, true, false, (byte)0, (list)-> {
            System.exit(0);
            return "";
        }));
        javaIoTActions.add(new JavaIoTAction("Shutdown", (byte)4, true, true, (byte)0, (list)-> {
            System.exit(0);
            return "";
        }));
        javaIoTActions.add(new JavaIoTAction("GetUptime", (byte)4, true, false, (byte)0, (list)-> {
            return String.valueOf(System.currentTimeMillis() - startTime);
        }));
        javaIoTActions.add(new JavaIoTAction("Print", (byte)4, true, false, (byte)1, (list) -> {
            println("Being asked to print: \"" + list.get(0).getString() + "\"");
            return "printed";
        }));
        javaIoTActions.add(new JavaIoTAction("GetNewScripts", (byte)4, true, true, (byte)1, (list)->{
            IoTPacketSectionList sectionList = new IoTPacketSectionList(actionsInQuestion.size() * 4);
            for(IoTAction action : actionsInQuestion) {
                sectionList.add(action.getUuid());
                sectionList.add(action.getName());
                sectionList.add(action.getSecurityLevel());
                sectionList.add(action.getArguments());
            }
            return new String(sectionList.getNetworkResponse()) + "\0";
        }));
        javaIoTActions.add(new JavaIoTAction("AddNewScriptToActions", (byte)4, true, true, (byte)1, (list)->{
            UUID uuid = UUID.fromString(list.get(0).getString());
            println("UUID: " + uuid);
            IoTAction action = actionsInQuestion.remove(uuid);
            if(action == null) {
                return "There is no action with that UUID.";
            } else {
                listOfActions.add(action);
                return "Success.";
            }
        }));

        // If the action was not found, then add it with default permissions
        for(JavaIoTAction action : javaIoTActions) {
            JavaIoTAction dbAction = ioTActionDAO.getJavaActionByName(action.getName());
            if(dbAction == null) {
                action.setUuid(UUID.randomUUID());
                println("JavaIoTAction was not in database. Adding now:\n" + action);
                ioTActionDAO.persist(action);
                listOfActions.add(action);
            } else {
                dbAction.setExec(action.getExec());
                listOfActions.add(dbAction);
            }
        }

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
        // TODO: find actions in the db which are stale
        try {
            listOfScripts = Files.list(scriptDir);
            println("Found files: ");
            listOfScripts.filter(file -> {
                boolean b = ScriptIoTAction.isValidFile(file);
                if(!b) {
                    println("Something is wrong with the permissions/file.");
                }
                return b;
            }).forEach(file->{
                String fileName = file.getFileName().toString();
                ScriptIoTAction dbAction = ioTActionDAO.getScriptActionByName(fileName);
                if(dbAction == null) {
                    // By default use highest security level
                    // Security level should be changed manually by user though other means. (Saved to database)
                    ScriptIoTAction action = new ScriptIoTAction(fileName, UUID.randomUUID(), (byte) 4, true, false, (byte) 0, file);
                    println("There is a new script to add to actions:\n" + action);
                    actionsInQuestion.add(action);
                } else {
                    if(dbAction.isValid()) {
                        listOfActions.add(dbAction);
                    } else {
                        println("Script action is no longer valid:\n" + dbAction);
                    }
                }
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
