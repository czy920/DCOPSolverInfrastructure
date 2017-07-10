package edu.cqu.benchmark;


import org.jdom2.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by YanChenDeng on 2016/4/18.
 */
public abstract class AbstractGraph {
    protected String name;
    protected String type = "DCOP";
    protected String benchmark = "RandomDCOP";
    protected String model = "Simple";
    protected String constraintModel = "TKC";
    protected String format = "XDisCSP 1.0";
    protected int nbAgent;
    protected int nbVariable;
    protected int nbConstraint;
    protected int nbRelation;
    protected int domainSize;
    protected int density;
    protected int tightness;
    protected int nbTuple;
    protected int minCost;
    protected int maxCost;
    protected List<Integer> source;
    protected List<Integer> dest;
    protected Random random;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getBenchmark() {
        return benchmark;
    }

    public String getModel() {
        return model;
    }

    public String getConstraintModel() {
        return constraintModel;
    }

    public String getFormat() {
        return format;
    }

    public int getNbAgent() {
        return nbAgent;
    }

    public int getNbVariable() {
        return nbVariable;
    }

    public int getNbConstraint() {
        return nbConstraint;
    }

    public int getNbRelation() {
        return nbRelation;
    }

    public int getDomainSize() {
        return domainSize;
    }

    public int getDensity() {
        return density;
    }

    public int getTightness() {
        return tightness;
    }

    public int getNbTuple() {
        return nbTuple;
    }

    public int getMinCost() {
        return minCost;
    }

    public int getMaxCost() {
        return maxCost;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setBenchmark(String benchmark) {
        this.benchmark = benchmark;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setConstraintModel(String constraintModel) {
        this.constraintModel = constraintModel;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setNbAgent(int nbAgent) {
        this.nbAgent = nbAgent;
    }

    public void setNbVariable(int nbVariable) {
        this.nbVariable = nbVariable;
    }

    public void setNbConstraint(int nbConstraint) {
        this.nbConstraint = nbConstraint;
    }

    public void setNbRelation(int nbRelation) {
        this.nbRelation = nbRelation;
    }

    public void setDomainSize(int domainSize) {
        this.domainSize = domainSize;
    }

    public void setDensity(int density) {
        this.density = density;
    }

    public void setTightness(int tightness) {
        this.tightness = tightness;
    }

    public void setNbTuple(int nbTuple) {
        this.nbTuple = nbTuple;
    }

    public void setMinCost(int minCost) {
        this.minCost = minCost;
    }

    public void setMaxCost(int maxCost) {
        this.maxCost = maxCost;
    }

    public AbstractGraph(String name, int nbAgent, int domainSize, int minCost, int maxCost) {
        this.name = name;
        this.nbAgent = nbAgent;
        this.nbVariable = nbAgent;
        this.domainSize = domainSize;
        this.minCost = minCost;
        this.maxCost = maxCost;
        this.source = new ArrayList<Integer>();
        this.dest = new ArrayList<Integer>();
        this.random = new Random();
    }

    public Element getAgents(){
        Element agentRootElement = new Element("agents");
        agentRootElement.setAttribute("nbAgents",String.valueOf(nbAgent));
        for (int i = 1; i <= nbAgent; i++){
            Element agent = new Element("agent");
            agent.setAttribute("name","A" + i);
            agent.setAttribute("id",String.valueOf(i));
            agent.setAttribute("description","Agent " + i);
            agentRootElement.addContent(agent);
        }
        return agentRootElement;
    }

    public abstract void generateConstraint();

    public Element getDomains(){
        Element domains = new Element("domains");
        domains.setAttribute("nbDomains","1");
        Element domain = new Element("domain");
        domain.setAttribute("name","D1");
        domain.setAttribute("nbValues",String.valueOf(domainSize));
        domain.addContent("1.." + domainSize);
        domains.addContent(domain);
        return domains;
    }
    public Element getVariables(){
        if (nbVariable != nbAgent)
            throw new IllegalArgumentException("variables not equals with Agents!");
        Element variableRootElement = new Element("variables");
        variableRootElement.setAttribute("nbVariables",String.valueOf(nbVariable));
        for (int i = 0; i < nbVariable; i++){
            Element variable = new Element("variable");
            variable.setAttribute("agent","A" + (i + 1));
            variable.setAttribute("name","X" + (i + 1) + ".1");
            variable.setAttribute("id","1");
            variable.setAttribute("domain","D1");
            variable.setAttribute("description","variable " + (i + 1) + ".1");
            variableRootElement.addContent(variable);
        }
        return variableRootElement;
    }

    public Element getConstraints(){
        Element allConstraint = new Element("constraints");
        allConstraint.setAttribute("nbConstraints",String.valueOf(nbConstraint));
        for (int i = 0; i < nbConstraint; i++){
            Element constaint = new Element("constraint");
            constaint.setAttribute("name","C" + i);
            constaint.setAttribute("model","TKC");
            constaint.setAttribute("arity","2");
            constaint.setAttribute("scope","X" + source.get(i) + ".1 X" + dest.get(i) + ".1");
            constaint.setAttribute("reference","R" + i);
            allConstraint.addContent(constaint);
        }
        return allConstraint;
    }

    public Element getRelations(){
        Element allRelations = new Element("relations");
        allRelations.setAttribute("nbRelations",String.valueOf(nbRelation));
        for (int i = 0; i < nbRelation; i++){
            Element relation = new Element("relation");
            relation.setAttribute("name","R" + i);
            relation.setAttribute("arity","2");
            relation.setAttribute("nbTuples",String.valueOf(domainSize * domainSize));
            relation.setAttribute("semantics","soft");
            relation.setAttribute("defaultCost","infinity");
            relation.addContent(getTuple());
            allRelations.addContent(relation);
        }
        return allRelations;
    }

    protected String getTuple(){
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i <= domainSize; i++){
            for (int j = 1; j <= domainSize; j++){
                int cost = randomCost(i,j);
                stringBuilder.append(cost + ":");
                stringBuilder.append(i + " " + j + "|");
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    protected int randomCost(int i,int j){
        return minCost + (int)(random.nextDouble() * (maxCost - minCost + 1));
    }

    public Element getPresentation(){
        Element presentation = new Element("presentation");
        presentation.setAttribute("name",name);
        presentation.setAttribute("type",type);
        presentation.setAttribute("benchmark",benchmark);
        presentation.setAttribute("model",model);
        presentation.setAttribute("constraintModel",constraintModel);
        presentation.setAttribute("format",format);
        return presentation;
    }

    public Element getGuiPresentation(){
        Element guiPresentation = new Element("GuiPresentation");
        guiPresentation.setAttribute("type",type);
        guiPresentation.setAttribute("benchmark",benchmark);
        guiPresentation.setAttribute("name",name);
        guiPresentation.setAttribute("model","TKC");
        guiPresentation.setAttribute("nbAgents",String.valueOf(nbAgent));
        guiPresentation.setAttribute("domainSize",String.valueOf(domainSize));
        guiPresentation.setAttribute("density",String.valueOf(density));
        guiPresentation.setAttribute("tightness",String.valueOf(tightness));
        guiPresentation.setAttribute("nbConstraints",String.valueOf(nbConstraint));
        guiPresentation.setAttribute("nbTuples",String.valueOf(domainSize * domainSize));

        return guiPresentation;
    }

}
