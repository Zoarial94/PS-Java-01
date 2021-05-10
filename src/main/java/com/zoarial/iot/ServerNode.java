package com.zoarial.iot;

import com.zoarial.PrintBaseClass;

import java.io.*;
import java.util.Properties;
import java.util.UUID;

import static com.zoarial.iot.ServerPublic.*;


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
        } catch (IOException ex) {
            return false;
        }

        if(!prop.getProperty("app.name").equals(APP_NAME)) {
            println("Given config file is not a Z-IoT config file.");
            return false;
        } else if (!prop.getProperty("app.version").equals(CONFIG_VERSION)) {
            println("Config file is not the correct version");
            return false;
        }
        if(!prop.containsKey(DEVICE + "uuid")) {
            _uuid = UUID.randomUUID();
            prop.setProperty(DEVICE + "uuid", _uuid.toString());
            try(final OutputStream outputStream = new FileOutputStream(CONFIG_FILE)) {
                prop.store(outputStream, "Added UUID");
            } catch (IOException ex) {
                return false;
            }
        } else {
            _uuid = UUID.fromString(prop.getProperty(DEVICE + "uuid"));
        }

        _hostname = prop.getProperty(DEVICE + "hostname", "PS-testing1");

        _nodeType = Byte.parseByte(prop.getProperty(DEVICE + "node_type", "0"));
        _isVolatile = Boolean.parseBoolean(prop.getProperty(DEVICE + "is_volatile", "true"));
        _serverPort = Integer.parseInt(prop.getProperty(DEVICE + "port", "9494"));
        _messageTimeout = Integer.parseInt(prop.getProperty(DEVICE + "message_timeout", "300"));
        _pingTimeout = Integer.parseInt(prop.getProperty(DEVICE + "ping_timeout", "90"));
        _isHeadCapable = Boolean.parseBoolean(prop.getProperty(DEVICE + "is_head_capable", "false"));

        _logFileName = prop.getProperty(LOGGING + "file_name", "/var/log/PS-Java-Test.log");
        _loggingLevel = Integer.parseInt(prop.getProperty(LOGGING + "level", "1"));

        println("Hostname is " + _hostname);
        println("Node Type is " + _nodeType);
        println("isVolatile is " + _isVolatile);
        println("Server port is " + _serverPort);
        println("Message timeout is " + _messageTimeout);
        println("Ping timeout is " + _pingTimeout);
        println("Log file name is " + _logFileName);
        println("Logging Level is " + _loggingLevel);
        println("isHeadCapable is " + _isHeadCapable);

        //  May throw
        try {
            _server = new ServerServer(_hostname, _uuid, _nodeType, _isVolatile, _serverPort, _messageTimeout, _pingTimeout, _isHeadCapable);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        _isInitialized = true;

        return true;
    }

    public boolean start() {
        if(!_isInitialized) {
            println("Node is not initialized yet. Cannot start.");
            return false;
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
        if(join) {
            _server.join();
        }
    }

    public void close() {
        close(false);
    }

}
