package com.zoarial.iot.models;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class ScriptIoTAction extends IoTAction {
    Path path;

    public ScriptIoTAction(String name, UUID uuid, byte level, byte args, Path file) {
        super(name, uuid, level, args);
        path = file;

    }

    @Override
    public String privExecute(List<String> args) {
        // Make sure permission are still valid, then execute.
        return "";
    }
}
