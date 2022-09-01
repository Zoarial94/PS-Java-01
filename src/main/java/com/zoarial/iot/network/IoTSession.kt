package com.zoarial.iot.network

// TODO: refactor all uses of this class
class IoTSession @JvmOverloads constructor(val sessionID: Int, val type: IoTSessionType = IoTSessionType.OTHER) {
    val sessionStart: Long = System.currentTimeMillis()
    var lastHeardFrom: Long = 0

    enum class IoTSessionType {
        ACTION, INFO, OTHER
    }

}