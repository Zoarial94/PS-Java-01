package com.zoarial;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class JavaIoTAction extends IoTAction {
    Supplier<String> exec;

    public JavaIoTAction(String name, UUID uuid, byte level, int args, Supplier<String> exec) {
        super(name, uuid, level, args);
        this.exec = exec;
    }

    @Override
    public String privExecute(List<String> args) {
        return exec.get();
    }
}
