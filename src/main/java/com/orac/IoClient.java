package com.orac;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.*;

public class IoClient {
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

        boolean running = true;
        while (running) {
            try {
                Thread.sleep(1000);
                System.out.println("Still here...");
            } catch (Exception ex) {
            }
        }
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
                System.out.println("Curve Build...");
                JSONObject jsonObject = (JSONObject) args[0];

                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        JSONObject obj = (JSONObject) jsonObject.get(key);
                        String name = (String) obj.get("name");
                        Double value = (Double) obj.get("value");
                        System.out.println("Got Name: " + name + ", Value: " + value);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                System.out.println("Curve Publish");

                try {
                    JSONObject obj = new JSONObject();
                    obj.put("curve", "server");
                    obj.put("binary", new byte[42]);
                    socket.emit("cooked", obj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
            socket.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
