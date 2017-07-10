package edu.cqu.core;

import edu.cqu.result.ResultCycle;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by dyc on 2017/6/16.
 */
public class SyncMailer extends Process{

    public static final int PHASE_AGENT = 1;
    public static final int PHASE_MAILER = 2;

    private Map<Integer,SyncAgent> agents;
    private Queue<Message> messageQueue;
    private int cycleCount;
    private int messageCount;
    private Set<Integer> agentReady;
    private AtomicInteger phase;
    private double[] costInCycle;
    private int tail;
    private ResultCycle resultCycle;
    private long startTime;
    private FinishedListener listener;

    public SyncMailer(){
        super("mailer");
        agents = new HashMap<>();
        messageQueue = new LinkedList<>();
        agentReady = new HashSet<>();
        phase = new AtomicInteger(PHASE_AGENT);
        costInCycle = new double[2];
    }

    public SyncMailer(FinishedListener finishedListener){
        this();
        listener = finishedListener;
    }

    private void expand(){
        double[] tmpCostInCycle = new double[costInCycle.length * 2];
        for (int i = 0 ; i < costInCycle.length; i++){
            tmpCostInCycle[i] = costInCycle[i];
        }
        costInCycle = tmpCostInCycle;
    }

    public void register(SyncAgent agent){
        agents.put(agent.id,agent);
    }

    public void addMessage(Message message) {
        synchronized (phase){
            while (phase.get() == PHASE_MAILER){
            }
            synchronized (messageQueue){
                messageQueue.add(message);
            }
        }
    }

    @Override
    public void preExecution() {
        startTime = new Date().getTime();
    }

    @Override
    public void execution() {
        if (phase.get() == PHASE_MAILER){
            synchronized (messageQueue){
                while (!messageQueue.isEmpty()){
                    Message message = messageQueue.poll();
                    messageCount++;
                    if (agents.get(message.getIdReceiver()).isRunning()){
                        agents.get(message.getIdReceiver()).addMessage(message);
                    }
                    else {

                    }
                }
                boolean canTerminate = true;
                double cost = 0;
                for (SyncAgent syncAgent : agents.values()){
                    if (syncAgent.isRunning()){
                        canTerminate = false;
                    }
                    cost += syncAgent.getLocalCost();
                }
                cost /= 2;
                if (tail == costInCycle.length - 1){
                    expand();
                }
                costInCycle[tail++] = cost;
                if (canTerminate){
                    stopProcess();
                }
                else {
                    cycleCount++;
                    agentReady.clear();
                    phase.set(PHASE_AGENT);
                }
            }
        }
    }

    public int getMessageCount() {
        return messageCount;
    }



    @Override
    public void postExecution() {

    }

    public synchronized void agentDone(int id){
        synchronized (agentReady){
            agentReady.add(id);
            if (agentReady.size() == agents.size()){
                phase.set(PHASE_MAILER);
            }
        }
    }

    public synchronized boolean isDone(int id){
        return agentReady.contains(id);
    }

    public synchronized int getPhase() {
        return phase.get();
    }

    public ResultCycle getResultCycle() {
        return resultCycle;
    }

    public synchronized void setResultCycle(int id,ResultCycle resultCycle){
        if (this.resultCycle == null){
            this.resultCycle = resultCycle;
        }
        else {
            this.resultCycle.add(resultCycle);
            this.resultCycle.setAgentValues(id,resultCycle.getAgentValue(id));
        }
        if (this.resultCycle.getAgents().size() == agents.size()){
            this.resultCycle.setTotalTime(new Date().getTime() - startTime);
            this.resultCycle.setMessageQuantity(messageCount);
            this.resultCycle.setCostInCycle(costInCycle,tail);
            if (listener != null){
                listener.onFinished(this.resultCycle);
            }
        }
    }
}
