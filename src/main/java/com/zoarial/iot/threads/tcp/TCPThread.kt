package com.zoarial.iot.threads.tcp

import com.zoarial.PrintBaseClass
import com.zoarial.iot.action.dao.IoTActionDAO
import com.zoarial.iot.dto.ZoarialDTO
import com.zoarial.iot.dto.ZoarialDTO.V1.Request.*
import com.zoarial.iot.network.IoTPacketSectionList
import com.zoarial.iot.network.IoTSession
import com.zoarial.iot.network.IoTSession.IoTSessionType
import com.zoarial.iot.node.dao.IoTNodeDAO
import com.zoarial.iot.server.ServerServer
import com.zoarial.iot.threads.tcp.networkModel.TCPStart
import me.zoarial.networkArbiter.ZoarialNetworkArbiter
import org.json.JSONException
import org.json.JSONObject
import java.io.EOFException
import java.io.IOException
import java.net.SocketException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

// TODO: redo the logic here. It may not close properly in all cases.
class TCPThread(private val _server: ServerServer, private val _inSocket: SocketHelper) : PrintBaseClass("TCPThread" + idNumber.getAndIncrement()), Runnable {
    private val _version = 0.toByte()
    var sessions = HashMap<Int, IoTSession>()
    private val ioTNodeDAO = IoTNodeDAO()
    private val ioTActionDAO = IoTActionDAO()
    private val networkArbiter: ZoarialNetworkArbiter = ZoarialNetworkArbiter
    override fun run() {
        // TODO: have some way to add and remove the socket to the list
        // We could then close the socket and have it flush before the ServerSocket is closed
        // UPDATE: we should use shutdown to shutdown the input, then clean up, then close.
        threads.add(Thread.currentThread())
        println("Starting connection with: " + _inSocket.inSocket.remoteSocketAddress)
        try { // This might be unnecessary, but better safe than sorry
            runLogic()
        } finally {
            // Make sure even if something goes wrong, that these things happen.
            threads.remove(Thread.currentThread())
            cleanup()
            println("Stopping.")
        }
    }

