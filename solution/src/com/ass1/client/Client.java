package com.ass1.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.io.FileWriter;
import java.util.stream.Collectors;


import com.ass1.server.ProxyInterface;
import com.ass1.server.ServiceInterface;
import com.ass1.client.Request;
import com.ass1.client.RequestStatistics;
import com.ass1.ServerInfo;
import com.ass1.client.Result;

public class Client {
    private static ProxyInterface proxyServer;
    private Map<String, RequestStatistics> statisticsMap= new HashMap<>();
    private static List<Request> requests=new ArrayList<>();

    private static String writeOriginalInputQuery(List<Object> args){
        return args.stream().map(Object::toString).collect(Collectors.joining(" "));
    }
     public static void main() throws Exception{
        //todo: should this be a thread as well?
        try {
            //first rmi lookup to find proxy
            Registry registry = LocateRegistry.getRegistry(1099);
            proxyServer = (ProxyInterface) registry.lookup("proxyClientAPI");
            //            System.out.println(server.Add(10,20));
            //System.out.println(proxyServer.GetServer());

        } catch (RemoteException | NotBoundException e) {
            throw new IllegalStateException(
                    "Could not find proxyClientAPI in the RMI registry at localhost:1099", e);
        }

         ///read input file with requests from client and split data
         File file = new File("exercise_1_input.txt");

         try (Scanner scanner1 = new Scanner(file);){
             while (scanner1.hasNextLine()){
                 String line= scanner1.nextLine().trim();
                 if(!line.isEmpty()){

                     requests.add(Request.readLine(line));
                     //System.out.println("file:"+requests.size());
                 }
             }
         } catch (FileNotFoundException e) {
             throw new RuntimeException(e);
         }


         //call request sending/ result writing 2 times with different delay T
         BufferedWriter writeOutputFile= new BufferedWriter(new FileWriter("naive_server.txt"));
        Client client= new Client();
        client.runClientWithDelayT(50,writeOutputFile);
         client.runClientWithDelayT(20,writeOutputFile);
         writeOutputFile.close();
        System.out.println(("done"));
    }

    public void runClientWithDelayT(Integer T, BufferedWriter writeOutputFile) throws Exception{
        writeOutputFile.write("----------T="+T+" ----------\n");
        ///for each request, invoke method remotely concurrently, simulate delay
        statisticsMap.clear();
        ExecutorService invokeRequestsConcurrently= Executors.newCachedThreadPool();
        for(Request request: requests) {
            //initialize new method entry
            statisticsMap.putIfAbsent(request.getMethodName(), new RequestStatistics());
            invokeRequestsConcurrently.submit(() -> {
                try {
                    ServerInfo correctServerInfo = proxyServer.GetServer(request.getZoneNumber());
                    //second rmi lookup for DB server and get ROR (remote object reference)
                    Registry serverRegistry = LocateRegistry.getRegistry(correctServerInfo.host, correctServerInfo.port);
                    ServiceInterface correctServer = (ServiceInterface) serverRegistry.lookup(correctServerInfo.serverName);

                    List<Object> methodArgs = new ArrayList<>();
                    methodArgs.addAll(request.getArgList());
                    //add T for measuring client/server zone delay in server
                    methodArgs.add(T);
                    //create new types array with +1 extra spot for the T
                    Class<?>[] argTypesWithT = new Class<?>[request.getArgTypesList().length+1];
                    //copy old array adn add T
                    System.arraycopy(request.getArgTypesList(),0,argTypesWithT,0,request.getArgTypesList().length);
                    argTypesWithT[request.getArgTypesList().length]=Integer.class;


                    System.out.println("Client: arguments before matching: " + methodArgs + "  types: " + Arrays.toString(request.getArgTypesList())); // argTypesWithT
                    //find matching method in server
                    Method serverMethod = ServiceInterface.class.getMethod(request.getMethodName(), request.getArgTypesList()); // argTypesWithT

                    long turnaroundTimeStart = System.currentTimeMillis();
                    //remote invoke the method
                    Result result = serverMethod.invoke(correctServer, methodArgs.toArray());
                    long turnaroundTimeEnd = System.currentTimeMillis();
                    long turnaroundTime = turnaroundTimeEnd - turnaroundTimeStart;
                    long executionTime =result.exeuctionTime;
                    long waitingTime =result.waitingTime;
                    statisticsMap.get(request.getMethodName()).sumTimeStatistics(turnaroundTime, executionTime, waitingTime);
                    //writing to output file
                    // <result> <input query> (turnaround time: YY ms, execution time:
                    //ZZ ms, waiting time: TT ms, processed by Server <server#>)
                    synchronized (writeOutputFile) {
                        writeOutputFile.write(
                                result + " " +
                                        request.getMethodName() + " " +
                                        writeOriginalInputQuery(request.getArgList()) + " Zone:" +
                                        request.getZoneNumber() + " (turnaround time: " +
                                        turnaroundTime + " ms, execution time: " +
                                        executionTime + " ms, waiting time: " +
                                        waitingTime + " ms, processed by Server " +
                                        correctServerInfo.serverName + ")\n"
                        );
                        writeOutputFile.flush();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            try {
                Thread.sleep(T);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        invokeRequestsConcurrently.shutdown();
        //invokeRequestsConcurrently.awaitTermination(10, TimeUnit.SECONDS);
        //6 entries of statistics per method
        for(Map.Entry<String,RequestStatistics> methodEntry: statisticsMap.entrySet()){
            writeOutputFile.write(methodEntry.getValue().getStatistics(methodEntry.getKey())+"\n");
        }
        writeOutputFile.write("\n\n\n");
    }
}
