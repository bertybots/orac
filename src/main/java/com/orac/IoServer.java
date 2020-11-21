package com.orac;

import io.socket.client.*;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.DataListener;

public class IoServer {
    static final int PORT = 9292;
    static SocketIOServer server;

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
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(PORT);
        server = new SocketIOServer(config);
        server.addEventListener("toServer", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) {
                client.sendEvent("toClient", "message from server");
            }
        });
        server.start();
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        server.stop();
    }
}
