package edu.cqu.algorithms.dcop.incomplete.maxsum.redundancy;

import java.util.HashMap;
import java.util.Map;

public abstract class Node {
    protected Map<Integer,int[]> messageContent;
    protected int[] neighbours;
    protected int id;

    public Node(int[] neighbours, int id) {
        this.messageContent = new HashMap<>();
        this.neighbours = neighbours;
        this.id = id;
    }

    public void addMessage(int sender, int[] content){
        messageContent.put(sender,content);
    }

    public abstract int[] computeMessage(int targetId);

}
