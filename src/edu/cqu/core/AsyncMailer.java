package edu.cqu.core;

import edu.cqu.result.Result;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by dyc on 2017/6/16.
 */
public class AsyncMailer extends Process {
    private Map<Integer,AsyncAgent> agents;
    private Result result;
    private Queue<Message> messageQueue;
    private int messageCount;
    private long startTime;
    private FinishedListener listener;

    public AsyncMailer() {
        super("mailer");
        messageQueue = new LinkedList<>();
        agents = new HashMap<>();
    }

    public AsyncMailer(FinishedListener finishedListener){
        this();
        listener = finishedListener;
    }

    public void registerAgent(AsyncAgent agent){
        agents.put(agent.id,agent);
    }

    public void addMessage(Message message){
        synchronized (messageQueue){
            messageQueue.add(message);
        }
    }

    @Override
    public void preExecution() {
        startTime = new Date().getTime();
    }

    @Override
    public void execution() {
        synchronized (messageQueue){
            while (!messageQueue.isEmpty()){
                Message message = messageQueue.poll();
                if (agents.get(message.getIdReceiver()).isRunning()) {
                    messageCount++;
                    agents.get(message.getIdReceiver()).addMessage(message);
                }
            }
            boolean canTerminate = true;
            for (AsyncAgent asyncAgent : agents.values()){
                if (asyncAgent.isRunning()){
                    canTerminate = false;
                    break;
                }
            }
            if (canTerminate){
                result.setMessageQuantity(messageCount);
                result.setTotalTime(new Date().getTime() - startTime);
                if (listener != null){
                    listener.onFinished(result);
                }
                stopProcess();
            }

        }
    }

    @Override
    public void postExecution() {

    }

    private synchronized void setResult(int id,Result result){
        if (this.result == null) {
            this.result = result;
        } else {
            this.result.add(result);
            this.result.setAgentValues(id, result.getAgentValue(id));
        }

    }

    public Result getResult() {
        return result;
    }

    public int getMessageCount() {
        return messageCount;
    }
}
