package edu.cqu.test;

import edu.cqu.core.FinishedListener;
import edu.cqu.core.ProgressChangedListener;
import edu.cqu.core.Solver;
import edu.cqu.result.Result;
import edu.cqu.result.ResultCycle;

import java.io.File;

/**
 * Created by dyc on 2017/6/29.
 */
public class Main {
    public static void main(String[] args){
        Solver solver = new Solver();


//        solver.solve("C:\\Users\\dyc\\Desktop\\am.xml", "DSA", "problem/RANDOMDCOP_120_10_density_0.1_0.xml", new FinishedListener() {
//            @Override
//            public void onFinished(Result result) {
//                ResultCycle resultCycle = (ResultCycle) result;
////                System.out.println(resultCycle.getTotalTime() + "ms");
//                int index = 1;
//                for (double i : resultCycle.costInCycle){
//                    System.out.println(index++ + "\t" + i);
//                }
//            }
//        },false,true);
        solver.batchSolve("C:\\Users\\dyc\\Desktop\\am.xml", "MaxsumADSSVP", "problem/dcop/sparse", 1, new FinishedListener() {
            @Override
            public void onFinished(Result result) {
                ResultCycle resultCycle = (ResultCycle) result;
//                System.out.println(resultCycle.getTotalTime() + "ms");
                int index = 1;
                for (double i : resultCycle.costInCycle){
                    System.out.println(index++ + "\t" + i);
                }
            }
        }, new ProgressChangedListener() {
            @Override
            public void onProgressChanged(double percentage) {
                System.out.println(percentage);
            }

            @Override
            public void interrupted(String reason) {
                System.out.println(reason);
            }
        });
    }
}
