package com.zoarial.iot.threads.tcp

import com.zoarial.PrintBaseClass
import com.zoarial.iot.server.ServerServer
import java.util.concurrent.atomic.AtomicBoolean

class TCPAcceptingThread(private val _server: ServerServer, private val _serverSocketHelper: ServerSocketHelper?) : PrintBaseClass("HeadTCPAcceptingThread"), Runnable {
    var _inSockets = ArrayList<SocketHelper>()
    override fun run() {
        // There should only be one Accepting thread.
        // The accepting thread will start a thread for each socket.
        if (_started.compareAndSet(false, true)) {
            // We acquired the lock, start the thread.
            println("Starting...")

            // Start
            loop()
            println("Shutting down...")

            // Since the server is closing, interrupt all created threads (That are still running)
            for (t in TCPThread.Companion.getThreads()) {
                t.interrupt()
            }
            println("Finished shutting down.")
        } else {
            println("A thread has already started. Not starting.")
        }
    }

    /*
     * LOOP
     */
    private fun loop() {
        while (!_server.isClosed) {
            /*
             * TCP Handler Loop
             */
            val socket = _serverSocketHelper!!.takeNextSocket() ?: continue

            // HeadTCPThread manages its own threads list.
            // Since they come and go, it is easier to let each thread add/remove itself
            Thread(TCPThread(_server, SocketHelper(socket))).start()
        }
    }

    companion object {
        private val _started = AtomicBoolean(false)
    }
}