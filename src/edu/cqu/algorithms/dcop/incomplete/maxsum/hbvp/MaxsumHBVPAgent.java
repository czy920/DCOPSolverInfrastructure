package edu.cqu.algorithms.dcop.incomplete.maxsum.hbvp;

import edu.cqu.algorithms.dcop.incomplete.maxsum.*;
import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;
import edu.cqu.result.ResultCycle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class MaxsumHBVPAgent extends SyncAgent {

    private static final int MAX_CYCLE = 4800;
    private static final int LONGEST_PATH_LENGTH = 240;

    private static final int MSG_Q = 0;
    private static final int MSG_R = 1;
    private static final int MSG_VALUE = 2;

    private static final int PHASE_Q = 0;
    private static final int PHASE_R = 1;

    private int phase;

    private VariableNode variableNode;
    private Map<Integer,FunctionNode> functionNodeList;
    private int cycle;
    private int previousNeighbourCount;
    private int previousReceivedCount;
    private boolean isPreviousSent;
    private int successorNeighbourCount;
    private int successorReceivedCount;
    private boolean isSuccessorSent;
    private Set<FunctionNode> functionNodesToSuccessor;
    private Set<FunctionNode> functionNodesToPrecursor;


    public MaxsumHBVPAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        variableNode = new VariableNode(neighbours,domain.length);
        functionNodeList = new HashMap<>();
        functionNodesToPrecursor = new HashSet<>();
        functionNodesToSuccessor = new HashSet<>();
        for (int neighbourId : neighbours){
            if (id < neighbourId){
                functionNodeList.put(neighbourId,new FunctionNode(new LocalFunction(constraintCosts.get(neighbourId),id,neighbourId)));
            }
            else {
                previousNeighbourCount++;
            }
        }
        successorNeighbourCount = neighbours.length - previousNeighbourCount;
    }

    private boolean canSendMessageToSuccessor(){
        return previousReceivedCount == previousNeighbourCount && !isSuccessorSent;
    }

    private boolean canSendMessageToPrecursor(){
        return successorNeighbourCount == successorReceivedCount && !isPreviousSent;
    }

    @Override
    protected void initRun() {
        if (previousNeighbourCount == 0) {
            int currentValueIndex = variableNode.argMin();
            if (currentValueIndex != valueIndex){
                assignValueIndex(currentValueIndex);
                for (int neighbourId : neighbours){
                    sendMessage(new Message(id,neighbourId,MSG_VALUE,currentValueIndex));
                }
            }
            for (int neighbourId : neighbours) {
                sendMessage(new Message(id, id, MSG_Q, variableNode.computeQMessage(neighbourId)));
            }
            isSuccessorSent = true;
        }
        if (successorNeighbourCount == 0){
            for (int neighbourId : neighbours) {
                sendMessage(new Message(id, neighbourId, MSG_Q, variableNode.computeQMessage(neighbourId)));
            }
            isPreviousSent = true;
        }
        phase = PHASE_R;
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
                    functionNodesToSuccessor.add(functionNodeList.get(qMessage.target));
                }
                else {
                    functionNodeList.get(message.getIdSender()).addMessage(message.getIdSender(),qMessage.utility);
                    functionNodesToPrecursor.add(functionNodeList.get(message.getIdSender()));
                }
                break;
            case MSG_R:
                RMessage rMessage = (RMessage) message.getValue();
                if (rMessage.source > id){
                    successorReceivedCount++;
                }
                else {
                    previousReceivedCount++;
                }
                variableNode.addMessage(rMessage.source,rMessage.utility);
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
            case PHASE_R:
                for (FunctionNode functionNode : functionNodeList.values()){
                    int target;
                    if (functionNodesToSuccessor.contains(functionNode)) {
                        target = Integer.max(functionNode.getColId(),functionNode.getColId());
                        sendMessage(new Message(functionNode.getColId(), target, MSG_R, functionNode.computeRMessage(target,valueIndex)));
                    }
                    if (functionNodesToPrecursor.contains(functionNode)) {
                        sendMessage(new Message(functionNode.getRowId(), id, MSG_R, functionNode.computeRMessage(id)));
                    }
                }
                functionNodesToPrecursor.clear();
                functionNodesToSuccessor.clear();
                phase = PHASE_Q;
                break;
            case PHASE_Q:
                if (canSendMessageToSuccessor()){
                    int currentValueIndex = variableNode.argMin();
                    if (currentValueIndex != valueIndex){
                        assignValueIndex(currentValueIndex);
                        for (int neighbourId : neighbours){
                            sendMessage(new Message(id,neighbourId,MSG_VALUE,currentValueIndex));
                        }
                    }
                }
                for (int neighbourId : neighbours){
                    if (neighbourId < id && canSendMessageToPrecursor()) {
                        sendMessage(new Message(id, neighbourId, MSG_Q, variableNode.computeQMessage(neighbourId)));
                    }
                    else if (neighbourId > id && canSendMessageToSuccessor()){
                        sendMessage(new Message(id,id,MSG_Q,variableNode.computeQMessage(neighbourId)));
                    }
                }
                if (canSendMessageToSuccessor()){
                    isSuccessorSent = true;
                }
                if (canSendMessageToPrecursor()){
                    isPreviousSent = true;
                }
                phase = PHASE_R;
                break;
        }
        if (cycle > 0 && cycle % LONGEST_PATH_LENGTH == 0){
            isPreviousSent = isSuccessorSent = false;
            previousReceivedCount = successorReceivedCount = 0;
            functionNodesToSuccessor.clear();
            functionNodesToPrecursor.clear();
            initRun();
        }
    }
}
