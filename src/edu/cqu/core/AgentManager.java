package edu.cqu.core;

import edu.cqu.gui.DOTrenderer;
import edu.cqu.ordering.DFSSyncAgent;
import edu.cqu.parser.AgentParser;
import edu.cqu.test.TestAsyncAgent;
import edu.cqu.test.TestSyncAgent;

import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by dyc on 2017/6/16.
 */
public class AgentManager {

    private static final String METHOD_ASYNC = "ASYNC";
    private static final String METHOD_SYNC = "SYNC";

    private static final String CONFIG_KEY_PRINT_CYCLE = "PRINTCYCLE";
    private static final String CONFIG_KEY_SUPPRESS_OUTPUT = "SUPPRESSOUTPUT";

    private List<Agent> agents;
    private AsyncMailer asyncMailer;
    private SyncMailer syncMailer;
    private static Map<String,AgentDescriptor> agentDescriptors;

    public AgentManager(String agentDescriptorPath,String agentType,Problem problem,FinishedListener listener,boolean showPesudoTreeGraph) {
        agents = new LinkedList<>();
        AgentParser agentParser = new AgentParser(agentDescriptorPath);
        agentDescriptors = agentParser.parse();
        if (agentDescriptors.size() == 0){
            throw new RuntimeException("No agent is defined in manifest");
        }
        Map<String,String> configurations = agentParser.parseConfigurations();
        AgentDescriptor descriptor = agentDescriptors.get(agentType.toUpperCase());
        boolean suppressOutput = false;
        if (descriptor.method.equals(METHOD_ASYNC)){
            asyncMailer = new AsyncMailer(listener);
        }
        else {
            syncMailer = new SyncMailer(listener);
            if (showPesudoTreeGraph){
                syncMailer.registerCycleListener(new ShowPesudoTreeGraph());
            }
            if (configurations.containsKey(CONFIG_KEY_PRINT_CYCLE)){
                if (configurations.get(CONFIG_KEY_PRINT_CYCLE).equals("TRUE")){
                    syncMailer.setPrintCycle(true);
                }
            }
            if (configurations.containsKey(CONFIG_KEY_SUPPRESS_OUTPUT)){
                if (configurations.get(CONFIG_KEY_SUPPRESS_OUTPUT).equals("TRUE")){
                    suppressOutput = true;
                }
            }
        }
        for (int id : problem.allId){
            Agent agent = null;
            try {
                Class clazz = Class.forName(descriptor.className); //  反射
                Constructor constructor = clazz.getConstructors()[0];
                agent = (Agent) constructor.newInstance(id,problem.domains.get(id),problem.neighbours.get(id),problem.constraintCost.get(id),problem.getNeighbourDomain(id),
                        syncMailer == null ? asyncMailer : syncMailer);
            } catch (Exception e) {
                throw new RuntimeException("init exception");
            }
            agent.setSuppressOutput(suppressOutput);
            agents.add(agent);
        }
    }

    public void startAgents(){
        for (Agent agent : agents){
            agent.startProcess();
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (asyncMailer != null){
            asyncMailer.startProcess();
        }
        else if (syncMailer != null){
            syncMailer.startProcess();
        }
    }

    public String getConstraintGraphDOTString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("graph constraint{\n");
        for (int i = 0; i < agents.size(); i++){
            Agent agent = agents.get(i);
            for (int neighbourId : agent.neighbours){
                if (neighbourId > agent.id){
                    stringBuilder.append("X" + agent.id + " -- " + "X" + neighbourId + ";\n");
                }
            }
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
    public String getPseudoTreeGraphDOTString(){
        StringBuilder stringBuilder = new StringBuilder();
        if (!(agents.get(0) instanceof DFSSyncAgent)){
            return "";
        }
        stringBuilder.append("digraph pesudoTree{\n");
        for (int i = 0; i < agents.size(); i++){
            DFSSyncAgent agent = (DFSSyncAgent) agents.get(i);
            stringBuilder.append(agent.toDOTString());
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private class ShowPesudoTreeGraph implements SyncMailer.CycleListener{
        @Override
        public void onCycleChanged(int cycle) {
            if (cycle == 2 * agents.size()){
                String dot = getPseudoTreeGraphDOTString();
                if (!dot.equals("")){
                    new DOTrenderer("constraint",dot,"auxiliary/graphviz/bin/dot");
                }
            }
        }
    }
}
