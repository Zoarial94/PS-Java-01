package com.zoarial.threads;

import com.zoarial.PrintBaseClass;
import com.zoarial.ServerServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class HeadTCPAcceptingThread extends PrintBaseClass implements Runnable {
    private final ServerServer _server;
    private static final AtomicBoolean _started = new AtomicBoolean(false);
    private final ServerSocketHelper _serverSocketHelper;
    ArrayList<inSocketWrapper> _inSockets = new ArrayList<>();

    public HeadTCPAcceptingThread(ServerServer server, ServerSocketHelper _helper) {
        super("HeadTCPAcceptingThread");
        _serverSocketHelper = _helper;
        _server = server;

    }

    public void run() {
        // There should only be one Accepting thread.
        // The accepting thread will start a thread for each socket.
        if(_started.compareAndSet(false, true)) {
            // We acquired the lock, start the thread.
            loop();
        } else {
            println("A thread has already started. Not starting.");
        }
    }

    /*
     * LOOP
     */
    private void loop() {
        int sleepTime = 5000;
        DatagramPacket dp;
        while(!_server.isClosed()) {
            try {
                /*
                 * TCP Handler Loop
                 */
                while(!_serverSocketHelper.isNextSocketEmpty()) {
                    _inSockets.add(new inSocketWrapper(_serverSocketHelper.pollNextSocket()));
                }

                for(inSocketWrapper socketWrapper : _inSockets) {
                    println("Socket Packet: " + socketWrapper.in.readUTF());
                }

                println("Sleeping for " + sleepTime + ".");
                Thread.sleep(sleepTime);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }

    }

}
