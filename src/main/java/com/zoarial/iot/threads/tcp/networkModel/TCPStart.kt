package com.zoarial.iot.threads.tcp.networkModel

import me.zoarial.networkArbiter.annotations.ZoarialNetworkObject
import me.zoarial.networkArbiter.annotations.ZoarialObjectElement
import java.util.*

@ZoarialNetworkObject
class TCPStart(@field:ZoarialObjectElement(placement = 1) var version: Byte, @field:ZoarialObjectElement(placement = 2) var sessionId: Int, request: String) {

    @ZoarialObjectElement(placement = 3)
    var request = String()

    init {
        this.request = request
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val tcpStart = other as TCPStart
        return version == tcpStart.version && sessionId == tcpStart.sessionId && request == tcpStart.request
    }

    override fun hashCode(): Int {
        return Objects.hash(version, sessionId, request)
    }
}