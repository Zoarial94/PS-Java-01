package com.zoarial.threads;

import com.zoarial.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HeadTCPThread extends PrintBaseClass implements Runnable {

    private final ServerServer _server;
    private static final AtomicInteger idNumber = new AtomicInteger(0);
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
        println("Starting...");

        while (!_server.isClosed()) {
            String response = "";
            try {
                String str = _inSocket.in.readUTF();
                if (!str.equals("ZIoT")) {
                    continue;
                }

                byte version = _inSocket.in.readByte();
                if(version != _version) {
                    continue;
                }

                int sessionID = _inSocket.in.readInt();
                if(sessions.containsKey(sessionID)) {
                    // Continue working with session
                } else {
                    // New session
                    str = _inSocket.in.readUTF();
                    switch(str) {
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

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void respondToSession(IoTSession session, String str) {
        _inSocket.out.write("ZIoT");
        _inSocket.out.write(session.getSessionID());
        _inSocket.out.write(str);
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
        Character[] characterArray = ServerServer.getNetworkResponse(sectionList);
        char[] charArray = new char[characterArray.length];
        // I don't think this will work
        System.arraycopy(characterArray, 0, charArray, 0, characterArray.length);

        _inSocket.out.write(charArray);

    }

}
