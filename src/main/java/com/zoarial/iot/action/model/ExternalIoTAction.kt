package com.zoarial.iot.action.model

import jakarta.persistence.Entity
import lombok.NoArgsConstructor
import java.lang.RuntimeException

@Entity
@NoArgsConstructor
class ExternalIoTAction : IoTAction() {
    override val isValid: Boolean
        get() = true

    override fun privExecute(args: IoTActionArgumentList): String {
        // TODO: do something similar to JavaIoTAction where a socket is queried when run
        throw RuntimeException("Attempting to execute a remote action.")
    }
}