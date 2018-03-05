package edu.cqu.benchmark.randomdcops;

public class RandomADCOPGenerator extends RandomDCOPGenerator {
    public RandomADCOPGenerator(String name, int nbAgent, int domainSize, int minCost, int maxCost, double density) {
        super(name, nbAgent, domainSize, minCost, maxCost, density);
        type = "ADCOP";
    }

    @Override
    protected String getTuple() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i <= domainSize; i++){
            for (int j = 1; j <= domainSize; j++){
                int cost1 = randomCost(i,j);
                int cost2 = randomCost(i,j);
                stringBuilder.append(cost1 + " " + cost2 + ":");
                stringBuilder.append(i + " " + j + "|");
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }
}
