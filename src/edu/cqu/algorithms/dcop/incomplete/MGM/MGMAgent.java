package edu.cqu.algorithms.dcop.incomplete.MGM;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;
import edu.cqu.result.ResultCycle;

import java.util.Map;

public class MGMAgent extends SyncAgent{

    private static final int MAX_CYCLE = 1000;

    private static final int MSG_VALUE = 0;
    private static final int MSG_GAIN = 1;

    private static final int PHASE_VALUE = 0;
    private static final int PHASE_GAIN = 1;

    private int phase;
    private int gain;
    private int pendingValueIndex;
    private boolean canChangeValue;

    private int currentCycle;

    public MGMAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        canChangeValue = true;
    }

    @Override
    protected void initRun() {
        valueIndex = (int)(Math.random() * domain.length);
        sendValueMessage();
    }

    private void sendValueMessage() {
        for (int neighborId : neighbours){
            sendMessage(new Message(id,neighborId,MSG_VALUE,valueIndex));
        }
    }

    private void sendGainMessage() {
        for (int neighborId : neighbours){
            sendMessage(new Message(id,neighborId,MSG_GAIN,gain));
        }
    }
    @Override
    public void runFinished() {
        ResultCycle resultCycle = new ResultCycle();
        resultCycle.setAgentValues(id,valueIndex);
        resultCycle.setTotalCost(getLocalCost());
        mailer.setResultCycle(id,resultCycle);
    }

    @Override
    public void disposeMessage(Message message) {
        phase = message.getType();
        switch (message.getType()){
            case MSG_VALUE:
                updateLocalView(message.getIdSender(),(int)message.getValue());
                break;
            case MSG_GAIN:
                int otherGain = (int)message.getValue();
                if (otherGain > gain){
                    canChangeValue = false;
                }
                break;
        }
    }

    @Override
    public void allMessageDisposed() {
        super.allMessageDisposed();
        switch (phase){
            case PHASE_VALUE:
                int minCost = Integer.MAX_VALUE;
                for (int i = 0; i < domain.length; i++){
                    int cost = calculateLocalCost(i);
                    if (minCost > cost){
                        minCost = cost;
                        pendingValueIndex = i;
                    }
                }
                gain = calculateLocalCost(valueIndex) - minCost;
                sendGainMessage();
                break;
            case PHASE_GAIN:
                if (canChangeValue){
                    boolean isNeedBroadcastValue = pendingValueIndex != valueIndex;
                    if (isNeedBroadcastValue) {
                        valueIndex = pendingValueIndex;
                        sendValueMessage();
                    }

                }
//                System.out.println("id:" + id + " " + canChangeValue);
                canChangeValue = true;

        }
        if (currentCycle++ >= MAX_CYCLE){
            stopProcess();
        }
    }

    private int calculateLocalCost(int index){
        int cost = 0;
        for (int neighbourId : neighbours){
            cost += constraintCosts.get(neighbourId)[index][getNeighbourValue(neighbourId)];
        }
        return cost;
    }
}
