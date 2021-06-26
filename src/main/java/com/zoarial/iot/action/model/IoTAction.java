package com.zoarial.iot.action.model;

import com.zoarial.iot.model.IoTBasicType;
import com.zoarial.iot.node.model.IoTNode;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

//TODO: transition to named queries for IoTActionDAO
@NamedQueries({
        @NamedQuery(name = "IoTAction.getEnabled", query = "SELECT a FROM IoTAction a WHERE a.enabled = true"),
        @NamedQuery(name = "IoTAction.getDisabled", query = "SELECT a FROM IoTAction a WHERE a.enabled = false")
})
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"uuid", "node_uuid"})})

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PROTECTED)
@Getter
@Setter
public abstract class IoTAction implements Serializable {
    // UUID is 16 bytes (128 bits)
    @Id @Column(unique = true, nullable = false, columnDefinition = "binary(16)")
    protected UUID uuid;
    @Column(nullable = false)
    protected String name;
    protected String description;

    @Column(nullable = false)
    protected IoTBasicType returnType = IoTBasicType.STRING;

    // Prevent getter and setter
    @Setter(AccessLevel.NONE)
    protected boolean enabled = true;

    // Default, anyone with the right level can use.
    //protected boolean allowByDefault = true;

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
    protected byte securityLevel;
    protected byte arguments;
    protected boolean encrypted;
    protected boolean local;


    @ManyToOne(optional = false)
    protected IoTNode node;

    protected IoTAction(String name, UUID uuid, byte level, boolean encrypted, boolean local, byte arguments) {
        this.name = name;
        this.uuid = uuid;
        securityLevel = level;
        this.arguments = arguments;
        this.encrypted = encrypted;
        this.local = local;
    }

    // Is this action still valid?
    // Is the required script still present? etc.
    public abstract boolean isValid();

    public void enable() {
        if(isValid()) {
            enabled = true;
        } else {
            enabled = false;
            throw new RuntimeException("Unable to enable action. Action is not valid.");
        }
    }
    public void disable() {
        enabled = false;
    }

    protected abstract String privExecute(IoTActionArgumentList args);

    public final String execute() {
        return execute(new IoTActionArgumentList());
    }
    //Ensure the argument size is correct
    public final String execute(IoTActionArgumentList args) {
        if(arguments != args.size()) {
            throw new IllegalArgumentException("(" + name + ") Incorrect amount of arguments. Needed: " + arguments + " Got: " + args.size());
        } else if(!isEnabled()) {
            throw new RuntimeException("Action is not enabled.");
        } else if(!isValid()) {
            throw new RuntimeException("Action is not valid.");
        }
        return privExecute(args);
    }

    // Just call the other execute function
    public final String execute(JSONArray args) {
        int len = args.length();
        IoTActionArgumentList list = new IoTActionArgumentList();
        for(int i = 0; i < len; i++) {
            list.add(args.getString(i));
        }
        String response = execute(list);
        if(response.contains("\0")) {
            throw new RuntimeException("Action response should not have null character");
        }
        return response;
    }

    public JSONObject toJson() {
        JSONObject jsonAction = new JSONObject();
        jsonAction.put("nodeUuid", node.getUuid());
        jsonAction.put("uuid", uuid);
        jsonAction.put("name", name);
        jsonAction.put("description", description);
        jsonAction.put("returnType", returnType);
        jsonAction.put("securityLevel", securityLevel);
        jsonAction.put("arguments", arguments);
        jsonAction.put("encrypted", encrypted);
        jsonAction.put("local", local);
        jsonAction.put("enabled", enabled);
        return jsonAction;
    }

    @Override
    public String toString() {
        return "IoTAction{" +
                "actionUuid=" + uuid +
                ", actionName='" + name + '\'' +
                ", description='" + description + '\'' +
                ", returnType=" + returnType +
                ", actionSecurityLevel=" + securityLevel +
                ", arguments=" + arguments +
                ", encrypted=" + encrypted +
                ", local=" + local +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IoTAction)) return false;
        IoTAction ioTAction = (IoTAction) o;
        return enabled == ioTAction.enabled && securityLevel == ioTAction.securityLevel && arguments == ioTAction.arguments && encrypted == ioTAction.encrypted && local == ioTAction.local && uuid.equals(ioTAction.uuid) && name.equals(ioTAction.name) && Objects.equals(description, ioTAction.description) && returnType == ioTAction.returnType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name, description, returnType, enabled, securityLevel, arguments, encrypted, local);
    }
}
