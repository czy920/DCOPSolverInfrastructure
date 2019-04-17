package edu.cqu.algorithms.dcop.incomplete.maxsum.adssvp;


import edu.cqu.core.Message;

public class MGM implements LocalSearch {

    private static final int PHASE_VALUE = 0;
    private static final int PHASE_GAIN = 1;

    private static final int MSG_GAIN = 100;

    private HostAgent agent;
    private int valueIndex;
    private int phase;
    private int gain;
    private boolean canReplaceValue;
    private int pendingValue;

    public MGM(HostAgent agent, int valueIndex) {
        this.agent = agent;
        this.valueIndex = valueIndex;
    }

    @Override
    public void initRun() {
        phase = PHASE_VALUE;
    }

    @Override
    public void disposeMessage(Message message) {
        int otherGain = (int) message.getValue();
        if (otherGain > gain){
            canReplaceValue = false;
        }
    }

    @Override
    public void allMessageDisposed() {
        switch (phase){
            case PHASE_VALUE:
                int minCost = Integer.MAX_VALUE;
                for (int i = 0; i < agent.getDomainLength(); i++){
                    int cost = calLocalCost(i);
                    if (minCost > cost){
                        minCost = cost;
                        pendingValue = i;
                    }
                }
                phase = PHASE_GAIN;
                gain = calLocalCost(valueIndex) - minCost;
                for (int neighbourId : agent.getNeighbours()){
                    agent.sendMessage(new Message(agent.getId(),neighbourId,MSG_GAIN,gain));
                }
                canReplaceValue = true;
                break;
            case PHASE_GAIN:
                if (canReplaceValue){
                    valueIndex = pendingValue;
                }
                phase = PHASE_VALUE;
        }
    }

    @Override
    public int getValueIndex() {
        return valueIndex;
    }

    private int calLocalCost(int valueIndex){
        int cost = 0;
        for (int neighbourId : agent.getNeighbours()){
            cost += agent.getConstraint(neighbourId)[valueIndex][agent.getNeighbourValue(neighbourId)];
        }
        return cost;
    }
}
