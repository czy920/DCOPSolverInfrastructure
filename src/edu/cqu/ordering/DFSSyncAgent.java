package edu.cqu.ordering;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;

import java.util.*;

public abstract class DFSSyncAgent extends SyncAgent{

    private static final int MSG_DEGREE = 0XFFFF0;
    private static final int MSG_DFS = 0XFFFF1;
    private static final int MSG_DFS_BACKTRACK = 0XFFFF2;

    protected Map<Integer,Integer> degreeView;
    protected List<Map.Entry<Integer,Integer>> orderedDegree;
    protected int parent;
    protected List<Integer> children;
    private int currentChildIndex;
    protected List<Integer> pseudoParents;
    protected int level;

    public DFSSyncAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        degreeView = new HashMap<>();
        children = new LinkedList<>();
        pseudoParents = new LinkedList<>();
        parent = -1;
    }

    @Override
    protected void initRun() {
        for (int neighbourId : neighbours){
            sendMessage(new Message(id,neighbourId,MSG_DEGREE,neighbours.length));
        }
    }

    @Override
    public void runFinished() {

    }

    @Override
    public void disposeMessage(Message message) {
        switch (message.getType()){
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
                    if (id == 1){
                        HashSet<Integer> visited = new HashSet<>();
                        visited.add(id);
                        parent = -1;
                        level = 0;
                        children.add(orderedDegree.get(0).getKey());

                        sendMessage(new Message(id,orderedDegree.get(0).getKey(),MSG_DFS,new DFSMessageContent(visited,level)));
                    }
                }
                break;
            case MSG_DFS: {
//                System.out.println(message);
                DFSMessageContent content = (DFSMessageContent) message.getValue();
                Set<Integer> visited = content.visited;
                level = content.level + 1;
                visited.add(id);
                parent = message.getIdSender();
                int selectedChild = 0;
                for (int i = 0; i < orderedDegree.size(); i++) {
                    if (visited.contains(orderedDegree.get(i).getKey())) {
                        if (orderedDegree.get(i).getKey() != parent){
                            pseudoParents.add(orderedDegree.get(i).getKey());
                        }
                    }
                }
                for (int i = 0; i < orderedDegree.size(); i++) {
                    if (visited.contains(orderedDegree.get(i).getKey())) {
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
                    sendMessage(new Message(id, parent, MSG_DFS_BACKTRACK, visited));
                }
                break;
            }
            case MSG_DFS_BACKTRACK:
                HashSet<Integer> visited = (HashSet) message.getValue();
                int selectedChild = 0;
                for (int i = currentChildIndex + 1; i < orderedDegree.size(); i++){
                    if (visited.contains(orderedDegree.get(i).getKey())) {
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
                    pseudoTreeCreated();
                    if (id != 1) {
                        sendMessage(new Message(id, parent, MSG_DFS_BACKTRACK, visited));
                    }
                }
                break;
        }
    }

    protected abstract void pseudoTreeCreated();
    private class DFSMessageContent{
        Set<Integer> visited;
        int level;

        public DFSMessageContent(Set<Integer> visited, int level) {
            this.visited = visited;
            this.level = level;
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
