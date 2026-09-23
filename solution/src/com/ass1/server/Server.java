package com.ass1.server;

import com.ass1.Database.DatabaseConnector;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server implements ServiceInterface {
    private static final java.util.concurrent.CountDownLatch STOP_LATCH = new java.util.concurrent.CountDownLatch(1);
    private DatabaseConnector databaseConnector;
    private static int DEFAULT_NETWORK_DELAY_MS = 80;
    private final TaskQueue taskQueue = new TaskQueue();
    private final AtomicBoolean requestProcessingThreadAlive = new AtomicBoolean(true);
    private int zone;

    public Server(Registry registry, int port, int zone) {
        super();
        try {
            this.zone = zone;
            this.databaseConnector = new DatabaseConnector();
            // Bind the server to the RMI registry
            String serverName = "server-" + port;
            ServiceInterface taskRegistrationServer = (ServiceInterface) UnicastRemoteObject.exportObject(this, 0);
            registry.bind(serverName, taskRegistrationServer);
            // Start the task processing thread
            Thread taskThread = new Thread(() -> {
                while (requestProcessingThreadAlive.get()) {
                    taskQueue.executeNext();
                }
            }, "server-task-worker-" + port);
            taskThread.start();
            // Register server on proxy server API
            ProxyInterfaceForServerRegistration proxyRegister=(ProxyInterfaceForServerRegistration) registry.lookup("proxyServerAPI");
            proxyRegister.registerServer(serverName, port, "localhost", this.zone);
            System.out.printf("Server bound as '%s' on registry%n", serverName);
            // keep running
            STOP_LATCH.await();
            requestProcessingThreadAlive.set(false);
        } catch (RemoteException | AlreadyBoundException | NotBoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public int getQueueSize() throws RemoteException {
        //TODO:change this logic
        int min=0;
        int max=18;
        int size= (int) (Math.random()*(max-min));
        return size;
    }

    @Override
    public Integer getPopulationofCountry(int clientZone, String countryName) throws RemoteException {
        //TODO: remember to gather statistics time on every method
//        long waitingTimeStart=System.currentTimeMillis();
        //add request to waiting list
//        long waitingTimeEnd= System.currentTimeMillis();
//        long executionTimeStart=System.currentTimeMillis();
        //Object result= this.databaseConnector....
//        long executionTimeEnd=System.currentTimeMillis();
//        long waitingTime=waitingTimeEnd-waitingTimeStart;
//        long executionTime=executionTimeEnd-executionTimeStart;
        //return object or list with arguments ; waitingTime; executionTime.
        return submitTaskAndAwaitResponse(clientZone, () -> databaseConnector.getPopulationofCountry(countryName));
    }

    @Override
    public Integer getNumberofCities(int clientZone, String countryName, Integer threshold, String comp) throws RemoteException {
        return submitTaskAndAwaitResponse(clientZone, () -> databaseConnector.getNumberofCities(countryName, threshold, comp));
    }

    @Override
    public Integer getNumberofCountries(int clientZone, Integer citycount, Integer threshold, String comp) throws RemoteException {
        return submitTaskAndAwaitResponse(clientZone, () -> databaseConnector.getNumberofCountries(citycount, threshold, comp));
    }

    @Override
    public Integer getNumberofCountriesMM(int clientZone, Integer citycount, Integer minpopulation, Integer maxpopulation) throws RemoteException {
        return submitTaskAndAwaitResponse(clientZone, () -> databaseConnector.getNumberofCountriesMM(
                citycount, minpopulation, maxpopulation));
    }

    private Integer submitTaskAndAwaitResponse(int clientZone, Callable<Integer> task) throws RemoteException {
        // Simulate network delay based on the client's zone
        simulateNetworkDelay(clientZone);

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
}
