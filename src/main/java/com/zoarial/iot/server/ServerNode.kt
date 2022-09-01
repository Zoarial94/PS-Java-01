package com.zoarial.iot.server

import com.zoarial.PrintBaseClass
import com.zoarial.iot.model.ServerInformation
import com.zoarial.iot.model.ServerPublic
import java.io.*
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

class ServerNode : PrintBaseClass("ServerNode") {
    var _close = false
    var _server: ServerServer? = null
    var prop = Properties()
    var `is`: InputStream? = null
    var _isInitialized = false

    //Server should reset if any of these change
    var _hostname: String? = null
    var _nodeType: Byte = 0
    var _isVolatile = false
    var _serverPort = 0
    var _isHeadCapable = false
    var _uuid: UUID? = null

    //Server can update these at runtime
    var _messageTimeout = 0
    var _pingTimeout = 0
    var _logFileName: String? = null
    var _loggingLevel = 0
    var headNodes: MutableList<InetAddress?> = ArrayList(3)
    var _networkDeviceName: String? = null
    var oldInfo: ServerInformation? = null

    init {
        println("Constructing...")
    }

    fun init(): Boolean {
        if (_isInitialized) {
            println("Already initialized.")
            return false
        }
        println("Initializing...")
        try {
            FileInputStream(ServerPublic.CONFIG_FILE).use { inputStream ->
                `is` = inputStream
                prop.load(`is`)
            }
        } catch (ex: FileNotFoundException) {
            println("Config file not found.")
            return false
        } catch (ex: IOException) {
            println("Error reading config file.")
            return false
        }
        if (prop.getProperty("app.name") != ServerPublic.APP_NAME) {
            println("Given config file is not a Z-IoT config file.")
            return false
        } else if (prop.getProperty("app.version") != ServerPublic.CONFIG_VERSION) {
            println("Config file is not the correct version")
            return false
        }
        if (!prop.containsKey(ServerPublic.DEVICE + "uuid")) {
            println("Generating new UUID.")
            prop.setProperty(ServerPublic.DEVICE + "uuid", UUID.randomUUID().toString())
            try {
                FileOutputStream(ServerPublic.CONFIG_FILE).use { outputStream -> prop.store(outputStream, "Updated UUID") }
            } catch (ignored: IOException) {
                throw RuntimeException("Unable to update properties in config file.")
            }
        }
        _uuid = UUID.fromString(prop.getProperty(ServerPublic.DEVICE + "uuid", UUID.randomUUID().toString()))
        _hostname = prop.getProperty(ServerPublic.DEVICE + "hostname", "PS-testing1")
        _nodeType = prop.getProperty(ServerPublic.DEVICE + "node_type", "0").toByte()
        _isVolatile = java.lang.Boolean.parseBoolean(prop.getProperty(ServerPublic.DEVICE + "is_volatile", "true"))
        _serverPort = prop.getProperty(ServerPublic.DEVICE + "port", "9494").toInt()
        _messageTimeout = prop.getProperty(ServerPublic.DEVICE + "message_timeout", "300").toInt()
        _pingTimeout = prop.getProperty(ServerPublic.DEVICE + "ping_timeout", "90").toInt()
        _isHeadCapable = java.lang.Boolean.parseBoolean(prop.getProperty(ServerPublic.DEVICE + "is_head_capable", "false"))
        _networkDeviceName = prop.getProperty(ServerPublic.DEVICE + "network_device", "eth0")
        _logFileName = prop.getProperty(ServerPublic.LOGGING + "file_name", "/var/log/PS-Java-Test.log")
        _loggingLevel = prop.getProperty(ServerPublic.LOGGING + "level", "1").toInt()
        try {
            headNodes.add(InetAddress.getByName(prop.getProperty(ServerPublic.DEVICE + "headNode1", "0.0.0.0")))
            headNodes.add(InetAddress.getByName(prop.getProperty(ServerPublic.DEVICE + "headNode2", "0.0.0.0")))
            headNodes.add(InetAddress.getByName(prop.getProperty(ServerPublic.DEVICE + "headNode3", "0.0.0.0")))
        } catch (ex: UnknownHostException) {
            ex.printStackTrace()
        }
        println("Hostname is $_hostname")
        println("Node Type is $_nodeType")
        println("isVolatile is $_isVolatile")
        println("Server port is $_serverPort")
        println("Message timeout is $_messageTimeout")
        println("Ping timeout is $_pingTimeout")
        println("Log file name is $_logFileName")
        println("Logging Level is $_loggingLevel")
        println("isHeadCapable is $_isHeadCapable")
        println("headNodes is $headNodes")

        //  May throw
        try {
            oldInfo = ServerInformation(_hostname!!, _uuid!!, _nodeType, _serverPort, _isVolatile, _isHeadCapable, _messageTimeout, _pingTimeout, _networkDeviceName!!,
                headNodes.filterNotNull().toList()
            )
            _server = ServerServer(ServerInformation(oldInfo!!))
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        _isInitialized = true
        return true
    }

    fun start(): Boolean {
        if (!_isInitialized) {
            println("Node is not initialized yet. Cannot start.")
            return false
        } else if (_server!!.isStarted) {
            println("Unable to start server: server has already started.")
        } else {
            println("Staring Server...")
        }
        val t1 = Thread(_server)
        t1.start()
        return true
    }

    @JvmOverloads
    fun close(join: Boolean = false) {
        _close = true
        _server!!.close()
        //TODO: check ServerServer for changes and then save those changes to the .config file
        val serverInfo = _server!!.serverInfo
        if (serverInfo != oldInfo) {
            println("Updating config file.")
            prop.setProperty(ServerPublic.DEVICE + "headNode1", serverInfo.headNodes[0].hostAddress)
            prop.setProperty(ServerPublic.DEVICE + "headNode2", serverInfo.headNodes[1].hostAddress)
            prop.setProperty(ServerPublic.DEVICE + "headNode3", serverInfo.headNodes[2].hostAddress)
            try {
                FileOutputStream(ServerPublic.CONFIG_FILE).use { outputStream -> prop.store(outputStream, "Updated") }
            } catch (ignored: IOException) {
                throw RuntimeException("Unable to update properties in config file.")
            }
        }
        if (join) {
            _server!!.join()
        }
    }
}