package com.zoarial.iot.action.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.UUID;
import java.util.function.Function;

@Entity

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JavaIoTAction extends IoTAction {
    @Transient @Column(nullable = false)
    Function<IoTActionArgumentList, String> exec;

    public JavaIoTAction(String name, byte level, boolean encrypted, boolean local, byte args, Function<IoTActionArgumentList, String> exec) {
        super.setName(name);
        super.setSecurityLevel(level);
        super.setArguments(arguments);
        super.setEncrypted(encrypted);
        super.setLocal(local);
        super.setArguments(args);
        this.exec = exec;
    }

    public JavaIoTAction(String name, UUID uuid, byte level, boolean encrypted, boolean local, byte args, Function<IoTActionArgumentList, String> exec) {
        super(name, uuid, level, encrypted, local, args);
        this.exec = exec;
    }

    @Override
    public boolean isValid() {
        return exec != null;
    }

    @Override
    public String privExecute(IoTActionArgumentList args) {
        return exec.apply(args);
    }
}
