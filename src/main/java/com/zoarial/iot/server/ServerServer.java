package com.zoarial.iot.server;

import com.zoarial.*;
import com.zoarial.iot.action.helper.JavaIoTActionExecHelper;
import com.zoarial.iot.action.model.*;
import com.zoarial.iot.dao.DAOHelper;
import com.zoarial.iot.action.dao.IoTActionDAO;
import com.zoarial.iot.model.ServerInformation;
import com.zoarial.iot.network.IoTPacketSectionList;
import com.zoarial.iot.node.dao.IoTNodeDAO;
import com.zoarial.iot.node.model.IoTNode;
import com.zoarial.iot.threads.tcp.SocketHelper;
import com.zoarial.iot.threads.tcp.TCPAcceptingThread;
import com.zoarial.iot.threads.tcp.ServerSocketHelper;
import com.zoarial.iot.threads.udp.DatagramSocketHelper;
import com.zoarial.iot.threads.udp.UDPThread;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

/*
 *
 * I'm not certain how I want this program to be structured.
 * For now, I will have ServerServer run as its own thread.
 * The ServerServer thread will start the Head or Non-Head threads and then stop.
 * In the future, this behavior may change depending on the needs of the program.
 *
 */

@Getter
@Slf4j
public class ServerServer extends PrintBaseClass implements Runnable {

    // Easier to pass all the server info around if its all in one object
    final private ServerInformation serverInfo;
    // an IoTNode representation of its self (the currently running node)
    final private IoTNode selfNode;

    final private boolean started = false;
    // an atomic flag for signaling to all the threads whether the server is closed or not.
    @Getter(AccessLevel.NONE)
    final private AtomicBoolean closed = new AtomicBoolean(false);

    // The ip address of the current device.
    // Is influenced by the user-configured network device, but is automatically selected from that device.
    final private InetAddress serverIP;

    // A list of threads this class starts.
    // Used to forcefully stop threads when the server is closing.
    @Getter(AccessLevel.NONE)
    final private List<Thread> threads = Collections.synchronizedList(new ArrayList<>(8));
    @Getter(AccessLevel.NONE)
    private final HashMap<UUID, Socket> externalNodeConnections = new HashMap<>();
    // This helper manages the TCP sockets
    // This is its own thread
    @Getter(AccessLevel.NONE)
    private ServerSocketHelper serverSocketHelper;
    // This helper manages the raw UDP packets for both sending and receiving
    // This is its own thread
    @Getter(AccessLevel.NONE)
    private DatagramSocketHelper datagramSocketHelper;
    // Used for GetUptime action
    private long startTime;


    public ServerServer(ServerInformation info) {
        super("ServerServer");

        println("Initializing...");

        serverInfo = info;
        selfNode = new IoTNode(info.hostname, info.uuid, info.nodeType);


        if (!serverInfo.isHeadCapable) {
            // I need to use a different database when running the non-head
            // This sets the entity manager factory before any DAO object acquire it
            DAOHelper.setEntityManagerFactory("ZIoTNonHead");
        }

        Enumeration<NetworkInterface> networkInterfaceEnumeration;
        try {
            networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get network interfaces.");
        }

        boolean found = false;
        InetAddress tmpInetAddress = null;
        println("Looking for network device: " + info.networkDeviceName);

        // Look for an ip address from the specified network device.
        while (networkInterfaceEnumeration.hasMoreElements() && !found) {
            NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
            println("Found interface: " + networkInterface.getName());
            if (!networkInterface.getName().equals(info.networkDeviceName)) {
                println("Skipping interface");
                continue;
            }
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements() && !found) {

                InetAddress address = inetAddresses.nextElement();
                System.out.println(networkInterface.getName() + ":  " + address.getHostAddress());
                if (!address.equals(Inet4Address.getLoopbackAddress()) && (address instanceof Inet4Address)) {
                    tmpInetAddress = address;
                    found = true;
                }
            }
        }

        if (tmpInetAddress == null) {
            throw new RuntimeException("Unable to find IP address.");
        }

        // Generate the IoTActions list so we can know what we can do
        generateIoTActions();

