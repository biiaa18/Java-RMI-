package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

//server-proxy API
public interface ProxyInterfaceForServerRegistration extends Remote{
    ///register host, port and server name so client can request correct server to talk to database
    /// server info is data structure class
    public void registerServer(String name, int port, String host, int zone) throws RemoteException;

}
