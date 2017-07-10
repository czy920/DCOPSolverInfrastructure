package edu.cqu.core;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by dyc on 2017/6/17.
 */
public abstract class SyncAgent extends Agent {

    private Queue<Message> messageQueue;
    protected SyncMailer mailer;

    public SyncAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains,SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains);
        messageQueue = new LinkedList<>();
        this.mailer = mailer;
        mailer.register(this);
    }

    @Override
    protected void postInit() {
        super.postInit();
        mailer.agentDone(this.id);
    }

    public void addMessage(Message message){
        messageQueue.add(message);
    }

    @Override
    public void sendMessage(Message message) {
        mailer.addMessage(message);
    }

    public void allMessageDisposed(){

    }

    @Override
    public void execution() {
        if (mailer.getPhase() == SyncMailer.PHASE_AGENT && !mailer.isDone(this.id)){
            while (!messageQueue.isEmpty()){
                disposeMessage(messageQueue.poll());
            }
            allMessageDisposed();
            mailer.agentDone(this.id);
        }
    }

}
