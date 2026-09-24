package com.ass1.server;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public class ServerJob {
    protected Callable<Integer> task;
    protected Semaphore taskCompletionSemaphore = new Semaphore(0);
    protected Integer result;
    protected Exception failure;
    private final long submissionTime;
    protected long waitingTime;
    protected long executionTime;

    public ServerJob(Callable<Integer> task) {
        this.submissionTime = System.currentTimeMillis();
        this.task = task;
    }

    public void execute() {
        try {
            this.waitingTime = System.currentTimeMillis() - this.submissionTime;
            long executionStartTime = System.currentTimeMillis();
            this.result = this.task.call();
            this.executionTime = System.currentTimeMillis() - executionStartTime;
        } catch (Exception e) {
            this.failure = e;
        } finally {
            this.taskCompletionSemaphore.release();
        }
    }
}
