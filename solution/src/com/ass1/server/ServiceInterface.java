package com.ass1.server;

import com.ass1.client.Result;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServiceInterface extends Remote{
    Result getPopulationofCountry(String countryName, Integer clientZone) throws RemoteException;

    Result getNumberofCities(String countryName, Integer threshold, String comp, Integer clientZone) throws RemoteException;

    Result getNumberofCountries(Integer citycount, Integer threshold, String comp, Integer clientZone) throws RemoteException;

    Result getNumberofCountriesMM(Integer citycount, Integer minpopulation, Integer maxpopulation, Integer clientZone) throws RemoteException;

    public Integer getQueueSize() throws RemoteException;
}
