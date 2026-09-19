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

        Thread proxyServerThread = new Thread(() -> ProxyServer.start(registry), "rmi-proxy-server-thread");
        proxyServerThread.start();

        // Initialize the database before starting the ServerSimulator
        try {
            DatabaseConnector.initializeDatabase();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        ServerSimulator serverSimulator = new ServerSimulator();
        serverSimulator.initServers(registry);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
