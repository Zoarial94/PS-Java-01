package com.zoarial.iot.threads.tcp.networkModel;

import me.zoarial.networkArbiter.annotations.ZoarialObjectElement;
import me.zoarial.networkArbiter.annotations.ZoarialNetworkObject;

import java.util.Objects;

@ZoarialNetworkObject
public class TCPStart {
    @ZoarialObjectElement(placement = 1)
    byte version;

    @ZoarialObjectElement(placement = 2)
    int sessionId;

    @ZoarialObjectElement(placement = 3)
    String request = new String();

    public byte getVersion() {
        return version;
    }

    public int getSessionId() {
        return sessionId;
    }

    public String getRequest() {
        return request;
    }

    public TCPStart(byte version, int sessionId, String request) {
        this.version = version;
        this.sessionId = sessionId;
        this.request = request;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TCPStart tcpStart = (TCPStart) o;
        return version == tcpStart.version && sessionId == tcpStart.sessionId && request.equals(tcpStart.request);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, sessionId, request);
    }
}
