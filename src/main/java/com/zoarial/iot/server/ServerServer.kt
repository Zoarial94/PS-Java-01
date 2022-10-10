package com.zoarial.iot.server

import com.zoarial.PrintBaseClass
import com.zoarial.iot.action.dao.IoTActionDAO
import com.zoarial.iot.action.helper.JavaIoTActionExecHelper
import com.zoarial.iot.action.model.*
import com.zoarial.iot.dao.DAOHelper
import com.zoarial.iot.dto.ZoarialDTO
import com.zoarial.iot.model.RequestContext
import com.zoarial.iot.model.ServerInformation
import com.zoarial.iot.network.IoTPacketSectionList
import com.zoarial.iot.node.dao.IoTNodeDAO
import com.zoarial.iot.node.model.IoTNode
import com.zoarial.iot.threads.tcp.ServerSocketHelper
import com.zoarial.iot.threads.tcp.SocketHelper
import com.zoarial.iot.threads.tcp.TCPAcceptingThread
import com.zoarial.iot.threads.tcp.networkModel.TCPStart
import com.zoarial.iot.threads.udp.DatagramSocketHelper
import com.zoarial.iot.threads.udp.UDPThread
import lombok.AccessLevel
import lombok.Getter
import me.zoarial.networkArbiter.ZoarialNetworkArbiter
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BiFunction
import java.util.function.Function
import java.util.stream.Stream
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.system.exitProcess

/*
 *
 * I'm not certain how I want this program to be structured.
 * For now, I will have ServerServer run as its own thread.
 * The ServerServer thread will start the Head or Non-Head threads and then stop.
 * In the future, this behavior may change depending on the needs of the program.
 *
 */
@Getter
class ServerServer(info: ServerInformation) : PrintBaseClass("ServerServer"), Runnable {
    // Easier to pass all the server info around if it's all in one object
    val serverInfo: ServerInformation

    // an IoTNode representation of its self (the currently running node)
    val selfNode: IoTNode
    private val started = false

    // an atomic flag for signaling to all the threads whether the server is closed or not.
    @Getter(AccessLevel.NONE)
    private val closed = AtomicBoolean(false)

    // The ip address of the current device.
    // Is influenced by the user-configured network device, but is automatically selected from that device.
    val serverIP: InetAddress

    // A list of threads this class starts.
    // Used to forcefully stop threads when the server is closing.
    @Getter(AccessLevel.NONE)
    private val threads = Collections.synchronizedList(ArrayList<Thread>(8))

    @Getter(AccessLevel.NONE)
    private val externalNodeConnections = HashMap<UUID, Socket>()

    // This helper manages the TCP sockets
    // This is its own thread
    @Getter(AccessLevel.NONE)
    private var serverSocketHelper: ServerSocketHelper? = null

    // This helper manages the raw UDP packets for both sending and receiving
    // This is its own thread
    @Getter(AccessLevel.NONE)
    private var datagramSocketHelper: DatagramSocketHelper? = null

    // Used for GetUptime action
    private var startTime: Long = 0

    private val networkArbiter: ZoarialNetworkArbiter = ZoarialNetworkArbiter

    private val networkRequestMap: Map<String, Any> = ConcurrentHashMap()

