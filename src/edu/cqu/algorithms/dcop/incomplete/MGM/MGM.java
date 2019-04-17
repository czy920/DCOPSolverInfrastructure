package edu.cqu.algorithms.dcop.incomplete.MGM;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;
import edu.cqu.result.ResultCycle;

import java.util.Map;

public class MGM extends SyncAgent {
    private final static int MSG_VALUE = 1;
    private final static int MSG_GAIN = 2;

    private static final int PHASE_VALUE = 0;
    private static final int PHASE_GAIN = 1;

    private final static double P = 0.5;
    private final static int MGM_ROUND = 1000;
    private int cycle;
    private int gain;
    private boolean canChangeValue;
    private int bestValue;
    private int phase;

    public MGM(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
    }

    @Override
    protected void initRun() {
        valueIndex = (int)(Math.random() * domain.length);
        assignValueIndex(valueIndex);
        broadcastValueMsg();
        phase = PHASE_VALUE;
        canChangeValue = true;
    }

    public void broadcastValueMsg() {
        for (int i : neighbours) {
            sendMessage(new Message(id, i, MSG_VALUE, valueIndex));
        }
    }

    public void broadcastGainMsg() {
        for (int i : neighbours) {
            sendMessage(new Message(id, i, MSG_GAIN, gain));
        }
    }

    @Override
    public void runFinished() {
//        super.runFinished();
        ResultCycle cycle = new ResultCycle();
        cycle.setTotalCost(getLocalCost() * 1.0 /2);
        cycle.setAgentValues(id,0);
        mailer.setResultCycle(id,cycle);
    }

    void selectBestValue() {
        int minCost = Integer.MAX_VALUE;
        int minValue = -1;
        int localCost = 0;
        for (int i = 0; i < domain.length; ++i) {
            int tmpCost = 0;
            for (int nId : neighbours) {
                tmpCost += constraintCosts.get(nId)[i][getNeighbourValue(nId)];
            }
            if (i == valueIndex) {
                localCost = tmpCost;
            }
            if (minCost > tmpCost) {
                minCost = tmpCost;
                minValue = i;
            }
        }
        gain = localCost - minCost;
        bestValue =  minValue;
    }

    @Override
    public void allMessageDisposed() {
        super.allMessageDisposed();
        if (++cycle > MGM_ROUND) {
            stopProcess();
        }
        else {
            if (phase == PHASE_VALUE) {
                selectBestValue();
                broadcastGainMsg();
                phase = PHASE_GAIN;
            }
            else if (phase == PHASE_GAIN) {
                if (canChangeValue) {
                    if (valueIndex != bestValue) {
                        assignValueIndex(bestValue);
                        broadcastValueMsg();
                    }
                }
                canChangeValue = true;
                phase = PHASE_VALUE;
            }

        }
    }

    @Override
    public void disposeMessage(Message message) {
        switch (message.getType()) {
            case MSG_VALUE : {
                int sendId = message.getIdSender();
                updateLocalView(sendId, (int)message.getValue());
                break;
            }
            case MSG_GAIN : {
                if ( (int)message.getValue() > gain)
                    canChangeValue = false;
                }
            }
        }
    }

