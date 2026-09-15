package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ProxyInterface extends Remote {
    int GetServer() throws RemoteException;
}
