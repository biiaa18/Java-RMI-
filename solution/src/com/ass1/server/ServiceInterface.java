package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServiceInterface extends Remote{
    Integer getPopulationofCountry(String countryName, Integer clientZone) throws RemoteException;

    Integer getNumberofCities(String countryName, Integer threshold, String comp, Integer clientZone) throws RemoteException;

    Integer getNumberofCountries(Integer citycount, Integer threshold, String comp, Integer clientZone) throws RemoteException;

    Integer getNumberofCountriesMM(Integer citycount, Integer minpopulation, Integer maxpopulation, Integer clientZone) throws RemoteException;

    public Integer getQueueSize() throws RemoteException;
}
