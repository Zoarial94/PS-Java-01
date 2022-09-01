package com.zoarial

import com.zoarial.iot.server.ServerNode

object Main {
    private var _server: ServerNode? = null
    fun println(str: String) {
        kotlin.io.println("Main: $str")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val daemonize = false
        var status: Boolean
        if (daemonize) {
            //daemonize
        }
        status = false
        _server = ServerNode()
        try {
            status = _server!!.init()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (!status) {
            return
        }
        status = false
        status = _server!!.start()
        if (status) {
            println("Server successfully started.")
        } else {
            println("Server failed to start.")
            return
        }
        Runtime.getRuntime().addShutdownHook(Thread {
            _server!!.close(true) // Wait on all threads
            System.out.flush() // Make sure we see all data on std out before we finish.
        })
    }
}