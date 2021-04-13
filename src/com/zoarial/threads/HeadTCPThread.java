package com.zoarial.threads;

import com.zoarial.PrintBaseClass;
import com.zoarial.ServerServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HeadTCPThread extends PrintBaseClass implements Runnable{

    private final ServerServer _server;
    private static final AtomicInteger idNumber = new AtomicInteger(0);

    inSocketWrapper _inSocket;

    public HeadTCPThread(ServerServer server, inSocketWrapper inSocket) {
        super("HeadTCPThread" + idNumber.getAndIncrement());
        _inSocket = inSocket;
        _server = server;
    }

    public void run() {

    }

}
