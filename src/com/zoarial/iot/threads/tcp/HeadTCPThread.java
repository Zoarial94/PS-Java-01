package com.zoarial.iot.threads.tcp;

import com.zoarial.*;
import com.zoarial.iot.models.IoTAction;
import com.zoarial.iot.ServerServer;
import com.zoarial.iot.models.IoTPacketSection;
import com.zoarial.iot.models.IoTPacketSectionList;
import com.zoarial.iot.models.IoTSession;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: redo the logic here. It may not close properly in all cases.
public class HeadTCPThread extends PrintBaseClass implements Runnable {

    private final ServerServer _server;
    private static final AtomicInteger idNumber = new AtomicInteger(0);
    private static final List<Thread> threads = Collections.synchronizedList(new ArrayList<>(8));
    private final byte _version = (byte)0;
    HashMap<Integer, IoTSession> sessions = new HashMap<>();
    private static final char NULL_BYTE = 0;

    SocketHelper _inSocket;

    public HeadTCPThread(ServerServer server, SocketHelper inSocket) {
        super("HeadTCPThread" + idNumber.getAndIncrement());
        _inSocket = inSocket;
        _server = server;
    }

    public void run() {
        // TODO: have some way to add and remove the socket to the list
        // We could then close the socket and have it flush before the ServerSocket is closed
        threads.add(Thread.currentThread());
        println("Starting connection with: " + _inSocket.inSocket.getRemoteSocketAddress());
        try { // This might be unnecessary, but better safe than sorry
            runLogic();
        } finally {
            // Make sure even if something goes wrong, that these things happen.
            threads.remove(Thread.currentThread());
            cleanup();
            println("Stopping.");
        }
    }

    // TODO: redo this to use a circular buffer for the input
    // Use the raw stream and data stream
    private void runLogic() {
        // If we leave this try/catch block, then the socket is closed.
        while (!_server.isClosed() && !_inSocket.inSocket.isClosed()) {
            String str;
            try {
                final String HEADER = "ZIoT";
                byte[] buf = new byte[256];
                int read = _inSocket.in.read(buf, 0, 4);
                // See if the first 4 bytes are ZIoT.
                // If they aren't then close the thread.
                if(read != 4) {
                    return;
                }
                int cmp = Arrays.compare(HEADER.getBytes(), 0, 4, buf, 0, 4);
                if(cmp != 0) {
                    return;
                }

                println("Found ZIoT");

                byte version = _inSocket.in.readByte();
                if (version != _version) {
                    println("Not the correct version.");
                    continue;
                }

                int sessionID = _inSocket.in.readInt();
                if (sessions.containsKey(sessionID)) {
                    // Continue working with session
                    println("Continuing to work with session: " + sessionID);
                } else {
                    IoTSession session = new IoTSession(sessionID);
                    // New session
                    str = readString();
                    println("Working with new session: " + sessionID);
                    println("Request: " + str);
                    switch (str) {
                        case "action" -> {
                            var actionsList = _server.getListOfActions();
                            long uuid_high = _inSocket.in.readLong();
                            long uuid_low = _inSocket.in.readLong();
                            UUID uuid = new UUID(uuid_high, uuid_low);
                            for(IoTAction action : actionsList) {
                                if(action.getUUID().equals(uuid)) {
                                    if(action.isEncrypted() && !_inSocket.isEncrypted()) {
                                        respondToSession(session, "You need to be encrypted to use this action.");
                                    }
                                    int numOfArguments = action.getNumberOfArguments();
                                    if(numOfArguments == 0) {
                                        str = action.execute();
                                    } else {
                                        ArrayList<String> arguments = new ArrayList<>(numOfArguments);
                                        for(int i = 0; i < numOfArguments; i++) {
                                            arguments.add(readString());
                                        }
                                        str = action.execute(arguments);
                                    }
                                    respondToSession(session, str);
                                    break;
                                };
                            }
                            respondToSession(session, "No action with UUID: " + uuid);
                        }
                        case "info" -> {
                            str = readString();
                            switch (str) {
                                case "actions" -> respondActionList(new IoTSession(sessionID, IoTSession.IoTSessionType.INFO));
                                case "general" -> {
                                    IoTPacketSectionList sectionList = new IoTPacketSectionList();
                                    sectionList.add(_server.getHostname());
                                    sectionList.add(_server.getUUID());
                                    sectionList.add(_server.isHeadCapable());
                                    sectionList.add(_server.isVolatile());
                                    respondToSession(new IoTSession(sessionID, IoTSession.IoTSessionType.INFO), sectionList);
                                }
                                default -> respondToSession(new IoTSession(sessionID, IoTSession.IoTSessionType.INFO), "Unknown option to get: " + str);
                            }
                        }
                        default -> respondToSession(new IoTSession(sessionID, IoTSession.IoTSessionType.OTHER), "Invalid request: " + str);
                    }
                }

                // We have to flush otherwise, out data may never be sent.
                // TODO: fixing the shutdown would prevent the need for this
                _inSocket.out.flush();
            } catch (EOFException e) {
                println("End of file: should be closed!");
                return; // Cleanup is done after function returns
            } catch (SocketException ex) {
                println("Interrupted, server must be closing.");
                return; // Cleanup is done after function returns
            } catch (IOException ex) {
                ex.printStackTrace();
                return; // Cleanup is done after function returns
            }
        }
    }

    private String readString() throws IOException {
        StringBuilder str = new StringBuilder();
        byte b = _inSocket.in.readByte();
        while(b != 0) {
            str.append((char)b);
            b = _inSocket.in.readByte();
        }
        return str.toString();
    }

    private void respondToSession(IoTSession session,  IoTPacketSectionList sectionList) {
        try {
            _inSocket.out.writeBytes("ZIoT");
            _inSocket.out.writeInt(session.getSessionID());
            _inSocket.out.write(sectionList.getNetworkResponse());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void respondToSession(IoTSession session, String str) {
        println("Responding: " + str);
        IoTPacketSectionList sectionList = new IoTPacketSectionList();
        sectionList.add("ZIoT");
        sectionList.add(session.getSessionID());
        sectionList.add(str);
        try {
            _inSocket.out.write(sectionList.getNetworkResponse());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void respondActionList(IoTSession session) {
        var actionList = _server.getListOfActions();
        IoTPacketSectionList sectionList = new IoTPacketSectionList(actionList.size() * 4 + 3);

        sectionList.add("ZIoT");
        sectionList.add(session.getSessionID());
        sectionList.add(actionList.size());

        for(IoTAction action : actionList) {
            sectionList.add(action.getUUID());
            sectionList.add(action.getName());
            sectionList.add(action.getSecurityLevel());
            sectionList.add(action.getNumberOfArguments());
        }

        try {
            _inSocket.out.write(sectionList.getNetworkResponse());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static List<Thread> getThreads() {
        return threads;
    }

    private void cleanup() {
        try {
            if(!_inSocket.isClosed()) {
                _inSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
