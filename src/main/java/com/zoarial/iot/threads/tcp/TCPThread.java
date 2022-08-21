package com.zoarial.iot.threads.tcp;

import com.zoarial.*;
import com.zoarial.iot.action.dao.IoTActionDAO;
import com.zoarial.iot.dto.ZoarialDTO;
import com.zoarial.iot.node.dao.IoTNodeDAO;
import com.zoarial.iot.node.model.IoTNode;
import com.zoarial.iot.action.model.IoTAction;
import com.zoarial.iot.server.ServerServer;
import com.zoarial.iot.network.IoTPacketSectionList;
import com.zoarial.iot.network.IoTSession;
import com.zoarial.iot.threads.tcp.networkModel.TCPStart;
import me.zoarial.networkArbiter.ZoarialNetworkArbiter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: redo the logic here. It may not close properly in all cases.
public class TCPThread extends PrintBaseClass implements Runnable {

    private final ServerServer _server;
    private static final AtomicInteger idNumber = new AtomicInteger(0);
    private static final List<Thread> threads = Collections.synchronizedList(new ArrayList<>(8));
    private final byte _version = (byte)0;
    HashMap<Integer, IoTSession> sessions = new HashMap<>();

    private final SocketHelper _inSocket;
    private final IoTNodeDAO ioTNodeDAO = new IoTNodeDAO();
    private final IoTActionDAO ioTActionDAO = new IoTActionDAO();

    private final ZoarialNetworkArbiter networkArbiter = ZoarialNetworkArbiter.getInstance();

    public TCPThread(ServerServer server, SocketHelper inSocket) {
        super("TCPThread" + idNumber.getAndIncrement());
        _inSocket = inSocket;
        _server = server;
    }

