package com.zoarial.iot.models.actions;

import java.util.*;

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

    public IoTActionList(Collection<? extends IoTAction> c) {
        super(c);
        uuidMap = new HashMap<>();
        for(IoTAction action : c) {
            uuidMap.put(action.getUuid(), action);
        }
    }

    @Override
    public boolean add(IoTAction action) {
        boolean b = super.add(action);
        if(b) {
            uuidMap.put(action.getUuid(), action);
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
