package main.java.com.ass1;

import main.java.com.ass1.client.Client;
import main.java.com.ass1.server.ServerSimulator;

import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) {
        CountDownLatch stopLatch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(stopLatch::countDown));

        Thread serverThread = new Thread(ServerSimulator::main, "rmi-server-thread");
        Thread clientThread = new Thread(Client::main, "rmi-client-thread");

        serverThread.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        clientThread.start();

        try {
            stopLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}