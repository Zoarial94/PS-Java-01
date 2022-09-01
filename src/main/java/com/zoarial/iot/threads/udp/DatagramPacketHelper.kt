package com.zoarial.iot.threads.udp

import com.zoarial.PrintBaseClass
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.net.DatagramPacket
import java.util.*

class DatagramPacketHelper(dp: DatagramPacket) : PrintBaseClass("Datagram Packet Helper") {
    val rawIn: BufferedInputStream
    val `in`: DataInputStream

    init {
        rawIn = BufferedInputStream(ByteArrayInputStream(dp.data))
        `in` = DataInputStream(rawIn)
    }

    @Throws(IOException::class)
    fun readString(): String {
        val str = StringBuilder()
        var b = `in`.readByte()
        while (b.toInt() != 0) {
            str.append(Char(b.toUShort()))
            b = `in`.readByte()
        }
        return str.toString()
    }

    @Throws(IOException::class)
    fun readUUID(): UUID {
        return UUID(`in`.readLong(), `in`.readLong())
    }

    @Throws(IOException::class)
    fun readInt(): Int {
        return `in`.readInt()
    }

    @Throws(IOException::class)
    fun readLong(): Long {
        return `in`.readLong()
    }

    @Throws(IOException::class)
    fun readByte(): Byte {
        return `in`.readByte()
    }

    @Throws(IOException::class)
    fun readBytes(i: Int): ByteArray {
        return `in`.readNBytes(i)
    }
}