package edu.cqu.algorithms.dcop.incomplete.maxsum.redundancy;

import java.util.HashMap;
import java.util.Map;

public class FunctionNode extends Node {
    private LocalFunction localFunction;

    public FunctionNode(int[] neighbours, int id, LocalFunction localFunction) {
        super(neighbours, id);
        this.localFunction = localFunction;
    }

    @Override
    public int[] computeMessage(int targetId) {
        Map<Integer,int[]> receivedCost = new HashMap<>(messageContent);
        receivedCost.remove(targetId);
        return localFunction.project(targetId,receivedCost);
    }
}
