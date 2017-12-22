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
        if (children.size() == 0){
            h = 0;
            sendMessage(new Message(id,parent,MSG_HEIGHT,h));
        }
        else if (heightReceived == children.size() && !isSent){
            isSent = true;
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
                int childHeight = (int) message.getValue();
                if (childHeight > h){
                    h = childHeight;
                }
                if (++heightReceived == children.size() && !isSent){
                    isSent = true;
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
                best = value[receivedStep % (level * 2 + h)];
                bestStep = receivedStep;
                break;
            case MSG_COST:
                sumOfCost += (int) message.getValue();
                break;
        }
    }
    private int testStep;

    @Override
    public void allMessageDisposed() {
        super.allMessageDisposed();

        if (counter != Integer.MAX_VALUE){
            if (--counter < 0){
                counter = Integer.MAX_VALUE;
                h++;
                localCost = new int[h];
                value = new int[level + h + level];
                for (int neighbourId : neighbours){
                    localView.put(neighbourId,new int[2 * (level + h)]);
                }
                alsStarted = true;
                if (id == 1){
                    bestCost = Integer.MAX_VALUE;
                }
                bestStep = 0;
                step = 0;
                alsReady();
            }
        }
        if (alsStarted){
            if (step < (m + h + level)){
                decision();
                localCost[step % h] = getLocalCost();
                cost = calculateStepCost();
                cost += sumOfCost;
                if (id != 1) {
                    sendMessage(new Message(id, parent, MSG_COST, cost));
                }
                for (int child : children){
                    sendMessage(new Message(id,child,MSG_BEST_STEP,bestStep));
                }
                if (id == 1){
                    if (cost < bestCost && step >= h){
                        bestCost = cost;
                        best = value[step % (level * 2 + h)];
                        bestStep = step;
                    }

                }
                sumOfCost = 0;
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
        testStep++;
    }

    protected abstract void alsReady();
    protected abstract void decision();

    protected void assignAlsValue(int value){
        this.value[step % (level * 2 + h)] = value;
    }

    protected void assignAlsNextValue(int value){
        this.value[(step + 1) % (level * 2 + h)] = value;
    }



    private int calculateStepCost(){
        int step;
        if (this.step >= h - 1)
            step = (this.step - h + 1) % h;
        else
            return 0;
        return localCost[step];
    }


    public double getBestCost(){
        return bestCost / 2;
    }


}
