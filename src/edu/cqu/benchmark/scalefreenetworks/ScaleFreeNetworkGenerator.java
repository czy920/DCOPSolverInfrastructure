package edu.cqu.benchmark.scalefreenetworks;

import edu.cqu.benchmark.AbstractGraph;

import java.util.*;

/**
 * Created by YanChenDeng on 2016/4/20.
 */
public class ScaleFreeNetworkGenerator extends AbstractGraph {

    public static final String EXTRA_PARAMETER_M1 = "m1";
    public static final String EXTRA_PARAMETER_M2 = "m2";

    private int m1;
    private int m2;
    private Map<Integer,Integer> edgeCounter;
    private Random random;
    private int[] sampled;
    private Set<Integer> connected;

    public ScaleFreeNetworkGenerator(String name, int nbAgent, int domainSize, int minCost, int maxCost,int m1,int m2) {
        super(name, nbAgent, domainSize, minCost, maxCost);
        if (m2 > m1)
            throw new IllegalArgumentException("m2 can't greater than m1");
        this.m1 = m1;
        this.m2 = m2;
        edgeCounter = new HashMap<Integer,Integer>();
        random = new Random();
        connected = new HashSet<Integer>();
    }

    private void generateInitConnectedGraph(){
        int[] agents = new int[nbAgent];
        sampled = new int[m1];
        for (int i = 0; i < nbAgent; i++){
            agents[i] = i + 1;
            edgeCounter.put(i + 1, 0);
        }

        for (int i = 0; i < m1; i++){
            int index = random.nextInt(nbAgent - i);
            sampled[i] = agents[index];
            agents[index] = agents[agents.length - 1 - i];
        }

     
        for (int i = 0; i < m1 - 1; i++){
        	
            source.add(sampled[i] < sampled[i + 1] ? sampled[i] :sampled[i + 1]);
            dest.add(sampled[i] > sampled[i + 1] ? sampled[i] :sampled[i + 1]);
            edgeCounter.put(sampled[i],edgeCounter.get(sampled[i]) + 1);
            edgeCounter.put(sampled[i + 1],edgeCounter.get(sampled[i + 1]) + 1);
            nbConstraint++;
            nbRelation++;
            connected.add(sampled[i]);
            connected.add(sampled[i + 1]);
        }
    }

    @Override
    public void generateConstraint() {
        generateInitConnectedGraph();

        ext:for (int i = 1; i <= nbAgent; i++){
            for (int id : sampled)
                if (id == i)
                    continue ext;
            int[] target = new int[m2];
            for (int j = 0; j < m2; j++){
                double randVal = Math.random();
                double right = 0;
                int correctNbRelation = 2 * nbRelation;
                if (correctNbRelation != 0){
                    for (int k = 0; k < j ; k++){
                        correctNbRelation -= edgeCounter.get(target[k]);
                    }
                    for (int id : connected){
                        right += (edgeCounter.get(id) / (double)correctNbRelation);
                        if (randVal < right) {
                            target[j] = id;
                            break;
                        }
                        System.out.println("randVal:" + randVal + " right:" + right);
                    }
                }
                else {
                    target[0] = sampled[0];
                }
                source.add(i < target[j] ? i : target[j]);
                dest.add(i > target[j] ? i : target[j]);
                connected.remove(target[j]);
            }
            for (int j = 0; j < m2; j++){
                connected.add(target[j]);
                edgeCounter.put(target[j],edgeCounter.get(target[j]) + 1);
                nbConstraint++;
                nbRelation++;
            }
            connected.add(i);
            edgeCounter.put(i,edgeCounter.get(i) + m2);

        }
    }
}
