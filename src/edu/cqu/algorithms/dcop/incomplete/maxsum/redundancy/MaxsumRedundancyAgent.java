package edu.cqu.algorithms.dcop.incomplete.maxsum.redundancy;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;
import edu.cqu.result.ResultCycle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MaxsumRedundancyAgent extends SyncAgent {

    private static final int MSG_Q = 0;
    private static final int MSG_R = 1;
    private static final int MSG_VALUE = 2;

    private static final int MAX_CYCLE = 50;


    private VariableNode variableNode;
    private FunctionNode functionNode;
    private Set<Integer> allNeighbours;
    private int cycle;

    public MaxsumRedundancyAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        Map<Integer,Integer> domainLength = new HashMap<>();
        domainLength.put(id,domain.length);
        allNeighbours = new HashSet<>();
        for (int neighbourId : neighbours){
            allNeighbours.add(neighbourId);
            domainLength.put(neighbourId,neighbourDomains.get(neighbourId).length);
        }
        allNeighbours.add(id);
        variableNode = new VariableNode(neighbours,id,domainLength);
        functionNode = new FunctionNode(neighbours,id,new LocalFunction(id,constraintCosts,domainLength));
    }

    @Override
    protected void initRun() {
        for (int id : allNeighbours){
            sendMessage(new Message(this.id,id,MSG_Q,variableNode.computeMessage(id)));
            sendMessage(new Message(this.id,id,MSG_R,functionNode.computeMessage(id)));
        }
    }

    @Override
    public void runFinished() {
        ResultCycle resultCycle = new ResultCycle();
        resultCycle.setAgentValues(id,0);
        mailer.setResultCycle(id,resultCycle);
    }

    @Override
    public void disposeMessage(Message message) {
        switch (message.getType()){
            case MSG_Q:
                functionNode.addMessage(message.getIdSender(),(int[])message.getValue());
                break;
            case MSG_R:
                variableNode.addMessage(message.getIdSender(),(int[])message.getValue());
                break;
            case MSG_VALUE:
                updateLocalView(message.getIdSender(),(int)message.getValue());
                break;
        }
    }

    @Override
    public void allMessageDisposed() {
        super.allMessageDisposed();
        if (cycle++ >= MAX_CYCLE){
            stopProcess();
            return;
        }
        for (int id : allNeighbours){
            sendMessage(new Message(this.id,id,MSG_Q,variableNode.computeMessage(id)));
            sendMessage(new Message(this.id,id,MSG_R,functionNode.computeMessage(id)));
        }

        int[] costs = variableNode.getCurrentCosts();
        int minCost = Integer.MAX_VALUE;
        int pending = -1;
        for (int i = 0; i < costs.length; i++){
            if (costs[i] < minCost){
                minCost = costs[i];
                pending = i;
            }
        }
        if (valueIndex != pending) {
            assignValueIndex(pending);
            for (int id : neighbours){
                sendMessage(new Message(this.id,id,MSG_VALUE,pending));
            }
        }

    }
}
