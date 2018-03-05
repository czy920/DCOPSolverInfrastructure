package edu.cqu.benchmark.randomdcops;

import edu.cqu.benchmark.AbstractGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by admin on 2017/5/18.
 */
public class RandomDCOPGenerator extends AbstractGraph {

    public static final String EXTRA_PARAMETER_DENSITY = "density";

    private double density;
    private Map<Integer,Set<Integer>> adjacentTable;
    public RandomDCOPGenerator(String name, int nbAgent, int domainSize, int minCost, int maxCost,double density) {
        super(name, nbAgent, domainSize, minCost, maxCost);
        this.density = density;
        adjacentTable = new HashMap<>();
    }

    private void generateInitConnectedGraph(){
        for (int i = 0; i < nbAgent - 1; i++){
            int endPoint;
            int startPoint;
            while (true) {
                startPoint = random.nextInt(nbAgent) + 1;
                endPoint = random.nextInt(nbAgent) + 1;
                Set<Integer> visited = new HashSet<>();
                if (isValidate(endPoint,startPoint,visited)){
                    break;
                }
            }
            source.add(Integer.min(startPoint,endPoint));
            dest.add(Integer.max(startPoint,endPoint));
            nbConstraint++;
            nbRelation++;
            Set<Integer> adjacent = adjacentTable.get(startPoint);
            if (adjacent == null){
                adjacent = new HashSet<>();
                adjacentTable.put(startPoint,adjacent);
            }
            adjacent.add(endPoint);
            adjacent = adjacentTable.get(endPoint);
            if (adjacent == null){
                adjacent = new HashSet<>();
                adjacentTable.put(endPoint,adjacent);
            }
            adjacent.add(startPoint);
        }
    }

    private boolean isValidate(int nextPoint, int target, Set<Integer> visited){
        if (nextPoint == target)
            return false;
        visited.add(nextPoint);
        Set<Integer> adjacent = adjacentTable.get(nextPoint);
        if (adjacent == null)
            return true;
        for (int adj : adjacent){
            if (visited.contains(adj))
                continue;
            if (!isValidate(adj,target,visited)){
                return false;
            }
        }
        return true;
    }

    @Override
    public void generateConstraint() {
        generateInitConnectedGraph();
        int maxEdges = (int) (nbAgent * (nbAgent - 1) * density / 2);
        for (int i = nbAgent - 1; i < maxEdges; i++){
            int startPoint = random.nextInt(nbAgent) + 1;
            int endPoint = random.nextInt(nbAgent) + 1;
            while (startPoint == endPoint || adjacentTable.get(startPoint).contains(endPoint)){
                startPoint = random.nextInt(nbAgent) + 1;
                endPoint = random.nextInt(nbAgent) + 1;
            }
            adjacentTable.get(startPoint).add(endPoint);
            adjacentTable.get(endPoint).add(startPoint);
            source.add(Integer.min(startPoint,endPoint));
            dest.add(Integer.max(startPoint,endPoint));
            nbConstraint++;
            nbRelation++;
        }
    }
}
