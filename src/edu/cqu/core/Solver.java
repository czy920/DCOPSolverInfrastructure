package edu.cqu.core;

import edu.cqu.parser.ProblemParser;

/**
 * Created by dyc on 2017/6/20.
 */
public class Solver {

    public void solve(String agentDescriptorPath,String agentType,String problemPath,FinishedListener listener){
        ProblemParser parser = new ProblemParser(problemPath);
        Problem problem = parser.parse();
        AgentManager manager = new AgentManager(agentDescriptorPath,agentType,problem,listener);
        manager.startAgents();
    }
}
