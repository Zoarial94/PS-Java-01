package com.zoarial;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerNode {

    ServerServer _server;

    Properties prop = new Properties();
    InputStream is = null;

    boolean _isInitialized = false;

    //Server should reset if any of these change
    String _hostname;
    int _nodeType;
    boolean _isVolatile;
    String _configFileName;
    int _serverPort;

    //Server can update these at runtime
    int _messageTimeout;
    int _pingTimeout;
    String _logFileName;
    int _loggingLevel;

    static final int RECV_NODE  =	0;
    static final int BASIC_NODE = 	1;
    static final int SATELLITE  =	2;

    static final String APP = "PS-Java-Test";
    static final String CONFIG_VERSION = "0.0.1";

    static final String PREFIX = "app.";
    static final String DEVICE = PREFIX + "device.";
    static final String LOGGING = PREFIX + "logging.";

    static final String CONFIG_FILE = "/etc/PS-Java-Test.config";

    private void print(String str) {
        System.out.println("ServerConfig: " + str);
    }

    public ServerNode() {
        print("Constructing...");
    }

    public void init() {
        print("Initializing...");
        try {
            is = new FileInputStream(CONFIG_FILE);
        } catch (FileNotFoundException ex) {
            return;
        }
        try {
            prop.load(is);
        } catch (IOException ex) {
            return;
        }

        _hostname = prop.getProperty(DEVICE + "hostname", "PS-testing1");
        _nodeType = Integer.parseInt(prop.getProperty(DEVICE + "node_type", "0"));
        _isVolatile = Boolean.parseBoolean(prop.getProperty(DEVICE + "is_volatile", "true"));
        _serverPort = Integer.parseInt(prop.getProperty(DEVICE + "port", "9494"));
        _messageTimeout = Integer.parseInt(prop.getProperty(DEVICE + "message_timeout", "300"));
        _pingTimeout = Integer.parseInt(prop.getProperty(DEVICE + "ping_timeout", "90"));

        _logFileName = prop.getProperty(LOGGING + "file_name", "/var/log/PS-Java-Test.log");
        _loggingLevel = Integer.parseInt(prop.getProperty(LOGGING + "level", "1"));

        print("Hostname is " + _hostname);
        print("Node Type is " + _nodeType);
        print("isVolatile is " + _isVolatile);
        print("Server port is " + _serverPort);
        print("Message timeout is " + _messageTimeout);
        print("Ping timeout is " + _pingTimeout);
        print("Log file name is " + _logFileName);
        print("Logging Level is " + _loggingLevel);

        _server = new ServerServer(_hostname, _nodeType, _isVolatile, _serverPort, _messageTimeout, _pingTimeout);

        _isInitialized = true;

    }

    public boolean start() throws IOException {
        if(!_isInitialized) {
            print("Node is not initialized yet. Cannot start.");
            return false;
        } else {
            print("Staring Server...");
        }

        try {
            _server.start();
        } catch(IOException ex) {
            throw ex;
        }

        return true;
    }

}
