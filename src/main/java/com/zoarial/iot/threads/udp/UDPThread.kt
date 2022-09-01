package com.zoarial.iot.threads.udp

import com.zoarial.PrintBaseClass
import com.zoarial.iot.network.IoTPacketSectionList
import com.zoarial.iot.node.dao.IoTNodeDAO
import com.zoarial.iot.node.model.IoTNode
import com.zoarial.iot.server.ServerServer
import com.zoarial.iot.server.ServerServer.Companion.printArray
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit

/*
 * This thread shouldn't have to be multithreaded, but could be.
 */
class UDPThread(private val _server: ServerServer, private val _datagramSocketHelper: DatagramSocketHelper?) : PrintBaseClass("UDPThread"), Runnable {
    private val ioTNodeDAO: IoTNodeDAO = IoTNodeDAO()

    override fun run() {
        println("Starting...")
        try {
            if (_server.serverInfo.isHeadCapable) {
                println("Starting Head Loop.")
                headUDPLoop()
            } else {
                println("Starting Non-Head Loop.")
                nonHeadUDPLoop()
            }
        } catch (ex: Exception) {
            println("Unexpectedly closed.")
            ex.printStackTrace()
        }
        println("Finished.")
    }

    private fun headUDPLoop() {
        /*
         * LOOP
         */
        var dp: Optional<DatagramPacket>
        while (!_server.isClosed) {
            /*
             * UDP Handler Loop
             */
            dp = try {
                _datagramSocketHelper!!.pollNextData(10, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                e.printStackTrace()
                continue
            }
            if (dp.isEmpty) {
                //println("Timed out.");
                continue
            }
            //String str = ServerServer.buildString(dp.getData()).toString();
            //println("Datagram Packet: " + str);
            replyHeadNodes(dp.get())
            //TODO: create a queue in ServerServer for things that need to be done
            //TODO: get info about node just discovered. (This should go into that queue)
        }
    }

    private fun nonHeadUDPLoop() {
        println("Initializing...")
        //Find head, either by reading from a save file, or a broadcast.
        while (!_server.isClosed) {
            val sectionList = IoTPacketSectionList()
            sectionList.add("ZIoT")
            sectionList.add(_server.serverInfo.nodeType)
            sectionList.add(_server.serverInfo.hostname)
            sectionList.add(_server.serverInfo.uuid)
            val buf = sectionList.networkResponse
            val addr = byteArrayOf(10, 94, 50, 95.toByte())
            println("Sending packet...")
            var optDp: Optional<DatagramPacket>
            var dp: DatagramPacket
            try {
                _datagramSocketHelper!!.send(DatagramPacket(buf, buf.size, InetAddress.getByAddress(addr), _server.serverInfo.serverPort))
            } catch (e: IOException) {
                println("Error sending packet.")
                e.printStackTrace()
                break
            }
            try {
                println("Waiting for response...")
                optDp = _datagramSocketHelper.pollNextData(30, TimeUnit.SECONDS)
                if (optDp.isEmpty) {
                    println("Must have timed out while waiting for response. Retrying.")
                    continue
                }
                dp = optDp.get()
                val data = dp.data
                if (String(data, 0, 4) != "ZIoT") {
                    println("Not Z-IoT. Trying again...")
                    continue
                }
                println("Packet is Z-IoT")
                println("Length: " + dp.length)
                val headAddrs = arrayOf(InetAddress.getByAddress(Arrays.copyOfRange(data, 4, 8)),
                        InetAddress.getByAddress(Arrays.copyOfRange(data, 8, 12)),
                        InetAddress.getByAddress(Arrays.copyOfRange(data, 12, 16)))
                for (headAddr in headAddrs) {
                    println("Address: $headAddr")
                    if (!Arrays.equals(headAddr.address, byteArrayOf(0, 0, 0, 0))) {
                        val size = _server.serverInfo.headNodes.size
                        for (i in 0 until size) {
                            val servAddr = _server.serverInfo.headNodes[i]
                            if (Arrays.equals(servAddr.address, headAddr.address)) {
                                // We already have this address in the list.
                                break
                            } else if (Arrays.equals(servAddr.address, byteArrayOf(0, 0, 0, 0))) {
                                println("Replacing server address...")
                                _server.serverInfo.headNodes.toMutableList()[i] = headAddr
                                break
                            }
                        }
                    }
                }
                printArray(data, dp.length)
                for (i in headAddrs) {
                    printArray(i.address)
                }
                return
            } catch (e: IOException) {
                //e.printStackTrace();
            } catch (e: InterruptedException) {
            }
        }
    }

    fun replyHeadNodes(dp: DatagramPacket) {
        val dpHelper = DatagramPacketHelper(dp)
        val HEADER = "ZIoT".toByteArray(StandardCharsets.UTF_8)
        try {
            if (Arrays.compare(HEADER, dpHelper.readBytes(4)) == 0) {
                println("Is Z-IoT Packet.")
            } else {
                println("Is not Z-IoT packet.")
                return
            }
            val nodeType = dpHelper.readByte()
            val hostname = dpHelper.readString()
            val uuid = dpHelper.readUUID()
            println("Node type is: $nodeType")
            println("Hostname is: $hostname")
            println("UUID is: $uuid")
            val response = ByteArray(16)
            System.arraycopy("ZIoT".toByteArray(), 0, response, 0, 4)
            System.arraycopy(_server.serverIP.address, 0, response, 4, 4)
            val dpResponse = DatagramPacket(response, response.size, dp.address, dp.port)
            _datagramSocketHelper!!.send(dpResponse)
            val newNode = IoTNode(hostname, uuid, nodeType)
            newNode.lastIp = dp.address.address
            newNode.lastHeardFrom = System.currentTimeMillis()
            if (!ioTNodeDAO.containsNode(newNode)) {
                ioTNodeDAO.persist(newNode)
            } else {
                ioTNodeDAO.update(newNode)
            }
            _server.getAndUpdateInfoAboutNode(newNode)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}