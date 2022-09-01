package com.zoarial.iot.network

import com.zoarial.iot.model.IoTBasicType
import java.nio.ByteBuffer
import java.util.*

class IoTPacketSection {
    val type: IoTBasicType
    private val str: String?
    private val num: Long
    private val uuid_low: Long

    constructor(str: String?) {
        type = if (str == "ZIoT") {
            IoTBasicType.HEADER
        } else {
            IoTBasicType.STRING
        }
        this.str = str
        num = 0
        uuid_low = 0
    }

    constructor(b: Byte) {
        type = IoTBasicType.BYTE
        num = (b.toInt() and 0xFF).toLong()
        uuid_low = 0
        str = ""
    }

    constructor(i: Int) {
        type = IoTBasicType.INTEGER
        num = i.toLong()
        uuid_low = 0
        str = ""
    }

    constructor(i: Long) {
        type = IoTBasicType.LONG
        num = i
        uuid_low = 0
        str = ""
    }

    constructor(uuid: UUID) {
        type = IoTBasicType.UUID
        num = uuid.mostSignificantBits
        uuid_low = uuid.leastSignificantBits
        str = ""
    }

    constructor(b: Boolean) {
        type = IoTBasicType.BOOLEAN
        num = (if (b) 1 else 0).toLong()
        uuid_low = 0
        str = ""
    }

    // Set null byte
    val byteList: ByteArray
        get() = when (type) {
            IoTBasicType.BYTE -> byteArrayOf(num.toByte())
            IoTBasicType.LONG -> ByteBuffer.allocate(8).putLong(num).array()
            IoTBasicType.UUID -> ByteBuffer.allocate(16).putLong(num).putLong(uuid_low).array()
            IoTBasicType.BOOLEAN -> ByteBuffer.allocate(1).put(num.toByte()).array()
            IoTBasicType.STRING, IoTBasicType.JSON -> {
                val strArr = str!!.toByteArray()
                val len = strArr.size
                val byteArr = ByteArray(strArr.size + 1)
                System.arraycopy(strArr, 0, byteArr, 0, len)
                byteArr[len] = 0 // Set null byte
                byteArr
            }

            IoTBasicType.INTEGER -> ByteBuffer.allocate(4).putInt(num.toInt()).array()
            IoTBasicType.HEADER -> str!!.toByteArray()
        }
}