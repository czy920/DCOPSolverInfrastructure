package edu.cqu.result;

import edu.cqu.result.annotations.AverageField;

public class ResultAls extends ResultCycle {
    @AverageField
    public double[] bestCostInCycle;

    public ResultAls(){
        bestCostInCycle = new double[0];
    }

    public void setBestCostInCycle(double[] bestCostInCycle, int tail) {
         this.bestCostInCycle = new double[tail];
         for (int i = 0; i < tail; i++){
             this.bestCostInCycle[i] = bestCostInCycle[i];
         }
    }
}
