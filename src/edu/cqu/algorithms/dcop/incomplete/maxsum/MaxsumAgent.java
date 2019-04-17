package edu.cqu.algorithms.dcop.incomplete.maxsum;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;
import edu.cqu.result.ResultCycle;

import java.util.HashMap;
import java.util.Map;

public class MaxsumAgent extends SyncAgent {

    private static final int MAX_CYCLE = 1000;

    private static final int MSG_Q = 0;
    private static final int MSG_R = 1;
    private static final int MSG_VALUE = 2;

    private static final int PHASE_Q = 0;
    private static final int PHASE_R = 1;

    private int phase;

    private VariableNode variableNode;
    private Map<Integer,FunctionNode> functionNodeList;
    private int cycle;


    public MaxsumAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        variableNode = new VariableNode(neighbours,domain.length);
        functionNodeList = new HashMap<>();
        for (int neighbourId : neighbours){
            if (id < neighbourId){
                functionNodeList.put(neighbourId,new FunctionNode(new LocalFunction(constraintCosts.get(neighbourId),id,neighbourId)));
            }
        }
    }

    @Override
    protected void initRun() {
        for (int neighbourId : neighbours){
            if (neighbourId < id) {
                sendMessage(new Message(id, neighbourId, MSG_Q, variableNode.computeQMessage(neighbourId)));
            }
            else {
                sendMessage(new Message(id,id,MSG_Q,variableNode.computeQMessage(neighbourId)));
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
                }
                else {
                    functionNodeList.get(message.getIdSender()).addMessage(message.getIdSender(),qMessage.utility);
                }
                break;
            case MSG_R:
                RMessage rMessage = (RMessage) message.getValue();
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
            case PHASE_Q:
                int currentValueIndex = variableNode.argMin();
                if (currentValueIndex != valueIndex){
                    assignValueIndex(currentValueIndex);
                    for (int neighbourId : neighbours){
                        sendMessage(new Message(id,neighbourId,MSG_VALUE,currentValueIndex));
                    }
                }
                for (FunctionNode functionNode : functionNodeList.values()){
                    sendMessage(new Message(functionNode.getColId(),functionNode.getRowId(),MSG_R,functionNode.computeRMessage(functionNode.getRowId())));
                    sendMessage(new Message(functionNode.getRowId(),functionNode.getColId(),MSG_R,functionNode.computeRMessage(functionNode.getColId())));
                }
                phase = PHASE_R;
                break;
            case PHASE_R:
                for (int neighbourId : neighbours){
                    if (neighbourId < id) {
                        sendMessage(new Message(id, neighbourId, MSG_Q, variableNode.computeQMessage(neighbourId)));
                    }
                    else {
                        sendMessage(new Message(id,id,MSG_Q,variableNode.computeQMessage(neighbourId)));
                    }
                }
                phase = PHASE_Q;
                break;
        }
    }
}
