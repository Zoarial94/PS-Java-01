package com.zoarial.iot.action.model

class IoTActionArgument {
    var string = ""
        private set
    var long: Long = 0
        private set
    var bool = false
        private set

    constructor(str: String) {
        string = str
    }

    constructor(i: Int) {
        long = i.toLong()
    }

    constructor(l: Long) {
        long = l
    }

    constructor(b: Boolean) {
        bool = b
    }

    constructor(b: Byte) {
        long = b.toLong() and 0xFFL
    }

    val int: Int
        get() = long.toInt()
    val byte: Byte
        get() = long.toByte()
}