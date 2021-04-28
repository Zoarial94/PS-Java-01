package com.zoarial.iot.models;

import java.util.UUID;

public class IoTNode {
    private byte nodeType;
    private UUID uuid;
    private long lastHeardFrom;

    public IoTNode(UUID uuid, byte nodeType) {
        this.uuid = uuid;
        this.nodeType = nodeType;
    }

    public byte getNodeType() {
        return nodeType;
    }

    public UUID getUuid() {
        return uuid;
    }
}
