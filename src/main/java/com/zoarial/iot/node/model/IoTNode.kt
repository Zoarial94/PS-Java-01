package com.zoarial.iot.node.model

import com.zoarial.iot.action.model.IoTAction
import jakarta.persistence.*
import lombok.Getter
import lombok.Setter
import java.util.*

@Getter
@Setter
@Entity
@NamedQueries(NamedQuery(name = "getAll", query = "SELECT n FROM IoTNode n"), NamedQuery(name = "getByUUID", query = "SELECT n FROM IoTNode n WHERE n.uuid = :uuid"), NamedQuery(name = "getByName", query = "SELECT n FROM IoTNode n WHERE n.hostname = :hostname"))
open class IoTNode {
    @Id
    @Column(unique = true, nullable = false)
    open var uuid: UUID? = null
    open var hostname: String? = null

    @Column(nullable = false)
    open var nodeType: Byte? = null

    @Column(nullable = false)
    open var lastHeardFrom: Long? = -1
    open var lastIp: ByteArray? = null

    // Map to the class member 'node' in IoTAction
    @OneToMany(mappedBy = "node")
    open val actions: List<IoTAction>? = null

    constructor()

    constructor(hostname: String, uuid: UUID, nodeType: Byte) {
        this.hostname = hostname
        this.uuid = uuid
        this.nodeType = nodeType
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IoTNode

        if (uuid != other.uuid) return false
        if (hostname != other.hostname) return false
        if (nodeType != other.nodeType) return false
        if (lastHeardFrom != other.lastHeardFrom) return false
        if (lastIp != null) {
            if (other.lastIp == null) return false
            if (!lastIp.contentEquals(other.lastIp)) return false
        } else if (other.lastIp != null) return false
        if (actions != other.actions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid?.hashCode() ?: 0
        result = 31 * result + (hostname?.hashCode() ?: 0)
        result = 31 * result + (nodeType ?: 0)
        result = 31 * result + (lastHeardFrom?.hashCode() ?: 0)
        result = 31 * result + (lastIp?.contentHashCode() ?: 0)
        result = 31 * result + (actions?.hashCode() ?: 0)
        return result
    }
}