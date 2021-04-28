package com.zoarial.iot;

import com.zoarial.PrintBaseClass;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static com.zoarial.iot.ServerPublic.*;


public class ServerNode extends PrintBaseClass {

    boolean _close = false;

    ServerServer _server;

    Properties prop = new Properties();
    InputStream is = null;

    boolean _isInitialized = false;

    //Server should reset if any of these change
    String _hostname;
    int _nodeType;
    boolean _isVolatile;
    int _serverPort;
    boolean _isHeadCapable;

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
        try {
            is = new FileInputStream(CONFIG_FILE);
        } catch (FileNotFoundException ex) {
            return false;
        }

        try {
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

        _hostname = prop.getProperty(DEVICE + "hostname", "PS-testing1");
        _nodeType = Integer.parseInt(prop.getProperty(DEVICE + "node_type", "0"));
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
            _server = new ServerServer(_hostname, _nodeType, _isVolatile, _serverPort, _messageTimeout, _pingTimeout, _isHeadCapable);
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

    public void close() {
        _close = true;
        _server.close();
    }

}
