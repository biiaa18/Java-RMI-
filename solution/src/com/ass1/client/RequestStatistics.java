package com.ass1.client;

public class RequestStatistics {
    long methodAllTurnAroundTime=0; //TAT
    long methodAllExecutionTime=0; //ET
    long methodAllWaitingTime=0; //WT
    long methodMinTurnAroundTime=0;
    long methodMaxTurnAroundTime=0;
    int amountOfEntries=0; //counter

    public synchronized void sumTimeStatistics(long TAT, long ET, long WT){
        methodAllTurnAroundTime+=TAT;
        methodAllExecutionTime+=ET;
        methodAllWaitingTime+=WT;
        methodMinTurnAroundTime=Math.min(methodMinTurnAroundTime,methodAllTurnAroundTime);
        methodMaxTurnAroundTime=Math.max(methodMaxTurnAroundTime,methodAllTurnAroundTime);
        amountOfEntries++;
    }

    // TODO: Safe guard against division by zero
    public String getStatistics(String methodName){
        if (amountOfEntries == 0) {
            return methodName + " No entries recorded.";
        }
        long avgTAT=methodAllTurnAroundTime/amountOfEntries;
        long avgET=methodAllExecutionTime/amountOfEntries;
        long avgWT=methodAllWaitingTime/amountOfEntries;
        return  methodName + " avg turn-around time: "+
                avgTAT +" ms, avg execution time: "+
                avgET + " ms, avg waiting time: "+
                avgWT + " ms, min turn-around time: "+
                methodMinTurnAroundTime + " ms, max turn-around time: "+
                methodMaxTurnAroundTime + " ms"
                ;
    }
}
