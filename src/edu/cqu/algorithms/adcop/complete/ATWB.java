package edu.cqu.algorithms.adcop.complete;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;
import edu.cqu.result.ResultCycle;
import edu.cqu.result.ResultWithPrivacy;
import edu.cqu.result.annotations.NotRecordCostInCycle;

import java.util.HashMap;
import java.util.Map;
@NotRecordCostInCycle
public class ATWB extends SyncAgent {

    private final static int MSG_CPA = 1;
    private final static int MSG_REQUEST = 2;
    private final static int MSG_RESPONSE = 3;
    private final static int MSG_NEWSOLUTION = 4;
    private final static int MSG_BACKTRACK = 5;
    private final static int MSG_TERMINATE = 6;

    private CPAMsg myCPA;
    private int UB;
    //    private int cpaCost; // it means that cpaCost that does not receive any response msg
    private int value;
    private final static int LASTID = 8;
    private Map<Integer, Integer> bestCPA;
    private Map<Integer, Integer> responseMsg;
    private Map<Integer, boolean[][]> privacyMat;

    private long messageSizeCount;

    public ATWB(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        myCPA = new CPAMsg();
        UB = Integer.MAX_VALUE;
        responseMsg = new HashMap<>();
        bestCPA = new HashMap<>();
        privacyMat = new HashMap<>();
        for (int nId : neighbours) {
            boolean[][] tmp = new boolean[domain.length][neighbourDomains.get(nId).length];
            privacyMat.put(nId, tmp);
        }
        messageSizeCount = 0;
    }

    @Override
    protected void initRun() {
        if (id == 1) {
            value = 0;
            myCPA.CPA.put(id, value);
            myCPA.cpa_cost = 0;
            myCPA.UB = Integer.MAX_VALUE;
            sendRequestMsg();
        }
    }

    @Override
    public void runFinished() {
//        super.runFinished();
        ResultWithPrivacy res = new ResultWithPrivacy();
        res.setAgentValues(id, valueIndex);
        res.setMessageSizeCount(messageSizeCount);
        int total = 0;
        int leaked = 0;
        for (int neighbor : neighbours){
            for (int i = 0; i < domain.length; i++){
                for (int j = 0; j < neighbourDomains.get(neighbor).length; j++){
                    total++;
                    if (privacyMat.get(neighbor)[i][j]){
                        leaked++;
                    }
                }
            }
        }
        res.setTotalEntropy(total);
        res.setLeakedEntropy(leaked);
        mailer.setResultCycle(id, res);
    }

