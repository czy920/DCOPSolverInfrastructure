package edu.cqu.algorithms.dcop.incomplete.maxsum.adssvp;

import edu.cqu.core.Message;

import java.lang.reflect.Constructor;

public class DSA implements LocalSearch {

    private static final double p = 0.4;

    private HostAgent agent;
    private int valueIndex;

    public DSA(HostAgent agent,int valueIndex) {
        this.agent = agent;
        this.valueIndex = valueIndex;
    }

    @Override
    public void initRun() {

    }

    @Override
    public void disposeMessage(Message message) {

    }

    @Override
    public void allMessageDisposed() {
        if (Math.random() < p){
            int minCost = Integer.MAX_VALUE;
            int bestValueIndex = -1;
            for (int i = 0; i < agent.getDomainLength(); i++){
                int cost = 0;
                for (int neighbourId : agent.getNeighbours()){
                    cost += agent.getConstraint(neighbourId)[i][agent.getNeighbourValue(neighbourId)];
                }
                if (cost < minCost){
                    minCost = cost;
                    bestValueIndex = i;
                }
            }
            valueIndex = bestValueIndex;
        }
    }

    @Override
    public int getValueIndex() {
        return valueIndex;
    }
}
