package com.ass1.server;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ProxyServer implements ProxyInterface {
    private static ProxyServer INSTANCE;
    private static final java.util.concurrent.CountDownLatch STOP_LATCH = new java.util.concurrent.CountDownLatch(1);

    public static void start(Registry registry){
        try {
            ProxyServer proxyServer = new ProxyServer();
            ProxyInterface proxyStub = (ProxyInterface) UnicastRemoteObject.exportObject(proxyServer, 0);
            registry.bind("proxyServer", proxyStub);
            INSTANCE = proxyServer; // keep a strong reference so it is not GC'd
            System.out.println("Proxy Server bound as 'proxyServer' on registry");
            // keep running
            STOP_LATCH.await();
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public int GetServer() throws RemoteException {
        return 0;
    }
}
