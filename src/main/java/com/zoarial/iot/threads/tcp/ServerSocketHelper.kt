package com.zoarial.iot.threads.tcp

import com.zoarial.PrintBaseClass
import com.zoarial.iot.server.ServerServer
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/*
 *
 * Just accepts new connections in a non-blocking fashion
 *
 */
class ServerSocketHelper(private val _server: ServerServer, private val servSocket: ServerSocket) : PrintBaseClass("ServerSocketHelper"), Runnable {
    //  Final doesn't mean const
    //  Final means it can't be reassigned and makes for a good concurrency lock.
    val _queue = ArrayBlockingQueue<Socket?>(32)
    override fun run() {
        var tmp: Socket? = null
        while (!_server.isClosed && !servSocket.isClosed) {
            try {
                tmp = servSocket.accept() // Throws SocketException if closed
                println("Accepting new socket.")
                _queue.put(tmp) // Throws InterruptedException if closed
            } catch (ex: InterruptedException) {
                println("Interrupted, no longer accepting new connections.")
                // TODO: redo this. Its messy and possibly redundant
                // Make sure we are closing if we aren't already
                close()
                // Make sure we close the socket which may or may-not have been added to the queue.
                if (tmp != null && !tmp.isClosed) {
                    try {
                        tmp.close()
                    } catch (ioException: IOException) {
                        ioException.printStackTrace()
                        println("Something must have gone terribly wrong to get here.")
                    }
                }
                // Make sure to clean up all sockets still in the queue
                cleanup()
            } catch (ex: SocketException) {
                println("Interrupted, no longer accepting new connections.")
                close()
                if (tmp != null && !tmp.isClosed) {
                    try {
                        tmp.close()
                    } catch (ioException: IOException) {
                        ioException.printStackTrace()
                        println("Something must have gone terribly wrong to get here.")
                    }
                }
                cleanup()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
        // If _server is closed, then ServerSocket is already closed too.
        println("Finished Thread")
    }

    // If we're closed, then the class will handle and close the sockets
    val isNextSocketEmpty: Boolean
        get() =// If we're closed, then the class will handle and close the sockets
            if (servSocket.isClosed) {
                true
            } else _queue.isEmpty()

    /*
     *  Function will not block. If there is no object, then it will return null
     */
    fun pollNextSocket(): Socket? {
        // If we're closed, then the class will handle and close the sockets
        return if (servSocket.isClosed) {
            null
        } else _queue.poll()
    }

    /*
     *  Function will block until the timeout is reached. It will then return null
     */
    fun pollNextSocket(timeout: Long, timeUnit: TimeUnit?): Socket? {
        // If we're closed, then the class will handle and close the sockets
        if (servSocket.isClosed) {
            return null
        }
        try {
            return _queue.poll(timeout, timeUnit)
        } catch (e: InterruptedException) {
            println("Interrupted, returning null.")
        }
        return null
    }

    /*
     *  Function will block until an object is retrieved.
     *  Function could return null if an exception occurs.
     */
    fun takeNextSocket(): Socket? {
        // If we're closed, then the class will handle and close the sockets
        if (servSocket.isClosed) {
            return null
        }
        try {
            return _queue.take()
        } catch (e: InterruptedException) {
            println("Interrupted, returning null.")
        }
        return null
    }

    val isClosed: Boolean
        get() = servSocket.isClosed

    fun close() {
        try {
            servSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun cleanup() {
        var s: Socket?
        // Close all sockets still in the queue
        while (!isNextSocketEmpty) {
            try {
                s = _queue.take()
                if (!s!!.isClosed) {
                    try {
                        s.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        println("Something went wrong in cleanup")
                    }
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}