package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServiceInterface extends Remote{
    Integer getPopulationofCountry(int clientZone, String countryName) throws RemoteException;

    Integer getNumberofCities(int clientZone, String countryName, Integer threshold, String comp) throws RemoteException;

    Integer getNumberofCountries(int clientZone, Integer citycount, Integer threshold, String comp) throws RemoteException;

    Integer getNumberofCountriesMM(int clientZone, Integer citycount, Integer minpopulation, Integer maxpopulation) throws RemoteException;

    public int getQueueSize() throws RemoteException;
}
