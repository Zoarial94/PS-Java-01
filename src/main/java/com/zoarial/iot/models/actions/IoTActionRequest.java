package com.zoarial.iot.models.actions;

import java.util.HashMap;
import java.util.UUID;

public class IoTActionRequest {
    public int sessionID;
    public int otp;
    public IoTActionArgumentList args;
    public byte version;
    public UUID actionUUID;
    public int length;
    public HashMap<String, String> map = new HashMap<>();
    public String request;



}
