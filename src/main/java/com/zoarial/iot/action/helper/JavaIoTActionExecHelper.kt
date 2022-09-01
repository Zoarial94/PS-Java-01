package com.zoarial.iot.action.helper

import com.zoarial.iot.action.model.IoTActionArgumentList
import java.util.function.Function

object JavaIoTActionExecHelper {
    val map = HashMap<String, Function<IoTActionArgumentList, String>>()
    fun getFunction(actionName: String): Function<IoTActionArgumentList, String>? {
        return map[actionName]
    }
}