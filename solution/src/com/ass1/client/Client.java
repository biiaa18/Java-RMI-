package com.ass1.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

import com.ass1.server.ProxyInterface;
import com.ass1.server.ServerInterface;
import com.ass1.client.Request;

public class Client {

    public static void main() {
        List<Request> requests=new ArrayList<>();

        //todo: should this be a thread as well?
        try {
            Registry registry = LocateRegistry.getRegistry(1099);
            ServerInterface server = (ServerInterface) registry.lookup("server");
            ProxyInterface proxyServer = (ProxyInterface) registry.lookup("proxyServer");
            System.out.println(server.Add(10,20));
            System.out.println(proxyServer.GetServer());
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }

        //input file with requests from client
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


        //just test
        for(Request request: requests){
            System.out.println("'"+request.getMethodName()+"'  '"+request.getArgList()+"'  '"+request.getZoneNumber()+"'");
        }
    }
}
