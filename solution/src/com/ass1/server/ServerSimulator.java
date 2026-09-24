package com.ass1.server;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class ServerSimulator {
    public static final int SERVER_COUNT = 5;
    Server[] servers = new Server[SERVER_COUNT];

    /*
    * Starts up the worker servers in their own threads. Takes in
    * the rmi registry as param.
    */
    public void initServers(Registry proxyRegistry, int firstWorkerPort,
                            CountDownLatch ready, AtomicReference<Throwable> startupFailure) {
        for (int zone = 1; zone <= SERVER_COUNT; zone++) {
            final int serverZone = zone;
            final int workerPort = firstWorkerPort + serverZone - 1;
            new Thread(() -> {
                try {
                    Registry workerRegistry = LocateRegistry.createRegistry(workerPort);
                    Server server = new Server(proxyRegistry, workerRegistry, workerPort, serverZone,
                            ready, startupFailure);
                    servers[serverZone - 1] = server;
                } catch (RemoteException e) {
                    startupFailure.compareAndSet(null, e);
                    ready.countDown();
                }
            }, "rmi-server-thread-" + serverZone).start();
        }
    }
}
