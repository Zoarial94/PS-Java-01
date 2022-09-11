package com.zoarial.iot.threads.tcp

import com.zoarial.PrintBaseClass
import me.zoarial.networkArbiter.ZoarialNetworkArbiter
import java.io.*
import java.net.Socket
import java.util.*
import kotlin.system.exitProcess

class SocketHelper(val inSocket: Socket) : PrintBaseClass("Socket Helper") {
    val rawOut: BufferedOutputStream
    val out: DataOutputStream
    val rawIn: BufferedInputStream
    val `in`: DataInputStream
    var isEncrypted = false
    var isLocal = false
    private var networkArbiter: ZoarialNetworkArbiter = ZoarialNetworkArbiter

    init {
        var tempOut: DataOutputStream? = null
        var tempIn: DataInputStream? = null
        var tempRawIn: BufferedInputStream? = null
        var tempRawOut: BufferedOutputStream? = null
        if (inSocket.remoteSocketAddress.toString().startsWith("/127.")) {
            isEncrypted = true // It's a local connection. Same effect as encryption.
            isLocal = true
        } else {
            isEncrypted = false
            isLocal = false
        }
        try {
            tempRawOut = BufferedOutputStream(inSocket.getOutputStream())
            tempOut = DataOutputStream(tempRawOut)
            tempRawIn = BufferedInputStream(inSocket.getInputStream())
            tempIn = DataInputStream(tempRawIn)
        } catch (ex: IOException) {
            println("Something happened while creating inSocketWrapper. Exiting.")
            exitProcess(-1)
        }
        out = tempOut
        `in` = tempIn
        rawOut = tempRawOut
        rawIn = tempRawIn
    }

    @Throws(IOException::class)
    fun readKey(): String {
        val str = StringBuilder()
        var b = `in`.readByte()
        while (b != ':'.code.toByte()) {
            str.append(Char(b.toUShort()))
            b = `in`.readByte()
        }
        return str.toString()
    }

    @Throws(IOException::class)
    fun readValue(): String {
        val str = StringBuilder()
        var b = `in`.readByte()
        while (b != ','.code.toByte() && b != '.'.code.toByte()) {
            str.append(Char(b.toUShort()))
            b = `in`.readByte()
        }
        return str.toString()
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
    fun readBoolean(): Boolean {
        return `in`.readBoolean()
    }

    @Throws(IOException::class)
    fun readJson(): String {
        val json = StringBuilder()
        var b = `in`.readByte()
        if (b != '{'.code.toByte()) {
            return ""
        }
        json.append(Char(b.toUShort()))
        var num = 1
        while (num != 0) {
            b = `in`.readByte()
            if (b == '{'.code.toByte()) {
                num++
            } else if (b == '}'.code.toByte()) {
                num--
            }
            json.append(Char(b.toUShort()))
        }

        // Read the 0 appended to the end
        if (`in`.readByte().toInt() != 0) {
            throw RuntimeException("Expected null byte termination")
        }
        return json.toString()
    }

    val isClosed: Boolean
        get() = inSocket.isClosed

    @Throws(IOException::class)
    fun close() {
        out.close()
        `in`.close()
        inSocket.close()
    }
}