package edu.cqu.core;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by dyc on 2017/6/16.
 */
public abstract class AsyncAgent extends Agent {
    private Queue<Message> messageQueue;
    protected AsyncMailer mailer;


    public AsyncAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains,AsyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains);
        messageQueue = new LinkedList<>();
        this.mailer = mailer;
        mailer.registerAgent(this);
    }

    @Override
    public void sendMessage(Message message) {
        mailer.addMessage(message);
    }

    public void addMessage(Message message) {
        synchronized (messageQueue){
            messageQueue.add(message);
        }
    }

    @Override
    public void execution() {
        Queue<Message> tmpQueue = new LinkedList<>();
        synchronized (messageQueue){
            while (!messageQueue.isEmpty()){
                tmpQueue.add(messageQueue.poll());
            }
        }
        while (!tmpQueue.isEmpty()){
            disposeMessage(tmpQueue.poll());
        }
    }
}
