package com.ass1.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.io.FileWriter;
import java.util.stream.Collectors;

import com.ass1.server.ProxyInterface;
import com.ass1.server.ServerInterface;
import com.ass1.client.Request;
import com.ass1.ServerInfo;

public class Client {
    private static ProxyInterface proxyServer;
    private static String writeOriginalInputQuery(List<Objects> args){
        return args.stream().map(Object::toString).collect(Collectors.joining(" "));
    }
     public static void main() throws Exception{
        BufferedWriter writeOutputFile= new BufferedWriter(new FileWriter("naive_server.txt"));
        List<Request> requests=new ArrayList<>();
        //todo: should this be a thread as well?
        try {
            //first rmi lookup to find proxy
            Registry registry = LocateRegistry.getRegistry(1099);
            proxyServer = (ProxyInterface) registry.lookup("proxyServer");
            //            System.out.println(server.Add(10,20));
            //System.out.println(proxyServer.GetServer());

        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
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

        ///for each request, invoke method remotely concurrently, simulate delay
        ExecutorService invokeRequestsConcurrently= Executors.newCachedThreadPool();
        long T=50; //TODO:: fix T switch between 50 or 20,or run twice with each value??
        for(Request request: requests){
            long requestSubmittedTime=System.currentTimeMillis();
            invokeRequestsConcurrently.submit(()->{
                try{
                    ServerInfo correctServerInfo=proxyServer.GetServer(request.getZoneNumber());
                    //second rmi lookup for DB server and get ROR (remote object reference)
                    Registry serverRegistry = LocateRegistry.getRegistry(correctServerInfo.host,correctServerInfo.port);
                    ServerInterface correctServer=(ServerInterface)serverRegistry.lookup(correctServerInfo.serverName);

                    Method serverMethod=correctServer.getClass().getMethod(request.getMethodName(), request.getArgTypesList());

                    long startOfInvocation=System.currentTimeMillis();
                    Object result=serverMethod.invoke(correctServer,request.getArgList().toArray());
                    long endOfInvocation=System.currentTimeMillis();

                    long executionTime=endOfInvocation-startOfInvocation;
                    long turnaroundTime=endOfInvocation-requestSubmittedTime;
                    long waitingTime=turnaroundTime-executionTime;
                    //writing to output file
                    // <result> <input query> (turnaround time: YY ms, execution time:
                    //ZZ ms, waiting time: TT ms, processed by Server <server#>)
                    synchronized(writeOutputFile){
                        writeOutputFile.write(
                                         result+" "+
                                        request.getMethodName() + " "+
                                                 writeOriginalInputQuery(request.getArgList()) + " Zone:"+
                                        request.getZoneNumber() + " (turnaround time: "+
                                        turnaroundTime +" ms, execution time: "+
                                        executionTime+" ms, waiting time: "+
                                        waitingTime+" ms, processed by Server "+
                                        correctServerInfo.serverName+")\n"
                        );
                        writeOutputFile.flush();
                    }

                }
                catch(Exception e){
                    e.printStackTrace();
                }
            });
            try{
            Thread.sleep(T);
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }

            //System.out.println("'"+request.getMethodName()+"'  '"+request.getArgList()+"'  '"+request.getZoneNumber()+"'");
        }
        invokeRequestsConcurrently.shutdown();
        //invokeRequestsConcurrently.awaitTermination(10, TimeUnit.SECONDS);
         writeOutputFile.close();
    }
}
