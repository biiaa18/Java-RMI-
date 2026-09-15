package com.ass1.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;

public class ServerSimulator implements ServerInterface{
    private static ServerSimulator INSTANCE;
    private static final java.util.concurrent.CountDownLatch STOP_LATCH = new java.util.concurrent.CountDownLatch(1);

    public int Add(int num1, int num2) {
        return num1 + num2;
    }

    public static void start(Registry registry){
        try {
            ServerSimulator server = new ServerSimulator();
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
            registry.bind("server", serverStub);
            INSTANCE = server; // keep a strong reference so it is not GC'd
            System.out.println("Server bound as 'server' on registry");
            // keep running
            STOP_LATCH.await();
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

}
