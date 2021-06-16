package com.zoarial.iot.threads.tcp;

import com.zoarial.PrintBaseClass;
import com.zoarial.iot.server.ServerServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/*
 *
 * Just accepts new connections in a non-blocking fashion
 *
 */
public class ServerSocketHelper extends PrintBaseClass implements Runnable {

    private final ServerServer _server;
    private final ServerSocket servSocket;

    //  Final doesn't mean const
    //  Final means it can't be reassigned and makes for a good concurrency lock.
    final ArrayBlockingQueue<Socket> _queue = new ArrayBlockingQueue<>(32);

    public ServerSocketHelper(ServerServer server, ServerSocket socket) {
        super("ServerSocketHelper");
        this.servSocket = socket;
        _server = server;
    }

    public void run() {

        Socket tmp = null;
        while(!_server.isClosed() && !servSocket.isClosed()) {
            try {
                tmp = servSocket.accept();          // Throws SocketException if closed
                println("Accepting new socket.");
                _queue.put(tmp);                    // Throws InterruptedException if closed
            } catch (InterruptedException | SocketException ex) {
                println("Interrupted, no longer accepting new connections.");
                // TODO: redo this. Its messy and possibly redundant
                // Make sure we are closing if we aren't already
                close();
                // Make sure we close the socket which may or may-not have been added to the queue.
                if(tmp != null && !tmp.isClosed()) {
                    try {
                        tmp.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                        println("Something must have gone terribly wrong to get here.");
                    }
                }
                // Make sure to clean up all sockets still in the queue
                cleanup();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        // If _server is closed, then ServerSocket is already closed too.
        println("Finished Thread");
    }


    public boolean isNextSocketEmpty() {
        // If we're closed, then the class will handle and close the sockets
        if(servSocket.isClosed()) {
            return true;
        }
        return _queue.isEmpty();
    }

    /*
     *  Function will not block. If there is no object, then it will return null
     */
    public Socket pollNextSocket() {
        // If we're closed, then the class will handle and close the sockets
        if(servSocket.isClosed()) {
            return null;
        }
        return _queue.poll();
    }

    /*
     *  Function will block until the timeout is reached. It will then return null
     */
    public Socket pollNextSocket(long timeout, TimeUnit timeUnit) {
        // If we're closed, then the class will handle and close the sockets
        if(servSocket.isClosed()) {
            return null;
        }
        try {
            return _queue.poll(timeout, timeUnit);
        } catch (InterruptedException e) {
            println("Interrupted, returning null.");
        }
        return null;
    }

    /*
     *  Function will block until an object is retrieved.
     *  Function could return null if an exception occurs.
     */
    public Socket takeNextSocket() {
        // If we're closed, then the class will handle and close the sockets
        if(servSocket.isClosed()) {
            return null;
        }
        try {
            return _queue.take();
        } catch (InterruptedException e) {
            println("Interrupted, returning null.");
        }
        return null;
    }

    public boolean isClosed() {
        return servSocket.isClosed();
    }

    public void close() {
        try {
            servSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cleanup() {
        Socket s;
        // Close all sockets still in the queue
        while(!isNextSocketEmpty()) {
            try {
                s = _queue.take();
                if(!s.isClosed()) {
                    try {
                        s.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        println("Something went wrong in cleanup");
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
