package edu.cqu.algorithms.dcop.incomplete.DSA;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;
import edu.cqu.result.ResultCycle;

import java.util.Map;

public class DSA extends SyncAgent {
    private final static int MSG_VALUE = 1;
    private final static double P = 1.0;
    private final static int DSA_ROUND = 1000;
    public int cycle = 0;

    public DSA(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);

    }

    @Override
    protected void initRun() {
        valueIndex = (int)(Math.random() * domain.length);
        assignValueIndex(valueIndex);
        broadcastValueMsg();
    }

    public void broadcastValueMsg() {
        for (int i : neighbours) {
            sendMessage(new Message(id, i, MSG_VALUE, valueIndex));
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

    int selectBestValue() {
        int minCost = Integer.MAX_VALUE;
        int minValue = -1;
        for (int i = 0; i <= domain.length; ++i) {
            int localCost = 0;
            for (int nId : neighbours) {
                localCost += constraintCosts.get(nId)[valueIndex][getNeighbourValue(nId)];
            }
            if (minCost < localCost) {
                minCost = localCost;
                minValue = i;
            }
        }
        return minValue;
    }

    @Override
    public void allMessageDisposed() {
        super.allMessageDisposed();
        if (++cycle < DSA_ROUND) {
            if (Math.random() < P) {
                int bestValue = selectBestValue();
                if (valueIndex != bestValue) {
                    assignValueIndex(bestValue);
                    broadcastValueMsg();
                }
            }
        }
        else {
            stopProcess();
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
        }
    }
}
