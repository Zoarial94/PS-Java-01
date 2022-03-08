package com.zoarial.iot.threads.udp;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.server.ServerServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DatagramSocketHelper extends PrintBaseClass implements Runnable {

    private final ServerServer _server;
    private final DatagramSocket _ds;
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
    public Optional<DatagramPacket> pollNextData() {
        return Optional.ofNullable(_queue.poll());
    }

    /*
     *  Function will block until the timeout is reached or an object is found.
     *  If the timeout is reached, null is returned.
     */
    public Optional<DatagramPacket> pollNextData(long timeout, TimeUnit timeUnit) throws InterruptedException {
        try {
            return Optional.ofNullable(_queue.poll(timeout, timeUnit));
        } catch (InterruptedException ex) {
            println("Interrupted, returning null.");
        }
        return Optional.empty();
    }

    /*
     *  Function will block until an object is retrieved.
     *  Function could return null if an exception occurs.
     */
    public Optional<DatagramPacket> takeNextData() {
        try {
            return Optional.of(_queue.take());
        } catch (InterruptedException e) {
            println("Interrupted, returning null.");
        }
        return Optional.empty();
    }

    public void run() {

        println("Starting thread on port " + _ds.getLocalPort());
        /*
        try {
            _ds.setSoTimeout(10000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
         */

        while(!_server.isClosed() && !_ds.isClosed()) {
            DatagramPacket dp = new DatagramPacket(new byte[BUF_SIZE], BUF_SIZE);
            try {
                _ds.receive(dp);
                println("Received packet, adding to queue.");
                _queue.put(dp);
            } catch (SocketException | InterruptedException ex) {
                println("Interrupted, no longer receiving new packets.");
            } catch (SocketTimeoutException ignore) {
                println("Timeout");
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
