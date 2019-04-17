package edu.cqu.algorithms.dcop.incomplete.maxsum.adssvp;

import edu.cqu.algorithms.dcop.incomplete.maxsum.*;
import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;
import edu.cqu.result.ResultCycle;

import java.util.HashMap;
import java.util.Map;

public class MaxsumADSSVPWithLocalSearchAgent extends SyncAgent implements HostAgent {

    private static final int LOCAL_SEARCH_DSA = 0;
    private static final int LOCAL_SEARCH_MGM = 1;
    private static final int LOCAL_SEARCH_MGM2 = 2;

    private static final int MAX_CYCLE = 4800;
    private static final int LONGEST_PATH_LENGTH = 240;
    private static final int TIMING = 2;
    private static final int LOCAL_SEARCH_PHASE_LENGTH = 20;
    private static final int LOCAL_SEARCH_ALGORITHM = LOCAL_SEARCH_MGM2;

    private static final int DIRECTION_GREATER = 0;
    private static final int DIRECTION_SMALLER = 1;

    public static final int MSG_Q = 0;
    public static final int MSG_R = 1;
    public static final int MSG_VALUE = 2;

    private static final int PHASE_BELIEF_PROPAGATION = 0;
    private static final int PHASE_VALUE_PROPAGATION = 1;
    private static final int PHASE_LOCAL_SEARCH = 2;
    private static final int PHASE_MODIFICATION = 3;

    private int direction;
    private int phase;
    private VariableNode variableNode;
    private Map<Integer,FunctionNode> functionNodeList;
    private int cycle;
    private int previousCount;
    private LocalSearch localSearch;
    private int cycleInPhase;



    public MaxsumADSSVPWithLocalSearchAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
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
    public int getLocalCostCorr() {
        return 0;
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
//                    System.out.println("q_" + id + "→" + qMessage.target + ":" + array2String(qMessage.utility));
                }
                else {
                    functionNodeList.get(message.getIdSender()).addMessage(message.getIdSender(),qMessage.utility);
//                    System.out.println("q_" + message.getIdSender() + "→" + id + ":" + array2String(qMessage.utility));
                }
                break;
            case MSG_R:
                RMessage rMessage = (RMessage) message.getValue();
                variableNode.addMessage(rMessage.source,rMessage.utility);
//                System.out.println("r_" + rMessage.source + "→" + id + ":" + array2String(rMessage.utility));
                break;
            case MSG_VALUE:
                updateLocalView(message.getIdSender(),(int)message.getValue());
                break;
            default:
                localSearch.disposeMessage(message);
        }
    }

    @Override
    public void allMessageDisposed() {
        if (cycle++ >= MAX_CYCLE){
            stopProcess();
            return;
        }
        switch (phase){
            case PHASE_BELIEF_PROPAGATION:
            case PHASE_VALUE_PROPAGATION:
            case PHASE_MODIFICATION:
                switch (phase){
                    case PHASE_BELIEF_PROPAGATION:
                    case PHASE_VALUE_PROPAGATION:
                        int currentValueIndex = variableNode.argMin();
                        if (currentValueIndex != valueIndex) {
                            valueIndex = currentValueIndex;
                            broadcastValueIndex();
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

                for (FunctionNode functionNode : functionNodeList.values()) {
                    int smaller = Integer.min(functionNode.getColId(), functionNode.getRowId());
                    int bigger = Integer.max(functionNode.getColId(), functionNode.getRowId());
                    if (direction == DIRECTION_GREATER) {
                        int assignment = phase != PHASE_BELIEF_PROPAGATION ? getAssignment(smaller) : -1;
                        sendMessage(new Message(smaller, bigger, MSG_R, functionNode.computeRMessage(bigger, assignment)));
                    } else if (direction == DIRECTION_SMALLER) {
                        int assignment = phase != PHASE_BELIEF_PROPAGATION ? getAssignment(bigger) : -1;
                        sendMessage(new Message(bigger, smaller, MSG_R, functionNode.computeRMessage(smaller, assignment)));
                    }
                }

                break;
            case PHASE_LOCAL_SEARCH:
                localSearch.allMessageDisposed();
                int currentValueIndex = localSearch.getValueIndex();
                if (currentValueIndex != valueIndex){
                    valueIndex = currentValueIndex;
                    broadcastValueIndex();
                }
        }
        cycleInPhase++;
        if (phase == PHASE_BELIEF_PROPAGATION || phase == PHASE_VALUE_PROPAGATION || phase == PHASE_MODIFICATION){
            if (cycleInPhase == LONGEST_PATH_LENGTH){
                cycleInPhase = 0;
                if (phase == PHASE_BELIEF_PROPAGATION){
                    if (cycle / LONGEST_PATH_LENGTH > (TIMING - 1)){
                        phase = PHASE_VALUE_PROPAGATION;
                    }
                    direction = 1 - direction;
                }
                else if (phase == PHASE_VALUE_PROPAGATION){
                    phase = PHASE_LOCAL_SEARCH;
                    localSearch = initLocalSearch();
                    localSearch.initRun();
                }
                else if (phase == PHASE_MODIFICATION){
                    phase = PHASE_BELIEF_PROPAGATION;
                    direction = 1 - direction;
                }
            }
        }
        else if (phase == PHASE_LOCAL_SEARCH){
            if (cycleInPhase == LOCAL_SEARCH_PHASE_LENGTH){
                cycleInPhase = 0;
                phase = PHASE_MODIFICATION;
            }
        }
    }

    private void broadcastValueIndex(){
        for (int neighbourId : neighbours) {
            sendMessage(new Message(id, neighbourId, MSG_VALUE, valueIndex));
        }
    }

    private LocalSearch initLocalSearch(){
        switch (LOCAL_SEARCH_ALGORITHM){
            case LOCAL_SEARCH_DSA:
                return new DSA(this,valueIndex);
            case LOCAL_SEARCH_MGM:
                return new MGM(this,valueIndex);
            case LOCAL_SEARCH_MGM2:
                return new MGM2(this,valueIndex);
            default:
                return null;
        }
    }

    private int getAssignment(int id){
        if (id == this.id){
            return valueIndex;
        }
        return getNeighbourValue(id);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int[][] getConstraint(int oppositeId) {
        return constraintCosts.get(oppositeId);
    }

    @Override
    public int[] getNeighbours() {
        return neighbours;
    }

    @Override
    public int getNeighbourValue(int neighbourId) {
        return super.getNeighbourValue(neighbourId);
    }

    @Override
    public int getDomainLength() {
        return domain.length;
    }
}
