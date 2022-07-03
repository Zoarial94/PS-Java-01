package com.zoarial.iot.action.model;

import com.zoarial.iot.action.helper.JavaIoTActionExecHelper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import java.util.function.Function;

@Entity

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JavaIoTAction extends IoTAction {
    @Transient
    @Getter(AccessLevel.NONE)
    private Function<IoTActionArgumentList, String> exec;

    public JavaIoTAction(String name, byte args, byte securityLevel, boolean encrypt, boolean local) {
        setName(name);
        setArguments(args);
        setSecurityLevel(securityLevel);
        setEncrypted(encrypt);
        setLocal(local);
    }

    // This tries to cache the function into the current object (if the function exists)
    private Function<IoTActionArgumentList, String> getExec() {
        if(exec != null) {
            return exec;
        }
        exec = JavaIoTActionExecHelper.getFunction(getName());
        return exec;
    }

    @Override
    public boolean isValid() {
        return getExec() != null;
    }

    @Override
     protected String privExecute(IoTActionArgumentList args) {
        // This function only can run if isValid is true.
        // This means this function will only run if exec is not null.
        return exec.apply(args);
    }
}
