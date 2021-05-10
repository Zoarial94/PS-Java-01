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
public class HeadUDPThread extends PrintBaseClass implements Runnable {

    private final ServerServer _server;
    private static final AtomicInteger idNumber = new AtomicInteger(0);

    final private DatagramSocketHelper _datagramSocketHelper;
    final private IoTNodeDAO ioTNodeDAO;

    public HeadUDPThread(ServerServer server, DatagramSocketHelper datagramSocketHelper) {
        super("HeadUDPThread" + idNumber.getAndIncrement());
        _datagramSocketHelper = datagramSocketHelper;
        _server = server;
        ioTNodeDAO = new IoTNodeDAO();
    }

    public void run() {

        /*
         * LOOP
         */
        int sleepTime = 5000;
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
            HeadUDPHandler(dp);
        }
    }

    void HeadUDPHandler(DatagramPacket dp) {

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
                ioTNodeDAO.persist(newNode);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}