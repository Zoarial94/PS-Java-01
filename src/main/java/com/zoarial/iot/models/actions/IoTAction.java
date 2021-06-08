package com.zoarial.iot.models.actions;

import com.zoarial.iot.models.IoTBasicType;
import com.zoarial.iot.models.IoTNode;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PROTECTED)
@Getter
@Setter
public abstract class IoTAction implements Serializable {
    // UUID is 16 bytes (128 bits)
    @Id @Column(unique = true, nullable = false, columnDefinition = "binary(16)")
    protected UUID uuid;
    @Column(nullable = false, unique = true)
    protected String name;
    protected String description;

    @Column(nullable = false)
    protected IoTBasicType returnType = IoTBasicType.STRING;

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
     * Level 2 needs stronger identification (public/protected keys)
     * Level 10 means only the local machine can run it.
     */
    protected byte securityLevel;
    protected byte arguments;
    protected boolean encrypted;
    protected boolean local;

    // If null, then the action belongs to the local node
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

    // This will be called and executed in its own thread
    protected abstract String privExecute(IoTActionArgumentList args);

    public final String execute() {
        return execute(new IoTActionArgumentList());
    }
    //Ensure the argument size is correct
    public final String execute(IoTActionArgumentList args) {
        if(arguments == args.size()) {
            return privExecute(args);
        }
        throw new IllegalArgumentException("(" + name + ") Incorrect amount of arguments. Needed: " + arguments + " Got: " + args.size());
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
        return securityLevel == ioTAction.securityLevel && arguments == ioTAction.arguments && encrypted == ioTAction.encrypted && local == ioTAction.local && uuid.equals(ioTAction.uuid) && name.equals(ioTAction.name) && Objects.equals(description, ioTAction.description) && returnType == ioTAction.returnType && node.equals(ioTAction.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name, description, returnType, securityLevel, arguments, encrypted, local, node);
    }
}
