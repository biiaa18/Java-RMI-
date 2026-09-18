package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import com.ass1.ServerInfo;

public interface ProxyInterface extends Remote {

    ///return host, port and server name so client can request correct server to talk to database
    /// server info is data structure class
    public ServerInfo GetServer(int zoneN) throws RemoteException;

}
