package edu.cqu.algorithms.dcop.incomplete.maxsum.adssvp;

import edu.cqu.core.Message;

import java.util.HashSet;

public class MGM2 implements LocalSearch {

    private int valueIndex;
    private HostAgent agent;

    public static double threshold = 0.3;
    public static double cycle = 1000;

    private static final int MSG_OFFER = 101;
    private static final int MSG_ACCEPT = 102;
    private static final int MSG_REJECT = 103;
    private static final int MSG_GAIN = 104;
    private static final int MSG_GO = 105;

    private static final int PHASE_VALUE = 0;
    private static final int PHASE_OFFER = 1;
    private static final int PHASE_ACCEPT_REJECT = 2;
    private static final int PHASE_GAIN = 3;
    private static final int PHASE_GO_NO_GO = 4;

    private int phase;
    private boolean isOffer;
    private int slaver;
    private int maxGain;
    private int masterValue;
    private int pendingValue;
    private int acceptedOffer;
    private HashSet<Integer> offers;
    private boolean isRejected;
    private boolean canChangeValue;
    private int currentCycle;

    public MGM2(HostAgent agent, int valueIndex) {
        this.valueIndex = valueIndex;
        this.agent = agent;
        offers = new HashSet<>();
    }


    @Override
    public void initRun() {

    }

    @Override
    public void disposeMessage(Message message) {
        switch (message.getType()){
            case MSG_OFFER:
                offers.add(message.getIdSender());
                if (isOffer){
                    return;
                }
                Offer offer = (Offer)message.getValue();
                for (int i = 0; i < offer.size; i++){
                    int unionGain = offer.gains[i] + calculatePartialGain(offer.suggestedValues[i],message.getIdSender());
                    if (unionGain > maxGain && unionGain > 0){
                        maxGain = unionGain;
                        pendingValue = offer.suggestedValues[i];
                        acceptedOffer = message.getIdSender();
                        masterValue = i;
                    }
                }
                break;
            case MSG_ACCEPT:
                Accept accept = (Accept) message.getValue();
                pendingValue = accept.masterValue;
                maxGain = accept.gain;
                break;
            case MSG_REJECT:
                isRejected = true;
                break;
            case MSG_GAIN:
                int otherGain = (int) message.getValue();
                if (otherGain > maxGain){
                    canChangeValue = false;
//                    System.out.println("id:" + id + " cannot change value since:" + message.getIdSender() + "'s gain is " + otherGain);
                }
                break;
            case MSG_GO:
                canChangeValue &= (boolean)message.getValue();

        }
    }

    private int calculatePartialGain(int suggestedValue,int master){
        int oldCost = agent.getLocalCostCorr();
        oldCost -= agent.getConstraint(master)[valueIndex][agent.getNeighbourValue(master)];
        int currentCost = 0;
        for (int neighbourId : agent.getNeighbours()){
            if (neighbourId == master){
                continue;
            }
            currentCost += agent.getConstraint(neighbourId)[suggestedValue][agent.getNeighbourValue(neighbourId)];
        }
        return oldCost - currentCost;
    }

    @Override
    public void allMessageDisposed() {
        switch (phase){
            case PHASE_VALUE:
                isOffer = Math.random() < threshold;
                if (isOffer){
                    slaver = agent.getNeighbours()[(int)(Math.random() * agent.getNeighbours().length)];
                    agent.sendMessage(new Message(agent.getId(),slaver,MSG_OFFER,calculateOffer()));
                }
                else {
                    slaver = -1;
                }
                phase = PHASE_OFFER;
                acceptedOffer = -1;
                maxGain = Integer.MIN_VALUE;
                offers.clear();
                isRejected = false;
                canChangeValue = true;
                break;
            case  PHASE_OFFER:
                for (int offer : offers){
                    if (offer != acceptedOffer){
                        agent.sendMessage(new Message(agent.getId(),offer,MSG_REJECT,null));
                    }
                    else {
                        Accept accept = new Accept();
                        accept.gain = maxGain;
                        accept.masterValue = masterValue;
                        agent.sendMessage(new Message(agent.getId(),offer,MSG_ACCEPT,accept));
                    }
                }
                phase = PHASE_ACCEPT_REJECT;
                break;
            case PHASE_ACCEPT_REJECT:
                if ((isOffer && !isRejected) || (!isOffer && acceptedOffer != -1)){
                    //union gain
                    for (int neighbourId : agent.getNeighbours()){
                        if (neighbourId == acceptedOffer || neighbourId == slaver)
                            continue;
                        agent.sendMessage(new Message(agent.getId(),neighbourId,MSG_GAIN,maxGain));
                    }
                }
                else {
                    //unilateral gain
                    int oldCost = agent.getLocalCostCorr();
                    int minCost = Integer.MAX_VALUE;
                    for (int i = 0; i < agent.getDomainLength(); i++){
                        int cost = calculateLocalCost(i);
                        if (minCost > cost){
                            minCost = cost;
                            pendingValue = i;
                        }
                    }
                    maxGain = oldCost - minCost;
                    broadcastMessage(MSG_GAIN,maxGain);
                }
                phase = PHASE_GAIN;
                break;
            case PHASE_GAIN:
                if (isOffer && !isRejected){
                    agent.sendMessage(new Message(agent.getId(),slaver,MSG_GO,canChangeValue));
                }
                else if (!isOffer && acceptedOffer != -1){
                    agent.sendMessage(new Message(agent.getId(),acceptedOffer,MSG_GO,canChangeValue));
                }
                phase = PHASE_GO_NO_GO;
                break;
            case PHASE_GO_NO_GO:
                if (canChangeValue){
                    valueIndex = pendingValue;
                }
                phase = PHASE_VALUE;
                break;
        }
    }

    private int calculateLocalCost(int index){
        int cost = 0;
        for (int neighbourId : agent.getNeighbours()){
            cost += agent.getConstraint(neighbourId)[index][agent.getNeighbourValue(neighbourId)];
        }
        return cost;
    }

    private Offer calculateOffer(){
        Offer offer = new Offer(agent.getDomainLength());
        int oldCost = agent.getLocalCostCorr();
        for (int i = 0; i <  agent.getDomainLength(); i++){
            //suggestedValue
            int minCost = Integer.MAX_VALUE;
            int suggestedValue = 0;
            for (int j = 0; j < agent.getDomainLength(); j++){
                int cost = agent.getConstraint(slaver)[i][j];
                if (minCost > cost){
                    minCost = cost;
                    suggestedValue = j;
                }
            }
            offer.suggestedValues[i] = suggestedValue;
            //gain
            int currentCost = 0;
            for (int neighbourId : agent.getNeighbours()){
                if (neighbourId != slaver){
                    currentCost += agent.getConstraint(neighbourId)[i][agent.getNeighbourValue(neighbourId)];
                }
            }
            currentCost += minCost;
            offer.gains[i] = oldCost - currentCost;
        }
        return offer;
    }

    private void broadcastMessage(int msgType,Object msgValue){
        for (int neighbourId : agent.getNeighbours()){
            agent.sendMessage(new Message(agent.getId(),neighbourId,msgType,msgValue));
        }
    }

    @Override
    public int getValueIndex() {
        return valueIndex;
    }

    private class Offer{
        int[] suggestedValues;
        int[] gains;
        int size;
        public Offer(int size){
            this.size = size;
            suggestedValues = new int[size];
            gains = new int[size];
        }
    }

    private class Accept{
        int gain;
        int masterValue;
    }
}
