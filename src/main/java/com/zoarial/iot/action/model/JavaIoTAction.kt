package com.zoarial.iot.action.model

import com.zoarial.iot.action.helper.JavaIoTActionExecHelper
import jakarta.persistence.Entity
import jakarta.persistence.Transient
import lombok.AccessLevel
import lombok.Getter
import lombok.NoArgsConstructor
import lombok.Setter
import java.util.function.Function

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
open class JavaIoTAction @JvmOverloads constructor(name: String? = null, args: Byte? = null, securityLevel: Byte? = null, encrypt: Boolean? = null, local: Boolean? = null) : IoTAction(name, null, securityLevel, encrypt, local, args) {

    @Transient
    @Getter(AccessLevel.NONE)
    private var exec: Function<IoTActionArgumentList, String>? = null

    // This tries to cache the function into the current object (if the function exists)
    private fun getExec(): Function<IoTActionArgumentList, String>? {
        if (exec != null) {
            return exec
        }
        exec = JavaIoTActionExecHelper.getFunction(name!!)
        return exec
    }

    override val isValid: Boolean
        get() = getExec() != null

    override fun privExecute(args: IoTActionArgumentList): String {
        // This function only can run if isValid is true.
        // This means this function will only run if exec is not null.
        return exec!!.apply(args)
    }
}