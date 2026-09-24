package com.ass1.server;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ass1.ServerInfo;
import com.ass1.client.RequestStatistics;
import com.ass1.server.ServiceInterface;

public class ProxyServer implements ProxyInterface, ProxyInterfaceForServerRegistration {
    private static ProxyServer INSTANCE;
    //private static final java.util.concurrent.CountDownLatch STOP_LATCH = new java.util.concurrent.CountDownLatch(1);
    private static Integer maxRequests=18;
    Map<Integer, ServerInfo> servers = new ConcurrentHashMap<>();
    Map<String,Integer> serversReturned = new ConcurrentHashMap<>();
    Map<String,Integer> serversWithQueueSize = new ConcurrentHashMap<>();

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

    ///find server that exists in the zone requested by the client
    private int checkServerInZone(int zone){
        int N=zone;
        while(!servers.containsKey(N)){
            N++;
            if(N>ServerSimulator.SERVER_COUNT){
                N=1;
            }
        }
        return N;
    }

    ///update server queue size with remote call
    /// called every time server is returned 18 times to client
    private void updateServerQueueSize (ServerInfo info){
        new Thread(()->{
            try{
                Registry registry = LocateRegistry.getRegistry(info.host,info.port);
                ServiceInterface server=(ServiceInterface) registry.lookup(info.serverName);
                int queueSize=server.getQueueSize();
                serversWithQueueSize.put(info.serverName,queueSize);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    private boolean isServerCloserClockwise(int clientZone, int serverZone1, int serverZone2){
        int client1Steps=(serverZone1-clientZone + ServerSimulator.SERVER_COUNT)
                % ServerSimulator.SERVER_COUNT;
        int client2Steps=(serverZone2-clientZone + ServerSimulator.SERVER_COUNT)
                % ServerSimulator.SERVER_COUNT;
        return client1Steps<client2Steps;
    }

    private synchronized ServerInfo getServerWithLeastQueueSize(int zone){
        int minQueueSize=5000;//just number bigger than all number of requests
        ServerInfo leastOverloaded=null;
        for( ServerInfo info: servers.values()){
            int queueSize=serversWithQueueSize.getOrDefault(info.serverName, 0);
            if(queueSize<minQueueSize){
                minQueueSize=queueSize;
                leastOverloaded=info;
            }
            //a draw: equal queue size, choose clockwise:
            else if (queueSize==minQueueSize){
                //if other server (info) is closer clockwise, will return true
                if(leastOverloaded != null
                        && isServerCloserClockwise(zone,info.zone,leastOverloaded.zone)){
                    //reassign to server which is closest clockwise
                    leastOverloaded=info;
                }
            }
        }
        return leastOverloaded;
    }

    @Override
    public ServerInfo GetServer(int zoneN) throws RemoteException {
        if (servers.isEmpty()) {
            throw new RemoteException("No worker servers have registered with the proxy");
        }
        int existingZone=checkServerInZone(zoneN);
        //found server in same zone as client's request, can check queue overload
        ServerInfo info=servers.get(existingZone);
        if (info == null) {
            throw new RemoteException("No worker server is registered for zone " + zoneN);
        }
        //check if server is not overloaded (waiting queue size <18)
        if(serversWithQueueSize.getOrDefault(info.serverName, 0) < maxRequests){
            //update that server has been returned to client once more
            Integer lastCount=serversReturned.getOrDefault(info.serverName, 0);
            serversReturned.put(info.serverName,lastCount+1);
            //update server queue size, if server was returned 18 times
            if(serversReturned.get(info.serverName)>=18){
                //reset number of returned times
                serversReturned.put(info.serverName,0);
                updateServerQueueSize(info);
            }
            return servers.get(existingZone);
        }
        //server is overloaded, need to find least overloaded server to return
        return getServerWithLeastQueueSize(zoneN);
        ///redo logic below:
        //        ServerInfo info=new ServerInfo(1099,"localhost","server-1",5);
        //        return info;

    }

    @Override
    public synchronized void registerServer(String name, int port, String host, int zone)
            throws RemoteException {
        ServerInfo info= new ServerInfo(port,host,name,zone);
        servers.put(zone,info);
        serversReturned.put(name,0);
        serversWithQueueSize.put(name,0);
        System.out.println(("in proxy registered: "+ name + " zone:"+ zone + "port:" + port + "host:"+host));
    }
}
