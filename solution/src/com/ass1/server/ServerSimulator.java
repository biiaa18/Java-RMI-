package com.ass1.server;

import java.rmi.registry.Registry;

public class ServerSimulator {
    public static final int SERVER_COUNT = 5;
    Server[] servers = new Server[SERVER_COUNT];

    /*
    * Starts up the worker servers in their own threads. Takes in
    * the rmi registry as param.
    */
    public void initServers(Registry registry) {
        for (int port = 1; port <= SERVER_COUNT; port++) {
            String serverName = "rmi-server-thread-" + port;
            int _port = port;
            new Thread(() -> {
                Server server = new Server(registry, _port, _port);
                servers[_port - 1] = server;
            }, serverName).start();
        }
    }
}
