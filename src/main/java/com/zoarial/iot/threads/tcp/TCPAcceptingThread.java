package com.zoarial.iot.threads.tcp;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.server.ServerServer;

import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPAcceptingThread extends PrintBaseClass implements Runnable {
    private final ServerServer _server;
    private static final AtomicBoolean _started = new AtomicBoolean(false);
    private final ServerSocketHelper _serverSocketHelper;
    ArrayList<SocketHelper> _inSockets = new ArrayList<>();

    public TCPAcceptingThread(ServerServer server, ServerSocketHelper _helper) {
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
            for(Thread t : TCPThread.getThreads()) {
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
            Socket socket = _serverSocketHelper.takeNextSocket();
            if(socket == null) {
                continue;
            }

            // HeadTCPThread manages its own threads list.
            // Since they come and go, it is easier to let each thread add/remove itself
            new Thread(new TCPThread(_server, new SocketHelper(socket))).start();

        }

    }

}
