package com.zoarial.iot.threads.udp;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.ServerServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DatagramSocketHelper extends PrintBaseClass implements Runnable {

    private final ServerServer _server;
    private DatagramSocket _ds;
    private final ArrayBlockingQueue<DatagramPacket> _queue = new ArrayBlockingQueue<>(32);
    private final int BUF_SIZE = 65535;

    public DatagramSocketHelper(ServerServer server, DatagramSocket ds) {
        super("DatagramSocketHelper");
        _ds = ds;
        _server = server;

    }

    public boolean isNextDataEmpty() {
        return _queue.isEmpty();
    }

    /*
     *  Function will not block. If there is no object, then it will return null
     */
    public DatagramPacket pollNextData() {
        return _queue.poll();
    }

    /*
     *  Function will block until the timeout is reached. It will then return null
     */
    public DatagramPacket pollNextData(long timeout, TimeUnit timeUnit) throws InterruptedException {
        try {
            return _queue.poll(timeout, timeUnit);
        } catch (InterruptedException ex) {
            println("Interrupted, returning null.");
        }
        return null;
    }

    /*
     *  Function will block until an object is retrieved.
     *  Function could return null if an exception occurs.
     */
    public DatagramPacket takeNextData() {
        try {
            return _queue.take();
        } catch (InterruptedException e) {
            println("Interrupted, returning null.");
        }
        return null;
    }

    public void run() {

        println("Starting thread...");

        while(!_server.isClosed() && !_ds.isClosed()) {
            DatagramPacket dp = new DatagramPacket(new byte[BUF_SIZE], BUF_SIZE);
            try {
                _ds.receive(dp);
                println("Received packet, adding to queue.");
                _queue.put(dp);
            } catch (SocketException | InterruptedException ex){
                println("Interrupted, no longer receiving new packets.");
            } catch (IOException e) {
                e.printStackTrace();
                println("Something went wrong!");
            }
        }
        // If _server is closed, then DatagramSocket is already closed
        println("Finished thread.");
    }

    void send(DatagramPacket dp) throws IOException {
        _ds.send(dp);
    }

    public boolean isClosed() {
        return _ds.isClosed();
    }

    public void close() {
        _ds.close();
    }

}
