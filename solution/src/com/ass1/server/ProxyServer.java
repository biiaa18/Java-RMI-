package com.ass1.server;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import com.ass1.ServerInfo;
import com.ass1.server.ServiceInterface;

public class ProxyServer implements ProxyInterface {
    private static ProxyServer INSTANCE;
    private static final java.util.concurrent.CountDownLatch STOP_LATCH = new java.util.concurrent.CountDownLatch(1);
    private static Integer maxRequests=18;

    public static void start(Registry registry){
        try {
            ProxyServer proxyServer = new ProxyServer();
            ProxyInterface proxyStub = (ProxyInterface) UnicastRemoteObject.exportObject(proxyServer, 0);
            registry.bind("proxyServer", proxyStub);
            INSTANCE = proxyServer; // keep a strong reference so it is not GC'd
            System.out.println("Proxy Server bound as 'proxyServer' on registry");
            // keep running
            STOP_LATCH.await();
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public ServerInfo GetServer(int zoneN) throws RemoteException {
        //lookup server with zoneN in server simulator
        //remember!!!! add logic of overloaded servers and non existent servers in client zone
        ///redo logic below:
        //ServerInterface server = (ServerInterface) registry.lookup("server");
        ServerInfo info=new ServerInfo();
        info.port=3;
        info.host=" ";
        info.serverName="server";
        return info;
    }
}
