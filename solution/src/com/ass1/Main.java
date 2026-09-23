package com.ass1;

import com.ass1.Database.DatabaseConnector;
import com.ass1.client.Client;
import com.ass1.server.ProxyServer;
import com.ass1.server.ServerSimulator;

import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicReference;


public class Main {
    public static Registry registry = null;

    static void main(String[] args) {
        CountDownLatch stopLatch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(stopLatch::countDown));

        try {
            registry = LocateRegistry.createRegistry(1099);
            System.out.println("Created registry on port 1099");
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }

        ProxyServer.start(registry);

        // Initialize the database before starting the ServerSimulator
        try {
            DatabaseConnector.initializeDatabase();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        ServerSimulator serverSimulator = new ServerSimulator();
        CountDownLatch workersReady = new CountDownLatch(ServerSimulator.SERVER_COUNT);
        AtomicReference<Throwable> workerStartupFailure = new AtomicReference<>();
        serverSimulator.initServers(registry, 1101, workersReady, workerStartupFailure);
        try {
            workersReady.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while starting worker servers", e);
        }
        if (workerStartupFailure.get() != null) {
            throw new RuntimeException("Worker server startup failed", workerStartupFailure.get());
        }

        Thread clientThread = new Thread(()->{
            try{
                Client.main();
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        },"rmi-client-thread");
        clientThread.start();

        try {
            stopLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
