package com.zoarial.iot.threads.udp;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.ServerServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NonHeadUDPThread extends PrintBaseClass implements Runnable {

    private static final AtomicBoolean _ready = new AtomicBoolean(false);
    private final ServerServer _server;
    private final DatagramSocketHelper _datagramSocketHelper;

    public NonHeadUDPThread(ServerServer server, DatagramSocketHelper socket) {
        super("NonHeadUDPThread");
        _server = server;
        _datagramSocketHelper = socket;

    }

    public void run() {
        if(!_ready.getAcquire()) {
            synchronized (_ready) {
                if(!_ready.getOpaque()) {
                    initialize();
                    _ready.setRelease(true);
                }
            }
        }
        start();
    }

    private void initialize() {
        println("Initializing...");
        //Find head, either by reading from a save file, or a broadcast.
        while(!_server.isClosed()) {

            byte[] buf = new byte[32];
            System.arraycopy("ZIoT".getBytes(), 0, buf, 0, 4);
            buf[7] = (byte)_server.getNodeType();
            System.arraycopy(_server.getHostname().getBytes(), 0, buf, 8, Math.min(24, _server.getHostname().length()));

            //printArray(buf);

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

    private void start() {
        println("Starting...");

        println("Finished.");
    }

}
