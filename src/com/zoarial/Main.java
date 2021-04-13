package com.zoarial;

import java.io.IOException;

public class Main {
    static ServerNode _server;

    static void print(String str) {
        System.out.println("Main: " + str);
    }

    public static void main(String[] args) {

        boolean daemonize = false;
        boolean status;

        if (daemonize) {
            //daemonize
        }

        status = false;
        _server = new ServerNode();
        try {
            status = _server.init();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(!status) {
            return;
        }


        status = false;
        status = _server.start();

        if(status) {
            print("Server successfully started.");
        } else {
            print("Server failed to start.");
            return;
        }


        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        _server.close();
        print("Main is finished.");
    }
}
