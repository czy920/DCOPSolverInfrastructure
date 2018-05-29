package edu.cqu.benchmark.randomdcops;

public class AMaxDCSPGenerator extends RandomADCOPGenerator {

    private double p2;
    public AMaxDCSPGenerator(String name, int nbAgent, int domainSize, int minCost, int maxCost, double density,double p2) {
        super(name, nbAgent, domainSize, minCost, maxCost, density);
        this.p2 = p2;
    }

    @Override
    protected String getTuple() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i <= domainSize; i++){
            for (int j = 1; j <= domainSize; j++){
                int cost1 = 0;
                int cost2 = 0;
                if (Math.random() < p2){
                    cost1 = 1;
                }
                if (Math.random() < p2){
                    cost2 = 1;
                }
                stringBuilder.append(cost1 + " " + cost2 + ":");
                stringBuilder.append(i + " " + j + "|");
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }
}