        serverIP = tmpInetAddress;
    }

    public static void printArray(byte[] arr) {
        printArray(arr, arr.length);
    }

    public static void printArray(byte[] arr, int len) {
        String prefix = "Array: ";
        for (int i = 0; i < len; i++) {
            if (i == 0) {
                System.out.print(prefix);
            } else if (i % 8 == 0) {
                System.out.print("\n" + prefix);
            }

            System.out.print("\t");

            if (Character.isLetterOrDigit(arr[i])) {
                System.out.print((char) arr[i]);
            } else {
                System.out.print("?");
            }
            // Java doesn't have unsigned bytes
            // Java will sign-extend the byte if >= 128
            // So binary-AND the first byte
            System.out.print("(" + ((int) arr[i] & 0xFF) + ") ");
        }
        System.out.println();
    }

    public void run() {
        // Maybe this should be an atomic flag, but for now this should work.
        if (!started) {
            println("Starting...");
            startTime = System.currentTimeMillis();

            try {
                println("Starting a server on port " + serverInfo.serverPort + ".");

                serverSocketHelper = new ServerSocketHelper(this, new ServerSocket(serverInfo.serverPort));
                createAndStartNewThread(serverSocketHelper);

                datagramSocketHelper = new DatagramSocketHelper(this, new DatagramSocket(serverInfo.serverPort));
                createAndStartNewThread(datagramSocketHelper);

            } catch (IOException ex) {
                println("Something happened when creating ths socket helpers. Exiting.");
                System.exit(-1);
            }

            /*
             * Start Threads
             */

            createAndStartNewThread(new UDPThread(this, datagramSocketHelper));
            createAndStartNewThread(new TCPAcceptingThread(this, serverSocketHelper));
        } else {
            println("Server already started.");
        }
    }

    /*
     *  This method is a mess and should be refactored/broken up
     */
    private void generateIoTActions() {

        IoTActionDAO ioTActionDAO = new IoTActionDAO();
        IoTNodeDAO ioTNodeDAO = new IoTNodeDAO();

        // Make sure our DB is updated with current information
        IoTNode dbNode = ioTNodeDAO.getNodeByUUID(serverInfo.uuid);
        if (dbNode == null) {
            ioTNodeDAO.persist(selfNode);
        } else if (!dbNode.equals(selfNode)) {
            ioTNodeDAO.update(selfNode);
        }

        // List all internal actions
        // We check later whether they are in the database (by name)
        // Maybe later, have a seed in the config file to generate random, but constant ids for JavaIoTActions
        // This would help with finding the actions by an id, rather than by name
        createNewJavaIoTAction("Stop", (byte) 0, (byte) 4, true, true, (list) -> {
            close();
            return "Stopping...";
        });
        createNewJavaIoTAction("Shutdown", (byte) 0, (byte) 4, true, true, (list) -> {
            System.exit(0);
            return "Shutting down...";
        });
        createNewJavaIoTAction("GetUptime", (byte) 0, (byte) 4, false, false, (list) -> String.valueOf(System.currentTimeMillis() - startTime));
        createNewJavaIoTAction("Print", (byte) 1, (byte) 4, false, false, (list) -> {
            println("Being asked to print: \"" + list.get(0).getString() + "\"");
            return "Printed";
        });

        // Lists actions from scriptActionsInQuestion
        createNewJavaIoTAction("GetDisabledActions", (byte) 0, (byte) 4, true, true, (list) -> {
            JSONObject root = new JSONObject();
            JSONArray actions = new JSONArray();
            root.put("actions", actions);
            for (IoTAction action : ioTActionDAO.getDisabledActions()) {
                actions.put(action.toJson());
            }
            return root.toString();
        });
        // Move actions from scriptActionsInQuestion to listOfActions
        createNewJavaIoTAction("EnableAction", (byte) 1, (byte) 4, true, true, (list) -> {
            UUID uuid = UUID.fromString(list.get(0).getString());
            println("UUID: " + uuid);

            var optAction = ioTActionDAO.findActionByUUID(uuid);
            if (optAction.isEmpty()) {
                return "There is no new action with that UUID.";
            } else {
                IoTAction action = optAction.get();
                if (action.isValid()) {
                    action.enable();
                    return "Success.";
                } else {
                    action.disable();
                    return "That action is not valid.";
                }
            }
        });
        createNewJavaIoTAction("UpdateSecurityLevel", (byte) 2, (byte) 4, true, true, (list) -> {
            UUID uuid = UUID.fromString(list.get(0).getString());
            byte level = list.get(1).getByte();
            if (level > 4 || level < 0) {
                return "Invalid security level";
            }
            var optAction = ioTActionDAO.findActionByUUID(uuid);
            if (optAction.isPresent()) {
                IoTAction action = optAction.get();
                action.setSecurityLevel(level);
                ioTActionDAO.update(action);
                return "Security level set.";
            } else {
                return "No action with that UUID found.";
            }
        });
        createNewJavaIoTAction("UpdateEncryptedToggle", (byte) 2, (byte) 4, true, true, (list) -> {
            UUID uuid = UUID.fromString(list.get(0).getString());
            var optAction = ioTActionDAO.findActionByUUID(uuid);
            if (optAction.isPresent()) {
                IoTAction action = optAction.get();
                action.setEncrypted(list.get(1).getBool());
                ioTActionDAO.update(action);
                return "Encryption toggle set.";
            } else {
                return "No action with that UUID found.";
            }
        });
        createNewJavaIoTAction("UpdateLocalToggle", (byte) 2, (byte) 4, true, true, (list) -> {
            UUID uuid = UUID.fromString(list.get(0).getString());
            var optAction = ioTActionDAO.findActionByUUID(uuid);
            if (optAction.isPresent()) {
                IoTAction action = optAction.get();
                action.setLocal(list.get(1).getBool());
                ioTActionDAO.update(action);
                return "Security level set.";
            } else {
                return "No action with that UUID found.";
            }
        });

        //TODO: remove stale JavaIoTActions


        // For now, assume we are always on linux.
        Path rootDir = Paths.get("/etc/ZoarialIoT");
        Path scriptDir = Paths.get("/etc/ZoarialIoT/scripts");
        Stream<Path> listOfScripts;

        // Check for things
        if (!Files.isDirectory(rootDir)) {
            println("Root directory does not exist.");
            return;
        }
        if (!Files.isDirectory(scriptDir)) {
            println("Script directory does not exist.");
            // If there is no script dir, then we can't create any actions.
            return;
        }

        // Try to generate the list
        // TODO: find actions in the db which are stale
        try {
            listOfScripts = Files.list(scriptDir);
            listOfScripts.filter(file -> {
                boolean b = ScriptIoTAction.isValidFile(file);
                if (!b) {
                    println("Something is wrong with the permissions/file.");
                }
                return b;
            }).forEach(file -> {
                String fileName = file.getFileName().toString();
                ScriptIoTAction dbAction = ioTActionDAO.getScriptActionByName(fileName);
                if (dbAction == null) {
                    // By default use highest security level
                    // Security level should be changed manually by user though other means. (Saved to database)
                    ScriptIoTAction action = new ScriptIoTAction(fileName, UUID.randomUUID(), (byte) 4, true, false, (byte) 0, file);
                    action.setNode(selfNode);
                    println("There is a new script to add to actions:\n" + action);
                    ioTActionDAO.persist(action);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createNewJavaIoTAction(String name, byte args, byte securityLevel, boolean encrypt, boolean local, Function<IoTActionArgumentList, String> exec) {
        // Make sure action hasn't already been created
        if (JavaIoTActionExecHelper.getFunction(name) != null) {
            throw new RuntimeException("JavaIoTAction with name " + name + " already exists.");
        }

        // Add to map and prep for db access
        var map = JavaIoTActionExecHelper.getMap();
        var action = new JavaIoTAction(name, args, securityLevel, encrypt, local);
        action.setNode(selfNode);
        var ioTActionDAO = new IoTActionDAO();

        map.put(name, exec);

        // See if the action exists in the DB
        // If it doesn't exist, add it
        // If it does exist, make sure its updated.
        JavaIoTAction dbAction = ioTActionDAO.getJavaActionByName(name);
        if (dbAction == null) {
            action.setUuid(UUID.randomUUID());
            println("JavaIoTAction was not in database. Adding now:\n" + action);
            ioTActionDAO.persist(action);
        } else {
            // Updated database in the case something has changed
            action.setUuid(dbAction.getUuid());
            if (!dbAction.isEnabled() || !dbAction.isValid()) {
                action.disable();
            }
            if (!action.equals(dbAction)) {
                ioTActionDAO.update(action);
            }
        }


    }

    public void getAndUpdateInfoAboutNode(IoTNode node) {
        Socket socket = getExternalNodeSocket(node);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        SocketHelper socketHelper = new SocketHelper(socket);
        IoTPacketSectionList packetSectionList = new IoTPacketSectionList();

// Header
        packetSectionList.add("ZIoT");
        // Version
        packetSectionList.add((byte) 0);
        // Session ID
        packetSectionList.add((int) (Math.random() * Integer.MAX_VALUE));
        packetSectionList.add("info");
        packetSectionList.add("actions");

        try {
            socketHelper.out.write(packetSectionList.getNetworkResponse());
            socketHelper.out.flush();

            if (!Arrays.equals("ZIoT".getBytes(), socketHelper.in.readNBytes(4))) {
                log.error("Did not receive ZIoT when getting actions from external node.");
            }

            System.out.println("Session ID: " + socketHelper.readInt());
            int numberOfActions = socketHelper.readInt();
            System.out.println("Number of Actions: " + numberOfActions);

            IoTNodeDAO nodeDAO = new IoTNodeDAO();
            IoTActionDAO actionDAO = new IoTActionDAO();

            for (int i = 0; i < numberOfActions; i++) {
                IoTAction action = new ExternalIoTAction();
                // Action UUID
                action.setUuid(socketHelper.readUUID());

                // Action Node
                UUID actionNodeUuid = socketHelper.readUUID();
                // This may throw. Need to check for non-existent node
                IoTNode actionNode = nodeDAO.getNodeByUUID(actionNodeUuid);
                if(actionNode == null) {
                    throw new RuntimeException("Unexpected null IoTNode.");
                }
                action.setNode(actionNode);

                action.setName(socketHelper.readString());
                action.setSecurityLevel(socketHelper.readByte());
                action.setArguments(socketHelper.readByte());
                action.setEncrypted(socketHelper.readBoolean());
                action.setLocal(socketHelper.readBoolean());

                if(action.getNode().equals(selfNode)) {
                    continue;
                }
                actionDAO.persistOrUpdate(action);
            }


        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            externalNodeConnections.remove(node.getUuid());

        }
    }

    public String runRemoteAction(IoTAction action, JSONObject args) {
        if(action.getNode().equals(selfNode)) {
            log.error("Action " + action.getName() + " has been run remotely instead of locally.");
            return "This action is local, but has been run as remote. This is an error.";
        }

        Socket socket;
        try {
            socket = getExternalNodeSocket(action.getNode());
        } catch (RuntimeException ex) {
            return "There was an error creating the socket.";
        }
        SocketHelper socketHelper = new SocketHelper(socket);

        IoTPacketSectionList packetSectionList = new IoTPacketSectionList();

        // Header
        packetSectionList.add("ZIoT");
        // Version
        packetSectionList.add((byte)0);
        // Session ID
        int sessionID = (int)(Math.random() * Integer.MAX_VALUE);
        System.out.println("Created SessionID: " + sessionID);
        packetSectionList.add(sessionID);
        packetSectionList.add("action");
        packetSectionList.add(action.getUuid());

        packetSectionList.add(args.toString());

        String response;
        try {
            socketHelper.out.write(packetSectionList.getNetworkResponse());
            socketHelper.out.flush();

            if(!Arrays.equals("ZIoT".getBytes(), socketHelper.in.readNBytes(4))) {
                throw new RuntimeException("Was not ZIoT response");
            }

            System.out.println("Session ID: " + socketHelper.readInt());


            response = socketHelper.readString();
        } catch (IOException e) {
            e.printStackTrace();
            response = "There was an error running the action remotely";
        }

        return response;

    }

    private Socket getExternalNodeSocket(IoTNode node) {
        Socket socket = externalNodeConnections.get(node.getUuid());
        if (socket == null || socket.isClosed()) {
            log.trace("Socket to node " + node.getHostname() + " does not exist. Creating one...");
            try {
                printArray(node.getLastIp());
                socket = new Socket(InetAddress.getByAddress(node.getLastIp()), getServerInfo().serverPort);
                externalNodeConnections.put(node.getUuid(), socket);
            } catch (UnknownHostException e) {
                e.printStackTrace();
                log.error("Unable to get InetAddress from bytes.");
                throw new RuntimeException();
            } catch (IOException e) {
                e.printStackTrace();
                log.error("Unable to connect to node");
                throw new RuntimeException();
            }
            log.trace("Created socket for " + node.getHostname() + ".");
        }
        return socket;
    }

    private void createAndStartNewThread(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.start();
        threads.add(t);
    }

    public void close() {
        close(false);
    }

    public void close(boolean join) {
        // Mark the server as closed
        closed.setOpaque(true);

        for(var entry : externalNodeConnections.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Close original sockets
        if (datagramSocketHelper != null && !datagramSocketHelper.isClosed()) {
            datagramSocketHelper.close();
        }
        if (serverSocketHelper != null && !serverSocketHelper.isClosed()) {
            //TODO: shutdown, then close for better cleanup.
            serverSocketHelper.close();
        }

        // Close all daughter threads. (Those started with the helper function at least)
        for (Thread t : threads) {
            if (t.isAlive() && !t.isInterrupted()) {
                t.interrupt();
            }
        }

        if (join) {
            join();
        }
    }

    public void join() {
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isClosed() {
        return closed.getOpaque();
    }


}
