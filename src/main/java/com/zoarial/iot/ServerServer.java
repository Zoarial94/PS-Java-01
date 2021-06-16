package com.zoarial.iot;

import com.zoarial.*;
import com.zoarial.iot.dao.DAOHelper;
import com.zoarial.iot.dao.IoTActionDAO;
import com.zoarial.iot.dao.IoTNodeDAO;
import com.zoarial.iot.models.IoTNode;
import com.zoarial.iot.models.IoTPacketSectionList;
import com.zoarial.iot.models.actions.*;
import com.zoarial.iot.threads.tcp.TCPAcceptingThread;
import com.zoarial.iot.threads.tcp.ServerSocketHelper;
import com.zoarial.iot.threads.udp.DatagramSocketHelper;
import com.zoarial.iot.threads.udp.UDPThread;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Getter
@Setter
public class ServerServer extends PrintBaseClass implements Runnable {

    final private ServerInformation serverInfo;
    final private IoTNode _selfNode;

    final private AtomicBoolean _started = new AtomicBoolean(false);
    final private AtomicBoolean _close = new AtomicBoolean(false);

    final private InetAddress _serverIP;

    final private List<Thread> threads = Collections.synchronizedList(new ArrayList<>(8));
    final private IoTActionList listOfActions = new IoTActionList();
    final private IoTActionList scriptActionsInQuestion = new IoTActionList();
    private long startTime;

    /*
     *
     *  I use socket helpers which only receive packets/sockets so I can separate the logic to use the packets/sockets elsewhere.
     *  For example, a head node needs to respond to UDP packets, but a non-head may not want to response, or may want to respond differently.
     *  For sockets, once they are accepted, they can be assigned to a thread which will handle it accordingly.
     *
     */
    ServerSocketHelper _serverSocketHelper;

    DatagramSocketHelper _datagramSocketHelper;