    // TODO: redo this to use a circular buffer for the input
    // may not need circular buffer. Maybe assume all data coming in is correct and if not, then the socket will eventually close.
    // Use the raw stream and data stream
    private fun runLogic() {
        // If we leave this try/catch block, then the socket is closed.
        while (!_server.isClosed && !_inSocket.inSocket.isClosed) {
            var request: String?
            var otp: Int
            var securityLevel: Byte
            try {
                securityLevel = 0
                val startInfo = networkArbiter.receiveObject(TCPStart::class.java, _inSocket.inSocket)
                if (startInfo.version != _version) {
                    println("Not the correct version.")
                    return
                }
                val sessionID = startInfo.sessionId
                if (sessions.containsKey(sessionID)) {
                    // Continue working with session
                    println("Continuing to work with session: $sessionID")
                } else {
                    val session = IoTSession(sessionID)
                    // New session
                    request = startInfo.request
                    println("Working with new session: $sessionID")
                    println("Request: $request")
                    when (request) {
                        "action" -> {
                            val actionData = networkArbiter.receiveObject(ZoarialDTO.V1.Request.Action::class.java, _inSocket.inSocket)
                            val jsonObject = JSONObject(actionData.data)
                            if (actionData.otp != null) {
                                securityLevel = 1
                            }
                            val args = jsonObject.getJSONArray("args")
                            val uuid = actionData.uuid
                            // Check for the action
                            val optAction = ioTActionDAO.findActionByUUID(uuid!!)
                            if (optAction.isPresent) {
                                val action = optAction.get()
                                if (action.node != _server.selfNode) {
                                    respondToSession(session, _server.runRemoteAction(action, jsonObject))
                                } else if (action.local!! && !_inSocket.isLocal) {
                                    respondToSession(session, "You don't have access to this. (Local).")
                                    // Check assess
                                } else if (action.encrypted!! && !(_inSocket.isEncrypted || _inSocket.isLocal)) {
                                    respondToSession(session, "You don't have access to this. (Encrypted).")
                                    // You have access, start doing stuff
                                } else {
                                    val numOfArguments = action.arguments!!.toInt()
                                    request = if (numOfArguments == 0) {
                                        action.execute()
                                    } else {
                                        action.execute(args)
                                    }
                                    respondToSession(session, request)
                                }
                            } else {
                                respondToSession(session, "No action with UUID: $uuid")
                            }
                        }

                        "updateActionSecurityLevel" -> {
                            val updateInfo = networkArbiter.receiveObject(UpdateActionSecurityLevel::class.java, _inSocket.inSocket)
                            val uuid = updateInfo.uuid
                            println("Updating security level of " + uuid + " to: " + updateInfo.level)
                            val optAction = ioTActionDAO.findActionByUUID(uuid!!)
                            if (optAction.isEmpty) {
                                respondToSession(session, "No action with UUID: $uuid")
                                break
                            }
                            val action = optAction.get()
                            if (action.node != _server.selfNode) {
                                respondToSession(session, "I can't update other node's actions.")
                                break
                            }
                            action.securityLevel = updateInfo.level
                            ioTActionDAO.update(action)
                            respondToSession(session, "Success.")
                        }

                        "updateActionEncrypt" -> {
                            val updateInfo = networkArbiter.receiveObject(UpdateActionEncrypt::class.java, _inSocket.inSocket)
                            val uuid = updateInfo.uuid
                            println("Updating encrypt toggle of " + uuid + " to :" + updateInfo.encrypt)
                            val optAction = ioTActionDAO.findActionByUUID(uuid!!)
                            if (optAction.isEmpty) {
                                respondToSession(session, "No action with UUID: $uuid")
                                break
                            }
                            val action = optAction.get()
                            if (action.node != _server.selfNode) {
                                respondToSession(session, "I can't update other node's actions.")
                                break
                            }
                            action.encrypted = updateInfo.encrypt
                            ioTActionDAO.update(action)
                            respondToSession(session, "Success.")
                        }

                        "updateActionLocal" -> {
                            val updateInfo = networkArbiter.receiveObject(UpdateActionLocal::class.java, _inSocket.inSocket)
                            val uuid = updateInfo.uuid
                            println("Updating local toggle of " + uuid + " to: " + updateInfo.local)
                            val optAction = ioTActionDAO.findActionByUUID(uuid!!)
                            if (optAction.isEmpty) {
                                respondToSession(session, "No action with UUID: $uuid")
                                break
                            }
                            val action = optAction.get()
                            if (action.node != _server.selfNode) {
                                respondToSession(session, "I can't update other node's actions.")
                                break
                            }
                            action.local = updateInfo.local
                            ioTActionDAO.update(action)
                            respondToSession(session, "Success.")
                        }

                        "updateActionDescription" -> {
                            val updateInfo = networkArbiter.receiveObject(UpdateActionDescription::class.java, _inSocket.inSocket)
                            val uuid = updateInfo.uuid
                            println("Updating local toggle of " + uuid + " to: " + updateInfo.description)
                            val optAction = ioTActionDAO.findActionByUUID(uuid!!)
                            if (optAction.isEmpty) {
                                respondToSession(session, "No action with UUID: $uuid")
                                break
                            }
                            val action = optAction.get()
                            if (action.node != _server.selfNode) {
                                respondToSession(session, "I can't update other node's actions.")
                                break
                            }
                            action.description = updateInfo.description
                            ioTActionDAO.update(action)
                            respondToSession(session, "Success.")
                        }

                        "info" -> {
                            request = _inSocket.readString()
                            println("Request info: $request")
                            when (request) {
                                "action" -> {
                                    val actionUUID = _inSocket.readUUID()
                                    println("Responding info about action: $actionUUID")
                                    val actionOptional = ioTActionDAO.findActionByUUID(actionUUID)
                                    val sectionList = IoTPacketSectionList()
                                    if (actionOptional.isEmpty) {
                                        sectionList.add(0.toByte())
                                        sectionList.add("An action with that UUID does not exist.")
                                        println("Action does not exist.")
                                    } else {
                                        println("Responding with action info.")
                                        sectionList.add(1.toByte())
                                        sectionList.add(actionOptional.get().toJson().toString())
                                    }
                                    respondToSession(IoTSession(sessionID, IoTSessionType.INFO), sectionList)
                                }

                                "actions" -> respondActionList(IoTSession(sessionID, IoTSessionType.INFO))
                                "general" -> {
                                    val sectionList = IoTPacketSectionList()
                                    sectionList.add(_server.serverInfo.hostname)
                                    sectionList.add(_server.serverInfo.uuid)
                                    sectionList.add(_server.serverInfo.isHeadCapable)
                                    sectionList.add(_server.serverInfo.isVolatile)
                                    respondToSession(IoTSession(sessionID, IoTSessionType.INFO), sectionList)
                                }

                                "nodes" -> {
                                    respondNodeList(IoTSession(sessionID, IoTSessionType.INFO))
                                }

                                else -> respondToSession(IoTSession(sessionID, IoTSessionType.INFO), "Unknown option to get: $request")
                            }
                        }

                        else -> respondToSession(IoTSession(sessionID, IoTSessionType.OTHER), "Invalid request: $request")
                    }
                }

                // We have to flush otherwise, out data may never be sent.
                // TODO: fixing the shutdown would prevent the need for this
                _inSocket.out!!.flush()
            } catch (e: EOFException) {
                println("End of file: should be closed!")
                return  // Cleanup is done after function returns
            } catch (ex: SocketException) {
                println("Interrupted, server must be closing.")
                return  // Cleanup is done after function returns
            } catch (ex: IOException) {
                ex.printStackTrace()
                return  // Cleanup is done after function returns
            } catch (ex: JSONException) {
                ex.printStackTrace()
                println("Something is wrong with the json format.")
                return
            }
        }
    }

