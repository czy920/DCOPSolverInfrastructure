package edu.cqu.algorithms.dcop.incomplete.maxsum.adssvp;

import edu.cqu.algorithms.dcop.incomplete.maxsum.*;
import edu.cqu.core.Message;
import edu.cqu.core.SyncMailer;
import edu.cqu.framework.ALSAgent;
import edu.cqu.result.ResultAls;

import java.util.HashMap;
import java.util.Map;

public class MaxsumADSSVPWithLocalSearchAlsAgent extends ALSAgent implements HostAgent {

    public static final int LOCAL_SEARCH_DSA = 0;
    public static final int LOCAL_SEARCH_MGM = 1;
    public static final int LOCAL_SEARCH_MGM2 = 2;

    private static final int MAX_CYCLE = 4560;
    private static final int LONGEST_PATH_LENGTH = 240;
    private static final int TIMING = 2;
    private static final int LOCAL_SEARCH_PHASE_LENGTH = 50;
    public static int LOCAL_SEARCH_ALGORITHM = LOCAL_SEARCH_MGM2;

    private static final int DIRECTION_GREATER = 0;
    private static final int DIRECTION_SMALLER = 1;

    public static final int MSG_Q = 0;
    public static final int MSG_R = 1;
    public static final int MSG_VALUE = 2;
    private static final int MSG_EXCHANGE_PREFERENCE = 3;

    private static final int PHASE_EXCHANGE_PREFERENCE = 0;
    private static final int PHASE_BELIEF_PROPAGATION = 1;
    private static final int PHASE_VALUE_PROPAGATION = 2;
    private static final int PHASE_LOCAL_SEARCH = 3;
    private static final int PHASE_MODIFICATION = 4;

    private int direction;
    private int phase;
    private VariableNode variableNode;
    private Map<Integer,FunctionNode> functionNodeList;
    private int cycle;
    private LocalSearch localSearch;
    private int cycleInPhase;
    private int nbAgent;


    public MaxsumADSSVPWithLocalSearchAlsAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer,MAX_CYCLE);
        variableNode = new VariableNode(neighbours,domain.length);
        functionNodeList = new HashMap<>();
        direction = DIRECTION_GREATER;
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
    public int getLocalCostCorr() {
        int sum = 0;
        for (int neighbourId : neighbours){
            sum += constraintCosts.get(neighbourId)[valueIndex][getNeighbourValue(neighbourId)];
        }
        return sum;
    }

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
            }
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
                break;
            default:
                if (localSearch != null)
                    localSearch.disposeMessage(message);
        }
    }

    @Override
    protected void decision() {
        cycle++;
        switch (phase){
            case PHASE_EXCHANGE_PREFERENCE:
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
                phase = TIMING == 0 ? PHASE_VALUE_PROPAGATION : PHASE_BELIEF_PROPAGATION;
                break;
            case PHASE_BELIEF_PROPAGATION:
            case PHASE_VALUE_PROPAGATION:
            case PHASE_MODIFICATION:
                switch (phase){
                    case PHASE_BELIEF_PROPAGATION:
                    case PHASE_VALUE_PROPAGATION:
                        int currentValueIndex = variableNode.argMin();
                        if (currentValueIndex != valueIndex) {
                            assignValueIndex(currentValueIndex);
                            broadcastValueIndex(currentValueIndex);
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
                    assignValueIndex(currentValueIndex);
                    broadcastValueIndex(currentValueIndex);
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


    private void broadcastValueIndex(int value){
        for (int neighbourId : neighbours) {
            sendMessage(new Message(id, neighbourId, MSG_VALUE, value));
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
