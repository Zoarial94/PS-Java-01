package com.zoarial.iot.model;

public class ServerPublic {
    static public final int RECV_NODE  =	0;
    static public final int BASIC_NODE = 	1;
    static public final int SATELLITE  =	2;

    static public final String APP_NAME = "PS-Java-Test";
    static public final String CONFIG_VERSION = "0.0.1";

    static public final String PREFIX = "app.";
    static public final String DEVICE = PREFIX + "device.";
    static public final String LOGGING = PREFIX + "logging.";

    static public final String CONFIG_FILE = "/etc/PS-Java-Test.config";
}
