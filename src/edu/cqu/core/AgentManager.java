package edu.cqu.core;

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

    private List<Agent> agents;
    private AsyncMailer asyncMailer;
    private SyncMailer syncMailer;
    private static Map<String,AgentDescriptor> agentDescriptors;

    public AgentManager(String agentDescriptorPath,String agentType,Problem problem,FinishedListener listener) {
        agents = new LinkedList<>();
        agentDescriptors = new AgentParser(agentDescriptorPath).parse();
        AgentDescriptor descriptor = agentDescriptors.get(agentType.toUpperCase());
        if (descriptor.method.equals(METHOD_ASYNC)){
            asyncMailer = new AsyncMailer(listener);
        }
        else {
            syncMailer = new SyncMailer(listener);
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
}
