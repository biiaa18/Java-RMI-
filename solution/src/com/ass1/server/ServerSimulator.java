package com.ass1.server;

import java.rmi.registry.Registry;

public class ServerSimulator {
    final int SERVER_COUNT = 7;
    Server[] servers = new Server[SERVER_COUNT];

    /*
    * Starts up the worker servers in their own threads. Takes in
    * the rmi registry as param.
    */
    public void initServers(Registry registry) {
        for (int port = 1; port < this.SERVER_COUNT + 1; port++) {
            String serverName = "rmi-server-thread-" + port;
            int _port = port;
            new Thread(() -> {
                Server server = new Server(registry, _port);
                servers[_port] = server;
            }, serverName).start();
        }
    }
}
