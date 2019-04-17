package edu.cqu.algorithms.dcop.incomplete.maxsum;

import edu.cqu.core.Message;
import edu.cqu.core.SyncMailer;
import edu.cqu.framework.ALSAgent;
import edu.cqu.result.ResultAls;

import java.util.HashMap;
import java.util.Map;

public class MaxsumAlsAgent extends ALSAgent{

    private static final int MAX_CYCLE = 4560;

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
    private double[] preference;
    private int nbAgent;

    public MaxsumAlsAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer, MAX_CYCLE);
        variableNode = new VariableNode(neighbours,domain.length);
        functionNodeList = new HashMap<>();
        preference = new double[domain.length];
        for (int i = 0; i < preference.length; i++){
            preference[i] = Math.random() - 0.5;
        }
        for (int neighbourId : neighbours){
            if (id < neighbourId){
                LocalFunction localFunction = new LocalFunction(constraintCosts.get(neighbourId),id,neighbourId);
                localFunction.setPreferences(id,preference);
                functionNodeList.put(neighbourId,new FunctionNode(localFunction));
            }
        }
//        initConstraint();
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

    @Override
    protected void alsReady() {
        for (int neighbourId : neighbours){
            if (neighbourId < id) {
                sendMessage(new Message(id,neighbourId,MSG_EXCHANGE_PREFERENCE,preference));
            }
        }
    }

    @Override
    protected void decision() {
        switch (phase){
            case PHASE_EXCHANGE_PREFERENCE:
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
            case PHASE_Q:
                for (FunctionNode functionNode : functionNodeList.values()){
                    sendMessage(new Message(functionNode.getColId(),functionNode.getRowId(),MSG_R,functionNode.computeRMessage(functionNode.getRowId())));
                    sendMessage(new Message(functionNode.getRowId(),functionNode.getColId(),MSG_R,functionNode.computeRMessage(functionNode.getColId())));
                }
                phase = PHASE_R;
                break;
            case PHASE_R:
                int currentValueIndex = variableNode.argMin();
                if (currentValueIndex != valueIndex){
                    assignValueIndex(currentValueIndex);
                    for (int neighbourId : neighbours){
                        sendMessage(new Message(id,neighbourId,MSG_VALUE,currentValueIndex));
                    }
                }
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

    @Override
    public void runFinished() {
        ResultAls resultCycle = new ResultAls();
        resultCycle.setAgentValues(id,valueIndex);
        resultCycle.setTotalCost(getLocalCost());
        mailer.setResultCycle(id,resultCycle);
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
}
