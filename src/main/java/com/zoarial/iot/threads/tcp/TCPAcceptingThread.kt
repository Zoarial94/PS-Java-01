package com.zoarial.iot.threads.tcp

import com.zoarial.PrintBaseClass
import com.zoarial.iot.dto.ZoarialDTO
import com.zoarial.iot.model.RequestContext
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
            println("Adding TCP requests to map")
            addTCPRequestsToMap()

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

    private fun addTCPRequestsToMap() {
        _server.registerRequest("action", ZoarialDTO.V1.Request.Action::class) { actionData: ZoarialDTO.V1.Request.Action, context: RequestContext ->
            return@registerRequest ""
        }
        
        _server.registerRequest("updateActionSecurityLevel", ZoarialDTO.V1.Request.UpdateActionSecurityLevel::class){ actionData: ZoarialDTO.V1.Request.UpdateActionSecurityLevel, context: RequestContext ->
            return@registerRequest ""
        }

        _server.registerRequest("updateActionEncrypt", ZoarialDTO.V1.Request.UpdateActionEncrypt::class){ actionData: ZoarialDTO.V1.Request.UpdateActionEncrypt, context: RequestContext ->
            return@registerRequest ""
        }

        _server.registerRequest("updateActionLocal", ZoarialDTO.V1.Request.UpdateActionDescription::class) { actionData: ZoarialDTO.V1.Request.UpdateActionDescription, context: RequestContext ->
            return@registerRequest ""
        }

        _server.registerRequest("infoAction", ZoarialDTO.V1.Request.InfoAction::class){ actionData: ZoarialDTO.V1.Request.InfoAction, context: RequestContext ->
            return@registerRequest ""
        }

        _server.registerRequest("infoActions", ZoarialDTO.V1.Request.InfoActions::class){ actionData: ZoarialDTO.V1.Request.InfoActions, context: RequestContext ->
            return@registerRequest ""
        }

        _server.registerRequest("infoGeneral", ZoarialDTO.V1.Request.InfoGeneral::class){ actionData: ZoarialDTO.V1.Request.InfoGeneral, context: RequestContext ->
            return@registerRequest ""
        }

        _server.registerRequest("infoNodes", ZoarialDTO.V1.Request.InfoNodes::class){ actionData: ZoarialDTO.V1.Request.InfoNodes, context: RequestContext ->
            return@registerRequest ""
        }

    }
    companion object {
        private val _started = AtomicBoolean(false)
    }
}