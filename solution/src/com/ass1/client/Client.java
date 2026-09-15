package com.ass1.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import com.ass1.server.ProxyInterface;
import com.ass1.server.ServerInterface;

public class Client {
    public static void main() {
        try {
            Registry registry = LocateRegistry.getRegistry(1099);
            ServerInterface server = (ServerInterface) registry.lookup("server");
            ProxyInterface proxyServer = (ProxyInterface) registry.lookup("proxyServer");
            System.out.println(server.Add(10,20));
            System.out.println(proxyServer.GetServer());
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
