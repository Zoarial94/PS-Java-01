package com.zoarial.iot.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Entity
public class IoTNode {
    @Id
    @Column(unique = true, nullable = false, columnDefinition = "binary(16)")
    private UUID uuid;
    @Column(nullable = false)
    private byte nodeType;
    @Column(nullable = false)
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