    private fun respondToSession(session: IoTSession, sectionList: IoTPacketSectionList) {
        try {
            _inSocket.out!!.writeBytes("ZIoT")
            _inSocket.out.writeInt(session.sessionID)
            _inSocket.out.write(sectionList.networkResponse)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun respondToSession(session: IoTSession, str: String) {
        println("Responding: $str")
        val sectionList = IoTPacketSectionList()
        sectionList.add("ZIoT")
        sectionList.add(session.sessionID)
        sectionList.add(str)
        try {
            _inSocket.out.write(sectionList.networkResponse)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun respondActionList(session: IoTSession) {
        val actionList = ioTActionDAO.enabledActions
        val sectionList = IoTPacketSectionList(actionList.size * 4 + 3)
        sectionList.add("ZIoT")
        sectionList.add(session.sessionID)
        sectionList.add(actionList.size)
        for (action in actionList) {
            sectionList.add(action.uuid!!)
            sectionList.add(action.node!!.uuid!!)
            sectionList.add(action.name!!)
            //sectionList.add(action.getReturnType())
            sectionList.add(action.securityLevel!!)
            sectionList.add(action.arguments!!)
            sectionList.add(action.encrypted!!)
            sectionList.add(action.local!!)
        }
        try {
            _inSocket.out.write(sectionList.networkResponse)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun respondNodeList(session: IoTSession) {
        val ioTNodeDAO = IoTNodeDAO()
        val nodeList = ioTNodeDAO.allNodes
        val sectionList = IoTPacketSectionList(nodeList.size * 4 + 3)
        sectionList.add("ZIoT")
        sectionList.add(session.sessionID)
        sectionList.add(nodeList.size)
        for (node in nodeList) {
            sectionList.add(node.uuid!!)
            sectionList.add(node.hostname!!)
            sectionList.add(node.nodeType!!)
            sectionList.add(node.lastHeardFrom!!)
        }
        try {
            _inSocket.out.write(sectionList.networkResponse)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun cleanup() {
        try {
            if (!_inSocket.isClosed) {
                _inSocket.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private val idNumber = AtomicInteger(0)
        private val threads = Collections.synchronizedList(ArrayList<Thread>(8))
        fun getThreads(): List<Thread> {
            return threads
        }
    }
}