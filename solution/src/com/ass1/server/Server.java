package com.ass1.server;

import com.ass1.Database.DatabaseConnector;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server implements ServiceInterface {
    private static final java.util.concurrent.CountDownLatch STOP_LATCH = new java.util.concurrent.CountDownLatch(1);
    private DatabaseConnector databaseConnector;
    private static int NETWORK_DELAY_MS = 80;
    private TaskQueue taskQueue = new TaskQueue();
    private Thread taskThread;
    private AtomicBoolean workerThreadAlive = new AtomicBoolean(true);

    public Server(Registry registry, int port) {
        super();
        try {
            this.databaseConnector = new DatabaseConnector();
            // Bind the server to the RMI registry
            ServiceInterface taskRegistrationServer = (ServiceInterface) UnicastRemoteObject.exportObject(this, 0);
            registry.bind("server-" + port, taskRegistrationServer);
            // Start the task processing thread
            this.taskThread = new Thread(() -> {
                while (workerThreadAlive.get()) {
                    taskQueue.executeNext();
                }
            }, "server-task-worker-" + port);
            this.taskThread.start();
            // TODO: Register server on proxy server API
            System.out.printf("Server bound as 'server-%s' on registry%n", port);
            // keep running
            STOP_LATCH.await();
            workerThreadAlive.set(false);
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Simulates network delay between requests.
     */
    private void simulateNetworkDelay() {
        try {
            Thread.sleep(NETWORK_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Integer getPopulationofCountry(String countryName) throws RemoteException {
        return submitTaskAndAwaitResponse(() -> databaseConnector.getPopulationofCountry(countryName));
    }

    @Override
    public Integer getNumberofCities(String countryName, Integer threshold, String comp) throws RemoteException {
        return submitTaskAndAwaitResponse(() -> databaseConnector.getNumberofCities(countryName, threshold, comp));
    }

    @Override
    public Integer getNumberofCountries(Integer citycount, Integer threshold, String comp) throws RemoteException {
        return submitTaskAndAwaitResponse(() -> databaseConnector.getNumberofCountries(citycount, threshold, comp));
    }

    @Override
    public Integer getNumberofCountriesMM(Integer citycount, Integer minpopulation, Integer maxpopulation) throws RemoteException {
        return submitTaskAndAwaitResponse(() -> databaseConnector.getNumberofCountriesMM(
                citycount, minpopulation, maxpopulation));
    }

    private Integer submitTaskAndAwaitResponse(Callable<Integer> task) throws RemoteException {
        ServerJob serverJob = new ServerJob(task);
        taskQueue.add(serverJob);

        try {
            serverJob.taskCompletionSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteException("Request interrupted while waiting for task completion", e);
        }

        if (serverJob.failure != null) {
            throw new RemoteException("Request failed", serverJob.failure);
        }
        return serverJob.result;
    }
}
