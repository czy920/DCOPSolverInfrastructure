package edu.cqu.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dyc on 2017/6/16.
 */
public abstract class Agent extends Process{
    protected int id;
    protected int[] domain;
    protected int[] neighbours;
    protected Map<Integer,int[][]> constraintCosts;
    protected Map<Integer,int[]> neighbourDomains;
    private Map<Integer,Integer> localView;
    protected int valueIndex;

    public Agent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts,Map<Integer,int[]> neighbourDomains) {
        super("Agent " + id);
        this.id = id;
        this.domain = domain;
        this.neighbours = neighbours;
        this.constraintCosts = constraintCosts;
        this.neighbourDomains = neighbourDomains;
        localView = new HashMap<>();
    }

    @Override
    public void preExecution() {
        for (int neighbourId : neighbours){
            localView.put(neighbourId,0);
        }
        initRun();
        postInit();
    }

    protected abstract void initRun();
    protected void postInit(){

    }

    public abstract void runFinished();

    @Override
    public void postExecution() {
        runFinished();
    }

    public abstract void sendMessage(Message message);


    public abstract void disposeMessage(Message message);

    public int getLocalCost(){
        int sum = 0;
        for (int neighbourId : neighbours){
            sum += constraintCosts.get(neighbourId)[valueIndex][localView.get(neighbourId)];
        }
        return sum;
    }

    protected int updateLocalView(int neighbourId,int valueIndex){
        return localView.put(neighbourId,valueIndex);
    }

    protected int getNeighbourValue(int neighbourId){
        return localView.get(neighbourId);
    }

}