    public void run() {
        // TODO: have some way to add and remove the socket to the list
        // We could then close the socket and have it flush before the ServerSocket is closed
        // UPDATE: we should use shutdown to shutdown the input, then clean up, then close.
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
    // may not need circular buffer. Maybe assume all data coming in is correct and if not, then the socket will eventually close.
    // Use the raw stream and data stream
    private void runLogic() {
        // If we leave this try/catch block, then the socket is closed.
        while (!_server.isClosed() && !_inSocket.inSocket.isClosed()) {
            String request;
            int otp;
            byte securityLevel;
            try {
                securityLevel = 0;

                TCPStart startInfo = networkArbiter.receiveObject(TCPStart.class, _inSocket.inSocket);
                if (startInfo.getVersion() != _version) {
                    println("Not the correct version.");
                    return;
                }

                int sessionID = startInfo.getSessionId();
                if (sessions.containsKey(sessionID)) {
                    // Continue working with session
                    println("Continuing to work with session: " + sessionID);
                } else {
                    IoTSession session = new IoTSession(sessionID);
                    // New session
                    request = startInfo.getRequest();
                    println("Working with new session: " + sessionID);
                    println("Request: " + request);
                    switch (request) {
                        case "action" -> {
                            var actionData = networkArbiter.receiveObject(ZoarialDTO.V1.Request.Action.class, _inSocket.inSocket);

                            JSONObject jsonObject = new JSONObject(actionData.getData());

                            if(actionData.getOtp() != null) {
                                securityLevel = 1;
                            }

                            JSONArray args = jsonObject.getJSONArray("args");
                            UUID uuid = actionData.getUuid();
                            // Check for the action
                            var optAction = ioTActionDAO.findActionByUUID(uuid);
                            if(optAction.isPresent()) {
                                var action = optAction.get();
                                if(!action.getNode().equals(_server.getSelfNode())) {
                                    respondToSession(session, _server.runRemoteAction(action, jsonObject));
                                } else if(action.isLocal() && !_inSocket.isLocal()) {
                                    respondToSession(session, "You don't have access to this. (Local).");
                                    // Check assess
                                } else if(action.isEncrypted() && !(_inSocket.isEncrypted() || _inSocket.isLocal())) {
                                    respondToSession(session, "You don't have access to this. (Encrypted).");
                                    // You have access, start doing stuff
                                } else {

                                    int numOfArguments = action.getArguments();
                                    if (numOfArguments == 0) {
                                        request = action.execute();
                                    } else {
                                        request = action.execute(args);
                                    }
                                    respondToSession(session, request);
                                }
                            } else {
                                respondToSession(session, "No action with UUID: " + uuid);
                            }
                        }
                        case "updateActionSecurityLevel" -> {
                            var updateInfo = networkArbiter.receiveObject(ZoarialDTO.V1.Request.UpdateActionSecurityLevel.class, _inSocket.inSocket);
                            UUID uuid = updateInfo.getUuid();
                            println("Updating security level of " + uuid + " to: " + updateInfo.getLevel());

                            var optAction = ioTActionDAO.findActionByUUID(uuid);
                            if(optAction.isEmpty()) {
                                respondToSession(session, "No action with UUID: " + uuid);
                                break;
                            }

                            var action = optAction.get();
                            if(!action.getNode().equals(_server.getSelfNode())) {
                                respondToSession(session, "I can't update other node's actions.");
                                break;
                            }

                            action.setSecurityLevel(updateInfo.getLevel());
                            ioTActionDAO.update(action);
                            respondToSession(session, "Success.");
                        }
                        case "updateActionEncrypt" -> {
                            var updateInfo = networkArbiter.receiveObject(ZoarialDTO.V1.Request.UpdateActionEncrypt.class, _inSocket.inSocket);
                            UUID uuid = updateInfo.getUuid();
                            println("Updating encrypt toggle of " + uuid + " to :" + updateInfo.isEncrypt());
                            var optAction = ioTActionDAO.findActionByUUID(uuid);
                            if(optAction.isEmpty()) {
                                respondToSession(session, "No action with UUID: " + uuid);
                                break;
                            }
                            var action = optAction.get();
                            if(!action.getNode().equals(_server.getSelfNode())) {
                                respondToSession(session, "I can't update other node's actions.");
                                break;
                            }
                            action.setEncrypted(updateInfo.isEncrypt());
                            ioTActionDAO.update(action);
                            respondToSession(session, "Success.");

                        }
                        case "updateActionLocal" -> {
                            var updateInfo = networkArbiter.receiveObject(ZoarialDTO.V1.Request.UpdateActionLocal.class, _inSocket.inSocket);
                            UUID uuid = updateInfo.getUuid();
                            println("Updating local toggle of " + uuid + " to: " + updateInfo.isLocal());

                            var optAction = ioTActionDAO.findActionByUUID(uuid);
                            if(optAction.isEmpty()) {
                                respondToSession(session, "No action with UUID: " + uuid);
                                break;
                            }

                            var action = optAction.get();
                            if(!action.getNode().equals(_server.getSelfNode())) {
                                respondToSession(session, "I can't update other node's actions.");
                                break;
                            }

                            action.setLocal(updateInfo.isLocal());
                            ioTActionDAO.update(action);
                            respondToSession(session, "Success.");
                        }
                        case "updateActionDescription" -> {
                            var updateInfo = networkArbiter.receiveObject(ZoarialDTO.V1.Request.UpdateActionDescription.class, _inSocket.inSocket);
                            UUID uuid = updateInfo.getUuid();
                            println("Updating local toggle of " + uuid + " to: " + updateInfo.getDescription());

                            var optAction = ioTActionDAO.findActionByUUID(uuid);
                            if(optAction.isEmpty()) {
                                respondToSession(session, "No action with UUID: " + uuid);
                                break;
                            }

                            var action = optAction.get();
                            if(!action.getNode().equals(_server.getSelfNode())) {
                                respondToSession(session, "I can't update other node's actions.");
                                break;
                            }

                            action.setDescription(updateInfo.getDescription());
                            ioTActionDAO.update(action);
                            respondToSession(session, "Success.");

                        }
                        case "info" -> {
                            request = _inSocket.readString();
                            println("Request info: " + request);
                            switch (request) {
                                case "action" -> {
                                    UUID actionUUID = _inSocket.readUUID();
                                    println("Responding info about action: " + actionUUID);
                                    IoTAction action = ioTActionDAO.getActionByUUID(actionUUID);
                                    IoTPacketSectionList sectionList = new IoTPacketSectionList();
                                    if(action == null) {
                                        sectionList.add((byte)0);
                                        sectionList.add("An action with that UUID does not exist.");
                                        println("Action does not exist.");
                                    } else {
                                        println("Responding with action info.");
                                        sectionList.add((byte)1);
                                        sectionList.add(action.toJson().toString());
                                    }
                                    respondToSession(new IoTSession(sessionID, IoTSession.IoTSessionType.INFO), sectionList);
                                }
                                case "actions" -> respondActionList(new IoTSession(sessionID, IoTSession.IoTSessionType.INFO));
                                case "general" -> {
                                    IoTPacketSectionList sectionList = new IoTPacketSectionList();
                                    sectionList.add(_server.getServerInfo().hostname);
                                    sectionList.add(_server.getServerInfo().uuid);
                                    sectionList.add(_server.getServerInfo().isHeadCapable);
                                    sectionList.add(_server.getServerInfo().isVolatile);
                                    respondToSession(new IoTSession(sessionID, IoTSession.IoTSessionType.INFO), sectionList);
                                }
                                case "nodes" -> {
                                    respondNodeList(new IoTSession(sessionID, IoTSession.IoTSessionType.INFO));
                                }
                                default -> respondToSession(new IoTSession(sessionID, IoTSession.IoTSessionType.INFO), "Unknown option to get: " + request);
                            }
                        }
                        default -> respondToSession(new IoTSession(sessionID, IoTSession.IoTSessionType.OTHER), "Invalid request: " + request);
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
            } catch (JSONException ex) {
                ex.printStackTrace();
                println("Something is wrong with the json format.");
                return;
            }
        }
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
        var actionList = ioTActionDAO.getEnabledActions();
        IoTPacketSectionList sectionList = new IoTPacketSectionList(actionList.size() * 4 + 3);

        sectionList.add("ZIoT");
        sectionList.add(session.getSessionID());
        sectionList.add(actionList.size());

        for(IoTAction action : actionList) {
            sectionList.add(action.getUuid());
            sectionList.add(action.getNode().getUuid());
            sectionList.add(action.getName());
            //sectionList.add(action.getReturnType())
            sectionList.add(action.getSecurityLevel());
            sectionList.add(action.getArguments());
            sectionList.add(action.isEncrypted());
            sectionList.add(action.isLocal());
        }

        try {
            _inSocket.out.write(sectionList.getNetworkResponse());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void respondNodeList(IoTSession session) {
        IoTNodeDAO ioTNodeDAO = new IoTNodeDAO();
        var nodeList = ioTNodeDAO.getAllNodes();
        IoTPacketSectionList sectionList = new IoTPacketSectionList(nodeList.size() * 4 + 3);


        sectionList.add("ZIoT");
        sectionList.add(session.getSessionID());
        sectionList.add(nodeList.size());

        for(IoTNode node : nodeList) {
            sectionList.add(node.getUuid());
            sectionList.add(node.getHostname());
            sectionList.add(node.getNodeType());
            sectionList.add(node.getLastHeardFrom());
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
