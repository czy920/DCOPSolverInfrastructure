package edu.cqu.core;

import edu.cqu.gui.DOTrenderer;
import edu.cqu.parser.ProblemParser;

/**
 * Created by dyc on 2017/6/20.
 */
public class Solver {

    public void solve(String agentDescriptorPath,String agentType,String problemPath,FinishedListener listener,boolean showConstraintGraph,boolean showPesudoTree){
        ProblemParser parser = new ProblemParser(problemPath);
        Problem problem = parser.parse();
        AgentManager manager = new AgentManager(agentDescriptorPath,agentType,problem,listener,showPesudoTree);
        if (showConstraintGraph){
            new DOTrenderer("constraint",manager.getConstraintGraphDOTString(),"auxiliary/graphviz/bin/dot");
        }

        manager.startAgents();
    }
}
