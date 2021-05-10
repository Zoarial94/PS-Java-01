package com.zoarial.iot.models;

import com.zoarial.iot.models.actions.IoTAction;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@NamedQueries({
        @NamedQuery(name = "getAll", query = "SELECT n FROM IoTNode n"),
        @NamedQuery(name = "getByUUID", query = "SELECT n FROM IoTNode n WHERE n.uuid = :uuid"),
        @NamedQuery(name = "getByName", query = "SELECT n FROM IoTNode n WHERE n.hostname = :hostname"),
})
public class IoTNode {
    @Id
    @Column(unique = true, nullable = false, columnDefinition = "binary(16)")
    private UUID uuid;
    private String hostname;
    @Column(nullable = false)
    private byte nodeType;
    @Column(nullable = false)
    private long lastHeardFrom;

    @OneToMany
    List<IoTAction> actions;

    public IoTNode() {

    }

    public IoTNode(String hostname, UUID uuid, byte nodeType) {
        this.hostname = hostname;
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public List<IoTAction> getActions() {
        return actions;
    }

    public void setActions(List<IoTAction> actions) {
        this.actions = actions;
    }

    public UUID getUuid() {
        return uuid;
    }
}
