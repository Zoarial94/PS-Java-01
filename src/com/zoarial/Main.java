package com.zoarial;

import java.io.IOException;

public class Main {
    static ServerNode _server;

    static void print(String str) {
        System.out.println("Main: " + str);
    }

    public static void main(String[] args) {

        boolean daemonize = false;

        if (daemonize) {
            //daemonize
        }

        _server = new ServerNode();
        _server.init();
        try {
            if(_server.start()) {
                print("Server sucessfully started.");
            } else {
                print("Server failed to start");
            }

        } catch(IOException ex) {
            print("Server start error.");
            return;
        }
    }
}
