package edu.cqu.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dyc on 2017/6/16.
 */
public abstract class Agent extends Process{
    protected int id;
    protected int[] domain;
    protected int[] neighbours;
    protected Map<Integer,int[][]> constraintCosts;
    protected Map<Integer,int[]> neighbourDomains;
    private Map<Integer,Integer> localView;
    protected int valueIndex;

    public int ncccs;

    public Agent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts,Map<Integer,int[]> neighbourDomains) {
        super("Agent " + id);
        this.id = id;
        this.domain = domain;
        this.neighbours = neighbours;
        this.constraintCosts = constraintCosts;
        this.neighbourDomains = neighbourDomains;
        localView = new HashMap<>();
    }

    @Override
    public void preExecution() {
        for (int neighbourId : neighbours){
            localView.put(neighbourId,0);
        }
        initRun();
        postInit();
    }

    protected abstract void initRun();
    protected void postInit(){

    }

    public abstract void runFinished();

    @Override
    public void postExecution() {
        runFinished();
    }

    public abstract void sendMessage(Message message);


    public abstract void disposeMessage(Message message);

    public int getLocalCost(){
        int sum = 0;
        for (int neighbourId : neighbours){
            sum += constraintCosts.get(neighbourId)[valueIndex][localView.get(neighbourId)];
        }
        return sum;
    }

    protected int updateLocalView(int neighbourId,int valueIndex){
        return localView.put(neighbourId,valueIndex);
    }

    protected int getNeighbourValue(int neighbourId){
        return localView.get(neighbourId);
    }

    public static String array2String(int[] arr){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        for (int i = 0; i < arr.length; i++){
            if (i != arr.length - 1){
                stringBuilder.append(arr[i] + ",");
            }
            else {
                stringBuilder.append(arr[i] + "]");
            }
        }
        return stringBuilder.toString();
    }

    public static String map2String(Map map){
        return map2String(map,null,null);
    }

    public static String map2String(Map map, Stringifier keyStringifier, Stringifier valueStringifier){
        StringBuilder stringBuilder = new StringBuilder();
        for (Object key : map.keySet()){
            String keyString = keyStringifier == null ? key.toString() : keyStringifier.stringify(key);
            String valueString = valueStringifier == null ? map.get(key).toString() : valueStringifier.stringify(map.get(key));
            stringBuilder.append(keyString + "=" + valueString + "\n");
        }
        return stringBuilder.toString();
    }


}
