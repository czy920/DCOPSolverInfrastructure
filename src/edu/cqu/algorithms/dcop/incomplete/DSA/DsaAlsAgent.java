package edu.cqu.algorithms.dcop.incomplete.DSA;


import edu.cqu.core.Message;
import edu.cqu.core.SyncMailer;
import edu.cqu.framework.ALSAgent;
import edu.cqu.result.ResultAls;

import java.util.HashMap;
import java.util.Map;

public class DsaAlsAgent extends ALSAgent {

    public final static int TYPE_VALUE_MESSAGE = 4560;
    public final static int CYCLE_COUNT_END = 1000;
    public final static String KEY_LOCALCOST="KEY_LOCALCOST";
    public final static double P = 0.4;

    private int nbAgent;
    private int cycleCount;
    private Map<Integer,Integer> localView;
    private int localCost;

    public DsaAlsAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer,CYCLE_COUNT_END);
        localView = new HashMap<>();
//        initConstraint();
    }
//    @Override
//    public int getLocalCost() {
//        int nZeroCost = 0;
//        for (int nId : neighbours){
//            int c = constraintCosts.get(nId)[valueIndex][getNeighbourValue(nId)];
//            if (c != 0){
//                nZeroCost++;
//            }
//            if (c < 0){
//                throw new RuntimeException();
//            }
//        }
//        if (nZeroCost != 0){
//            return nbAgent * 2;
//        }
//        return 0;
//    }

    private void initConstraint(){
        for (int neighbour : neighbours){
            int c = 0;
            if (id < neighbour){
                for (int i = 0; i < neighbourDomains.get(neighbour).length; i++){
                    int a = constraintCosts.get(neighbour)[0][i];
                    if (a < 0){
                        c = a;
                        constraintCosts.get(neighbour)[0][i] = 0;
                    }
                }
            }
            else {
                for (int i = 0; i < domain.length; i++){
                    int a = constraintCosts.get(neighbour)[i][0];
                    if (a < 0){
                        c = a;
                        constraintCosts.get(neighbour)[i][0] = 0;
                    }
                }
            }
            if (c == 0){
                throw new RuntimeException();
            }
            c = -c;
            if (id < neighbour){
                c = (c >> 8);
            }
            else {
                c = c & 255;
            }
            if (nbAgent != 0 && nbAgent != c){
                throw new RuntimeException();
            }
            nbAgent = c;
        }
    }


    @Override
    protected void alsReady() {
        int value = (int)(Math.random()*domain.length);
        assignValueIndex (value);
        assignAlsValue(value);
        sendValueMessage(value);
    }


    private void sendValueMessage(int value) {
        for (int neighborId : neighbours) {
            Message msg = new Message(this.id,neighborId,TYPE_VALUE_MESSAGE,value);
            sendMessage(msg);
        }
    }

    @Override
    protected void decision() {
        localCost = calLocalCost(valueIndex);
        dsaWork();
    }

    private void dsaWork() {
        int minCost = Integer.MAX_VALUE;
        int minCostIndex = -1;
        int count = 0;
        for (int i = 0; i < domain.length; i++) {
            if (calLocalCost(i) < minCost){
                minCost = calLocalCost(i);
                minCostIndex = i;
            }
        }
        if (minCost < localCost && Math.random() < P){
            assignValueIndex(minCostIndex);
            sendValueMessage(minCostIndex);
        }
    }

    private int calLocalCost(int value) {
        int cost = 0;
        for (int neighborId : neighbours) {
            int ov = localView.containsKey(neighborId) ? localView.get(neighborId) : 0;
            cost += constraintCosts.get(neighborId)[value][ov];
        }
        return cost;
    }



    @Override
    public void disposeMessage(Message msg) {
        super.disposeMessage(msg);
        switch (msg.getType()){
            case TYPE_VALUE_MESSAGE:
                disposeValueMessage(msg);
                break;
        }
    }

    private void disposeValueMessage(Message msg) {
        localView.put(msg.getIdSender(),(int)(msg.getValue()));
        updateLocalView(msg.getIdSender(), (int)msg.getValue());
    }

    @Override
    public void runFinished() {
        ResultAls resultCycle = new ResultAls();
        resultCycle.setAgentValues(id,valueIndex);
        mailer.setResultCycle(id,resultCycle);
    }


}
