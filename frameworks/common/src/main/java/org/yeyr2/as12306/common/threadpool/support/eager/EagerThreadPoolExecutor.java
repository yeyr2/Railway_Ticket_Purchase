package org.yeyr2.as12306.common.threadpool.support.eager;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// 快速消费线程池
public class EagerThreadPoolExecutor extends ThreadPoolExecutor {
    public EagerThreadPoolExecutor(int corePoolSize,
                                   int maximumPoolSize,
                                   long keepAliveTime,
                                   TimeUnit unit,
                                   BlockingQueue<Runnable> workQueue,
                                   ThreadFactory threadFactory,
                                   RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    private final AtomicInteger submittedTaskCount = new AtomicInteger(0);

    public int getSubmittedTaskCount(){
        return submittedTaskCount.get();
    }

    @Override
    public void afterExecute(Runnable r,Throwable t){
        submittedTaskCount.decrementAndGet();
    }

    @Override
    public void execute(Runnable command){
        submittedTaskCount.incrementAndGet();
        try{
            super.execute(command);
        }catch (RejectedExecutionException ex){
            TaskThreadQueue taskThreadQueue = (TaskThreadQueue) super.getQueue();
            try {
                if(!taskThreadQueue.retryOffer(command,0,TimeUnit.MILLISECONDS)){
                    submittedTaskCount.decrementAndGet();
                    throw new RejectedExecutionException("Queue capacity is full.",ex);
                }
            } catch (InterruptedException e) {
                submittedTaskCount.decrementAndGet();
                throw new RuntimeException(e);
            }
        }catch (Exception ex){
            submittedTaskCount.decrementAndGet();
            throw ex;
        }
    }
}
