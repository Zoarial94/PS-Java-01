package com.zoarial.iot.models.actions;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class JavaIoTAction extends IoTAction {
    Function<IoTActionArgumentList, String> exec;

    public JavaIoTAction(String name, UUID uuid, byte level, boolean encrypted, boolean local, byte args, Function<IoTActionArgumentList, String> exec) {
        super(name, uuid, level, encrypted, local, args);
        this.exec = exec;
    }

    @Override
    public String privExecute(IoTActionArgumentList args) {
        return exec.apply(args);
    }
}