    @Override
    public void disposeMessage(Message message) {
        switch (message.getType()) {
            case MSG_CPA : {
                myCPA = (CPAMsg) message.getValue();
                messageSizeCount += myCPA.CPA.size()*2 + 2;
                value = 0;
                myCPA.CPA.put(id, value);
                sendRequestMsg();
                break;
            }
            case MSG_REQUEST : {
                CPAMsg newCPA = (CPAMsg) message.getValue();
                messageSizeCount += newCPA.CPA.size()*2 + 2;
                int sendId = message.getIdSender();
                int est = 0;
                if (sendId < id) { //after, return the estimate
                    int minEst = Integer.MAX_VALUE;
                    for (int i = 0; i < domain.length; ++i) {
                        int tmpEst = 0;
                        for (int nId : neighbours) {
                            if (newCPA.CPA.containsKey(nId)) {
                                ++ncccs;
                                tmpEst += constraintCosts.get(nId)[i][newCPA.CPA.get(nId)];
                            }
                        }
                        if (minEst > tmpEst) {
                            minEst = tmpEst;
                        }
                    }
                    est = minEst;
                }
                else { //before, return the backwards' cpa_cost
                    if (!cpaConsistency(newCPA)) {
                        throw new RuntimeException("id:" + id + "'s cpa is not consistency!");
                    }
                    else {
                        for (int i : neighbours) {
                            if (i == sendId) {
                                ++ncccs;
                                est = constraintCosts.get(sendId)[newCPA.CPA.get(id)][newCPA.CPA.get(sendId)];
                                privacyMat.get(sendId)[newCPA.CPA.get(id)][newCPA.CPA.get(sendId)] = true;
                                break;
                            }
                        }
                    }
                }
                sendMessage(new Message(id, sendId, MSG_RESPONSE, est));
                break;
            }
            case MSG_RESPONSE : {
                int sendId = message.getIdSender();
                int est = (int) message.getValue();
                messageSizeCount++;
                responseMsg.put(sendId, est);
                if (responseMsg.size() == LASTID - 1) { // receive others response
                    int localCost = 0;
                    if (value != myCPA.CPA.get(id)) {
                        throw new RuntimeException("value does not equal to cpa's! value:" + value + " but cpa's :" + myCPA.CPA.get(id));
                    }
                    for (int i : neighbours) {
                        if (i < id) { //before node id
                            if (myCPA.CPA.containsKey(i)) {
                                ++ncccs;
                                localCost += constraintCosts.get(i)[value][myCPA.CPA.get(i)];
                            }
                            else {
                                throw new RuntimeException("id:" + id + "'s cpa is not complete! since ");
                            }
                        }
                    }
                    int laterCost = 0;
                    int priorCost = 0;
                    for (int i : responseMsg.keySet()) {
                        if (i < id) {
                            priorCost += responseMsg.get(i);
                        }
                        else {
                            laterCost += responseMsg.get(i);
                        }
                    }
                    if (laterCost + priorCost + localCost + myCPA.cpa_cost >= myCPA.UB) {
                        if (++value >= domain.length) {
                            if (id == 1) {
                                for (int i = 2; i <= LASTID; ++i)
                                    sendMessage(new Message(id, i, MSG_TERMINATE, null));
                                stopProcess();
                            }
                            else {
                                sendMessage(new Message(id, id - 1, MSG_BACKTRACK, null));
                            }
                        }
                        else {
                            myCPA.CPA.put(id, value);
                            sendRequestMsg();
                        }

                    }
                    else {
                        if (id == LASTID) { // new solution
                            UB = myCPA.cpa_cost + laterCost + priorCost + localCost;
                            myCPA.UB = UB;
                            for (int i = 1; i <= LASTID; ++i) {
                                sendMessage(new Message(id, i, MSG_NEWSOLUTION, myCPA.clone()));
                            }
                            if (++value >= domain.length) {
                                sendMessage(new Message(id, id - 1, MSG_BACKTRACK, null));
                            }
                            else {
                                myCPA.CPA.put(id, value);
                                sendRequestMsg();
                            }
                        }
                        else { //continue cpa
                            CPAMsg newCPA = myCPA.clone();
                            newCPA.cpa_cost = myCPA.cpa_cost + priorCost + localCost;
                            sendMessage(new Message(id, id + 1, MSG_CPA, newCPA));
                        }
                    }
                }
                break;
            }
            case MSG_NEWSOLUTION : {
                CPAMsg tmpMsg = (CPAMsg) message.getValue();
                messageSizeCount += tmpMsg.CPA.size() * 2 + 2;
                if (UB > tmpMsg.UB) {
                    UB = tmpMsg.UB;
                    bestCPA.putAll(tmpMsg.CPA);
                    myCPA.UB = UB;
                }
                break;
            }
            case MSG_BACKTRACK : {
                messageSizeCount++;
                if (++ value >= domain.length) {
                    if (id == 1) { // search over
                        for (int i = 1; i <= LASTID; ++i) {
                            sendMessage(new Message(id, i, MSG_TERMINATE, null));
                        }
                        stopProcess();
                    }
                    else {
                        sendMessage(new Message(id, id - 1, MSG_BACKTRACK, null));
                    }
                }
                else {
                    myCPA.CPA.put(id, value);
                    sendRequestMsg();
                }
                break;
            }
            case MSG_TERMINATE : {
                messageSizeCount ++;
                if (id == 2) {
                    System.out.println("UB:" + UB);
                    System.out.println("CPA:" + bestCPA);
                }
                stopProcess();
                break;
            }
        }
    }

    public void sendRequestMsg() {
        responseMsg.clear();
        for (int i = 1; i <= LASTID; ++i) {
            if (i != id) {
                sendMessage(new Message(id, i, MSG_REQUEST, myCPA));
            }
        }
    }

    public boolean cpaConsistency(CPAMsg newCPA) {
        for (int i : myCPA.CPA.keySet()) {
            if (!newCPA.CPA.containsKey(i) || !(newCPA.CPA.get(i).intValue() == myCPA.CPA.get(i))) {
                return false;
            }
        }
        return true;
    }

    class CPAMsg {
        Map<Integer, Integer> CPA;
        int cpa_cost;
        int UB;

        public CPAMsg() {
            CPA = new HashMap<>();
            cpa_cost = 0;
            UB = Integer.MAX_VALUE;
        }

        protected CPAMsg clone() {
            CPAMsg cpaMsg = new CPAMsg();
            cpaMsg.CPA.putAll(this.CPA);
            cpaMsg.cpa_cost = this.cpa_cost;
            cpaMsg.UB = this.UB;
            return cpaMsg;
        }
    }

}
