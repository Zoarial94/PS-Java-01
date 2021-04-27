package com.zoarial.threads;

import com.zoarial.*;

import java.io.EOFException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HeadTCPThread extends PrintBaseClass implements Runnable {

    private final ServerServer _server;
    private static final AtomicInteger idNumber = new AtomicInteger(0);
    private static final List<Thread> threads = Collections.synchronizedList(new ArrayList<>(8));
    private final byte _version = (byte)0;
    HashMap<Integer, IoTSession> sessions = new HashMap<>();
    private static final char NULL_BYTE = 0;

    inSocketWrapper _inSocket;

    public HeadTCPThread(ServerServer server, inSocketWrapper inSocket) {
        super("HeadTCPThread" + idNumber.getAndIncrement());
        _inSocket = inSocket;
        _server = server;
    }

    public void run() {
        threads.add(Thread.currentThread());
        println("Starting...");
        runLogic();
        threads.remove(Thread.currentThread());
        println("Stopping.");
    }

    private void runLogic() {
        // If we leave this try/catch block, then the socket is closed.
        try (_inSocket.inSocket) {
            while (!_server.isClosed() && !_inSocket.inSocket.isClosed()) {
                String str;
                try {
                    final String HEADER = "ZIoT";
                    boolean found = true;
                    byte[] buf = new byte[256];
                    int read = _inSocket.in.read(buf, 0, 4);
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
                    _inSocket.in.readByte();
                    if (sessions.containsKey(sessionID)) {
                        // Continue working with session
                        println("Continuing to work with session: " + sessionID);
                    } else {
                        // New session
                        str = readString();
                        println("Working with new session: " + sessionID);
                        println("Request: " + str);
                        switch (str) {
                            case "action":
                                respondActionList(new IoTSession(sessionID, IoTSession.IoTSessionType.ACTION));
                                break;
                            case "info":
                                respondToSession(new IoTSession(sessionID, IoTSession.IoTSessionType.INFO), "Here's some info");
                                break;
                            default:
                                respondToSession(new IoTSession(sessionID, IoTSession.IoTSessionType.OTHER), "Invalid request: " + str);
                        }
                    }

                } catch (EOFException e) {
                    System.out.println("End of file: should be closed!");
                    _inSocket.out.flush();
                    _inSocket.inSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        try {
            _inSocket.out.writeChars("ZIoT");
            _inSocket.out.writeInt(session.getSessionID());
            _inSocket.out.writeChars(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void respondActionList(IoTSession session) {
        var actionList = _server.getListOfActions();
        ArrayList<IoTPacketSection> sectionList = new ArrayList<>(actionList.size() * 4 + 4);

        sectionList.add(new IoTPacketSection("ZIoT"));
        sectionList.add(new IoTPacketSection(session.getSessionID()));
        sectionList.add(new IoTPacketSection(actionList.size()));

        for(IoTAction action : actionList) {
            sectionList.add(new IoTPacketSection(action.getUUID()));
            sectionList.add(new IoTPacketSection(action.getName()));
            sectionList.add(new IoTPacketSection(action.getSecurityLevel()));
            sectionList.add(new IoTPacketSection(action.getNumberOfArguments()));
        }

        try {
            _inSocket.out.write(ServerServer.getNetworkResponse(sectionList));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static List<Thread> getThreads() {
        return threads;
    }

}
