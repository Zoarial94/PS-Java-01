package com.zoarial;

import com.zoarial.iot.ServerNode;

public class Main {
    static ServerNode _server;

    static void println(String str) {
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
            println("Server successfully started.");
        } else {
            println("Server failed to start.");
            return;
        }


        /*
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        _server.close();
        println("Main is finished.");
        println("Waiting for other threads to finish.");
         */


        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            _server.close(true); // Wait on all threads
            System.out.flush(); // Make sure we see all data on std out before we finish.
        }));

    }
}
