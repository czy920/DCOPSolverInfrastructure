package edu.cqu.algorithms.dcop.incomplete.maxsum.advp;

import edu.cqu.algorithms.dcop.incomplete.maxsum.*;
import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;
import edu.cqu.result.ResultCycle;

import java.util.*;

public class MaxsumADAgent extends SyncAgent {

    private static final int MAX_CYCLE = 2000;
    private static final int LONGEST_PATH_LENGTH = 100;
    private static final int TIMING = 2;


    private static final int DIRECTION_GREATER = 0;
    private static final int DIRECTION_SMALLER = 1;

    private static final int MSG_Q = 0;
    private static final int MSG_R = 1;
    private static final int MSG_VALUE = 2;

    private static final int PHASE_Q = 0;
    private static final int PHASE_R = 1;
    private int direction;
    private int phase;
    private VariableNode variableNode;
    private Map<Integer,FunctionNode> functionNodeList;
    private int cycle;
    private int previousCount;



    public MaxsumADAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        variableNode = new VariableNode(neighbours,domain.length);
        functionNodeList = new HashMap<>();
        direction = DIRECTION_GREATER;
        for (int neighbourId : neighbours){
            if (id < neighbourId){
                functionNodeList.put(neighbourId,new FunctionNode(new LocalFunction(constraintCosts.get(neighbourId),id,neighbourId)));
            }
            else {
                previousCount++;
            }
        }
    }

    @Override
    protected void initRun() {
        if (previousCount == 0) {
            for (int neighbourId : neighbours) {
                if (direction == DIRECTION_GREATER && neighbourId < id) {
                    continue;
                }
                if (direction == DIRECTION_SMALLER && neighbourId > id) {
                    continue;
                }
                if (neighbourId < id) {
                    sendMessage(new Message(id, neighbourId, MSG_Q, variableNode.computeQMessage(neighbourId)));
                } else {
                    sendMessage(new Message(id, id, MSG_Q, variableNode.computeQMessage(neighbourId)));
                }
            }
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
            case MSG_Q:
                QMessage qMessage = (QMessage) message.getValue();
                if (message.getIdSender() == id){
                    functionNodeList.get(qMessage.target).addMessage(id,qMessage.utility);
                    //System.out.println("q_" + id + "→" + qMessage.target + ":" + array2String(qMessage.utility));
                }
                else {
                    functionNodeList.get(message.getIdSender()).addMessage(message.getIdSender(),qMessage.utility);
                    //System.out.println("q_" + message.getIdSender() + "→" + id + ":" + array2String(qMessage.utility));
                }
                break;
            case MSG_R:
                RMessage rMessage = (RMessage) message.getValue();
                variableNode.addMessage(rMessage.source,rMessage.utility);
                //System.out.println("r_" + rMessage.source + "→" + id + ":" + array2String(rMessage.utility));
                break;
            case MSG_VALUE:
                updateLocalView(message.getIdSender(),(int)message.getValue());
        }
    }

    @Override
    public void allMessageDisposed() {
        if (cycle++ >= MAX_CYCLE){
            stopProcess();
            return;
        }

        switch (phase){
            case PHASE_Q:
                boolean enableValuePropation = false;
                int round = cycle / LONGEST_PATH_LENGTH;
                if (round >= TIMING) {
                    enableValuePropation = true;
                }
                for (FunctionNode functionNode : functionNodeList.values()) {
                    int smaller = Integer.min(functionNode.getColId(), functionNode.getRowId());
                    int bigger = Integer.max(functionNode.getColId(), functionNode.getRowId());
                    if (direction == DIRECTION_GREATER) {
                        int assignment = enableValuePropation ? getAssignment(smaller) : -1;
                        sendMessage(new Message(smaller, bigger, MSG_R, functionNode.computeRMessage(bigger, assignment)));
                    } else if (direction == DIRECTION_SMALLER) {
                        int assignment = enableValuePropation ? getAssignment(bigger) : -1;
                        sendMessage(new Message(bigger, smaller, MSG_R, functionNode.computeRMessage(smaller, assignment)));
                    }
                }

                phase = PHASE_R;
                break;
            case PHASE_R:
                int currentValueIndex = variableNode.argMin();
                if (currentValueIndex != valueIndex) {
                    valueIndex = currentValueIndex;
                    for (int neighbourId : neighbours) {
                        sendMessage(new Message(id, neighbourId, MSG_VALUE, valueIndex));
                    }
                }
                for (int neighbourId : neighbours) {
                    if (direction == DIRECTION_GREATER && neighbourId < id) {
                        continue;
                    }
                    if (direction == DIRECTION_SMALLER && neighbourId > id) {
                        continue;
                    }
                    if (neighbourId < id) {
                        sendMessage(new Message(id, neighbourId, MSG_Q, variableNode.computeQMessage(neighbourId)));
                    } else {
                        sendMessage(new Message(id, id, MSG_Q, variableNode.computeQMessage(neighbourId)));
                    }
                }
                phase = PHASE_Q;
                break;
        }
        if (cycle % LONGEST_PATH_LENGTH == 0 && cycle != 0){
            direction = 1 - direction;
            //System.out.println("belief:" +array2String(variableNode.getBelief()) + " " + id + "=" + valueIndex);
            previousCount = neighbours.length - previousCount;
        }
    }

    private int getAssignment(int id){
        if (id == this.id){
            return valueIndex;
        }
        return getNeighbourValue(id);
    }
}
