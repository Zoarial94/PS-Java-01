package com.zoarial.threads;

import com.zoarial.PrintBaseClass;
import com.zoarial.ServerServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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
        try {
            //  Set socket timeout so that it can stop blocking and be shutdown, eventually.
            servSocket.setSoTimeout(10000);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        Socket tmp = null;
        while(!_server.isClosed()) {
            try {
                tmp = servSocket.accept();
                println("Accepting new socket.");
                _queue.put(tmp);
            } catch (SocketTimeoutException e) {
                //println("Socked Timeout");
            } catch (InterruptedException e) {
                e.printStackTrace();
                println("Unable to add socket to queue! Closing and dropping socket...");
                try {
                    tmp.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    println("Something must have gone terribly wrong to get here.");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        println("Finished Thread");
    }


    public boolean isNextSocketEmpty() {
        return _queue.isEmpty();
    }

    /*
     *  Function will not block. If there is no object, then it will return null
     */
    public Socket pollNextSocket() {
        return _queue.poll();
    }

    /*
     *  Function will block until the timeout is reached. It will then return null
     */
    public Socket pollNextSocket(long timeout, TimeUnit timeUnit) {
        try {
            return _queue.poll(timeout, timeUnit);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     *  Function will block until an object is retrieved.
     *  Function could return null if an exception occurs.
     */
    public Socket takeNextSocket() {
        try {
            return _queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