    init {
        println("Initializing...")
        serverInfo = info
        selfNode = IoTNode(info.hostname, info.uuid, info.nodeType)
        if (!serverInfo.isHeadCapable) {
            // I need to use a different database when running the non-head
            // This sets the entity manager factory before any DAO object acquire it
            DAOHelper.setEntityManagerFactory("ZIoTNonHead")
        }
        val networkInterfaceEnumeration: Enumeration<NetworkInterface> = try {
            NetworkInterface.getNetworkInterfaces()
        } catch (e: SocketException) {
            e.printStackTrace()
            throw RuntimeException("Failed to get network interfaces.")
        }
        var found = false
        var tmpInetAddress: InetAddress? = null
        println("Looking for network device: " + info.networkDeviceName)

        // Look for an ip address from the specified network device.
        while (networkInterfaceEnumeration.hasMoreElements() && !found) {
            val networkInterface = networkInterfaceEnumeration.nextElement()
            println("Found interface: " + networkInterface.name)
            if (networkInterface.name != info.networkDeviceName) {
                println("Skipping interface")
                continue
            }
            val inetAddresses = networkInterface.inetAddresses
            while (inetAddresses.hasMoreElements() && !found) {
                val address = inetAddresses.nextElement()
                println(networkInterface.name + ":  " + address.hostAddress)
                if (address != Inet4Address.getLoopbackAddress() && address is Inet4Address) {
                    tmpInetAddress = address
                    found = true
                }
            }
        }
        if (tmpInetAddress == null) {
            throw RuntimeException("Unable to find IP address.")
        }

        // Generate the IoTActions list, so we can know what we can do
        generateIoTActions()
        serverIP = tmpInetAddress

    }

    override fun run() {
        // Maybe this should be an atomic flag, but for now this should work.
        if (!started) {
            println("Starting...")
            startTime = System.currentTimeMillis()
            try {
                println("Starting a server on port " + serverInfo.serverPort + ".")
                serverSocketHelper = ServerSocketHelper(this, ServerSocket(serverInfo.serverPort))
                createAndStartNewThread(serverSocketHelper!!)
                datagramSocketHelper = DatagramSocketHelper(this, DatagramSocket(serverInfo.serverPort, serverIP))
                createAndStartNewThread(datagramSocketHelper!!)
            } catch (ex: IOException) {
                println("Something happened when creating ths socket helpers. Exiting.")
                System.exit(-1)
            }

            /*
             * Start Threads
             */createAndStartNewThread(UDPThread(this, datagramSocketHelper))
            createAndStartNewThread(TCPAcceptingThread(this, serverSocketHelper))
        } else {
            println("Server already started.")
        }
    }

