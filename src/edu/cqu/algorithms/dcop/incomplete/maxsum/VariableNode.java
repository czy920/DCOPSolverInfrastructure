package edu.cqu.algorithms.dcop.incomplete.maxsum;

import java.util.HashMap;
import java.util.Map;

public class VariableNode {
    private Map<Integer,double[]> comingMessages;
    private int[] neighbours;
    protected int domainLength;

    public VariableNode(int[] neighbours,int domainLength) {
        this.neighbours = neighbours;
        this.domainLength = domainLength;
        comingMessages = new HashMap<>();
    }

    public void addMessage(int idSender,double[] content){
        comingMessages.put(idSender,content);
    }

    public QMessage computeQMessage(int target){
        double[] result = new double[domainLength];
        for (int neighbourId : neighbours){
            if (neighbourId == target){
                continue;
            }
            if (!comingMessages.containsKey(neighbourId)){
                continue;
            }
            for (int i = 0; i < domainLength; i++){
                result[i] += comingMessages.get(neighbourId)[i];
            }
        }
        normalize(result);
        QMessage qMessage = new QMessage();
        qMessage.target = target;
        qMessage.utility = result;
        return qMessage;
    }

    public int argMin(){
        double[] utility = getBelief();
        double minCost = Double.MAX_VALUE;
        int index = 0;
        for (int i = 0; i < domainLength; i++){
            if (minCost > utility[i]){
                minCost = utility[i];
                index = i;
            }
        }
        return index;
    }

    public double[] getBelief(){
        double[] utility = new double[domainLength];
        for (int neighbourId : neighbours){
            if (!comingMessages.containsKey(neighbourId)){
                continue;
            }
            for (int i = 0; i < domainLength; i++){
                utility[i] += comingMessages.get(neighbourId)[i];
            }
        }
        return utility;
    }

    private void normalize(double[] utility){
        double sum = 0;
        for (double u : utility){
            sum += u;
        }
        for (int i = 0; i < utility.length; i++){
            utility[i] -= sum / utility.length;
        }
    }
}
