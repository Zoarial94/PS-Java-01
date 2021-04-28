package com.zoarial.threads;

import com.zoarial.PrintBaseClass;
import com.zoarial.ServerServer;

import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
            println("Starting...");

            // Start
            loop();

            // Since the server is closing, interrupt all created threads (That are still running)
            for(Thread t : HeadTCPThread.getThreads()) {
                t.interrupt();
            }
        } else {
            println("A thread has already started. Not starting.");
        }
    }

    /*
     * LOOP
     */
    private void loop() {
        while(!_server.isClosed()) {
            /*
             * TCP Handler Loop
             */
            Socket socket = _serverSocketHelper.pollNextSocket();
            if(socket == null) {
                continue;
            }

            // HeadTCPThread manages its own threads list.
            // Since they come and go, it is easier to let each thread add/remove itself
            new Thread(new HeadTCPThread(_server, new inSocketWrapper(socket))).start();

        }

    }

}
