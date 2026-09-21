package com.ass1.server;

import com.ass1.Database.DatabaseConnector;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;

public class Server implements ServiceInterface {
    private static final java.util.concurrent.CountDownLatch STOP_LATCH = new java.util.concurrent.CountDownLatch(1);
    private DatabaseConnector databaseConnector;

    public Server(Registry registry, int port) {
        super();
        try {
            this.databaseConnector = new DatabaseConnector();
            ServiceInterface serverStub = (ServiceInterface) UnicastRemoteObject.exportObject(this, 0);
            registry.bind("server-" + port, serverStub);
            // TODO: Register server on proxy server API (change values)
            ProxyInterfaceForServerRegistration proxyRegister=(ProxyInterfaceForServerRegistration) registry.lookup("proxyServerAPI");
            proxyRegister.registerServer("server-1",1099,"localhost",1);
            //
            System.out.printf("Server bound as 'server-%s' on registry%n", port);
            // keep running
            STOP_LATCH.await();
        } catch (RemoteException | AlreadyBoundException | NotBoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Integer getPopulationofCountry(String countryName) throws RemoteException {
        return this.databaseConnector.getPopulationofCountry(countryName);
    }

    @Override
    public Integer getNumberofCities(String countryName, Integer threshold, String comp) throws RemoteException {
        return this.databaseConnector.getNumberofCities(countryName, threshold, comp);
    }

    @Override
    public Integer getNumberofCountries(Integer citycount, Integer threshold, String comp) throws RemoteException {
        return this.databaseConnector.getNumberofCountries(citycount, threshold, comp);
    }

    @Override
    public Integer getNumberofCountriesMM(Integer citycount, Integer minpopulation, Integer maxpopulation) throws RemoteException {
        return this.databaseConnector.getNumberofCountriesMM(citycount, minpopulation, maxpopulation);
    }
}
