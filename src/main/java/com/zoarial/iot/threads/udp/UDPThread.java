package com.zoarial.iot.threads.udp;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.server.ServerServer;
import com.zoarial.iot.node.dao.IoTNodeDAO;
import com.zoarial.iot.node.model.IoTNode;
import com.zoarial.iot.network.IoTPacketSectionList;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/*
 * This thread shouldn't have to be multithreaded, but could be.
 */
public class UDPThread extends PrintBaseClass implements Runnable {

    private final ServerServer _server;

    final private DatagramSocketHelper _datagramSocketHelper;
    final private IoTNodeDAO ioTNodeDAO;

    public UDPThread(ServerServer server, DatagramSocketHelper datagramSocketHelper) {
        super("UDPThread");
        _datagramSocketHelper = datagramSocketHelper;
        _server = server;
        ioTNodeDAO = new IoTNodeDAO();
    }

    public void run() {

        println("Starting...");
        try {
            if (_server.getServerInfo().isHeadCapable) {
                println("Starting Head Loop.");
                headUDPLoop();
            } else {
                println("Starting Non-Head Loop.");
                nonHeadUDPLoop();
            }
        } catch (Exception ex) {
            println("Unexpectedly closed.");
            ex.printStackTrace();
        }
        println("Finished.");

    }

    void headUDPLoop() {
        /*
         * LOOP
         */
        Optional<DatagramPacket> dp;
        while (!_server.isClosed()) {
            /*
             * UDP Handler Loop
             */
            try {
                dp = _datagramSocketHelper.pollNextData(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
            if(dp.isEmpty()) {
                //println("Timed out.");
                continue;
            }
            //String str = ServerServer.buildString(dp.getData()).toString();
            //println("Datagram Packet: " + str);
            replyHeadNodes(dp.get());
            //TODO: create a queue in ServerServer for things that need to be done
            //TODO: get info about node just discovered. (This should go into that queue)
        }
    }

    void nonHeadUDPLoop() {
        println("Initializing...");
        //Find head, either by reading from a save file, or a broadcast.
        while(!_server.isClosed()) {

            IoTPacketSectionList sectionList = new IoTPacketSectionList();
            sectionList.add("ZIoT");
            sectionList.add(_server.getServerInfo().nodeType);
            sectionList.add(_server.getServerInfo().hostname);
            sectionList.add(_server.getServerInfo().uuid);

            byte[] buf = sectionList.getNetworkResponse();

            byte[] addr = {10, 94, 50, (byte) 95};
            println("Sending packet...");
            Optional<DatagramPacket> optDp;
            DatagramPacket dp;
            try {
                _datagramSocketHelper.send(new DatagramPacket(buf, buf.length, InetAddress.getByAddress(addr), _server.getServerInfo().serverPort));
            } catch (IOException e) {
                println("Error sending packet.");
                e.printStackTrace();
                break;
            }

            try {
                println("Waiting for response...");
                optDp = _datagramSocketHelper.pollNextData(30, TimeUnit.SECONDS);
                if(optDp.isEmpty()) {
                    println("Must have timed out while waiting for response. Retrying.");
                    continue;
                }
                dp = optDp.get();
                byte[] data = dp.getData();
                if(!new String(data, 0, 4).equals("ZIoT")) {
                    println("Not Z-IoT. Trying again...");
                    continue;
                }
                println("Packet is Z-IoT");
                println("Length: " + dp.getLength());

                InetAddress[] headAddrs = { InetAddress.getByAddress(Arrays.copyOfRange(data, 4, 8)),
                        InetAddress.getByAddress(Arrays.copyOfRange(data, 8, 12)),
                        InetAddress.getByAddress(Arrays.copyOfRange(data, 12, 16))};

                for(InetAddress headAddr : headAddrs) {
                    println("Address: " + headAddr);
                    if(!Arrays.equals(headAddr.getAddress(), new byte[] {0, 0, 0, 0})) {
                        int size = _server.getServerInfo().headNodes.size();
                        for(int i = 0; i < size; i++) {
                            InetAddress servAddr = _server.getServerInfo().headNodes.get(i);
                            if(Arrays.equals(servAddr.getAddress(), headAddr.getAddress())) {
                                // We already have this address in the list.
                                break;
                            } else if(Arrays.equals(servAddr.getAddress(), new byte[] {0, 0, 0, 0})) {
                                println("Replacing server address...");
                                _server.getServerInfo().headNodes.set(i, headAddr);
                                break;
                            }
                        }
                    }
                }

                ServerServer.printArray(data, dp.getLength());

                for(InetAddress i : headAddrs) {
                    ServerServer.printArray(i.getAddress());
                }
                return;
            } catch (IOException | InterruptedException e) {
                //e.printStackTrace();
            }
        }
    }

    void replyHeadNodes(DatagramPacket dp) {

        DatagramPacketHelper dpHelper = new DatagramPacketHelper(dp);
        final byte[] HEADER = "ZIoT".getBytes(StandardCharsets.UTF_8);
        try {
            if(Arrays.compare(HEADER, dpHelper.readBytes(4)) == 0) {
                println("Is Z-IoT Packet.");
            } else {
                println("Is not Z-IoT packet.");
                return;
            }

            byte nodeType = dpHelper.readByte();
            String hostname = dpHelper.readString();
            UUID uuid = dpHelper.readUUID();

            println("Node type is: " + nodeType);
            println("Hostname is: " + hostname);
            println("UUID is: " + uuid);

            byte[] response = new byte[16];
            System.arraycopy("ZIoT".getBytes(), 0, response, 0, 4);

            System.arraycopy(_server.getServerIP().getAddress(), 0, response, 4, 4);

            DatagramPacket dpResponse = new DatagramPacket(response, response.length, dp.getAddress(), dp.getPort());

            _datagramSocketHelper.send(dpResponse);

            IoTNode newNode = new IoTNode(hostname, uuid, nodeType);
            newNode.setLastIp(dp.getAddress().getAddress());
            newNode.setLastHeardFrom(System.currentTimeMillis());
            if(!ioTNodeDAO.containsNode(newNode)) {
                ioTNodeDAO.persist(newNode);
            } else {
                ioTNodeDAO.update(newNode);
            }

            _server.getAndUpdateInfoAboutNode(newNode);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}