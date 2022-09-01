package com.zoarial.iot.threads.udp

import com.zoarial.PrintBaseClass
import com.zoarial.iot.server.ServerServer
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class DatagramSocketHelper(private val _server: ServerServer, private val _ds: DatagramSocket) : PrintBaseClass("DatagramSocketHelper"), Runnable {
    private val _queue = ArrayBlockingQueue<DatagramPacket>(32)
    private val BUF_SIZE = 65535
    val isNextDataEmpty: Boolean
        get() = _queue.isEmpty()

    /*
     *  Function will not block. If there is no object, then it will return null
     */
    fun pollNextData(): Optional<DatagramPacket> {
        return Optional.ofNullable(_queue.poll())
    }

    /*
     *  Function will block until the timeout is reached or an object is found.
     *  If the timeout is reached, null is returned.
     */
    @Throws(InterruptedException::class)
    fun pollNextData(timeout: Long, timeUnit: TimeUnit?): Optional<DatagramPacket> {
        try {
            return Optional.ofNullable(_queue.poll(timeout, timeUnit))
        } catch (ex: InterruptedException) {
            println("Interrupted, returning null.")
        }
        return Optional.empty()
    }

    /*
     *  Function will block until an object is retrieved.
     *  Function could return null if an exception occurs.
     */
    fun takeNextData(): Optional<DatagramPacket> {
        try {
            return Optional.of(_queue.take())
        } catch (e: InterruptedException) {
            println("Interrupted, returning null.")
        }
        return Optional.empty()
    }

    override fun run() {
        println("Starting thread on port " + _ds.localPort)
        /*
        try {
            _ds.setSoTimeout(10000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
         */while (!_server.isClosed && !_ds.isClosed) {
            val dp = DatagramPacket(ByteArray(BUF_SIZE), BUF_SIZE)
            try {
                _ds.receive(dp)
                println("Received packet, adding to queue.")
                _queue.put(dp)
            } catch (ex: SocketException) {
                println("Interrupted, no longer receiving new packets.")
            } catch (ex: InterruptedException) {
                println("Interrupted, no longer receiving new packets.")
            } catch (ignore: SocketTimeoutException) {
                println("Timeout")
            } catch (e: IOException) {
                e.printStackTrace()
                println("Something went wrong!")
            }
        }
        // If _server is closed, then DatagramSocket is already closed
        println("Finished thread.")
    }

    @Throws(IOException::class)
    fun send(dp: DatagramPacket?) {
        _ds.send(dp)
    }

    val isClosed: Boolean
        get() = _ds.isClosed

    fun close() {
        _ds.close()
    }
}