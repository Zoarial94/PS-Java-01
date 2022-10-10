package com.zoarial.iot.model

import com.zoarial.iot.action.dao.IoTActionDAO
import com.zoarial.iot.node.dao.IoTNodeDAO
import me.zoarial.networkArbiter.ZoarialNetworkArbiter

data class RequestContext(val ioTNodeDAO: IoTNodeDAO, val ioTActionDAO: IoTActionDAO, val networkArbiter: ZoarialNetworkArbiter)