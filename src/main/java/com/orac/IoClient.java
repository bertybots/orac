package com.orac;

import io.socket.client.*;

public class IoClient {
    static final int PORT = 9292;

    public static void main(String[] args) {
        System.out.println("Starting Server");

        Thread ts = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runServer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        ts.start();
    }

    public static void runServer() {
        try {
            var socket = IO.socket("http://localhost:8200");
            socket.on(Socket.EVENT_CONNECT, args -> {
                System.out.println("Connectd to Server");
            });
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                System.out.println("Disconnected from Server");
            });
            socket.on("curve", args -> {
                System.out.println("Let's Build a curve");
            });
            socket.connect();
        } catch (Exception ex) {

        }
    }
}
