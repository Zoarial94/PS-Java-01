package com.zoarial.iot.action.model

import java.util.*

class IoTActionList : ArrayList<IoTAction> {
    private val uuidMap: HashMap<UUID, IoTAction>

    constructor(len: Int) : super(len) {
        uuidMap = HashMap(len)
    }

    constructor() : super() {
        uuidMap = HashMap()
    }

    constructor(c: Collection<IoTAction>) : super(c) {
        uuidMap = HashMap<UUID, IoTAction>()
        for (action in c) {
            uuidMap[action.uuid!!] = action
        }
    }

    fun remove(uuid: UUID): IoTAction? {
        val index = indexOf(uuid)
        return if (index != -1) {
            uuidMap.remove(uuid)
            removeAt(index)
        } else {
            null
        }
    }

    fun indexOf(uuid: UUID): Int {
        val action = get(uuid)
        return action?.let { indexOf(it) } ?: -1
    }

    override fun add(action: IoTAction): Boolean {
        val b = super.add(action)
        if (b) {
            uuidMap[action.uuid!!] = action
        }
        return b
    }

    operator fun contains(uuid: UUID): Boolean {
        return uuidMap.containsKey(uuid)
    }

    operator fun get(uuid: UUID): IoTAction? {
        return uuidMap[uuid]
    }
}