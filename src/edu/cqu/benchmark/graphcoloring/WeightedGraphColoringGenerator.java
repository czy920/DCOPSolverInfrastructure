package edu.cqu.benchmark.graphcoloring;

import edu.cqu.benchmark.randomdcops.RandomDCOPGenerator;

/**
 * Created by dyc on 2017/6/12.
 */
public class WeightedGraphColoringGenerator extends RandomDCOPGenerator {
    public WeightedGraphColoringGenerator(String name, int nbAgent, int domainSize, int minCost,int maxCost,double density) {
        super(name, nbAgent, domainSize,minCost,maxCost, density);
    }

    @Override
    protected int randomCost(int i, int j) {
        return i == j ? super.randomCost(i,j) : 0;
    }
}
