package com.zoarial.iot.model

import java.net.InetAddress
import java.util.*

class ServerInformation(val hostname: String, val uuid: UUID, val nodeType: Byte,
                        val serverPort: Int, val isVolatile: Boolean, val isHeadCapable: Boolean,
                        var messageTimeout: Int, var pingTimeout: Int, val networkDeviceName: String,
                        var headNodes: List<InetAddress>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ServerInformation) return false
        return nodeType == other.nodeType && isVolatile == other.isVolatile && serverPort == other.serverPort && isHeadCapable == other.isHeadCapable && messageTimeout == other.messageTimeout && pingTimeout == other.pingTimeout && hostname == other.hostname && uuid == other.uuid && networkDeviceName == other.networkDeviceName && headNodes == other.headNodes
    }

    override fun hashCode(): Int {
        return Objects.hash(hostname, uuid, nodeType, isVolatile, serverPort, isHeadCapable, networkDeviceName, messageTimeout, pingTimeout, headNodes)
    }

    constructor(info: ServerInformation) : this(info.hostname, info.uuid, info.nodeType, info.serverPort, info.isVolatile, info.isHeadCapable, info.messageTimeout, info.pingTimeout, info.networkDeviceName, ArrayList<InetAddress>(info.headNodes)) {}
}