package com.ass1.server;

import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import com.ass1.ServerInfo;
import com.ass1.client.RequestStatistics;
import com.ass1.server.ServiceInterface;

public class ProxyServer implements ProxyInterface, ProxyInterfaceForServerRegistration {
    private static ProxyServer INSTANCE;
    //private static final java.util.concurrent.CountDownLatch STOP_LATCH = new java.util.concurrent.CountDownLatch(1);
    private static Integer maxRequests=18;
    Map<Integer, ServerInfo> servers= new HashMap<>();
    Map<String,Integer> serversReturned=new HashMap<>();

    public static void start(Registry registry){
        try {
            ProxyServer proxyServer = new ProxyServer();
            Remote exportStub=UnicastRemoteObject.exportObject(proxyServer, 0);

            ProxyInterface proxyStub = (ProxyInterface) exportStub;
            registry.bind("proxyClientAPI", proxyStub);
            System.out.println("Proxy Server bound 'proxyClientAPI' on registry");

            ProxyInterfaceForServerRegistration serverProxyStub = (ProxyInterfaceForServerRegistration) exportStub;
            registry.bind("proxyServerAPI", serverProxyStub);
            System.out.println("Proxy Server bound 'proxyServerAPI' on registry");

            INSTANCE = proxyServer; // keep a strong reference so it is not GC'd
            // keep running
            //STOP_LATCH.await();
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }
//         catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
    }

    @Override
    public ServerInfo GetServer(int zoneN) throws RemoteException {
        //lookup server with zoneN in server simulator
        //remember!!!! add logic of overloaded servers and non existent servers in client zone
        //
//        int closestZoneN=zoneN;
//        //find server in same zone as client's request
//        if(servers.containsKey(zoneN)){
//            ServerInfo info=servers.get(zoneN);
//            if(serversReturned.get(info.serverName)<18){
//                Integer newCount=serversReturned.get(info.serverName);
//                serversReturned.put(info.serverName,newCount+1);
//                return servers.get(zoneN);
//            }
//            else{
//                //fetch update
//
//                //reset counter
//                serversReturned.put(info.serverName,0);
//                return servers.get(zoneN);
//            }
//        }
//        // no server in zone, change zone clockwise, TODO:: make more dynamic
//        else{
//            if(closestZoneN < servers.size()){
//                closestZoneN++;
//                GetServer(closestZoneN);
//            }
//            else{
//                closestZoneN=1;
//                GetServer(closestZoneN);
//            }
//        }
        ///redo logic below:
        //ServerInterface server = (ServerInterface) registry.lookup("server-"+zoneN);
        ServerInfo info=new ServerInfo(1099,"localhost","server-1",5);
        return info;
    }

    @Override
    public void registerServer(String name, int port, String host, int zone) throws RemoteException{
        ServerInfo info= new ServerInfo(port,host,name,zone);
        servers.put(zone,info);
        serversReturned.put(name,0);
        System.out.println(("in proxy registered: "+ name + " zone:"+ zone + "port:" + port + "host:"+host));
    }
}
