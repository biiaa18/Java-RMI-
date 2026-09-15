package com.ass1;

import com.ass1.client.Client;
import com.ass1.server.ServerSimulator;

import java.util.concurrent.CountDownLatch;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;

public class Main {
    public static void main(String[] args) {
        CountDownLatch stopLatch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(stopLatch::countDown));

        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(1099);
            System.out.println("Created registry on port 1099");
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }

        Thread serverThread = new Thread(() -> ServerSimulator.start(registry), "rmi-server-thread");
        serverThread.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Thread clientThread = new Thread(Client::main, "rmi-client-thread");
        clientThread.start();

        try {
            stopLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
