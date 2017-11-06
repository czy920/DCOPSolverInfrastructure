package edu.cqu.parser;

import edu.cqu.core.Problem;
import org.jdom2.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by dyc on 2017/6/18.
 */
public class Parser {
    protected Element rootElement;
    protected Problem problem;
    protected Map<String,int[]> domains;
    protected Map<String,Integer> agentNameId;
    protected Map<String,AgentPair> constraintInfo;
    protected Map<String,Integer> variableNameAgentId;

    public Parser(Element rootElement, Problem problem) {
        this.rootElement = rootElement;
        this.problem = problem;
        domains = new HashMap<>();
        agentNameId = new HashMap<>();
        constraintInfo = new HashMap<>();
        variableNameAgentId = new HashMap<>();
    }

    public void parseContent(){
        parseAgents();
        parseDomain();
        parseVariables();
        parseConstraints();
    }

    protected void parseAgents(){
        List<Element> agentElements = rootElement.getChild("agents").getChildren("agent");
        problem.allId = new int[agentElements.size()];
        int index = 0;
        for (Element agentElement : agentElements){
            int id = Integer.parseInt(agentElement.getAttributeValue("id"));
            String name = agentElement.getAttributeValue("name");
            problem.allId[index++] = id;
            agentNameId.put(name,id);
        }
    }

    protected void parseVariables(){
        List<Element> variableElements = rootElement.getChild("variables").getChildren("variable");
        problem.domains = new HashMap<>();
        for (Element variableElement : variableElements){
            String domain = variableElement.getAttributeValue("domain");
            String agentName = variableElement.getAttributeValue("agent");
            String name = variableElement.getAttributeValue("name");
            variableNameAgentId.put(name,agentNameId.get(agentName));
            problem.domains.put(agentNameId.get(agentName),domains.get(domain));
        }
    }

    protected void parseDomain(){
        List<Element> domainElements = rootElement.getChild("domains").getChildren("domain");
        for (Element domainElement : domainElements){
            String name = domainElement.getAttributeValue("name");
            int[] domain = new int[Integer.parseInt(domainElement.getAttributeValue("nbValues"))];
            for (int i = 0; i < domain.length; i++){
                domain[i] = i + 1;
            }
            domains.put(name,domain);
        }
    }

    protected void parseConstraints(){
        problem.constraintCost = new HashMap<>();
        problem.neighbours = new HashMap<>();
        List<Element> constraintElements = rootElement.getChild("constraints").getChildren("constraint");
        for (Element constraintElement : constraintElements){
            String constraintName = constraintElement.getAttributeValue("reference");
            AgentPair agentPair = new AgentPair(constraintElement.getAttributeValue("scope"));
            constraintInfo.put(constraintName,agentPair);
        }
        constraintElements = rootElement.getChild("relations").getChildren("relation");
        for (Element constraintElement : constraintElements){
            String name = constraintElement.getAttributeValue("name");
            processTuple(constraintElement.getText(),name);
        }
        for (int agentId : problem.allId){
            Set<Integer> neighbours = problem.constraintCost.get(agentId).keySet();
            int[] neighbourArray = new int[neighbours.size()];
            int index = 0;
            for (int neighbourId : neighbours){
                neighbourArray[index++] = neighbourId;
            }
            problem.neighbours.put(agentId,neighbourArray);
        }
    }

    protected void processTuple(String tuple,String constraintName){
        String[] tuples = tuple.split("\\|");
        AgentPair pair = constraintInfo.get(constraintName);
        int[][] formerConstraintCost = new int[problem.domains.get(pair.former).length][problem.domains.get(pair.latter).length];
        int[][] latterConstraintCost = new int[problem.domains.get(pair.latter).length][problem.domains.get(pair.former).length];
        for (String t : tuples){
            String[] info = t.split("[:| ]");
            int formerValue = Integer.parseInt(info[1]) - 1;
            int latterValue = Integer.parseInt(info[2]) - 1;
            int cost = Integer.parseInt(info[0]);
            formerConstraintCost[formerValue][latterValue] = cost;
            latterConstraintCost[latterValue][formerValue] = cost;
        }
        Map<Integer,int[][]> constraintCost = problem.constraintCost.get(pair.former);
        if (constraintCost == null){
            constraintCost = new HashMap<>();
            problem.constraintCost.put(pair.former,constraintCost);
        }
        constraintCost.put(pair.latter,formerConstraintCost);
        constraintCost = problem.constraintCost.get(pair.latter);
        if (constraintCost == null){
            constraintCost = new HashMap<>();
            problem.constraintCost.put(pair.latter,constraintCost);
        }
        constraintCost.put(pair.former,latterConstraintCost);
    }

    protected class AgentPair{
        int former;
        int latter;

        public AgentPair(String scope){
            String[] ids = scope.split(" ");
            if (ids.length != 2){
                throw new IllegalArgumentException();
            }
            former = variableNameAgentId.get(ids[0]);
            latter = variableNameAgentId.get(ids[1]);
        }
    }
}
