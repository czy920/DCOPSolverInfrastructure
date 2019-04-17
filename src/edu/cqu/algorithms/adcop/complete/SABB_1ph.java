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
public class SABB_1ph extends SyncAgent {

    private final static int MSG_CPA = 1;
    private final static int MSG_CPA_BACK = 2;
    private final static int MSG_NEWSOLUTION = 3;
    private final static int MSG_TERNIMATE = 4;
    private final static int MSG_BACKTRACK = 5;

    private final static int LASTID = 8;
    private CPAMsg myCPA;
    private Map<Integer, Integer> bestCPA;
    private int value;
    int UB;
    private int cpaCost;
    private int pruneCount;
    private Map<Integer, boolean[][]> privacyMat;

    private long messageSizeCount;



    public SABB_1ph(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        myCPA = new CPAMsg();
        bestCPA =new HashMap<>();
        UB = Integer.MAX_VALUE;
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
            myCPA.cpa_cost = 0;
            myCPA.CPA.put(id, 0);
            myCPA.UB = Integer.MAX_VALUE;
            sendMessage(new Message(id, id + 1, MSG_CPA, myCPA.clone()));
        }
    }

    @Override
    public void runFinished() {
        ResultWithPrivacy res = new ResultWithPrivacy();
        res.setMessageSizeCount(messageSizeCount);
        res.setAgentValues(id, valueIndex);
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
        mailer.setResultCycle(id,res);

//        System.out.println("id:" + id + " pruneCount:" + pruneCount);
    }

    public int getCost(boolean isPrecede) {
        int precedeCost = 0;
        for (int i : neighbours) {
            if (isPrecede) {
                if (i < id) { // the precede agent
                    if (myCPA.CPA.containsKey(i)) {
                        ++ncccs;
                        precedeCost += constraintCosts.get(i)[value][myCPA.CPA.get(i)];
//                        privacyMat.get(i)[value][myCPA.CPA.get(i)] = true;
                    } else {
                        throw new RuntimeException("id: " + id + "'s precede cpa is not integrity!");
                    }
                }
            }
            else {
                if (i > id) { // the afterward agent
                    if (myCPA.CPA.containsKey(i)) {
                        ++ncccs;
                        precedeCost += constraintCosts.get(i)[value][myCPA.CPA.get(i)];
                    }
                }
            }
        }
        return precedeCost;
    }


    @Override
    public void disposeMessage(Message message) {
//        System.out.println(message);
        switch (message.getType()) {
            case MSG_CPA : {
                myCPA = (CPAMsg) message.getValue();
                messageSizeCount += myCPA.CPA.size()*2 + 2;
//                if (myCPA.CPA.size() >= 7) {
//                    if ((myCPA.CPA.get(1) == 0) && (myCPA.CPA.get(2) == 1) && (myCPA.CPA.get(3) == 0) &&
//                            (myCPA.CPA.get(4) == 2) && (myCPA.CPA.get(5) == 2) && (myCPA.CPA.get(6) == 1)) {
//                        System.out.println("got it !");
//                    }
//                }
                if (myCPA.CPA.containsKey(id)) { // search next sendValue
                    value = myCPA.CPA.get(id);
                    if (++value >= domain.length) {
                        sendMessage(new Message(id, id - 1, MSG_BACKTRACK, null));
                        return;
                    }
                }
                else {
                    value = 0;
                    cpaCost = myCPA.cpa_cost;
                }

                int precedeCost = getCost(true);
                while (cpaCost + precedeCost > myCPA.UB && value < domain.length) { //backtrack
                    ++value;
                    if (value >= domain.length ) {
                        sendMessage(new Message(id, id -1, MSG_BACKTRACK, null));
                        return;
                    }
                    precedeCost = getCost(true);
                }

                if (value >= domain.length) {
                    sendMessage(new Message(id, id -1, MSG_BACKTRACK, null));
                }
                else { //cpa back
                    myCPA.cpa_cost = cpaCost + precedeCost;
                    myCPA.CPA.put(id, value);
                    sendMessage(new Message(id, id - 1, MSG_CPA_BACK, myCPA.clone()));
                }
                break;
            }
            case MSG_CPA_BACK : {
                CPAMsg backCPA = (CPAMsg) message.getValue();
                messageSizeCount += backCPA.CPA.size()*2 + 2;
                int lastId = message.getIdSender();
                // ?? It's complexity!
                for (int i : backCPA.CPA.keySet()) {
                    if (lastId < i) {
                        lastId = i;
                    }
                }
                int cost = 0;
                if (value == backCPA.CPA.get(id)) {
                    boolean isNeighbour = false;
                    for (int i : neighbours) {
                        if (lastId == i) {
                            isNeighbour = true;
                            break;
                        }
                    }
                    if (isNeighbour) { //Arrays.asList(neighbours).contains(lastId)
                        ++ncccs;
                        cost = constraintCosts.get(lastId)[value][backCPA.CPA.get(lastId)];
                        privacyMat.get(lastId)[value][backCPA.CPA.get(lastId)] = true;
                    }
                }
                else {
                    throw new RuntimeException("id:" + id + "'s cpa is wrong!");
                }
                if (backCPA.cpa_cost + cost >= backCPA.UB) {
                    sendMessage(new Message(id, lastId, MSG_CPA, backCPA.clone()));
                    pruneCount++;
                }
                else if (id != 1) {//continue cpa back
                    backCPA.cpa_cost += cost;
                    sendMessage(new Message(id, id - 1, MSG_CPA_BACK, backCPA.clone()));
                }
                else if (lastId == LASTID) {//find the lower ub
                    backCPA.UB = backCPA.cpa_cost + cost;
                    for (int i = 1; i <= LASTID; ++i) {
                        sendMessage(new Message(id, i, MSG_NEWSOLUTION, backCPA.clone()));
                    }
                    sendMessage(new Message(id, lastId, MSG_CPA, backCPA.clone()));
                }
                else {
                    backCPA.cpa_cost += cost;
                    sendMessage(new Message(id, lastId + 1, MSG_CPA, backCPA.clone()));
                }
                break;
            }
            case MSG_NEWSOLUTION : {
                CPAMsg tmpCPA = (CPAMsg) message.getValue();
                messageSizeCount += tmpCPA.CPA.size() * 2 + 2;
                if (UB > (tmpCPA.UB)) {
                    UB = (tmpCPA.UB);
                    bestCPA = tmpCPA.CPA;
                    myCPA.UB = UB;
                    if (id == 1)
                    System.out.println("bound:" + UB);
                }
                break;
            }
            case MSG_TERNIMATE : {
                messageSizeCount ++;
                stopProcess();
                break;
            }
            case MSG_BACKTRACK : {
                messageSizeCount++;
                if (++value < domain.length) {
                    int precedeCost = getCost(true);
                    if (cpaCost + precedeCost > myCPA.UB) { //backtrack
                        sendMessage(new Message(id, id - 1, MSG_BACKTRACK, null));
                        pruneCount++;
                    } else { //cpa back
                        myCPA.CPA.put(id, value);
                        myCPA.cpa_cost = cpaCost + precedeCost;
                        for (int neiId : neighbours) {
                            if (neiId < id) {
                                if (precedeCost == 0) {
                                    privacyMat.get(neiId)[value][myCPA.CPA.get(neiId)] = true;
                                }
                            }
                        }
                        if (id == 1) {
                            System.out.println(value);

                            sendMessage(new Message(id, id + 1, MSG_CPA, myCPA.clone()));
                        }
                        else {
                            sendMessage(new Message(id, id - 1, MSG_CPA_BACK, myCPA.clone()));
                        }
                    }
                }
                else {
                    if (id == 1) { // done!
                        for (int i = 1; i <= LASTID; ++i) {
                            sendMessage(new Message(id, i, MSG_TERNIMATE, null));
                        }
                        System.out.println("UB: " + UB);
                        System.out.println(" CPA: " + (bestCPA));
                        stopProcess();
                    }
                    else {
                        sendMessage(new Message(id, id - 1, MSG_BACKTRACK, null));
                    }
                }
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

