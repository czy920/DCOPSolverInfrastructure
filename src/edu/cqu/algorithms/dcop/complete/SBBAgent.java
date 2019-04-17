package edu.cqu.algorithms.dcop.complete;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;
import edu.cqu.result.ResultWithPrivacy;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dyc on 2017/9/26.
 */
public class SBBAgent extends SyncAgent {

    public static int MAX_ID = 5;

    private static final int MSG_TO_NEXT = 0;
    private static final int MSG_BACKTRACK = 1;
    private static final int MSG_TERMINATE = 2;
    int ub;

    public SBBAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
    }

    @Override
    protected void initRun() {
        if (id == 1){
            valueIndex = 0;
            Map<Integer,Integer> assignment = new HashMap<>();
            assignment.put(id,valueIndex);
            assignment.put(-1,0);
            assignment.put(-2,Integer.MAX_VALUE);
            sendMessage(new Message(id,id + 1,MSG_TO_NEXT,assignment));
        }
    }

    @Override
    public void runFinished() {
        ResultWithPrivacy cycle = new ResultWithPrivacy();
        if (id == 1)
            cycle.setUb(ub);
        cycle.setAgentValues(id,0);
        mailer.setResultCycle(id,cycle);
    }

    @Override
    public void disposeMessage(Message message) {
//        System.out.println(message);
        switch (message.getType()){
            case MSG_TO_NEXT: {
//                System.out.println(id);
                Map<Integer, Integer> assignment = (Map) message.getValue();
                int upperBound = assignment.get(-2);
                int accumulatedCost = assignment.get(-1);
                boolean needBacktrack = true;
                for (int i = 0; i < domain.length; i++) {
                    int cost = 0;
                    for (int neighborId : neighbours) {
                        if (neighborId < id) {
                            cost += constraintCosts.get(neighborId)[i][assignment.get(neighborId)];
                            ncccs++;
                        }
                    }
                    if (accumulatedCost + cost < upperBound) {
                        valueIndex = i;
                        if (id != MAX_ID) {
                            needBacktrack = false;
                            assignment.put(id, valueIndex);
                            assignment.put(-1, accumulatedCost + cost);
                            sendMessage(new Message(id, id + 1, MSG_TO_NEXT, assignment));
                            break;
                        } else {
                            upperBound = accumulatedCost + cost;
//                            System.out.println(upperBound);
                        }
                    }
                }
                if (id == MAX_ID) {
                    //complete solution
                    assignment.put(-2, upperBound);
                    sendMessage(new Message(id, id - 1, MSG_BACKTRACK, assignment));
                }
                else if (needBacktrack){
                    sendMessage(new Message(id, id - 1, MSG_BACKTRACK, assignment));
                }
            }
            break;
            case MSG_BACKTRACK:
                Map<Integer, Integer> assignment = (Map) message.getValue();
                int upperBound = assignment.get(-2);
                int accumulatedCost = assignment.get(-1);
                for (int neighborId : neighbours){
                    if (neighborId < id){
                        int oppositeValue = assignment.get(neighborId);
                        accumulatedCost -= constraintCosts.get(neighborId)[valueIndex][oppositeValue];
                        ncccs++;
                    }
                }
                boolean canBacktrack = true;
                int oldValue = valueIndex;
                for (int i = valueIndex + 1; i < domain.length; i++){
                    int cost = 0;
                    for (int neighborId : neighbours) {
                        if (neighborId < id) {
                            cost += constraintCosts.get(neighborId)[i][assignment.get(neighborId)];
                            ncccs++;
                        }
                    }
                    if (accumulatedCost + cost < upperBound){
                        valueIndex = i;
                        assignment.put(id, valueIndex);
                        assignment.put(-1, accumulatedCost + cost);
                        sendMessage(new Message(id, id + 1, MSG_TO_NEXT, assignment));
                        break;
                    }
                }
                canBacktrack = oldValue == valueIndex;
                if (canBacktrack){
                    if (id == 1) {
//                        System.out.println(assignment.get(-2));
                        ub = assignment.get(-2);
                        for (int i = 1; i <= MAX_ID; i++) {
                            sendMessage(new Message(id, i, MSG_TERMINATE, null));
                        }
                        stopProcess();
                    }
                    else {
                        assignment.put(-1,accumulatedCost);
                        sendMessage(new Message(id, id - 1, MSG_BACKTRACK, assignment));
                    }
                }
                break;
            case MSG_TERMINATE:
                stopProcess();
        }
    }
}
