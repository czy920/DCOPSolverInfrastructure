package edu.cqu.algorithms.dcop.incomplete.MGM;

import edu.cqu.core.Message;
import edu.cqu.core.SyncMailer;
import edu.cqu.framework.ALSAgent;
import edu.cqu.result.ResultAls;
import edu.cqu.result.ResultCycle;

import java.util.Map;

public class MGMAlsAgent extends ALSAgent {

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

    public MGMAlsAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer, 800);
        canChangeValue = true;
    }

    @Override
    protected void alsReady() {
        valueIndex = 0;
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
    public void disposeMessage(Message message) {
        super.disposeMessage(message);

        switch (message.getType()){
            case MSG_VALUE:
                phase = message.getType();
                updateLocalView(message.getIdSender(),(int)message.getValue());
                break;
            case MSG_GAIN:
                phase = message.getType();
                int otherGain = (int)message.getValue();
                if (otherGain > gain){
                    canChangeValue = false;
                }
                break;
        }
    }

    @Override
    protected void decision() {
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
    }

    private int calculateLocalCost(int index){
        int cost = 0;
        for (int neighbourId : neighbours){
            cost += constraintCosts.get(neighbourId)[index][getNeighbourValue(neighbourId)];
        }
        return cost;
    }


    protected void sendAllMessages() {

    }

    @Override
    public void runFinished() {
        ResultAls resultCycle = new ResultAls();
        resultCycle.setAgentValues(id,valueIndex);
        resultCycle.setTotalCost(getLocalCost());
        mailer.setResultCycle(id,resultCycle);
    }
}
