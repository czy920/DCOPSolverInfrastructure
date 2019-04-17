package edu.cqu.algorithms.dcop.incomplete.maxsum.advp;

import edu.cqu.algorithms.dcop.incomplete.maxsum.VariableNode;

import java.util.LinkedList;
import java.util.List;

public class KDepthVariableNode extends VariableNode {

    private int K;

    public KDepthVariableNode(int[] neighbours, int domainLength, int k) {
        super(neighbours, domainLength);
        K = k;
    }

    public int argMin(int l){
        if (K < l){
            return argMin();
        }
        double[] utility = getBelief();
        List<Integer> accepted = new LinkedList<>();

        for (int i = 0; i < K - l; i++){
            int pending = -1;
            double runningMin = Integer.MAX_VALUE;
            for (int j = 0; j < utility.length; j++){
                if (accepted.contains(j)){
                    continue;
                }
                if (utility[j] < runningMin){
                    runningMin = utility[j];
                    pending = j;
                }
            }
            accepted.add(pending);
        }
        return accepted.get((int) (Math.random() * accepted.size()));
    }
}
