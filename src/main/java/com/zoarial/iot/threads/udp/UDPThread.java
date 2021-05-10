package com.zoarial.iot.threads.udp;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.ServerServer;
import com.zoarial.iot.dao.IoTNodeDAO;
import com.zoarial.iot.models.IoTNode;

import java.io.IOException;
import java.net.DatagramPacket;
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