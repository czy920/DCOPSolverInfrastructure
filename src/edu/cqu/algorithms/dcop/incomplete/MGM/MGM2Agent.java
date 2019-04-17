package edu.cqu.algorithms.dcop.incomplete.MGM;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;
import edu.cqu.result.ResultCycle;

import java.util.HashSet;
import java.util.Map;

public class MGM2Agent extends SyncAgent {

    public static double threshold = 0.3;
    public static double cycle = 1000;

    private static final int MSG_VALUE = 0;
    private static final int MSG_OFFER = 1;
    private static final int MSG_ACCEPT = 2;
    private static final int MSG_REJECT = 3;
    private static final int MSG_GAIN = 4;
    private static final int MSG_GO = 5;

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

    public MGM2Agent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        offers = new HashSet<>();
    }

    @Override
    protected void initRun() {
         valueIndex = (int) (domain.length * Math.random());
         broadcastMessage(MSG_VALUE,valueIndex);
    }

    private void broadcastMessage(int msgType,Object msgValue){
        for (int neighbourId : neighbours){
            sendMessage(new Message(id,neighbourId,msgType,msgValue));
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
        switch (message.getType()){
            case MSG_VALUE:
                updateLocalView(message.getIdSender(),(int)message.getValue());
                break;
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
        int oldCost = getLocalCost();
        oldCost -= constraintCosts.get(master)[valueIndex][getNeighbourValue(master)];
        int currentCost = 0;
        for (int neighbourId : neighbours){
            if (neighbourId == master){
                continue;
            }
            currentCost += constraintCosts.get(neighbourId)[suggestedValue][getNeighbourValue(neighbourId)];
        }
        return oldCost - currentCost;
    }

    @Override
    public void allMessageDisposed() {
        if (currentCycle++ >= cycle){
            stopProcess();
            return;
        }
        switch (phase){
            case PHASE_VALUE:
                isOffer = Math.random() < threshold;
                if (isOffer){
                    slaver = neighbours[(int)(Math.random() * neighbours.length)];
                    sendMessage(new Message(id,slaver,MSG_OFFER,calculateOffer()));
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
                        sendMessage(new Message(id,offer,MSG_REJECT,null));
                    }
                    else {
                        Accept accept = new Accept();
                        accept.gain = maxGain;
                        accept.masterValue = masterValue;
                        sendMessage(new Message(id,offer,MSG_ACCEPT,accept));
                    }
                }
                phase = PHASE_ACCEPT_REJECT;
                break;
            case PHASE_ACCEPT_REJECT:
                if ((isOffer && !isRejected) || (!isOffer && acceptedOffer != -1)){
                    //union gain
                    for (int neighbourId : neighbours){
                        if (neighbourId == acceptedOffer || neighbourId == slaver)
                            continue;
                        sendMessage(new Message(id,neighbourId,MSG_GAIN,maxGain));
                    }
                }
                else {
                    //unilateral gain
                    int oldCost = getLocalCost();
                    int minCost = Integer.MAX_VALUE;
                    for (int i = 0; i < domain.length; i++){
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
                    sendMessage(new Message(id,slaver,MSG_GO,canChangeValue));
                }
                else if (!isOffer && acceptedOffer != -1){
                    sendMessage(new Message(id,acceptedOffer,MSG_GO,canChangeValue));
                }
                phase = PHASE_GO_NO_GO;
                break;
            case PHASE_GO_NO_GO:
                if (canChangeValue){
                    valueIndex = pendingValue;
                    broadcastMessage(MSG_VALUE,valueIndex);

                }
                phase = PHASE_VALUE;
                break;
        }
    }

    private int calculateLocalCost(int index){
        int cost = 0;
        for (int neighbourId : neighbours){
            cost += constraintCosts.get(neighbourId)[index][getNeighbourValue(neighbourId)];
        }
        return cost;
    }

    private Offer calculateOffer(){
        Offer offer = new Offer(domain.length);
        int oldCost = getLocalCost();
        for (int i = 0; i <  domain.length; i++){
            //suggestedValue
            int minCost = Integer.MAX_VALUE;
            int suggestedValue = 0;
            for (int j = 0; j < neighbourDomains.get(slaver).length; j++){
                int cost = constraintCosts.get(slaver)[i][j];
                if (minCost > cost){
                    minCost = cost;
                    suggestedValue = j;
                }
            }
            offer.suggestedValues[i] = suggestedValue;
            //gain
            int currentCost = 0;
            for (int neighbourId : neighbours){
                if (neighbourId != slaver){
                    currentCost += constraintCosts.get(neighbourId)[i][getNeighbourValue(neighbourId)];
                }
            }
            currentCost += minCost;
            offer.gains[i] = oldCost - currentCost;
        }
        return offer;
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
