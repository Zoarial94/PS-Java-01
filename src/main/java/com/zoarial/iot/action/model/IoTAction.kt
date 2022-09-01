package com.zoarial.iot.action.model

import com.zoarial.iot.model.IoTBasicType
import com.zoarial.iot.node.model.IoTNode
import jakarta.persistence.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.util.*

//TODO: transition to named queries for IoTActionDAO
@NamedQueries(NamedQuery(name = "IoTAction.getEnabled", query = "SELECT a FROM IoTAction a WHERE a.enabled = true"), NamedQuery(name = "IoTAction.getDisabled", query = "SELECT a FROM IoTAction a WHERE a.enabled = false"))
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["uuid", "node_uuid"])])
abstract class IoTAction constructor(@Column(nullable = false) open var name: String? = null,
        // UUID is 16 bytes (128 bits)
        @Column(unique = true, nullable = false)
        @Id
        open var uuid: UUID? = null,

        /*
                @OneToMany @JoinTable(name = "ACTION_WHITELIST")
                List<IoTNode> whiteList;
                @OneToMany
                List<IoTNode> blackList;
                */
        /*
                 * securityLevel is how privileged the request has to be in order to start
                 * This action can be run from a higher level, but not a lower one.
                 * Level 0 means no security
                 * Level 1 means basic security. Needs HTOP or TOTP
                 * Level 2 needs stronger identification (public/private keys)
                 */
        open var securityLevel: Byte? = null,
        open var encrypted: Boolean? = null,
        open var local: Boolean? = null,
        open var arguments: Byte? = null,
        open var description: String? = null,

): Serializable {


    // Default, anyone with the right level can use.
    //protected boolean allowByDefault = true;


    @Column(nullable = false)
    open var returnType = IoTBasicType.STRING

    private var enabled = false

    fun isEnabled(): Boolean {
        return enabled
    }

    @ManyToOne(optional = false)
    open var node: IoTNode? = null

    // Is this action still valid?
    // Is the required script still present? etc.
    abstract val isValid: Boolean;

    fun enable() {
        if (isValid) {
            enabled = true
        } else {
            enabled = false
            throw RuntimeException("Unable to enable action. Action is not valid.")
        }
    }

    fun disable() {
        enabled = false
    }

    protected open fun privExecute(args: IoTActionArgumentList): String {
        throw java.lang.RuntimeException("Not implemented");
    }

    //Ensure the argument size is correct
    @JvmOverloads
    fun execute(args: IoTActionArgumentList = IoTActionArgumentList()): String {
        require(arguments!!.toInt() == args.size) { "(" + name + ") Incorrect amount of arguments. Needed: " + arguments + " Got: " + args.size }
        if (!enabled) {
            throw RuntimeException("Action is not enabled.")
        } else if (!isValid) {
            throw RuntimeException("Action is no longer valid.")
        }
        return privExecute(args)
    }

    // Just call the other execute function
    fun execute(args: JSONArray): String {
        val len = args.length()
        val list = IoTActionArgumentList()
        for (i in 0 until len) {
            list.add(args.getString(i))
        }
        val response = execute(list)
        if (response!!.contains("\u0000")) {
            throw RuntimeException("Action response should not have null character")
        }
        return response
    }

    fun toJson(): JSONObject {
        val jsonAction = JSONObject()
        jsonAction.put("nodeUuid", node!!.uuid)
        jsonAction.put("uuid", uuid)
        jsonAction.put("name", name)
        jsonAction.put("description", description)
        jsonAction.put("returnType", returnType)
        jsonAction.put("securityLevel", securityLevel!!.toInt())
        jsonAction.put("arguments", arguments!!.toInt())
        jsonAction.put("encrypted", encrypted)
        jsonAction.put("local", local)
        jsonAction.put("enabled", enabled)
        return jsonAction
    }
}