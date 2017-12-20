package edu.cqu.framework;

import edu.cqu.core.Message;
import edu.cqu.core.SyncMailer;
import edu.cqu.ordering.BFSSyncAgent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ALSAgent extends BFSSyncAgent {

    private static final int MSG_HEIGHT = 0xFFAFA;
    private static final int MSG_INFORM_HEIGHT = 0xFFAFB;
    private static final int MSG_COST = 0xFFAFC;
    private static final int MSG_BEST_STEP = 0xFFAFD;

    private int h;
    private int bestCost;
    private int bestStep;
    private int best;
    private int step;
    private int[] localCost;
    private int[] value;
    private int cost;
    private Map<Integer,int[]> localView;

    private int heightReceived;
    private int counter;

    private int m;
    private boolean alsStarted;

    private int iteration;

    private boolean isSent;

    private int sumOfCost;

    public ALSAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer,int m) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        counter = Integer.MAX_VALUE;
        localView = new HashMap<>();
        this.m = m;
    }

    @Override
    protected void pseudoTreeCreated() {
//        System.out.println(level);
        if (children.size() == 0){
            h = 0;
            sendMessage(new Message(id,parent,MSG_HEIGHT,h));
        }
        else if (heightReceived == children.size() && !isSent){
            isSent = true;
            System.out.println(id + " is done");
            if (id != 1) {
                sendMessage(new Message(id, parent, MSG_HEIGHT, ++h));
            }
            else {
                h++;
                counter = h;
                for (int child : children){
                    sendMessage(new Message(id,child,MSG_INFORM_HEIGHT,h));
                }
            }
        }
    }

    @Override
    public void disposeMessage(Message message) {
        super.disposeMessage(message);
        switch (message.getType()){
            case MSG_HEIGHT:
//                System.out.println(message);
                int childHeight = (int) message.getValue();
                if (childHeight > h){
                    h = childHeight;
                }
                if (++heightReceived == children.size() && !isSent){
                    isSent = true;
                    System.out.println(id + " is done");
                    if (id != 1) {
                        sendMessage(new Message(id, parent, MSG_HEIGHT, ++h));
                    }
                    else {
                        h++;
                        counter = h;
                        for (int child : children){
                            sendMessage(new Message(id,child,MSG_INFORM_HEIGHT,h));
                        }
                    }
                }
                break;
            case MSG_INFORM_HEIGHT:
                counter = h = (int) message.getValue() - 1;
                for (int child : children){
                    sendMessage(new Message(id,child,MSG_INFORM_HEIGHT,h));
                }
                break;
            case MSG_BEST_STEP:
                int receivedStep = (int) message.getValue();
                best = value[roundIndex(receivedStep)];
                bestStep = receivedStep;
                break;
            case MSG_COST:
                sumOfCost += (int) message.getValue();
                break;
        }
    }

    @Override
    public void allMessageDisposed() {
        super.allMessageDisposed();
        if (counter != Integer.MAX_VALUE){
            if (--counter < 0){
                counter = Integer.MAX_VALUE;
                localCost = new int[2 * (level + h)];
                value = new int[2 * (level + h)];
                System.out.println(level + h);
                for (int neighbourId : neighbours){
                    localView.put(neighbourId,new int[2 * (level + h)]);
                }
                alsStarted = true;
                if (id == 1){
                    bestCost = Integer.MAX_VALUE;
                }
                bestStep = 0;
                alsReady();
            }
        }
        if (alsStarted){
            if (step < (m + h + level)){
                if (id != 1) {
                    sendMessage(new Message(id, parent, MSG_COST, cost));
                }
                sendAllMessages();
                for (int child : children){
                    sendMessage(new Message(id,child,MSG_BEST_STEP,bestStep));
                }
                localCost[roundIndex(step)] = getLocalCost();
                cost = calculateStepCost();
                cost += sumOfCost;

                if (id == 1){
                    if (cost < bestCost && step > (level + h + 1)){
                        bestCost = cost;
                        best = value[roundIndex(step)];
                        bestStep = step;
                    }

                }
                sumOfCost = 0;
                decision();
                step++;
            }
            else if (iteration++ < (level + h)) {
                for (int child : children){
                    sendMessage(new Message(id,child,MSG_BEST_STEP,bestStep));
                }
            }
            else {
                stopProcess();
            }
        }
    }

    protected abstract void alsReady();
    protected abstract void decision();
    protected abstract void sendAllMessages();

    protected void assignValue (int value){
        this.value[roundIndex(step)] = value;
    }

    protected void assignNextValue(int value){
        this.value[roundIndex(step + 1)] = value;
    }



    private int calculateStepCost(){
        int step;
        if (this.step >= h)
            step = roundIndex(this.step - h);
        else
            step = this.step;

        return localCost[step];
    }

    private int roundIndex(int step){
        return step % (2 * (level + h));
    }

    public double getBestCost(){
        return bestCost / 2;
    }


}
