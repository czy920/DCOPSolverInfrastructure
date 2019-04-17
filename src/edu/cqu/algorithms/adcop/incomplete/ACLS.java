package edu.cqu.algorithms.adcop.incomplete;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;
import edu.cqu.result.ResultCycle;

import java.util.*;

public class ACLS extends SyncAgent {

    private final static int MSG_VALUE = 1;
    private final static int MSG_PV = 2;
    private final static int MSG_COST_PV = 3;

    private static final int PHASE_VALUE = 0;
    private static final int PHASE_PV = 1;
    private static final int PHASE_COST_PV = 2;


    private final static double P = 0.5;
    private final static double C = 0.8;
    private final static int ACLS_ROUND = 200;
    private int phase;
    private int cycle = 0;
    private int currentState = Integer.MAX_VALUE;
    private int pvCost = 0;
    private int pv = 0;

    public ACLS(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
    }
    @Override
    protected void initRun() {
        int value = (int)(Math.random() * domain.length);
        assignValueIndex(value);
        broadcastValueMsg(value);
        phase = PHASE_VALUE;
    }
    @Override
    public int getLocalCost() {
        return super.getLocalCost() * 2;
    }
    public void broadcastValueMsg(int value) {
        for (int i : neighbours) {
            sendMessage(new Message(id, i, MSG_VALUE, value));
        }
    }
    public void broadcastPVMsg() {
        for (int i : neighbours) {
            sendMessage(new Message(id, i, MSG_PV, pv));
        }
    }
    @Override
    public void runFinished() {
        ResultCycle cycle = new ResultCycle();
        cycle.setTotalCost(getLocalCost() * 1.0 /2);
        cycle.setAgentValues(id,0);
        mailer.setResultCycle(id,cycle);
    }
    @Override
    public void disposeMessage(Message message) {
        switch (message.getType()) {
            case MSG_VALUE : {
                updateLocalView(message.getIdSender(), (int)message.getValue());
                break;
            }
            case MSG_PV : {
                int pv = (int) message.getValue();
                int sender = message.getIdSender();
                sendMessage(new Message(id, message.getIdSender(), MSG_COST_PV, constraintCosts.get(sender)[valueIndex][pv]));
                break;
            }
            case MSG_COST_PV : {
                pvCost += (int) message.getValue();
                break;
            }
        }
    }
    @Override
    public void allMessageDisposed() {
        super.allMessageDisposed();
        if (++cycle < ACLS_ROUND) {
            if (phase == PHASE_VALUE) {
                createPV(); //get pv and pc cost(one side)
                broadcastPVMsg();
                phase = PHASE_PV;
            }
            else if (phase == PHASE_PV) {
                phase = PHASE_COST_PV;
            }
            else if (phase == PHASE_COST_PV) {
                if (pvCost * C < currentState) {
                    if (Math.random() < P && pv != valueIndex) {
                        assignValueIndex(pv);
                        currentState = pvCost;  //since value has changed to pv, so current state is pv cost (two side)
                        broadcastValueMsg(pv);
                    }
                }
                phase = PHASE_VALUE;
            }
        }
        else {
            stopProcess();
        }
    }
    void createPV() {
        //improvement set
        Map<Integer, Integer> impSet = new HashMap<>();
        int localCost = 0;
        for (int nId : neighbours) {
            localCost += constraintCosts.get(nId)[valueIndex][getNeighbourValue(nId)];
        }
        for (int i = 0; i < domain.length; ++i) {
            if (i == valueIndex)
                continue;
            int tmpCost = 0;
            for (int nId : neighbours) {
                tmpCost += constraintCosts.get(nId)[i][getNeighbourValue(nId)];
            }
            if (tmpCost < localCost)
                impSet.put(i, localCost - tmpCost);
        }
        // pv and pvCost
        if (impSet.size() > 0) {
            int[] wheelP = new int[impSet.size()];
            int[] wheel = new int[impSet.size()];
            int tmpSum = 0;
            int j = 0;
            for (int i : impSet.keySet()) {
                tmpSum += impSet.get(i);
                wheel[j] = i;
                wheelP[j++] = tmpSum;
            }
            double rand = Math.random();
            for (j = 0; j < domain.length; ++j) {
                if (rand - (double) wheelP[j] / tmpSum < 0) {
                    pv = wheel[j];
                    pvCost = localCost-impSet.get(pv);//gain subtract local cost
                    break;
                }
            }
        }
    }
}
