package com.zoarial.iot.action.model

class IoTActionArgumentList : ArrayList<IoTActionArgument> {
    constructor() : super() {}
    constructor(len: Int) : super(len) {}

    fun add(str: String): Boolean {
        return super.add(IoTActionArgument(str))
    }

    fun add(i: Int): Boolean {
        return super.add(IoTActionArgument(i))
    }

    fun add(l: Long): Boolean {
        return super.add(IoTActionArgument(l))
    }

    fun add(b: Boolean): Boolean {
        return super.add(IoTActionArgument(b))
    }
}