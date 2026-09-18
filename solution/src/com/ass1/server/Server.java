package com.ass1.server;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;
import java.util.ArrayList;

public class Server implements ServiceInterface {
    private static final java.util.concurrent.CountDownLatch STOP_LATCH = new java.util.concurrent.CountDownLatch(1);

    public Server(Registry registry, int port) {
        super();
        try {
            ServiceInterface serverStub = (ServiceInterface) UnicastRemoteObject.exportObject(this, 0);
            registry.bind("server-" + port, serverStub);
            // TODO: Register server on proxy server API
            System.out.printf("Server bound as 'server-%s' on registry%n", port);
            // keep running
            STOP_LATCH.await();
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Integer getPopulationofCountry(String countryName) throws RemoteException {
        return null;
    }

    @Override
    public Integer getNumberofCities(String countryName, Integer threshold, String comp) throws RemoteException {
        return null;
    }

    @Override
    public Integer getNumberofCountries(Integer citycount, Integer threshold, String comp) throws RemoteException {
        return null;
    }

    @Override
    public Integer getNumberofCountriesMM(Integer citycount, Integer minpopulation, Integer maxpopulation) throws RemoteException {
        return null;
    }
}
