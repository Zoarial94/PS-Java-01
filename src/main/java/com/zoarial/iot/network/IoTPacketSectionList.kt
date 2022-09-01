package com.zoarial.iot.network

import java.util.*

class IoTPacketSectionList : ArrayList<IoTPacketSection> {
    constructor() : super()
    constructor(initialLength: Int) : super(initialLength) {}

    fun add(str: String) {
        super.add(IoTPacketSection(str))
    }

    fun add(b: Boolean) {
        super.add(IoTPacketSection(b))
    }

    fun add(i: Int) {
        super.add(IoTPacketSection(i))
    }

    fun add(b: Byte) {
        super.add(IoTPacketSection(b))
    }

    fun add(l: Long) {
        super.add(IoTPacketSection(l))
    }

    fun add(uuid: UUID) {
        super.add(IoTPacketSection(uuid))
    }

    val networkResponse: ByteArray
        get() {
            val fullBytesArr: ByteArray
            var len = 0
            for (section in this) {
                len += section.byteList.size
            }
            fullBytesArr = ByteArray(len)
            len = 0
            for (section in this) {
                System.arraycopy(section.byteList, 0, fullBytesArr, len, section.byteList.size)
                len += section.byteList.size
            }
            return fullBytesArr
        }
}