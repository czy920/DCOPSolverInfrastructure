package edu.cqu.result;

import edu.cqu.result.annotations.AverageField;

public class ResultAls extends ResultCycle {
    @AverageField
    public double[] bestCostInCycle;

    public ResultAls(){
        bestCostInCycle = new double[0];
    }

    public void setBestCostInCycle(double[] bestCostInCycle, int tail) {
        int zeroLength = 0;
        int infinityLength = 0;
        for (double cost : bestCostInCycle){
            if (cost < 1e-5){
                zeroLength++;
            }
            else if (cost > 1e8){
                infinityLength++;
            }
            else {
                break;
            }
        }
         this.bestCostInCycle = new double[tail - zeroLength - infinityLength];
        int index = 0;
         for (int i = 0; i < tail; i++){
             if (bestCostInCycle[i] < 1e-5 || bestCostInCycle[i] > 1e8){
                 continue;
             }
             this.bestCostInCycle[index++] = bestCostInCycle[i];
         }
         double[] costInCycle = new double[tail - zeroLength - infinityLength];
        index = 0;
        for (int i = 0; i < this.costInCycle.length; i++){
            if (i <= zeroLength){
                continue;
            }
            if (i > this.costInCycle.length - infinityLength){
                break;
            }
            costInCycle[index++] = this.costInCycle[i];
        }
        this.costInCycle = costInCycle;
    }
}