    /*
     *  This method is a mess and should be refactored/broken up
     */
    private fun generateIoTActions() {
        val ioTActionDAO = IoTActionDAO()
        val ioTNodeDAO = IoTNodeDAO()

        // Make sure our DB is updated with current information
        val dbNode = ioTNodeDAO.getNodeByUUID(serverInfo.uuid)
        if (dbNode == null) {
            ioTNodeDAO.persist(selfNode)
        } else if (dbNode != selfNode) {
            ioTNodeDAO.update(selfNode)
        }

        // List all internal actions
        // We check later whether they are in the database (by name)
        // Maybe later, have a seed in the config file to generate random, but constant ids for JavaIoTActions
        // This would help with finding the actions by an id, rather than by name
        createNewJavaIoTAction("Stop", 0.toByte(), 4.toByte(), true, true) {
            close()
            "Stopping..."
        }
        createNewJavaIoTAction("Shutdown", 0.toByte(), 4.toByte(), true, true) {
            exitProcess(0)
        }
        createNewJavaIoTAction("GetUptime", 0.toByte(), 4.toByte(), false, false) { (System.currentTimeMillis() - startTime).toString() }
        createNewJavaIoTAction("Print", 1.toByte(), 4.toByte(), false, false) { list: IoTActionArgumentList ->
            println("Being asked to print: \"" + list[0].string + "\"")
            "Printed"
        }

        // Lists actions from scriptActionsInQuestion
        createNewJavaIoTAction("GetDisabledActions", 0.toByte(), 4.toByte(), true, true) {
            val root = JSONObject()
            val actions = JSONArray()
            root.put("actions", actions)
            for (action in ioTActionDAO.disabledActions) {
                actions.put(action.toJson())
            }
            root.toString()
        }
        // Move actions from scriptActionsInQuestion to listOfActions
        createNewJavaIoTAction("EnableAction", 1.toByte(), 4.toByte(), true, true) { list: IoTActionArgumentList ->
            val uuid = UUID.fromString(list[0].string)
            println("UUID: $uuid")
            val optAction = ioTActionDAO.findActionByUUID(uuid)
            if (optAction.isEmpty) {
                return@createNewJavaIoTAction "There is no new action with that UUID."
            } else {
                val action = optAction.get()
                if (action.isValid) {
                    action.enable()
                    return@createNewJavaIoTAction "Success."
                } else {
                    action.disable()
                    return@createNewJavaIoTAction "That action is not valid."
                }
            }
        }
        createNewJavaIoTAction("UpdateSecurityLevel", 2.toByte(), 4.toByte(), true, true) { list: IoTActionArgumentList ->
            val uuid = UUID.fromString(list[0].string)
            val level = list[1].byte
            if (level > 4 || level < 0) {
                return@createNewJavaIoTAction "Invalid security level"
            }
            val optAction = ioTActionDAO.findActionByUUID(uuid)
            if (optAction.isPresent) {
                val action = optAction.get()
                action.securityLevel = level
                ioTActionDAO.update(action)
                return@createNewJavaIoTAction "Security level set."
            } else {
                return@createNewJavaIoTAction "No action with that UUID found."
            }
        }
        createNewJavaIoTAction("UpdateEncryptedToggle", 2.toByte(), 4.toByte(), true, true) { list: IoTActionArgumentList? ->
            val uuid = UUID.fromString(list!![0].string)
            val optAction = ioTActionDAO.findActionByUUID(uuid)
            if (optAction.isPresent) {
                val action = optAction.get()
                action.encrypted = list[1].bool
                ioTActionDAO.update(action)
                return@createNewJavaIoTAction "Encryption toggle set."
            } else {
                return@createNewJavaIoTAction "No action with that UUID found."
            }
        }
        createNewJavaIoTAction("UpdateLocalToggle", 2.toByte(), 4.toByte(), true, true) { list: IoTActionArgumentList? ->
            val uuid = UUID.fromString(list!![0].string)
            val optAction = ioTActionDAO.findActionByUUID(uuid)
            if (optAction.isPresent) {
                val action = optAction.get()
                action.local = list[1].bool
                ioTActionDAO.update(action)
                return@createNewJavaIoTAction "Security level set."
            } else {
                return@createNewJavaIoTAction "No action with that UUID found."
            }
        }

        //TODO: remove stale JavaIoTActions


        // For now, assume we are always on linux.
        val rootDir = Paths.get("/etc/ZoarialIoT")
        val scriptDir = Paths.get("/etc/ZoarialIoT/scripts")
        val listOfScripts: Stream<Path>

        // Check for things
        if (!Files.isDirectory(rootDir)) {
            println("Root directory does not exist.")
            return
        }
        if (!Files.isDirectory(scriptDir)) {
            println("Script directory does not exist.")
            // If there is no script dir, then we can't create any actions.
            return
        }

        // Try to generate the list
        // TODO: find actions in the db which are stale
        try {
            listOfScripts = Files.list(scriptDir)
            listOfScripts.filter { file: Path? ->
                val b: Boolean = ScriptIoTAction.Companion.isValidFile(file)
                if (!b) {
                    println("Something is wrong with the permissions/file.")
                }
                b
            }.forEach { file: Path ->
                val fileName = file.fileName.toString()
                val dbAction = ioTActionDAO.getScriptActionByName(fileName)
                if (dbAction == null) {
                    // By default, use the highest security level.
                    // Security level should be changed manually by user though other means. (Saved to database)
                    val action = ScriptIoTAction(fileName, UUID.randomUUID(), 4.toByte(), true, false, 0.toByte(), file)
                    action.node = selfNode
                    println("There is a new script to add to actions:\n$action")
                    ioTActionDAO.persist(action)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun createNewJavaIoTAction(name: String, args: Byte, securityLevel: Byte, encrypt: Boolean, local: Boolean, exec: Function<IoTActionArgumentList, String>) {
        // Make sure action hasn't already been created
        if (JavaIoTActionExecHelper.getFunction(name) != null) {
            throw RuntimeException("JavaIoTAction with name $name already exists.")
        }

        // Add to map and prep for db access
        val map = JavaIoTActionExecHelper.map
        val action = JavaIoTAction(name, args, securityLevel, encrypt, local)
        action.node = selfNode
        val ioTActionDAO = IoTActionDAO()
        map[name] = exec

        // See if the action exists in the DB
        // If it doesn't exist, add it
        // If it does exist, make sure its updated.
        val dbAction = ioTActionDAO.getJavaActionByName(name)
        if (dbAction == null) {
            action.uuid = UUID.randomUUID()
            println("JavaIoTAction was not in database. Adding now:\n$action")
            ioTActionDAO.persist(action)
        } else {
            if (dbAction.node != selfNode) {
                dbAction.node = selfNode
                ioTActionDAO.update(dbAction)
            }
            // Updated database in the case something has changed
            action.uuid = dbAction.uuid
            if (!dbAction.isEnabled() || !dbAction.isValid) {
                action.disable()
            }
            if (action != dbAction) {
                ioTActionDAO.update(action)
            }
        }
    }

    fun <T : Any> registerRequest(requestName: String, ZIoTRequest: KClass<T>, exec: BiFunction<T, RequestContext, String>) {


    }

    fun getAndUpdateInfoAboutNode(node: IoTNode) {
        val optionalSocket: Optional<Socket> = getExternalNodeSocket(node)
        val socket: Socket = if (optionalSocket.isPresent) {
            optionalSocket.get()
        } else {
            log.error("There was an error trying to update info about node: " + node.hostname)
            return
        }
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        networkArbiter.sendObject(TCPStart(0, (Math.random() * Int.MAX_VALUE).toInt(), ))

        val socketHelper = SocketHelper(socket)
        val packetSectionList = IoTPacketSectionList()

        // Header
        packetSectionList.add("ZIoT")
        // Version
        packetSectionList.add(0.toByte())
        // Session ID
        packetSectionList.add()
        packetSectionList.add("info")
        packetSectionList.add("actions")
        try {
            socketHelper.out.write(packetSectionList.networkResponse)
            socketHelper.out.flush()
            if (!Arrays.equals("ZIoT".toByteArray(), socketHelper.`in`.readNBytes(4))) {
                log.error("Did not receive ZIoT when getting actions from external node.")
            }
            println("Session ID: " + socketHelper.readInt())
            val numberOfActions = socketHelper.readInt()
            println("Number of Actions: $numberOfActions")
            val nodeDAO = IoTNodeDAO()
            val actionDAO = IoTActionDAO()
            for (i in 0 until numberOfActions) {
                val action: IoTAction = ExternalIoTAction()
                // Action UUID
                action.uuid = socketHelper.readUUID()
                log.trace("Remote action UUID: " + action.uuid)

                // Action Node
                val actionNodeUuid = socketHelper.readUUID()
                log.trace("Remote action node UUID: $actionNodeUuid")
                // This may throw. Need to check for non-existent node
                val actionNode = nodeDAO.getNodeByUUID(actionNodeUuid)
                        ?: throw RuntimeException("Unexpected null IoTNode.")
                action.node = actionNode
                action.name = socketHelper.readString()
                action.securityLevel = socketHelper.readByte()
                action.arguments = socketHelper.readByte()
                action.encrypted = socketHelper.readBoolean()
                action.local = socketHelper.readBoolean()
                if (action.node == selfNode) {
                    continue
                }
                actionDAO.persistOrUpdate(action)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            try {
                socket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            externalNodeConnections.remove(node.uuid)
        }
    }

    fun runRemoteAction(action: IoTAction, args: JSONObject): String {
        if(action.node == null) {
            log.error("Action has no node.")
            return "Action has no node."
        } else if (action.node == selfNode) {
            log.error("Action " + action.name + " has been run remotely instead of locally.")
            return "This action is local, but has been run as remote. This is an error."
        }
        val optionalSocket: Optional<Socket> = getExternalNodeSocket(action.node!!)
        val socket: Socket = if (optionalSocket.isPresent) {
            optionalSocket.get()
        } else {
            return "There was an error creating the socket."
        }
        val socketHelper = SocketHelper(socket)
        val packetSectionList = IoTPacketSectionList()

        // Header
        packetSectionList.add("ZIoT")
        // Version
        packetSectionList.add(0.toByte())
        // Session ID
        val sessionID = (Math.random() * Int.MAX_VALUE).toInt()
        println("Created SessionID: $sessionID")
        packetSectionList.add(sessionID)
        packetSectionList.add("action")
        packetSectionList.add(action.uuid!!)
        packetSectionList.add(args.toString())
        val response: String = try {
            socketHelper.out.write(packetSectionList.networkResponse)
            socketHelper.out.flush()
            if (!Arrays.equals("ZIoT".toByteArray(), socketHelper.`in`.readNBytes(4))) {
                throw RuntimeException("Was not ZIoT response")
            }
            println("Session ID: " + socketHelper.readInt())
            socketHelper.readString()
        } catch (e: IOException) {
            e.printStackTrace()
            "There was an error running the action remotely"
        }
        return response
    }

    private fun getExternalNodeSocket(node: IoTNode): Optional<Socket> {
        var socket = externalNodeConnections[node.uuid]
        if (socket == null || socket.isClosed) {
            log.trace("Socket to node " + node.hostname + " does not exist. Creating one...")
            val addr: InetAddress
            addr = try {
                InetAddress.getByAddress(node.lastIp)
            } catch (e: UnknownHostException) {
                log.error("Invalid ip address.")
                return Optional.empty()
            }
            try {
                printArray(node.lastIp!!)
                socket = Socket(addr, serverInfo.serverPort)
                externalNodeConnections[node.uuid!!] = socket
            } catch (e: UnknownHostException) {
                e.printStackTrace()
                log.error("Unable to get InetAddress from bytes.")
                return Optional.empty()
            } catch (ignore: ConnectException) {
                log.error("Unable to connect to remote node: " + addr.hostAddress)
                return Optional.empty()
            } catch (e: IOException) {
                e.printStackTrace()
                log.error("Unable to connect to node")
                return Optional.empty()
            }
            log.trace("Created socket for " + node.hostname + ".")
        }
        return Optional.of(socket)
    }

    private fun createAndStartNewThread(runnable: Runnable) {
        val t = Thread(runnable)
        t.start()
        threads.add(t)
    }

    @JvmOverloads
    fun close(join: Boolean = false) {
        // Mark the server as closed
        closed.opaque = true
        for ((_, value) in externalNodeConnections) {
            try {
                value.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // Close original sockets
        if (datagramSocketHelper != null && !datagramSocketHelper!!.isClosed) {
            datagramSocketHelper!!.close()
        }
        if (serverSocketHelper != null && !serverSocketHelper!!.isClosed) {
            //TODO: shutdown, then close for better cleanup.
            serverSocketHelper!!.close()
        }

        // Close all daughter threads. (Those started with the helper function at least)
        for (t in threads) {
            if (t.isAlive && !t.isInterrupted) {
                t.interrupt()
            }
        }
        if (join) {
            join()
        }
    }

    fun join() {
        for (t in threads) {
            try {
                t.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    val isClosed: Boolean
        get() = closed.opaque

    val isStarted: Boolean
        get() = started

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java);

        @JvmOverloads
        fun printArray(arr: ByteArray, len: Int = arr.size) {
            val prefix = "Array: "
            for (i in 0 until len) {
                if (i == 0) {
                    print(prefix)
                } else if (i % 8 == 0) {
                    print("""
    
    $prefix
    """.trimIndent())
                }
                print("\t")
                if (Character.isLetterOrDigit(arr[i].toInt())) {
                    print(Char(arr[i].toUShort()))
                } else {
                    print("?")
                }
                // Java doesn't have unsigned bytes
                // Java will sign-extend the byte if >= 128
                // So binary-AND the first byte
                print("(" + (arr[i].toInt() and 0xFF) + ") ")
            }
            println()
        }
    }
}