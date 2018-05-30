package edu.cqu.benchmark.meetingscheduling;

import edu.cqu.benchmark.AbstractGraph;
import edu.cqu.core.CommunicationStructure;
import org.jdom2.Element;

import java.util.*;

public class MeetingScheduling extends AbstractGraph{
    private int nbAgents;
    private int nbMeetings;
    private int meetingsPerAgent;
    private int minTravelTime;
    private int maxTravelTime;
    private int timeSlots;
    private Map<Integer,List<Integer>> agentMeetings;
    private Map<Integer,List<Integer>> meetingAgents;
    private int[][] travelTime;
    private int[][] cost;

    public MeetingScheduling(int nbAgents,int nbMeetings,int meetingsPerAgent,int maxTravelTime,int minTravelTime,int timeSlots){
        super();
        this.nbAgents = nbAgents;
        name = "ms";
        this.nbMeetings = nbMeetings;
        this.meetingsPerAgent = meetingsPerAgent;
        this.maxTravelTime = maxTravelTime;
        this.minTravelTime = minTravelTime;
        domainSize = this.timeSlots = timeSlots;
        agentMeetings = new HashMap<>();
        meetingAgents = new HashMap<>();
        neqs = new LinkedList<>();
        initialize();
    }

    private void initialize(){
        for (int i = 1; i <= nbAgents; i++){
            int[] meetingIds = new int[nbMeetings];
            for (int j = 0; j < nbMeetings; j++){
                meetingIds[j] = j;
            }
            int tail = nbMeetings;
            for (int j = 0; j < meetingsPerAgent; j++){
                int meetingIndex = (int) (Math.random() * tail);
                int meetingId = meetingIds[meetingIndex];
                meetingIds[meetingIndex] = meetingIds[--tail];
                if (!agentMeetings.containsKey(i)){
                    agentMeetings.put(i,new LinkedList<>());
                }
                agentMeetings.get(i).add(meetingId);
                if (!meetingAgents.containsKey(meetingId)){
                    meetingAgents.put(meetingId,new LinkedList<>());
                }
                meetingAgents.get(meetingId).add(i);
            }
        }
        travelTime = new int[nbMeetings][nbMeetings];
        cost = new int[nbMeetings][nbMeetings];
        List<Integer> allMeetings = new LinkedList<>(meetingAgents.keySet());
        for (int i = 0; i < allMeetings.size(); i++){
            int m1 = allMeetings.get(i);
            for (int j = i + 1; j < allMeetings.size(); j++){
                Set<Integer> agents = new HashSet<>(meetingAgents.get(m1));
                int tt = (int) (Math.random() * (maxTravelTime - minTravelTime + 1)) + minTravelTime;
                int m2 = allMeetings.get(j);
                travelTime[m2][m1] = travelTime[m1][m2] = tt;
                agents.addAll(meetingAgents.get(m2));
                cost[m2][m1] = cost[m1][m2] = 1;
//                cost[m2][m1] = cost[m1][m2] = agents.size();
            }
        }

    }

    public Element getAgents(){
        Element agentRootElement = new Element("agents");
        agentRootElement.setAttribute("nbAgents",String.valueOf(nbAgents * meetingsPerAgent));
        int ind = 1;
        for (int i = 1; i <= nbAgents; i++){
            for (int meetingId : agentMeetings.get(i)){
                Element agent = new Element("agent");
                agent.setAttribute("name","A" + i + "_M" + meetingId);
                agent.setAttribute("id",String.valueOf(ind++));
                agent.setAttribute("description","Agent " + i);
                agentRootElement.addContent(agent);
            }
        }
        return agentRootElement;
    }

