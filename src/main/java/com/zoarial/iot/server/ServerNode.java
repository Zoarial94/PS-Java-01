package com.zoarial.iot.server;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.model.ServerInformation;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static com.zoarial.iot.model.ServerPublic.*;


public class ServerNode extends PrintBaseClass {

    boolean _close = false;

    ServerServer _server;

    Properties prop = new Properties();
    InputStream is = null;

    boolean _isInitialized = false;

    //Server should reset if any of these change
    String _hostname;
    byte _nodeType;
    boolean _isVolatile;
    int _serverPort;
    boolean _isHeadCapable;
    UUID _uuid;

    //Server can update these at runtime
    int _messageTimeout;
    int _pingTimeout;
    String _logFileName;
    int _loggingLevel;

    List<InetAddress> headNodes = new ArrayList<>(3);
    String _networkDeviceName;

    ServerInformation oldInfo;

    public ServerNode() {
        super("ServerNode");
        println("Constructing...");
    }

    public boolean init() {
        if(_isInitialized) {
            println("Already initialized.");
            return false;
        }
        println("Initializing...");
        try(FileInputStream inputStream = new FileInputStream(CONFIG_FILE)) {
            is = inputStream;
            prop.load(is);
        } catch (FileNotFoundException ex) {
            println("Config file not found.");
            return false;
        } catch (IOException ex) {
            println("Error reading config file.");
            return false;
        }

        if(!prop.getProperty("app.name").equals(APP_NAME)) {
            println("Given config file is not a Z-IoT config file.");
            return false;
        } else if (!prop.getProperty("app.version").equals(CONFIG_VERSION)) {
            println("Config file is not the correct version");
            return false;
        }

        _uuid = UUID.fromString(prop.getProperty(DEVICE + "uuid", UUID.randomUUID().toString()));

        _hostname = prop.getProperty(DEVICE + "hostname", "PS-testing1");

        _nodeType = Byte.parseByte(prop.getProperty(DEVICE + "node_type", "0"));
        _isVolatile = Boolean.parseBoolean(prop.getProperty(DEVICE + "is_volatile", "true"));
        _serverPort = Integer.parseInt(prop.getProperty(DEVICE + "port", "9494"));
        _messageTimeout = Integer.parseInt(prop.getProperty(DEVICE + "message_timeout", "300"));
        _pingTimeout = Integer.parseInt(prop.getProperty(DEVICE + "ping_timeout", "90"));
        _isHeadCapable = Boolean.parseBoolean(prop.getProperty(DEVICE + "is_head_capable", "false"));
        _networkDeviceName = prop.getProperty(DEVICE + "network_device", "eth0");

        _logFileName = prop.getProperty(LOGGING + "file_name", "/var/log/PS-Java-Test.log");
        _loggingLevel = Integer.parseInt(prop.getProperty(LOGGING + "level", "1"));

        try {
            headNodes.add(InetAddress.getByName(prop.getProperty(DEVICE + "headNode1", "0.0.0.0")));
            headNodes.add(InetAddress.getByName(prop.getProperty(DEVICE + "headNode2", "0.0.0.0")));
            headNodes.add(InetAddress.getByName(prop.getProperty(DEVICE + "headNode3", "0.0.0.0")));
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }

        println("Hostname is " + _hostname);
        println("Node Type is " + _nodeType);
        println("isVolatile is " + _isVolatile);
        println("Server port is " + _serverPort);
        println("Message timeout is " + _messageTimeout);
        println("Ping timeout is " + _pingTimeout);
        println("Log file name is " + _logFileName);
        println("Logging Level is " + _loggingLevel);
        println("isHeadCapable is " + _isHeadCapable);
        println("headNodes is " + headNodes);

        //  May throw
        try {
            oldInfo = new ServerInformation(_hostname, _uuid, _nodeType, _serverPort, _isVolatile, _isHeadCapable,_messageTimeout, _pingTimeout, _networkDeviceName, headNodes);
            _server = new ServerServer(new ServerInformation(oldInfo));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        _isInitialized = true;

        return true;
    }

    public boolean start() {
        if (!_isInitialized) {
            println("Node is not initialized yet. Cannot start.");
            return false;
        } else if (_server.isStarted()) {
            println("Unable to start server: server has already started.");
        } else {
            println("Staring Server...");
        }

        Thread t1 = new Thread(_server);
        t1.start();

        return true;
    }

    public void close(boolean join) {
        _close = true;
        _server.close();
        //TODO: check ServerServer for changes and then save those changes to the .config file
        ServerInformation serverInfo = _server.getInfo();
        if(!serverInfo.equals(oldInfo)) {
            println("Updating config file.");
            prop.setProperty(DEVICE + "uuid", serverInfo.uuid.toString());
            prop.setProperty(DEVICE + "headNode1", serverInfo.headNodes.get(0).getHostAddress());
            prop.setProperty(DEVICE + "headNode2", serverInfo.headNodes.get(1).getHostAddress());
            prop.setProperty(DEVICE + "headNode3", serverInfo.headNodes.get(2).getHostAddress());
            try(final OutputStream outputStream = new FileOutputStream(CONFIG_FILE)) {
                prop.store(outputStream, "Updated");
            } catch (IOException ignored) {
            }
        }
        if(join) {
            _server.join();
        }
    }

    public void close() {
        close(false);
    }

}
