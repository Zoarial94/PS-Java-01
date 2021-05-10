package com.zoarial.iot;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ServerInformation {
    final public String hostname;
    final public UUID uuid;
    final public byte nodeType;
    final public boolean isVolatile;
    final public int serverPort;
    final public boolean isHeadCapable;

    public int messageTimeout;
    public int pingTimeout;
    public List<InetAddress> headNodes;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerInformation)) return false;
        ServerInformation that = (ServerInformation) o;
        return nodeType == that.nodeType && isVolatile == that.isVolatile && serverPort == that.serverPort && isHeadCapable == that.isHeadCapable && messageTimeout == that.messageTimeout && pingTimeout == that.pingTimeout && hostname.equals(that.hostname) && uuid.equals(that.uuid) && headNodes.equals(that.headNodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, uuid, nodeType, isVolatile, serverPort, isHeadCapable, messageTimeout, pingTimeout, headNodes);
    }

    public ServerInformation(ServerInformation info) {
        this(info.hostname, info.uuid, info.nodeType, info.serverPort, info.isVolatile, info.isHeadCapable, info.messageTimeout, info.pingTimeout, new ArrayList<>(info.headNodes));
    }

    public ServerInformation(String hostname, UUID uuid, byte nodeType,
                      int serverPort, boolean isVolatile, boolean isHeadCapable,
                      int messageTimeout, int pingTimeout, List<InetAddress> headNodes) {

        this.hostname = hostname;
        this.uuid = uuid;
        this.nodeType = nodeType;
        this.serverPort = serverPort;
        this.isVolatile = isVolatile;
        this.isHeadCapable = isHeadCapable;

        this.messageTimeout = messageTimeout;
        this.pingTimeout = pingTimeout;
        this.headNodes = headNodes;
    }

}