    public ServerServer(ServerInformation info) throws Exception {
        super("ServerServer");

        println("Initializing...");

        serverInfo = info;
        _selfNode = new IoTNode(info.hostname, info.uuid, info.nodeType);


        if(!serverInfo.isHeadCapable) {
            // I need to use a different database when running the non-head
            DAOHelper.setEntityManagerFactory("ZIoTNonHead");
        }

        Enumeration<NetworkInterface> n;
        try {
            n = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
            throw new Exception("Failed to get network interfaces.");
        }
        boolean found = false;
        InetAddress tmp = null;
        //TODO: Create someway to to find a usable interface. Maybe
        //      this should be user specified.
        println("Looking for network device: " + info.networkDeviceName);
        while (n.hasMoreElements() && !found) {
            NetworkInterface e = n.nextElement();
            println("Found interface: " + e.getName());
            if(!e.getName().equals(info.networkDeviceName)) {
                println("Skipping interface");
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

            try {
                println("Starting a server on port " + serverInfo.serverPort + ".");

                _serverSocketHelper = new ServerSocketHelper(this, new ServerSocket(serverInfo.serverPort));
                createAndStartNewThread(_serverSocketHelper);

                _datagramSocketHelper = new DatagramSocketHelper(this, new DatagramSocket(serverInfo.serverPort));
                createAndStartNewThread(_datagramSocketHelper);

            } catch(IOException ex) {
                System.out.println("Something happened when starting the server. Exiting.");
                System.exit(-1);
            }

            /*
             * Start Threads
             */

            createAndStartNewThread(new UDPThread(this, _datagramSocketHelper));
            createAndStartNewThread(new TCPAcceptingThread(this, _serverSocketHelper));
        } else {
            println("Server Already started.");
        }
    }

    private void generateIoTActions() {

        IoTActionDAO ioTActionDAO = new IoTActionDAO();
        IoTNodeDAO ioTNodeDAO = new IoTNodeDAO();

        IoTNode dbNode = ioTNodeDAO.getNodeByUUID(getUUID());
        if(dbNode == null) {
            ioTNodeDAO.persist(_selfNode);
        } else if(!dbNode.equals(_selfNode)) {
            ioTNodeDAO.update(_selfNode);
        }

        // List all internal actions
        // We check later whether they are in the database (by name)
        // Maybe later, have a seed in the config file to generate random, but constant ids for JavaIoTActions
        // This would help with finding the actions by an id, rather than by name
        ArrayList<JavaIoTAction> javaIoTActions = new ArrayList<>(4);
        javaIoTActions.add(new JavaIoTAction("Stop", (byte)4, true, false, (byte)0, (list)-> {
            System.exit(0);
            return "Stopping...";
        }));
        javaIoTActions.add(new JavaIoTAction("Shutdown", (byte)4, true, true, (byte)0, (list)-> {
            System.exit(0);
            return "Shutting down...";
        }));
        javaIoTActions.add(new JavaIoTAction("GetUptime", (byte)0, false, false, (byte)0, (list)-> String.valueOf(System.currentTimeMillis() - startTime)));
        javaIoTActions.add(new JavaIoTAction("Print", (byte)4, true, false, (byte)1, (list)-> {
            println("Being asked to print: \"" + list.get(0).getString() + "\"");
            return "Printed";
        }));
        javaIoTActions.add(new JavaIoTAction("GetNewScripts", (byte)4, true, true, (byte)0, (list)->{
            JSONObject root = new JSONObject();
            JSONArray actions = new JSONArray();
            root.put("actions", actions);
            for(IoTAction action : scriptActionsInQuestion) {
                actions.put(action.toJson());
            }
            return root.toString();
        }));
        javaIoTActions.add(new JavaIoTAction("AddNewScriptToActions", (byte)4, true, true, (byte)1, (list)->{
            UUID uuid = UUID.fromString(list.get(0).getString());
            println("UUID: " + uuid);

            boolean actionExists = scriptActionsInQuestion.contains(uuid);
            if(!actionExists) {
                return "There is no new action with that UUID.";
            } else {
                ScriptIoTAction action = (ScriptIoTAction) scriptActionsInQuestion.get(uuid);
                if(action.isValid()) {
                    scriptActionsInQuestion.remove(action);
                    listOfActions.add(action);
                    return "Success.";
                } else {
                    return "That action is not valid.";
                }
            }
        }));
        javaIoTActions.add(new JavaIoTAction("UpdateSecurityLevel", (byte)4, true, true, (byte)2, (list)-> {
            UUID uuid = UUID.fromString(list.get(0).getString());
            byte level = list.get(1).getByte();
            if(level > 4 || level < 0) {
                return "Invalid security level";
            }
            IoTAction action = listOfActions.get(uuid);
            if (action != null) {
                action.setSecurityLevel(level);
                ioTActionDAO.update(action);
                return "Security level set.";
            } else {
                return "No action with that UUID found.";
            }
        }));
        javaIoTActions.add(new JavaIoTAction("UpdateEncryptedToggle", (byte)4, true, true, (byte)2, (list)-> {
            UUID uuid = UUID.fromString(list.get(0).getString());
            IoTAction action = listOfActions.get(uuid);
            if (action != null) {
                action.setEncrypted(list.get(1).getBool());
                ioTActionDAO.update(action);
                return "Encryption toggle set.";
            } else {
                return "No action with that UUID found.";
            }
        }));
        javaIoTActions.add(new JavaIoTAction("UpdateLocalToggle", (byte)4, true, true, (byte)2, (list)-> {
            UUID uuid = UUID.fromString(list.get(0).getString());
            IoTAction action = listOfActions.get(uuid);
            if (action != null) {
                action.setLocal(list.get(1).getBool());
                ioTActionDAO.update(action);
                return "Security level set.";
            } else {
                return "No action with that UUID found.";
            }
        }));


        // If the action was not found, then add it with default permissions
        for(JavaIoTAction action : javaIoTActions) {
            action.setNode(_selfNode);
            JavaIoTAction dbAction = ioTActionDAO.getJavaActionByName(action.getName());
            if(dbAction == null) {
                action.setUuid(UUID.randomUUID());
                action.setNode(_selfNode);
                println("JavaIoTAction was not in database. Adding now:\n" + action);
                ioTActionDAO.persist(action);
                listOfActions.add(action);
            } else {
                // Updated database in the case something has changed
                action.setUuid(dbAction.getUuid());
                if(!action.equals(dbAction)) {
                    ioTActionDAO.update(action);
                }
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
                    action.setNode(_selfNode);
                    println("There is a new script to add to actions:\n" + action);
                    scriptActionsInQuestion.add(action);
                    ioTActionDAO.persist(action);
                } else {
                    if(dbAction.isValid() && dbAction.isEnabled()) {
                        listOfActions.add(dbAction);
                    } else {
                        println("Script action is no longer valid:\n" + dbAction);
                        scriptActionsInQuestion.add(dbAction);
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
        return serverInfo.serverPort;
    }

    public byte getNodeType() {
        return serverInfo.nodeType;
    }

    public String getHostname() {
        return serverInfo.hostname;
    }

    public UUID getUUID() {
        return serverInfo.uuid;
    }

    public void close() {
        close(false);
    }
    public void close(boolean join) {
        // Mark the server as closed
        _close.setOpaque(true);

        // Close original sockets
        if(_datagramSocketHelper != null && !_datagramSocketHelper.isClosed()) {
            _datagramSocketHelper.close();
        }
        if(_serverSocketHelper != null && !_serverSocketHelper.isClosed()) {
            //TODO: shutdown, then close for better cleanup.
            _serverSocketHelper.close();
        }

        // Close all daughter threads. (Those started with the helper function at least)
        for(Thread t : threads) {
            if(t.isAlive() && !t.isInterrupted()) {
                t.interrupt();
            }
        }

        if(join) {
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
        return _close.getOpaque();
    }

    public boolean isVolatile() {
        return serverInfo.isVolatile;
    }

    public ServerInformation getInfo() {
        return serverInfo;
    }

    public boolean isHeadCapable() {
        return serverInfo.isHeadCapable;
    }

}
