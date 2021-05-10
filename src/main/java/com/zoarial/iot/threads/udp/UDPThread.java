package com.zoarial.iot.threads.udp;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.ServerServer;
import com.zoarial.iot.dao.IoTNodeDAO;
import com.zoarial.iot.models.IoTNode;
import com.zoarial.iot.models.IoTPacketSectionList;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * This thread shouldn't have to be multithreaded, but could be.
 */
public class UDPThread extends PrintBaseClass implements Runnable {

    private final ServerServer _server;
    private static final AtomicInteger idNumber = new AtomicInteger(0);

    final private DatagramSocketHelper _datagramSocketHelper;
    final private IoTNodeDAO ioTNodeDAO;

    public UDPThread(ServerServer server, DatagramSocketHelper datagramSocketHelper) {
        super("HeadUDPThread" + idNumber.getAndIncrement());
        _datagramSocketHelper = datagramSocketHelper;
        _server = server;
        ioTNodeDAO = new IoTNodeDAO();
    }

    public void run() {

        if(_server.isHeadCapable()) {
            headUDPLoop();
        } else {
            nonHeadUDPLoop();
        }

    }

    void headUDPLoop() {
        /*
         * LOOP
         */
        DatagramPacket dp;
        while (!_server.isClosed()) {
            /*
             *
             * UDP Handler Loop
             *
             */
            try {
                dp = _datagramSocketHelper.pollNextData(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
            if(dp == null) {
                //println("Timed out.");
                continue;
            }
            //String str = ServerServer.buildString(dp.getData()).toString();
            //println("Datagram Packet: " + str);
            replyHeadNodes(dp);
        }
    }

    void nonHeadUDPLoop() {
        println("Initializing...");
        //Find head, either by reading from a save file, or a broadcast.
        while(!_server.isClosed()) {

            IoTPacketSectionList sectionList = new IoTPacketSectionList();
            sectionList.add("ZIoT");
            sectionList.add(_server.getNodeType());
            sectionList.add(_server.getHostname());
            sectionList.add(_server.getUUID());

            byte[] buf = sectionList.getNetworkResponse();

            byte[] addr = {10, 94, 50, (byte) 146};
            println("Sending packet...");
            DatagramPacket dp;
            try {
                dp = new DatagramPacket(buf, buf.length, InetAddress.getByAddress(addr), _server.getPort());
                _datagramSocketHelper.send(dp);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                println("Waiting for response...");
                dp = _datagramSocketHelper.pollNextData(30, TimeUnit.SECONDS);
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

                println("Checking for new addresses...");
                for(InetAddress headAddr : headAddrs) {
                    println("Address: " + headAddr);
                    if(!Arrays.equals(headAddr.getAddress(), new byte[] {0, 0, 0, 0})) {
                        println("Found address: " + headAddr);
                        int size = _server.getInfo().headNodes.size();
                        for(int i = 0; i < size; i++) {
                            InetAddress servAddr = _server.getInfo().headNodes.get(i);
                            println("Comparing: " + servAddr);
                            if(Arrays.equals(servAddr.getAddress(), new byte[] {0, 0, 0, 0})) {
                                println("Replacing server address...");
                                _server.getInfo().headNodes.set(i, headAddr);
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

            System.arraycopy(_server.getIP().getAddress(), 0, response, 4, 4);

            DatagramPacket dpResponse = new DatagramPacket(response, response.length, dp.getAddress(), _server.getPort());


            _datagramSocketHelper.send(dpResponse);

            IoTNode newNode = new IoTNode(hostname, uuid, nodeType);
            if(!ioTNodeDAO.containsNode(newNode)) {
                newNode.setLastHeardFrom(System.currentTimeMillis());
                ioTNodeDAO.persist(newNode);
            } else {
                ioTNodeDAO.updateTimestamp(uuid, System.currentTimeMillis());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}