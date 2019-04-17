package edu.cqu.algorithms.dcop.incomplete.maxsum.hbvp;

import edu.cqu.algorithms.dcop.incomplete.maxsum.*;
import edu.cqu.core.Message;
import edu.cqu.core.SyncMailer;
import edu.cqu.framework.ALSAgent;
import edu.cqu.result.ResultAls;
import edu.cqu.result.ResultCycle;

import java.util.*;

public class MaxsumHBVPAlsAgent extends ALSAgent {

    private static final int MAX_CYCLE = 4560;
    private static final int LONGEST_PATH_LENGTH = 240;

    private static final int MSG_Q = 0;
    private static final int MSG_R = 1;
    private static final int MSG_VALUE = 2;
    private static final int MSG_EXCHANGE_PREFERENCE = 3;

    private static final int PHASE_EXCHANGE_PREFERENCE = 0;
    private static final int PHASE_Q = 1;
    private static final int PHASE_R = 2;

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
    private int nbAgent;

    public MaxsumHBVPAlsAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer, MAX_CYCLE);
        variableNode = new VariableNode(neighbours,domain.length);
        functionNodeList = new HashMap<>();
        functionNodesToPrecursor = new HashSet<>();
        functionNodesToSuccessor = new HashSet<>();
//        initConstraint();
    }

    private boolean canSendMessageToSuccessor(){
        return previousReceivedCount == previousNeighbourCount && !isSuccessorSent;
    }

    private boolean canSendMessageToPrecursor(){
        return successorNeighbourCount == successorReceivedCount && !isPreviousSent;
    }
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

    @Override
    protected void alsReady() {
        double[] preference = new double[domain.length];
        for (int i = 0; i < preference.length; i++){
            preference[i] = Math.random() - 0.5;
        }
//        Arrays.fill(preference,0);
        for (int neighbourId : neighbours){
            if (id < neighbourId){
                LocalFunction localFunction = new LocalFunction(constraintCosts.get(neighbourId),id,neighbourId);
                localFunction.setPreferences(id,preference);
                functionNodeList.put(neighbourId,new FunctionNode(localFunction));
            }
            else {
                sendMessage(new Message(id,neighbourId,MSG_EXCHANGE_PREFERENCE,preference));
                previousNeighbourCount++;
            }
        }
        successorNeighbourCount = neighbours.length - previousNeighbourCount;
    }

    @Override
    protected void decision() {
        cycle++;
        switch (phase){
            case PHASE_EXCHANGE_PREFERENCE:
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
                break;
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
        }
    }

    @Override
    public void disposeMessage(Message message) {
        super.disposeMessage(message);
        switch (message.getType()){
            case MSG_EXCHANGE_PREFERENCE:
                int idSender = message.getIdSender();
                double[] preference = (double[]) message.getValue();
                functionNodeList.get(idSender).setPreference(idSender,preference);
                break;
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
    public void runFinished() {
        ResultCycle resultCycle = new ResultAls();
        resultCycle.setAgentValues(id,valueIndex);
        resultCycle.setTotalCost(getLocalCost());
        mailer.setResultCycle(id,resultCycle);
    }
}
