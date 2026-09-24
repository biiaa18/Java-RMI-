package com.ass1.client;

public class Result {
    public Integer result;
    public long exeuctionTime;
    public long waitingTime;

    public Result(Integer result, long exeuctionTime, long waitingTime) {
        this.result = result;
        this.exeuctionTime = exeuctionTime;
        this.waitingTime = waitingTime;
    }
}
