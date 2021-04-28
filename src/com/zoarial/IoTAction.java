package com.zoarial;

import java.util.List;
import java.util.UUID;

public abstract class IoTAction {
    private final String actionName;
    private final String description = "";
    private final UUID actionUuid;

    // Default, anyone with the right level can use.
    private boolean allowByDefault = true;
    List<IoTNode> whiteList;
    List<IoTNode> blackList;

    /*
     * securityLevel is how privileged the request has to be in order to start
     * This action can be run from a higher level, but not a lower one.
     * Level 0 means no security
     * Level 1 means basic security. Needs HTOP or TOTP
     * Level 2 needs stronger identification (public/private keys)
     * From here on, all communications need to be fully encrypted
     * Level 3 is the same as level 1, but communication needs to be encrypted. (SSL/TLS probably over HTTP)
     * Level 4 is the same as level 2, but communication needs to be encrypted.
     * Level 10 means only the local machine can run it.
     */
    private final byte actionSecurityLevel;
    private final byte arguments;

    protected IoTAction(String name, UUID uuid, byte level, byte arguments) {
        actionName = name;
        actionUuid = uuid;
        actionSecurityLevel = level;
        this.arguments = arguments;
    }

    // This will be called and executed in its own thread
    protected abstract String privExecute(List<String> args);

    //Ensure the argument size is correct
    public final String execute(List<String> args) {
        if(arguments == args.size()) {
            return privExecute(args);
        }
        throw new IllegalArgumentException("Incorrect amount of arguments. Needed: " + arguments + "Got: " + args.size());
    }

    public String getName() {
        return actionName;
    }

    public UUID getUUID() {
        return actionUuid;
    }

    public byte getSecurityLevel() {
        return actionSecurityLevel;
    }

    public byte getNumberOfArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        return getUUID().toString() + " " + getSecurityLevel() + " " + getName() + " " + getNumberOfArguments();
    }

}
