package com.zoarial.iot.node.model;

import com.zoarial.iot.action.model.IoTAction;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
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
    private byte[] lastIp;

    // Map to the class member 'node' in IoTAction
    @OneToMany(mappedBy = "node")
    private List<IoTAction> actions;

    public IoTNode() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IoTNode)) return false;
        IoTNode ioTNode = (IoTNode) o;
        return nodeType == ioTNode.nodeType && uuid.equals(ioTNode.uuid) && hostname.equals(ioTNode.hostname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, hostname, nodeType);
    }

    public IoTNode(String hostname, UUID uuid, byte nodeType) {
        this.hostname = hostname;
        this.uuid = uuid;
        this.nodeType = nodeType;
    }
}
