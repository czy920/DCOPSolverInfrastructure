package edu.cqu.algorithms.adcop.incomplete;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;
import edu.cqu.result.ResultCycle;

import java.util.HashMap;
import java.util.Map;

public class GCA_MGM extends SyncAgent {
    private final static int MSG_VALUE = 1;
    private final static int MSG_LR = 2;
    private final static int MSG_COST = 3;

    private static final int PHASE_VALUE = 0;
    private static final int PHASE_LR = 1;
    private static final int PHASE_COST = 2;
    private final static int MGM_ROUND = 200;
    private int cycle;
    private int lr;
    private boolean canChangeValue;
    private int bestValue;
    private int phase;
    private Map<Integer, Integer> neighoursLR;

    public GCA_MGM(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        neighoursLR = new HashMap<>();
    }

    @Override
    protected void initRun() {
        int value = (int)(Math.random() * domain.length);
        assignValueIndex(value);
        broadcastValueMsg(value);
        phase = PHASE_VALUE;
        canChangeValue = true;
    }

    public void broadcastValueMsg(int value) {
        for (int i : neighbours) {
            sendMessage(new Message(id, i, MSG_VALUE, value));
        }
    }
    public void broadcastLrMsg() {
        for (int i : neighbours) {
            sendMessage(new Message(id, i, MSG_LR, lr));
        }
    }
    public void bestResponse() {
        int minCost = Integer.MAX_VALUE;
        int localCost = 0;
        for (int i = 0; i < domain.length; ++i) {

            int tmpCost = 0;
            for (int nId : neighbours) {
                tmpCost += constraintCosts.get(nId)[i][getNeighbourValue(nId)];
            }
            if (i == valueIndex) {
                localCost = tmpCost;
            }
            if (tmpCost < minCost) {
                minCost = tmpCost;
                bestValue = i;
            }
        }
        lr = localCost - minCost;
    }



    @Override
    public void runFinished() {
        ResultCycle cycle = new ResultCycle();
        cycle.setTotalCost(getLocalCost() * 1.0 /2);
        cycle.setAgentValues(id,0);
        mailer.setResultCycle(id,cycle);
    }

    @Override
    public int getLocalCost() {
        return 2 * super.getLocalCost();
    }

    @Override
    public void disposeMessage(Message message) {
        switch (message.getType()) {
            case MSG_VALUE : {
                int sender = message.getIdSender();
                int value = (int) message.getValue();
                int delta = constraintCosts.get(sender)[valueIndex][value] - constraintCosts.get(sender)[valueIndex][getNeighbourValue(sender)];
                updateLocalView(sender, value);
                //if (!neighoursLR.containsKey(sender) || (neighoursLR.containsKey(sender) && delta > neighoursLR.get(sender))) {
                if (delta > 0){
                    //line9
                    int cost = constraintCosts.get(sender)[valueIndex][value];
                    constraintCosts.get(sender)[valueIndex][value] = 0;
                    sendMessage(new Message(id, sender, MSG_COST, new Cost(valueIndex, value, cost)));
                }
                break;
            }
            case MSG_COST : {
                int sender = message.getIdSender();
                Cost cost = (Cost) message.getValue();
                constraintCosts.get(sender)[cost.valueReceiver][cost.valueSender] += cost.cost;
                break;
            }
            case MSG_LR : {
                neighoursLR.put(message.getIdSender(), (int) message.getValue());
                if (lr < (int) message.getValue())
                    canChangeValue = false;
                break;
            }
        }
    }

    @Override
    public void allMessageDisposed() {
        super.allMessageDisposed();
        if (++cycle < MGM_ROUND) {
            if (phase == PHASE_VALUE) {
                phase = PHASE_COST;
            }
            else if (phase == PHASE_COST) {
                bestResponse();
                broadcastLrMsg();
                phase = PHASE_LR;
            }
            else if (phase == PHASE_LR) {
                if (lr > 0 && canChangeValue && valueIndex != bestValue) {
                    assignValueIndex(bestValue);
                    broadcastValueMsg(bestValue);
                }
                canChangeValue = true;
                phase = PHASE_VALUE;
            }
        }
        else {
            stopProcess();
        }
    }

    private class Cost{
        int valueSender;
        int valueReceiver;
        int cost;

        public Cost(int valueSender, int valueReceiver, int cost) {
            this.valueSender = valueSender;
            this.valueReceiver = valueReceiver;
            this.cost = cost;
        }
    }
}
