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

import com.ass1.ServerInfo;
import com.ass1.client.RequestStatistics;
import com.ass1.server.ServiceInterface;

public class ProxyServer implements ProxyInterface, ProxyInterfaceForServerRegistration {
    private static ProxyServer INSTANCE;
    //private static final java.util.concurrent.CountDownLatch STOP_LATCH = new java.util.concurrent.CountDownLatch(1);
    private static Integer maxRequests=18;
    Map<Integer, ServerInfo> servers= new HashMap<>();
    Map<String,Integer> serversReturned=new HashMap<>();
    Map<String,Integer> serversWithQueueSize=new HashMap<>();

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
            if(N>servers.size()){
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
        int client1Steps=(serverZone1-clientZone + servers.size())%servers.size();
        int client2Steps=(serverZone2-clientZone + servers.size())%servers.size();
        return client1Steps<client2Steps;
    }

    private synchronized ServerInfo getServerWithLeastQueueSize(int zone){
        int minQueueSize=5000;//just number bigger than all number of requests
        ServerInfo leastOverloaded=null;
        for( ServerInfo info: servers.values()){
            int queueSize=serversWithQueueSize.get(info.serverName);
            if(queueSize<minQueueSize){
                minQueueSize=queueSize;
                leastOverloaded=info;
            }
            //a draw: equal queue size, choose clockwise:
            else if (queueSize==minQueueSize){
                //if other server (info) is closer clockwise, will return true
                if(isServerCloserClockwise(zone,info.zone,leastOverloaded.zone)){
                    //reassign to server which is closest clockwise
                    leastOverloaded=info;
                }
            }
        }
        return leastOverloaded;
    }

    @Override
    public ServerInfo GetServer(int zoneN) throws RemoteException {
        int existingZone=checkServerInZone(zoneN);
        //found server in same zone as client's request, can check queue overload
        ServerInfo info=servers.get(existingZone);
        //check if server is not overloaded (waiting queue size <18)
        if(serversWithQueueSize.get(info.serverName)<maxRequests){
            //update that server has been returned to client once more
            Integer lastCount=serversReturned.get(info.serverName);
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
    public void registerServer(String name, int port, String host, int zone) throws RemoteException{
        ServerInfo info= new ServerInfo(port,host,name,zone);
        servers.put(zone,info);
        serversReturned.put(name,0);
        serversWithQueueSize.put(name,0);
        System.out.println(("in proxy registered: "+ name + " zone:"+ zone + "port:" + port + "host:"+host));
    }
}
