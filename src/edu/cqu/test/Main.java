package edu.cqu.test;

import edu.cqu.core.FinishedListener;
import edu.cqu.core.Solver;
import edu.cqu.result.Result;
import edu.cqu.result.ResultCycle;

/**
 * Created by dyc on 2017/6/29.
 */
public class Main {
    public static void main(String[] args){
        Solver solver = new Solver();
        solver.solve("agentManifest.xml", "DSA", "problems/RandomDCOP_120_10_1.xml", new FinishedListener() {
            @Override
            public void onFinished(Result result) {
                ResultCycle resultCycle = (ResultCycle) result;
                for (double i : resultCycle.costInCycle){
                    System.out.println(i);
                }
            }
        });
    }
}
