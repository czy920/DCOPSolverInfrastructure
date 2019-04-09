package edu.cqu.ordering;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;

import java.util.*;

public abstract class DFSSyncAgent_MaxDegreeRoot extends SyncAgent{

    private static final int MSG_DEGREE = 0XFFFF0;
    private static final int MSG_DFS = 0XFFFF1;
    private static final int MSG_DFS_BACKTRACK = 0XFFFF2;
    private static final int MSG_START = 0XFFFF3;
    private static final int MSG_MAXDEGREE = 0XFFFF4;
    private static final int MSG_ROOT = 0XFFFF5;

    protected Map<Integer,Integer> degreeView;
    protected List<Map.Entry<Integer,Integer>> orderedDegree;
    protected int parent;
    protected List<Integer> children;
    private int currentChildIndex;
    protected List<Integer> pseudoParents;
    protected int level;
    protected int height;
    private int maxSubHeight;
    protected Map<Integer,Integer> visited;

    private Map<Integer, Integer> degreeMap;
    private boolean choosed;
    public DFSSyncAgent_MaxDegreeRoot(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        degreeView = new HashMap<>();
        children = new LinkedList<>();
        pseudoParents = new LinkedList<>();
        parent = -1;
        visited = new HashMap<>();
        degreeMap = new HashMap<>();
        choosed = false;
    }

    protected boolean isRootAgent(){
        return parent <= 0;
    }

    protected boolean isLeafAgent(){
        return children.size() == 0;
    }

    @Override
    protected void initRun() {
        for (int neighbourId : neighbours){
            sendMessage(new Message(id,neighbourId,MSG_DEGREE,neighbours.length));
        }
        sendMessage(new Message(id,1, MSG_MAXDEGREE, neighbours.length));
    }

    @Override
    public void runFinished() {

    }


    @Override
    public void allMessageDisposed() {
        super.allMessageDisposed();
        if (!choosed && id==1 && degreeMap.size() > neighbours.length  ) {
            //todo: make sure that all agent have send MAXDEGREE msg to id(1)
            int maxDegree = neighbours.length;
            int maxInd = id;
            for (int key : degreeMap.keySet()) {
                if (maxDegree < degreeMap.get(key)) {
                    maxDegree = degreeMap.get(key);
                    maxInd = key;
                }
            }
            choosed = true;
            sendMessage(new Message(id, maxInd, MSG_ROOT, null));
        }
    }



    @Override
    public void disposeMessage(Message message) {
        switch (message.getType()){
            case MSG_MAXDEGREE:{
                int senderId = message.getIdSender();
                int degree = (int) message.getValue();
                degreeMap.put(senderId, degree);
                break;
            }
            case MSG_ROOT:{
                parent = -1;
                level = 0;
                children.add(orderedDegree.get(0).getKey());
                visited.put(id,level);
                sendMessage(new Message(id,orderedDegree.get(0).getKey(),MSG_DFS,new DFSMessageContent(visited,level)));
                break;
            }
            case MSG_DEGREE:
                degreeView.put(message.getIdSender(),(int)message.getValue());
                if (degreeView.size() == neighbours.length){
                    orderedDegree = new ArrayList<>(degreeView.entrySet());
                    orderedDegree.sort(new Comparator<Map.Entry<Integer, Integer>>() {
                        @Override
                        public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                            return o2.getValue().compareTo(o1.getValue());
                        }
                    });
//                    if (id == 1){
//                        parent = -1;
//                        level = 0;
//                        children.add(orderedDegree.get(0).getKey());
//                        visited.put(id,level);
//                        sendMessage(new Message(id,orderedDegree.get(0).getKey(),MSG_DFS,new DFSMessageContent(visited,level)));
//                    }
                }
                break;
            case MSG_DFS: {
//                System.out.println(message);
                DFSMessageContent content = (DFSMessageContent) message.getValue();
                visited = content.visited;
                level = content.level + 1;
                visited.put(id,level);
                parent = message.getIdSender();
                int selectedChild = 0;
                for (int i = 0; i < orderedDegree.size(); i++) {
                    if (visited.keySet().contains(orderedDegree.get(i).getKey())) {
                        if (orderedDegree.get(i).getKey() != parent){
                            pseudoParents.add(orderedDegree.get(i).getKey());
                        }
                    }
                }
                for (int i = 0; i < orderedDegree.size(); i++) {
                    if (visited.keySet().contains(orderedDegree.get(i).getKey())) {
                        continue;
                    }
                    selectedChild = orderedDegree.get(i).getKey();
                    currentChildIndex = i;
                    break;
                }
                if (selectedChild != 0) {
                    children.add(selectedChild);
                    sendMessage(new Message(id, selectedChild, MSG_DFS, new DFSMessageContent(visited,level)));
                }
                else {
                    height = 0;
                    sendMessage(new Message(id, parent, MSG_DFS_BACKTRACK, new BacktrackMessageContent(visited,height)));
                }

                break;
            }
            case MSG_DFS_BACKTRACK:
                BacktrackMessageContent content = (BacktrackMessageContent) message.getValue();
                visited = content.visited;
                if (content.height > maxSubHeight){
                    maxSubHeight = content.height;
                }
                int selectedChild = 0;
                for (int i = currentChildIndex + 1; i < orderedDegree.size(); i++){
                    if (visited.keySet().contains(orderedDegree.get(i).getKey())) {
                        continue;
                    }
                    selectedChild = orderedDegree.get(i).getKey();
                    currentChildIndex = i;
                    break;
                }
                if (selectedChild != 0){
                    children.add(selectedChild);
                    sendMessage(new Message(id, selectedChild, MSG_DFS, new DFSMessageContent(visited,level)));
                }
                else {
                     height = maxSubHeight + 1;
                    if (!isRootAgent()) {
                        sendMessage(new Message(id, parent, MSG_DFS_BACKTRACK, new BacktrackMessageContent(visited,height)));
                    }
                    else {
                        for (int childId : children){
                            sendMessage(new Message(id,childId,MSG_START,null));
                        }
                        pseudoTreeCreated();
                        System.out.println("Agent " + id + " start !");
                    }
                }
                break;
            case MSG_START:
                for (int childId : children){
                    sendMessage(new Message(id,childId,MSG_START,null));
                }
                pseudoTreeCreated();
                System.out.println("Agent " + id + " start !");
                break;
        }
    }

    protected abstract void pseudoTreeCreated();
    private class DFSMessageContent{
        Map<Integer,Integer> visited;
        int level;

        public DFSMessageContent(Map<Integer,Integer> visited, int level) {
            this.visited = visited;
            this.level = level;
        }
    }

    private class BacktrackMessageContent{
        Map<Integer,Integer> visited;
        int height;

        public BacktrackMessageContent(Map<Integer,Integer> visited, int height) {
            this.visited = visited;
            this.height = height;
        }
    }

    public String toDOTString(){
        StringBuilder stringBuilder = new StringBuilder();
        if (parent > 0){
            stringBuilder.append("X" + parent + "->X" + id + ";\n");
        }
        for (int pp : pseudoParents){
            stringBuilder.append("X" + pp + "->X" + id + " [style=dotted];\n");
        }
        return stringBuilder.toString();
    }
}
