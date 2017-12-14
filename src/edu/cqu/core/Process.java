package edu.cqu.core;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by dyc on 2017/6/16.
 */
public abstract class Process {

    private String threadName;
    private Thread thread;
    private AtomicBoolean isRunning;
    private boolean suppressOutput;

    public Process(String threadName) {
        this.threadName = threadName;
        isRunning = new AtomicBoolean(false);
        suppressOutput = false;
    }

    public void startProcess(){
        synchronized (isRunning) {
            if (isRunning.get()) {
                return;
            }
            isRunning.set(true);
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                preExecution();
                while (true){
                    synchronized (isRunning){
                        if (!isRunning.get()){
                            break;
                        }
                    }
                    execution();
                }
                postExecution();
                if (!suppressOutput) {
                    System.out.println(threadName + " is stopped");
                }
            }
        },threadName);
        thread.start();
    }

    public void stopProcess(){
        synchronized (isRunning){
            if (isRunning.get()){
                isRunning.set(false);
            }
        }
    }

    public abstract void preExecution();

    public abstract void execution();

    public abstract void postExecution();

    public boolean isRunning(){
        return isRunning.get();
    }

    public void setSuppressOutput(boolean suppressOutput) {
        this.suppressOutput = suppressOutput;
    }
}
