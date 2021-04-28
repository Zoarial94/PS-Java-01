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
        println("Starting...");
        try { // This might be unnecessary, but better safe than sorry
            runLogic();
        } finally {
            // Make sure even if something goes wrong, that these things happen.
            threads.remove(Thread.currentThread());
            cleanup();
            println("Stopping.");
        }
    }

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
                    // New session
                    str = readString();
                    println("Working with new session: " + sessionID);
                    println("Request: " + str);
                    switch (str) {
                        case "action" -> respondToSession(new IoTSession(sessionID, IoTSession.IoTSessionType.ACTION), "I haven't implemented this yet.");
                        case "info" -> {
                            str = readString();
                            switch (str) {
                                case "get" -> {
                                    str = readString();
                                    switch (str) {
                                        case "actions" -> respondActionList(new IoTSession(sessionID, IoTSession.IoTSessionType.INFO));
                                        default -> respondToSession(new IoTSession(sessionID, IoTSession.IoTSessionType.INFO), "Unknown option to get: " + str);
                                    }
                                }
                                case "general" -> respondToSession(new IoTSession(sessionID, IoTSession.IoTSessionType.INFO), "Here's some general information");
                                default -> respondToSession(new IoTSession(sessionID, IoTSession.IoTSessionType.OTHER), "Invalid info request");
                            }
                        }
                        default -> respondToSession(new IoTSession(sessionID, IoTSession.IoTSessionType.OTHER), "Invalid request: " + str);
                    }
                }

                // We have to flush otherwise, out data may never be sent.
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
