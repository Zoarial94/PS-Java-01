package com.zoarial.threads;

import com.zoarial.PrintBaseClass;
import com.zoarial.ServerServer;

public class NonHeadUDPThread extends PrintBaseClass implements Runnable {

    private final ServerServer _server;

    NonHeadUDPThread(ServerServer server) {
        super("NonHeadUDPThread");
        _server = server;

    }

    public void run() {

    }
}
