package com.zoarial.iot.models.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class IoTActionList extends ArrayList<IoTAction> {
    final private HashMap<UUID, IoTAction> uuidMap;

    public IoTActionList(int len) {
        super(len);
        uuidMap = new HashMap<>(len);
    }

    public IoTActionList() {
        super();
        uuidMap = new HashMap<>();
    }

    @Override
    public boolean add(IoTAction action) {
        boolean b = super.add(action);
        if(b) {
            uuidMap.put(action.getUUID(), action);
        }
        return b;
    }

    public boolean contains(UUID uuid)  {
        return uuidMap.containsKey(uuid);
    }

    public IoTAction get(UUID uuid) {
        return uuidMap.get(uuid);
    }
}
