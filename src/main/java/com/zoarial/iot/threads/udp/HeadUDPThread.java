package com.zoarial.iot.threads.udp;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.ServerServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * This thread shouldn't have to be multithreaded, but could be.
 */
public class HeadUDPThread extends PrintBaseClass implements Runnable {

    private final ServerServer _server;
    private static final AtomicInteger idNumber = new AtomicInteger(0);

    final private DatagramSocketHelper _datagramSocketHelper;

    public HeadUDPThread(ServerServer server, DatagramSocketHelper datagramSocketHelper) {
        super("HeadUDPThread" + idNumber.getAndIncrement());
        _datagramSocketHelper = datagramSocketHelper;
        _server = server;
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
            String str = ServerServer.buildString(dp.getData()).toString();
            println("Datagram Packet: " + str);
            HeadUDPHandler(dp);
        }
    }

    void HeadUDPHandler(DatagramPacket dp) {

        byte[] data = dp.getData();
        if (new String(data, 0, 4).equals("ZIoT")) {
            println("Is Z-IoT Packet.");
        } else {
            println("Is not Z-IoT packet.");
            return;
        }

        int node = data[7];
        String hostname = new String(data, 8, 24);

        println("Node type is: " + node);
        println("Hostname is: " + hostname);

        byte[] response = new byte[16];
        System.arraycopy("ZIoT".getBytes(), 0, response, 0, 4);

        System.arraycopy(_server.getIP().getAddress(), 0, response, 4, 4);

        DatagramPacket dpResponse = new DatagramPacket(response, response.length, dp.getAddress(), _server.getPort());


        try {
            _datagramSocketHelper.send(dpResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}