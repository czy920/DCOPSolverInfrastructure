package edu.cqu.algorithms.adcop.complete;

import edu.cqu.core.*;
import edu.cqu.result.ResultCycle;
import edu.cqu.result.annotations.NotRecordCostInCycle;

import java.util.HashMap;
import java.util.Map;
@NotRecordCostInCycle
public class SBB_2ph extends SyncAgent {
    private final static int MSG_CPA = 1;
    private final static int MSG_CPA_BACK = 2;
    private final static int MSG_NEWSOLUTION = 3;
    private final static int MSG_TERNIMATE = 4;
    private final static int MSG_BACKTRACK = 5;
    
    private final static int LASTID = 6;
    private int value;
    private CPAMsg myCPA;
    private int myLocalCost;
    private Map<Integer, Integer> bestCPA;

    public SBB_2ph(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        value = 0;
        bestCPA = new HashMap<>();
    }

    @Override
    protected void initRun() {
        if (id == 1) {
            value = 0;
            assignValueIndex(value);
            myCPA = new CPAMsg();
            myCPA.cpa_cost = 0;
            myCPA.CPA.put(id, 0);
            myCPA.UB = Integer.MAX_VALUE;
            sendMessage(new Message(id, id + 1, MSG_CPA, myCPA.clone()));
        }
    }

    @Override
    public void runFinished() {
        ResultCycle res = new ResultCycle();
        res.setAgentValues(id, valueIndex);
        mailer.setResultCycle(id, res);
    }

    public int getForwardLocalCost() {
        int localCost = 0;
        for (int i : neighbours ) {
            if (i < id) { // the node before it
                if (myCPA == null) {
                    throw new RuntimeException("id:" + id + "'s CPA is empty!");
                }
                if (myCPA.CPA.containsKey(i)) {
                    ++ncccs;
                    localCost += constraintCosts.get(i)[value][myCPA.CPA.get(i)];
                }
                else {
                    throw new RuntimeException("id:" + id + "'s CPA is not full");
                }
            }
        }
        return localCost;
    }

    @Override
    public void disposeMessage(Message message) {
        switch (message.getType()) {
            case MSG_CPA: {
                myCPA = (CPAMsg) message.getValue();
                if (!myCPA.CPA.containsKey(id)) {
                    myLocalCost = myCPA.cpa_cost;
                    value = 0;
                }
                else { // when receive the cpa msg from cpa back process
                    value = myCPA.CPA.get(id);
                    ++ value;
                    if (value >= domain.length) {
                        sendMessage(new Message(id, id - 1, MSG_BACKTRACK, null));
                        return;
                    }
                    myCPA.cpa_cost = myLocalCost;
                }

                int localCost = 0;
                while (value < domain.length) {
                    localCost = getForwardLocalCost();
                    if (myCPA.cpa_cost + localCost >= myCPA.UB) {
                        ++value;
                    }
                    else {
                        break;
                    }
                }

                if (value >= domain.length) {
                    sendMessage(new Message(id, id - 1, MSG_BACKTRACK, null));
                    return;
                }//continue send CPA to next agent
                else {
                    localCost = getForwardLocalCost();
                    CPAMsg newCPA = myCPA.clone();
                    newCPA.CPA.put(id, value);
                    newCPA.cpa_cost += localCost;
                    if (id == LASTID) { // cpa back procession don't need to change CPA
                        sendMessage(new Message(id, id - 1, MSG_CPA_BACK, newCPA));
                    }
                    else {
                        sendMessage(new Message(id, id + 1, MSG_CPA, newCPA));
                    }
                }
                break;
            }
            case MSG_BACKTRACK : {
                if (++value < domain.length) { // search next sendValue
                    int localCost = getForwardLocalCost();
                    if (myCPA.cpa_cost + localCost >= myCPA.UB) {
                        if (id == 1) {
                            myCPA.CPA.put(id, value);
                            myCPA.cpa_cost = 0;
                            sendMessage(new Message(id, id + 1, MSG_CPA, myCPA.clone()));
                        }
                        else {
                            sendMessage(new Message(id, id - 1, MSG_BACKTRACK, null));
                        }
                        return;
                    }
                    else {
                        CPAMsg newCPA = myCPA.clone();
                        newCPA.CPA.put(id, value);
                        newCPA.cpa_cost += localCost;
                        sendMessage(new Message(id, id + 1, MSG_CPA, newCPA));
                    }
                }
                else {
                    //search over
                    if (id == 1) {
                        System.out.println("UB:" + myCPA.UB + "\n cpa: \n" + map2String(bestCPA));
                        for (int i = 1; i <= LASTID; ++i)
                            sendMessage(new Message(id, i, MSG_TERNIMATE, null));
                        stopProcess();
                    }
                    else {
                        sendMessage(new Message(id, id - 1, MSG_BACKTRACK, null));
                    }
                }
                break;
            }
            case MSG_CPA_BACK : {
                CPAMsg newCPA = (CPAMsg) message.getValue();
                int localCost = 0;
                for (int i : neighbours) {
                    if (i > id) { // the node behind it;
                        if (newCPA.CPA.containsKey(i)) {
                            ++ncccs;
                            localCost += constraintCosts.get(i)[value][newCPA.CPA.get(i)];
                        }
                        else {
                            throw new RuntimeException("id:" + id + "'s CPA is not full");
                        }
                    }
                }
                if (newCPA.cpa_cost + localCost >= newCPA.UB) { //back
                    sendMessage(new Message(id, LASTID, MSG_CPA, newCPA.clone()));
                }
                else if (id ==1) {
                    newCPA.UB = newCPA.cpa_cost + localCost;
                    for (int i = 1; i <= LASTID; ++i) {
                        sendMessage(new Message(id, i, MSG_NEWSOLUTION, newCPA));
                    }
                    sendMessage(new Message(id, LASTID, MSG_CPA, newCPA));
                }
                else {
                    newCPA.cpa_cost += localCost;
                    sendMessage(new Message(id, id - 1, MSG_CPA_BACK, newCPA));
                }
                break;
            }
            case MSG_NEWSOLUTION : {
                CPAMsg cpa = (CPAMsg) message.getValue();
                if (myCPA.UB > cpa.UB) {
                    myCPA.UB = cpa.UB;
                    bestCPA.putAll(cpa.CPA);
                }

                break;
            }
            case MSG_TERNIMATE : {
                stopProcess();
                break;
            }
        }
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
