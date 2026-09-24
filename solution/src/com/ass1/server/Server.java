package com.ass1.server;

import com.ass1.Database.DatabaseConnector;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class Server implements ServiceInterface {
    private static final java.util.concurrent.CountDownLatch STOP_LATCH = new java.util.concurrent.CountDownLatch(1);
    private DatabaseConnector databaseConnector;
    private static int DEFAULT_NETWORK_DELAY_MS = 80;
    private final TaskQueue taskQueue = new TaskQueue();
    private final AtomicBoolean requestProcessingThreadAlive = new AtomicBoolean(true);
    private int zone;
    private String serverQueueLogFile;

    public Server(Registry proxyRegistry, Registry workerRegistry, int workerPort, int zone,
                  CountDownLatch ready, AtomicReference<Throwable> startupFailure) {
        super();
        try {
            this.zone = zone;
            this.databaseConnector = new DatabaseConnector();
            // Bind the server to the RMI registry
            String serverName = "server-" + zone;
            this.serverQueueLogFile = "server_%d_queue_log.txt".formatted(zone);
            ServiceInterface taskRegistrationServer = (ServiceInterface) UnicastRemoteObject.exportObject(this, 0);
            workerRegistry.bind(serverName, taskRegistrationServer);
            // Start the task processing thread
            Thread taskThread = new Thread(() -> {
                while (requestProcessingThreadAlive.get()) {
                    taskQueue.executeNext();
                }
            }, "server-task-worker-" + workerPort);
            taskThread.start();
            // Register server on proxy server API
            ProxyInterfaceForServerRegistration proxyRegister =
                    (ProxyInterfaceForServerRegistration) proxyRegistry.lookup("proxyServerAPI");
            proxyRegister.registerServer(serverName, workerPort, "localhost", this.zone);
            System.out.printf("Server bound as '%s' on registry%n", serverName);
            ready.countDown();
            // keep running
            STOP_LATCH.await();
            requestProcessingThreadAlive.set(false);
        } catch (RemoteException | AlreadyBoundException | NotBoundException e) {
            startupFailure.compareAndSet(null, e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            startupFailure.compareAndSet(null, e);
        } finally {
            ready.countDown();
        }
    }

    @Override
    public Integer getQueueSize() throws RemoteException {
        return taskQueue.getQueueSize();
    }

    @Override
    public Integer getPopulationofCountry(String countryName, Integer clientZone) throws RemoteException {
        return submitTaskAndAwaitResponse(clientZone, () -> databaseConnector.getPopulationofCountry(countryName));
    }

    @Override
    public Integer getNumberofCities(String countryName, Integer threshold, String comp, Integer clientZone) throws RemoteException {
        return submitTaskAndAwaitResponse(clientZone, () -> databaseConnector.getNumberofCities(countryName, threshold, comp));
    }

    @Override
    public Integer getNumberofCountries(Integer citycount, Integer threshold, String comp, Integer clientZone) throws RemoteException {
        return submitTaskAndAwaitResponse(clientZone, () -> databaseConnector.getNumberofCountries(citycount, threshold, comp));
    }

    @Override
    public Integer getNumberofCountriesMM(Integer citycount, Integer minpopulation, Integer maxpopulation, Integer clientZone) throws RemoteException {
        return submitTaskAndAwaitResponse(clientZone, () -> databaseConnector.getNumberofCountriesMM(
                citycount, minpopulation, maxpopulation));
    }

    private Integer submitTaskAndAwaitResponse(Integer clientZone, Callable<Integer> task) throws RemoteException {
        // Simulate network delay based on the client's zone
        simulateNetworkDelay(clientZone);
        System.out.println("");

        writeToLogFile("Request received from client in zone %d at %d".formatted(clientZone, System.currentTimeMillis()));

        //TODO: gather statistics time on every method
//        long waitingTimeStart=System.currentTimeMillis();
        //add request to waiting list
//        long waitingTimeEnd= System.currentTimeMillis();
//        long executionTimeStart=System.currentTimeMillis();
        //Object result= this.databaseConnector....
//        long executionTimeEnd=System.currentTimeMillis();
//        long waitingTime=waitingTimeEnd-waitingTimeStart;
//        long executionTime=executionTimeEnd-executionTimeStart;
        //return object or list with arguments ; waitingTime; executionTime.

        // Create a ServerJob for the task and add it to the queue
        ServerJob serverJob = new ServerJob(task);
        taskQueue.add(serverJob);

        // Wait for the task to complete and return the result
        try {
            serverJob.taskCompletionSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteException("Request interrupted while waiting for task completion", e);
        }

        writeToLogFile("Request from client in zone %d completed at %d".formatted(clientZone, System.currentTimeMillis()));

        if (serverJob.failure != null) {
            throw new RemoteException("Request failed", serverJob.failure);
        }
        return serverJob.result;
    }

    /**
     * Simulates network delay between requests.
     */
    private void simulateNetworkDelay(int clientZone) {
        int networkDelayMs = DEFAULT_NETWORK_DELAY_MS;

        // Calulate the network delay based on the zone difference between the client and server
        if (clientZone != this.zone) {
            int zoneDifference = 0;
            int currentZone = clientZone;
            while (currentZone != this.zone) {
                currentZone++;
                if (currentZone > ServerSimulator.SERVER_COUNT) {
                    currentZone = 1;
                }
                zoneDifference++;
            }
            networkDelayMs += zoneDifference * 30;
        }

        try {
            Thread.sleep(networkDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void writeToLogFile(String message) {
        try (java.io.FileWriter writer = new java.io.FileWriter("./server_logs/%s".formatted(this.serverQueueLogFile), true)) {
            writer.write(message + System.lineSeparator());
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
