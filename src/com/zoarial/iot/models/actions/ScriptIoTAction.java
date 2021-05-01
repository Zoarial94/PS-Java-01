package com.zoarial.iot.models.actions;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class ScriptIoTAction extends IoTAction {
    Path path;

    public ScriptIoTAction(String name, UUID uuid, byte level, boolean encrypted, boolean local, byte args, Path file) {
        super(name, uuid, level, encrypted, local, args);
        path = file;

    }

    @Override
    public String privExecute(IoTActionArgumentList args) {
        // Make sure permission are still valid, then execute.
        return "";
    }
}
