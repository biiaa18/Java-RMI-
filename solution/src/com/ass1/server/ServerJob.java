package com.ass1.server;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public class ServerJob {
    protected Callable<Integer> task;
    protected Semaphore taskCompletionSemaphore = new Semaphore(0);
    protected Integer result;
    protected Exception failure;

    public ServerJob(Callable<Integer> task) {
        this.task = task;
    }

    public void execute() {
        try {
            this.result = this.task.call();
        } catch (Exception e) {
            this.failure = e;
        } finally {
            this.taskCompletionSemaphore.release();
        }
    }
}
