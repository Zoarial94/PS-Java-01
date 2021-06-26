package com.zoarial.iot.action.helper;

import com.zoarial.iot.action.model.IoTActionArgumentList;

import java.util.HashMap;
import java.util.function.Function;

public class JavaIoTActionExecHelper {
    final private static HashMap<String, Function<IoTActionArgumentList, String>> map = new HashMap<>();
    public static Function<IoTActionArgumentList, String> getFunction(String actionName) {
        return map.get(actionName);
    }
    public static HashMap<String, Function<IoTActionArgumentList, String>> getMap() {
        return map;
    }
}
