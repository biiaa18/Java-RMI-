package main.java.com.ass1.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import main.java.com.ass1.server.ServerInterface;

public class Client {
    public static void main() {
        try {
            Registry registry = LocateRegistry.getRegistry(1099);
            ServerInterface server = (ServerInterface) registry.lookup("server");
            System.out.println(server.Add(10,20));
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
