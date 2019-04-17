package edu.cqu.algorithms.dcop.incomplete.maxsum.redundancy;

import java.util.HashMap;
import java.util.Map;

public class LocalFunction {
    private int id;
    private Map<Integer,int[][]> costs;
    private Map<Integer,Integer> domainLength;

    public LocalFunction(int id, Map<Integer, int[][]> costs, Map<Integer, Integer> domainLength) {
        this.id = id;
        this.costs = costs;
        this.domainLength = domainLength;
    }

    public int[] project(int targetId, Map<Integer,int[]> receivedCost){
        int[] result = new int[domainLength.get(targetId)];
        Map<Integer,Integer> indexer = new HashMap<>();
        int[] sortedId = new int[costs.size()];
        int index = 0;
        for (int neighbourId : costs.keySet()){
            if (neighbourId != targetId)
                sortedId[index++] = neighbourId;
        }
        if (targetId != id){
            sortedId[index] = id;
        }
        for (int i = 0; i < result.length; i++){
            int[] currentPosition = new int[costs.size()];
            int minCost = Integer.MAX_VALUE;
            boolean flag = true;
            while (flag){
                for (int j = 0; j < sortedId.length; j++){
                    indexer.put(sortedId[j],currentPosition[j]);
                }
                indexer.put(targetId,i);
                int cost = getValue(indexer);
                for (int neighbourId : sortedId){
                    if (receivedCost.containsKey(neighbourId))
                        cost += receivedCost.get(neighbourId)[indexer.get(neighbourId)];
                }
                if (cost < minCost){
                    minCost = cost;
                }
                currentPosition[currentPosition.length - 1]++;
                for (int j = currentPosition.length - 1; j >= 0; j--){
                    if (currentPosition[j] == domainLength.get(sortedId[j])){
                        currentPosition[j] = 0;
                        if (j - 1 >= 0)
                            currentPosition[j - 1]++;
                        else {
                            flag = false;
                            break;
                        }
                    }
                }
            }
            result[i] = minCost;
        }
        return result;
    }

    private int getValue(Map<Integer,Integer> assignment){
        int cost = 0;
        for (int neighbourId : costs.keySet()){
            cost += costs.get(neighbourId)[assignment.get(id)][assignment.get(neighbourId)];
        }
        return cost;
    }


}
