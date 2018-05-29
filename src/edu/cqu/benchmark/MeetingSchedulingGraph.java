package edu.cqu.benchmark;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class MeetingSchedulingGraph extends AbstractGraph {
    String path;
    List<String> variables;
    Element relations;
    Element constraints;
    public MeetingSchedulingGraph(String path) {
        super("ms", 0, 0, 0, 0);
        this.path = path;
        variables = new LinkedList<>();
        try {
            parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parse() throws Exception{
        Element root = new SAXBuilder().build(new File(path)).getRootElement();
        parseDomain(root);
        parseVariables(root);
        parseRelation(root);
        parseConstraint(root);
    }

    private void parseDomain(Element root){
        Element domainElement = root.getChild("domains").getChild("domain");
        domainSize = Integer.parseInt(domainElement.getAttributeValue("nbValues"));
    }

    private void parseVariables(Element root){
        List<Element> variableElements = root.getChild("variables").getChildren("variable");
        for (Element ve : variableElements){
            variables.add(ve.getAttributeValue("name"));
            variables.add("dummy-" + ve.getAttributeValue("name"));
        }
        nbVariable = nbAgent = variables.size();
    }

    @Override
    public Element getVariables() {
        if (nbVariable != nbAgent)
            throw new IllegalArgumentException("variables not equals with Agents!");
        Element variableRootElement = new Element("variables");
        variableRootElement.setAttribute("nbVariables",String.valueOf(nbVariable));
        for (int i = 0; i < nbVariable; i++){
            Element variable = new Element("variable");
            variable.setAttribute("agent","A" + (i + 1));
            variable.setAttribute("name",variables.get(i));
            variable.setAttribute("id","1");
            variable.setAttribute("domain","D1");
            variable.setAttribute("description","variable " + (i + 1) + ".1");
            variableRootElement.addContent(variable);
        }
        return variableRootElement;
    }

    @Override
    public Element getConstraints() {
        return constraints;
    }

    @Override
    public Element getRelations() {
        return relations;
    }

    private void parseRelation(Element root){
        relations = new Element("relations");
        List<Element> relationElements = new LinkedList<>(root.getChild("relations").getChildren("relation"));
        root.getChild("relations").removeChildren("relation");
        for (Element re : relationElements){
            re.setAttribute("nbTuples",String.valueOf(domainSize * domainSize));
            if (re.getAttributeValue("arity").equals("1")){
                re.setAttribute("arity","2");
                re.setText(augmentText(re.getText()));
            }
            else {
                if (re.getAttributeValue("name").equals("NEQ")){
                    re.setText(augmentEQ(false));
                }
                else {
                    re.setText(augmentEQ(true));
                }
            }
            relations.addContent(re);
        }
        relations.setAttribute("nbVariables",relations.getChildren().size() + "");
    }

    private String augmentEQ(boolean eq){
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i <= domainSize; i++){
            for (int j = 1; j <= domainSize; j++){
                int cost;
                if (eq){
                    if (i == j){
                        cost = 0;
                    }
                    else {
                        cost = 10000;
                    }
                }
                else {
                    if (i != j){
                        cost = 0;
                    }
                    else {
                        cost = 10000;
                    }
                }
                stringBuilder.append(cost + ":" + i + " " + j + "|");
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    private String augmentText(String text){
        StringBuilder stringBuilder = new StringBuilder();
        String[] tuples = text.split("\\|");
        for (String t : tuples){
            for (int i = 1; i <= domainSize; i++){
                if (t.charAt(t.length() - 1) == ' ')
                    stringBuilder.append(t + i);
                else
                    stringBuilder.append(t + " " + i);
                stringBuilder.append("|");
            }

        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    private void parseConstraint(Element root){
        constraints = new Element("constraints");
        List<Element> constraintElements = new LinkedList<>(root.getChild("constraints").getChildren("constraint"));
        root.getChild("constraints").removeChildren("constraint");
        for (Element ce : constraintElements){
            ce.removeAttribute("agent");
            if (ce.getAttributeValue("arity").equals("1")){
                ce.setAttribute("arity","2");
                String variable = ce.getAttributeValue("scope");
                ce.setAttribute("scope",variable + " dummy-" + variable);
            }
            constraints.addContent(ce);
        }
        constraints.setAttribute("nbConstraints",String.valueOf(constraints.getChildren().size()));
    }

    @Override
    public void generateConstraint() {

    }
}
