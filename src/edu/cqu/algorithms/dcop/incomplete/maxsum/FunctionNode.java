package edu.cqu.algorithms.dcop.incomplete.maxsum;

import edu.cqu.core.Message;

import java.util.HashMap;
import java.util.Map;

public class FunctionNode {
    private LocalFunction localFunction;
    private Map<Integer,double[]> comingMessages;
    public FunctionNode(LocalFunction localFunction) {
        this.localFunction = localFunction;
        comingMessages = new HashMap<>();
    }

    public void addMessage(int idSender,double[] content){
        comingMessages.put(idSender,content);
    }

    public RMessage computeRMessage(int target,int sourceValue){
        int source = target == localFunction.rowId ? localFunction.colId : localFunction.rowId;

        double[] utility = null;
        if (sourceValue >= 0){
            utility = localFunction.restrict(source,sourceValue);
        }
        else {
            utility = localFunction.min(source,comingMessages.get(source));
        }
        RMessage rMessage = new RMessage();
        rMessage.source = source;
        rMessage.utility = utility;
        return rMessage;
    }

    public RMessage computeRMessage(int target){
        return computeRMessage(target,-1);
    }

    public int getRowId(){
        return localFunction.rowId;
    }

    public int getColId(){
        return localFunction.colId;
    }

    public void setPreference(int id, double[] preference){
        localFunction.setPreferences(id,preference);
    }

}
