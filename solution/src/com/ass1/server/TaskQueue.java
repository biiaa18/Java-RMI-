package com.ass1.server;

import java.util.concurrent.LinkedBlockingQueue;

public class TaskQueue {
    private final LinkedBlockingQueue<ServerJob> jobQueue = new LinkedBlockingQueue<>();

    public void add(ServerJob serverJob) {
        try {
            jobQueue.put(serverJob);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void executeNext() {
        try {
            jobQueue.take().execute();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getQueueSize() {
        return jobQueue.size();
    }
}
