package edu.cqu.algorithms.dcop.incomplete.maxsum.redundancy;

import java.util.Map;

public class VariableNode extends Node {

    private Map<Integer,Integer> domainLength;

    public VariableNode(int[] neighbours, int id, Map<Integer, Integer> domainLength) {
        super(neighbours, id);
        this.domainLength = domainLength;
    }

    @Override
    public int[] computeMessage(int targetId) {
        int[] result = new int[domainLength.get(targetId)];

        for (int id : messageContent.keySet()){
            if (id != targetId){
                for (int i = 0; i < result.length; i++){
                    result[i] += messageContent.get(id)[i];
                }
            }
        }
        normalize(result);
        return result;
    }

    private void normalize(int[] result){
        int normalizer = 0;
        for (int data : result){
            normalizer += data;
        }
        normalizer /= result.length;
        for (int i = 0; i < result.length; i++){
            result[i] -= normalizer;
        }
    }

    public int[] getCurrentCosts(){
        int[] result = new int[domainLength.get(id)];
        for (int id : messageContent.keySet()){
            for (int i = 0; i < result.length; i++){
                result[i] += messageContent.get(id)[i];
            }
        }
        return result;
    }

}
