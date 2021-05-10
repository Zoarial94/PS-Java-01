package com.zoarial.iot.models;

import com.zoarial.iot.models.actions.IoTAction;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;
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

    @OneToMany
    List<IoTAction> actions;

    public IoTNode(UUID uuid, byte nodeType) {
        this.uuid = uuid;
        this.nodeType = nodeType;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setNodeType(byte nodeType) {
        this.nodeType = nodeType;
    }

    public long getLastHeardFrom() {
        return lastHeardFrom;
    }

    public void setLastHeardFrom(long lastHeardFrom) {
        this.lastHeardFrom = lastHeardFrom;
    }

    public byte getNodeType() {
        return nodeType;
    }

    public UUID getUuid() {
        return uuid;
    }
}
