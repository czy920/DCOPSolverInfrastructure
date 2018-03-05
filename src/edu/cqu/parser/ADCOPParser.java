package edu.cqu.parser;

import edu.cqu.core.Problem;
import org.jdom2.Element;

import java.util.HashMap;
import java.util.Map;

public class ADCOPParser extends Parser {

    public ADCOPParser(Element rootElement, Problem problem) {
        super(rootElement, problem);
    }

    @Override
    protected void processTuple(String tuple, String constraintName) {
        String[] tuples = tuple.split("\\|");
        AgentPair pair = constraintInfo.get(constraintName);
        int[][] formerConstraintCost = new int[problem.domains.get(pair.former).length][problem.domains.get(pair.latter).length];
        int[][] latterConstraintCost = new int[problem.domains.get(pair.latter).length][problem.domains.get(pair.former).length];
        for (String t : tuples){
            String[] info = t.split("[:| ]");
            int formerValue = Integer.parseInt(info[2]) - 1;
            int latterValue = Integer.parseInt(info[3]) - 1;
            int formerCost = Integer.parseInt(info[0]);
            int latterCost = Integer.parseInt(info[1]);
            formerConstraintCost[formerValue][latterValue] = formerCost;
            latterConstraintCost[latterValue][formerValue] = latterCost;
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
}
