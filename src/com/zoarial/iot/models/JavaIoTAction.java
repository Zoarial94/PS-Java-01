package com.zoarial.iot.models;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class JavaIoTAction extends IoTAction {
    Function<List<String>, String> exec;

    public JavaIoTAction(String name, UUID uuid, byte level, byte args, Function<List<String>, String> exec) {
        super(name, uuid, level, args);
        this.exec = exec;
    }

    @Override
    public String privExecute(List<String> args) {
        return exec.apply(args);
    }
}
