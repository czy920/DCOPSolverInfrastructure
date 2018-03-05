package edu.cqu.core;

import edu.cqu.gui.DOTrenderer;
import edu.cqu.parser.ProblemParser;
import edu.cqu.result.Result;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by dyc on 2017/6/20.
 */
public class Solver {
    private AtomicBoolean isSolving;
    private Thread threadMonitor;

    public Solver(){
        isSolving = new AtomicBoolean(false);
    }

    public void solve(String agentDescriptorPath,String agentType,String problemPath,FinishedListener listener,boolean showConstraintGraph,boolean showPesudoTree){
        ProblemParser parser = new ProblemParser(problemPath);
        Problem problem = parser.parse();
        AgentManager manager = new AgentManager(agentDescriptorPath,agentType,problem,listener,showPesudoTree);
        if (showConstraintGraph){
            new DOTrenderer("constraint",manager.getConstraintGraphDOTString(),"auxiliary/graphviz/bin/dot");
        }
        manager.startAgents();
    }

    public void solve(String agentDescriptorPath,String agentType,String problemPath,FinishedListener listener,boolean showConstraintGraph,boolean showPesudoTree,AgentIteratedOverListener agentIteratedOverListener){
        ProblemParser parser = new ProblemParser(problemPath);
        Problem problem = parser.parse();
        AgentManager manager = new AgentManager(agentDescriptorPath,agentType,problem,listener,showPesudoTree);
        manager.addAgentIteratedOverListener(agentIteratedOverListener);
        if (showConstraintGraph){
            new DOTrenderer("constraint",manager.getConstraintGraphDOTString(),"auxiliary/graphviz/bin/dot");
        }
        manager.startAgents();
    }

    public void batchSolve(String agentDescriptorPath,String agentType,String problemDir,int validateTime,FinishedListener listener,ProgressChangedListener progressChangedListener){
        File dir = new File(problemDir);
        List<File> acceptedProblemFiles = new LinkedList<>();
        for (File file : dir.listFiles()){
            if (file.isDirectory()){
                continue;
            }
            if (file.getName().endsWith(".xml")){
                acceptedProblemFiles.add(file);
            }
        }
        String[] problemPaths = new String[acceptedProblemFiles.size()];
        for (int i = 0; i < acceptedProblemFiles.size(); i++){
            problemPaths[i] = acceptedProblemFiles.get(i).getAbsolutePath();
        }
        new Thread(new Monitor(agentDescriptorPath,agentType,problemPaths,listener,progressChangedListener,validateTime)).start();
    }

    private class EachProblemFinishedListener implements FinishedListener{
        Result[] problemResults;
        int validateTime;
        int currentValidateTime;
        int currentProblemIndex;
        Result validatingResult;
        ProgressChangedListener progressChangedListener;

        public EachProblemFinishedListener(int validateTime,int problemCount,ProgressChangedListener progressChangedListener) {
            this.validateTime = validateTime;
            problemResults = new Result[problemCount];
            this.progressChangedListener = progressChangedListener;
        }

        @Override
        public void onFinished(Result result) {
            if (validatingResult == null){
                validatingResult = result;
            }
            else {
                validatingResult.add(result);
            }
            currentValidateTime++;
            progressChangedListener.onProgressChanged((1.0 * currentProblemIndex * validateTime + currentValidateTime) / (problemResults.length * validateTime),result);
            if (currentValidateTime == validateTime){
                currentValidateTime = 0;
                validatingResult.average(validateTime);
                problemResults[currentProblemIndex] = validatingResult;
                currentProblemIndex++;
                validatingResult = null;
            }
            synchronized (isSolving){
                isSolving.set(false);
                isSolving.notify();
            }
        }

        public Result getAveragedResult(){
            Result result = problemResults[0];
            for (int i = 1; i < problemResults.length; i++){
                result.add(problemResults[i]);
            }
            result.average(problemResults.length);
            return result;
        }
    }

    private class Monitor implements Runnable{
        String agentDescriptorPath;
        String agentType;
        String[] problemPaths;
        FinishedListener finishedListener;
        ProgressChangedListener progressChangedListener;
        EachProblemFinishedListener eachProblemFinishedListener;
        int validateTime;
        int currentValidateTime;
        int currentProblemIndex;


        public Monitor(String agentDescriptorPath, String agentType, String[] problemPaths, FinishedListener finishedListener, ProgressChangedListener progressChangedListener,int validateTime) {
            this.agentDescriptorPath = agentDescriptorPath;
            this.agentType = agentType;
            this.problemPaths = problemPaths;
            this.finishedListener = finishedListener;
            this.progressChangedListener = progressChangedListener;
            eachProblemFinishedListener = new EachProblemFinishedListener(validateTime,problemPaths.length,progressChangedListener);
            this.validateTime = validateTime;
        }

        @Override
        public void run() {
            while (true){
                synchronized (isSolving){
                    while (isSolving.get()){
                        try {
                            isSolving.wait();
                        } catch (InterruptedException e) {
                            if (progressChangedListener != null){
                                progressChangedListener.interrupted(e.toString());
                            }
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        progressChangedListener.interrupted(e.toString());
                    }
                    if (++currentValidateTime == validateTime + 1){
                        currentValidateTime = 1;
                        currentProblemIndex++;
                    }
                    if (currentProblemIndex == problemPaths.length){
                        break;
                    }
                    isSolving.set(true);
                    solve(agentDescriptorPath,agentType,problemPaths[currentProblemIndex],eachProblemFinishedListener,false,false);
                }
            }
            if (finishedListener != null){
                finishedListener.onFinished(eachProblemFinishedListener.getAveragedResult());
            }
        }
    }
}