    public Element getVariables(){
        Element variableRootElement = new Element("variables");
        variableRootElement.setAttribute("nbVariables",String.valueOf(nbAgents * meetingsPerAgent));
        int ind = 1;
        for (int i = 1; i <= nbAgents; i++){
            for (int meetingId : agentMeetings.get(i)) {
                Element variable = new Element("variable");
                variable.setAttribute("agent", "A" + i + "_M" + meetingId);
                variable.setAttribute("name", "X" + i + "_M" + meetingId);
                variable.setAttribute("id", "1");
                variable.setAttribute("domain", "D1");
                variable.setAttribute("description", "variable " + String.valueOf(ind++));
                variableRootElement.addContent(variable);
            }
        }
        return variableRootElement;
    }
    private List<int[]> neqs;
    public Element getConstraints(){
        Element allConstraint = new Element("constraints");
        nbConstraint = 1;
        // intra links
        for (int id : agentMeetings.keySet()){
            List<Integer> meetings = agentMeetings.get(id);
            for (int i = 0; i < meetings.size(); i++) {
                for (int j = i + 1; j < meetings.size(); j++){
                    int m1 = meetings.get(i);
                    int m2 = meetings.get(j);
                    int[] neq = new int[]{m1,m2};
                    neqs.add(neq);
                    Element constaint = new Element("constraint");
                    constaint.setAttribute("name", "C" + String.valueOf(nbConstraint));
                    constaint.setAttribute("model", "TKC");
                    constaint.setAttribute("arity", "2");
                    constaint.setAttribute("scope", "X" + id + "_M" + m1 + " X" + id + "_M" + m2);
                    constaint.setAttribute("reference", "R" + String.valueOf(nbConstraint++));
                    allConstraint.addContent(constaint);
                }
            }
        }
        // inter links
        for (int meetingId : meetingAgents.keySet()){
            List<Integer> ids = meetingAgents.get(meetingId);
            for (int i = 0; i < ids.size(); i++){
                for (int j = i + 1; j < ids.size(); j++){
                    int id1 = ids.get(i);
                    int id2 = ids.get(j);
                    Element constaint = new Element("constraint");
                    constaint.setAttribute("name", "C" + String.valueOf(nbConstraint));
                    constaint.setAttribute("model", "TKC");
                    constaint.setAttribute("arity", "2");
                    constaint.setAttribute("scope", "X" + id1 + "_M" + meetingId + " X" + id2 + "_M" + meetingId);
                    constaint.setAttribute("reference", "R" + String.valueOf(nbConstraint++));
                    allConstraint.addContent(constaint);
                }
            }
        }
        allConstraint.setAttribute("nbConstraints",String.valueOf(--nbConstraint));
        return allConstraint;
    }
    public Element getRelations(){
        Element allRelations = new Element("relations");
        nbRelation = 1;
        //intra
        for (int[] neq : neqs){
            Element relation = new Element("relation");
            relation.setAttribute("name","R" + String.valueOf(nbRelation++));
            relation.setAttribute("arity","2");
            relation.setAttribute("nbTuples",String.valueOf(domainSize * domainSize));
            relation.setAttribute("semantics","soft");
            relation.setAttribute("defaultCost","infinity");
            relation.addContent(getTuple(neq));
            allRelations.addContent(relation);
        }
        //inter
        for (;nbRelation <= nbConstraint; nbRelation++){
            Element relation = new Element("relation");
            relation.setAttribute("name","R" + String.valueOf(nbRelation));
            relation.setAttribute("arity","2");
            relation.setAttribute("nbTuples",String.valueOf(domainSize * domainSize));
            relation.setAttribute("semantics","soft");
            relation.setAttribute("defaultCost","infinity");
            relation.addContent(getTuple());
            allRelations.addContent(relation);
        }
        allRelations.setAttribute("nbRelations",String.valueOf(--nbRelation));
        return allRelations;
    }

    protected String getTuple(int[] neq) {
        StringBuilder stringBuilder = new StringBuilder();
        int[] pref = new int[domainSize];
        for (int i = 0; i < domainSize; i++){
            pref[i] = (int) (Math.random() * 2);
        }
        for (int i = 1; i <= domainSize; i++){
            for (int j = 1; j <= domainSize; j++){
                int c = Math.abs(i - j) > travelTime[neq[0]][neq[1]] ? pref[i - 1] + pref[j - 1] : cost[neq[0]][neq[1]];
//                c += ;
                stringBuilder.append(c + ":");
                stringBuilder.append(i + " " + j + "|");
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    @Override
    protected String getTuple() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i <= domainSize; i++){
            for (int j = 1; j <= domainSize; j++){
                int cost = i == j ? 0 : 10000;
                stringBuilder.append(cost + ":");
                stringBuilder.append(i + " " + j + "|");
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    @Override
    public void generateConstraint() {

    }


}
