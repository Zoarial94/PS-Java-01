package com.zoarial.iot.action.model;

import java.util.ArrayList;

public class IoTActionArgumentList extends ArrayList<IoTActionArgument> {


    public IoTActionArgumentList() {
        super();
    }
    public IoTActionArgumentList(int len) {
        super(len);
    }

    public boolean add(String str) {
        return super.add(new IoTActionArgument(str));
    }

    public boolean add(int i) {
        return super.add(new IoTActionArgument(i));
    }

    public boolean add(long l) {
        return super.add(new IoTActionArgument(l));
    }

    public boolean add(boolean b) {
        return super.add(new IoTActionArgument(b));
    }



}
