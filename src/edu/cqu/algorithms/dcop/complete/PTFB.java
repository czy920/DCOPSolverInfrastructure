package edu.cqu.algorithms.dcop.complete;

import edu.cqu.core.Message;
import edu.cqu.core.SyncMailer;
import edu.cqu.ordering.DFSSyncAgent;
import edu.cqu.result.ResultWithPrivacy;

import java.util.*;

public class PTFB extends DFSSyncAgent {

    private static final int MSG_PCB = 0;
    private static final int MSG_ECA = 1;
    private static final int MSG_LATEST_DIVIDER = 2;
    private static final int MSG_LATEST_DIVIDER_ACK = 3;
    private static final int MSG_CPA = 4;
    private static final int MSG_REQUEST = 5;
    private static final int MSG_REPORT = 6;
    private static final int MSG_UB = 7;
    private static final int MSG_BACKTRACK = 8;
    private static final int MSG_REDUCE_UB = 9;
    private static final int MSG_TERMINATE = 10;

    private static final int NULL = -1;
    private final int EMPTY = (int)(domain.length);

    //TODO: to be sorted: from big to small
    private List<NeighborLevel> pseudoParentLevels;
    private Set<Integer> latestDividers;
    private Map<Integer,Set<Integer>> pcb;
    private Map<Integer,Set<Integer>> pcbCache;
    private boolean started;
    private Set<Integer> pseudoChildren;
    private int latestDividerAckCount;

    private int receiveCost;
    private int ub;
    private int receiveUb;
    private Map<Integer,Integer> parentLbReports;
    private Map<Integer,Integer> subtreeLb;
    private int[] domainLb;
    private Map<Integer,Set<Integer>> desc;
    private CPA cpa;
    private Map<Integer,int[]> localCosts;
    private Map<Integer,Integer> srchVal;
    private Map<Integer,Integer> lbAdjust;
    private int lastSentUb;
    private Map<Integer,int[]> subtreeLB;
    private Map<Integer,int[]> subtreeUB;
    private Map<Integer,Integer> lbReport;
    private long messageSizeCount;

    public PTFB(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        pcbCache = new HashMap<>();
        localCosts = new HashMap<>();
        srchVal = new HashMap<>();
        lbReport = new HashMap<>();
        lbAdjust = new HashMap<>();
        subtreeUB = new HashMap<>();
        pcbCache = new HashMap<>();
        subtreeLB = new HashMap<>();
    }

    @Override
    protected void pseudoTreeCreated() {
        pseudoParentLevels = new LinkedList<>();
        latestDividers = new HashSet<>();
        pcb = new HashMap<>();
        pseudoChildren = new HashSet<>();
        for (int neighbor : neighbours){
            if (neighbor != parent && !pseudoParents.contains(neighbor) && !children.contains(neighbor)){
                pseudoChildren.add(neighbor);
            }
        }
        if (isLeafAgent()){
            Set<Integer> pcbSet = new HashSet<>();
            pcbSet.add(id);
            sendMessage(new Message(id,parent,MSG_PCB,pcbSet));
        }
        started = true;
        if (!isLeafAgent() && pcbCache != null && pcbCache.size() == children.size()){
            calculatePCB();
        }
        if (isRootAgent()){
            localCosts.put(-1,new int[domain.length]);
            cpa = new CPA(new HashMap<>(),(int) 1e8,0,new HashMap<>());
        }
    }

    private void calculatePCB(){
        Set<Integer> pcbSet = new HashSet<>();
        for (int c : pcbCache.keySet()){
            pcbSet.addAll(pcbCache.get(c));
            Set<Integer> set = pcb.get(c);
            if (set == null){
                set = new HashSet<>();
                pcb.put(c,set);
            }
            for (int desc : pcbCache.get(c)){
                if (pseudoChildren.contains(desc)){
                    pcb.get(c).add(desc);
                }
            }
            pcb.get(c).add(c);
        }
        pcbSet.add(id);
        if (!isRootAgent())
            sendMessage(new Message(id,parent,MSG_PCB,pcbSet));
        else {
            sendECA(new HashMap<>());
            sendLatestDivider();
        }
        desc = pcbCache;
        pcbCache = null;
    }

    private void sendECA(Map<Integer,Integer> levels){
        levels.put(id,level);
        for (int pc : children){
            sendMessage(new Message(id,pc,MSG_ECA,levels));
        }
    }

    private void sendLatestDivider(){
        if (isLeafAgent()){
            sendMessage(new Message(id,parent,MSG_LATEST_DIVIDER_ACK,null));
            return;
        }
        Set<Integer> sendinglatestDividers = new HashSet<>(latestDividers);
        if (children.size() > 1){
            sendinglatestDividers.clear();
        }
        sendinglatestDividers.add(id);
        for (int c : children){
            sendMessage(new Message(id,c, MSG_LATEST_DIVIDER,sendinglatestDividers));
        }
    }


    @Override
    public void disposeMessage(Message message) {
        super.disposeMessage(message);
        if (message.getType() < 20 && message.getType() >= 4){
            int iii = 0;
        }
        switch (message.getType()){
            case MSG_PCB:
                Set<Integer> msg = (Set<Integer>) message.getValue();
                messageSizeCount += msg.size();
                pcbCache.put(message.getIdSender(),msg);
                if (started && pcbCache.size() == children.size()){
                    calculatePCB();
                }
                break;
            case MSG_ECA:
                Map<Integer,Integer> levels = (Map<Integer, Integer>) message.getValue();
                messageSizeCount += levels.size()*2;
                for (int highId : pseudoParents) {
                    pseudoParentLevels.add(new NeighborLevel(highId, levels.get(highId)));
                }
                //sort
                Collections.sort(pseudoParentLevels, new Comparator<NeighborLevel>() {
                    @Override
                    public int compare(NeighborLevel o1, NeighborLevel o2) {
                        return o2.level - o1.level;
                    }
                });
                sendECA(new HashMap<>(levels));
                break;
            case MSG_LATEST_DIVIDER:
                latestDividers = new HashSet<>((Set) message.getValue());
                messageSizeCount += latestDividers.size();
                sendLatestDivider();
                break;
            case MSG_LATEST_DIVIDER_ACK:
                messageSizeCount ++;
                if (++latestDividerAckCount == children.size()){
                    if (!isRootAgent()){
                        sendMessage(new Message(id,parent,MSG_LATEST_DIVIDER_ACK,null));
                    }
                    else {
                        disposeCpaMessage();
                    }
                }
                break;
            case MSG_REQUEST: {
                messageSizeCount ++;
                int val = (int) message.getValue();
                //System.out.println("id:" + id  +" request from " + message.getIdSender() + " val:" + val);
                int sender = message.getIdSender();
                int[] costs = updateLocalCost(sender, val);
                int minCost = Integer.MAX_VALUE;
                for (int c : costs) {
                    if (minCost > c) {
                        minCost = c;
                    }
                }
                sendMessage(new Message(id, sender, MSG_REPORT, new LBResponse(val, minCost)));
            }
                break;
            case MSG_REPORT:{
                LBResponse response = (LBResponse) message.getValue();
                messageSizeCount = messageSizeCount + 2;
                int sender = message.getIdSender();
                //System.out.println("id:" + id + " receive lb from " + sender + " for " + response.value);
                lbReport.put(sender,response.estimate);
                int branch = -1;
                for (int c : pcb.keySet()){
                    if (pcb.get(c).contains(sender)){
                        branch = c;
                        break;
                    }
                }
                for (int pc : pcb.get(branch)){
                    if (!lbReport.containsKey(pc)){
                        return;
                    }
                }
                int subtreeAdjust = 0;
                for (int pc : pcb.get(branch)){
                    int shift = parentLbReports.containsKey(pc) ? parentLbReports.get(pc) : 0;
                    subtreeAdjust += lbReport.get(pc) - shift;
                    lbAdjust.put(pc,lbReport.get(pc) - shift);
                }
                srchVal.put(branch,response.value);
                domainLb[response.value] += subtreeAdjust;
                if (!subtreeLB.containsKey(branch)){
                    subtreeLB.put(branch,new int[domain.length]);
                }
                subtreeLB.get(branch)[response.value] = subtreeLb.get(branch) + subtreeAdjust;
                for (int c : children){
                    if (c == branch || !srchVal.containsKey(c) || srchVal.get(c) != response.value){
                        continue;
                    }
                    sendMessage(new Message(id,c,MSG_REDUCE_UB,subtreeAdjust));
                }
                if (domainLb[response.value] < ub){
                    continueAssignment(response.value,branch);
                }
                else {

                    assignNextVal(response.value,branch);
                }
            }
                break;
            case MSG_UB:{
                UB Ub = (UB) message.getValue();
                messageSizeCount ++;
                if (id == 1)
                    System.out.println("UB:"+ub);
                int sender = message.getIdSender();
                if (children.size() == 1){
                    ub = Ub.ub;
                }
                else {
                    //todo combine cpa
                    int branch = -1;
                    for (int c : desc.keySet()){
                        if (desc.get(c).contains(sender)){
                            branch = c;
                            break;
                        }
                    }
                    int val = srchVal.get(branch);
                    subtreeUB.get(branch)[val] = Ub.ub;
                    int sum = 0;
                    for (int c : children){
                        if (subtreeUB.containsKey(c)){
                            sum += subtreeUB.get(c)[val];
                        }
                    }
                    int totalUb = receiveCost + localCosts.get(parent)[val] + sum;
                    totalUb = subtreeUB.size() < children.size() ? Integer.MAX_VALUE : totalUb;
                    if (totalUb < ub){
                        ub = totalUb;
                        for (int ancestor : latestDividers){
                            sendMessage(new Message(id,ancestor,MSG_UB,new UB(ub,new HashMap<>())));
                        }
                    }
                }
            }
            break;
            case MSG_BACKTRACK:{
                messageSizeCount ++;
                int sender = message.getIdSender();
                int val = srchVal.get(sender);
                if (children.size() > 1){
                    int diff = subtreeUB.get(sender)[val] - subtreeLB.get(sender)[val];
                    subtreeLB.get(sender)[val] = subtreeUB.get(sender)[val];
                    for (int c : children){
                        if (c == sender || !srchVal.containsKey(c) || srchVal.get(c) != val){
                            continue;
                        }
                        sendMessage(new Message(id,c,MSG_REDUCE_UB,diff));
                    }
                }
                //System.out.println("id:" + id + " receive backtrack from " + sender + " srchVal:" + val);
                assignNextVal(val,sender);
            }
                break;
            case MSG_REDUCE_UB:{
                messageSizeCount ++;
                receiveUb -= (int) message.getValue();
                if (receiveUb < ub){
                    int newDiff;
                    if (children.size() > 1){
                        newDiff = ub - receiveUb;
                    }
                    else {
                        newDiff = lastSentUb - receiveUb;
                        lastSentUb = receiveUb;
                    }
                    ub = receiveUb;
                    for (int c : children){
                        sendMessage(new Message(id,c,MSG_REDUCE_UB,newDiff));
                    }
                }
            }
                break;
            case MSG_TERMINATE:
                messageSizeCount ++;
                for (int c : children){
                    sendMessage(new Message(id,c,MSG_TERMINATE,null));
                }
                stopProcess();
                break;
            case MSG_CPA:
                cpa = (CPA) message.getValue();
                messageSizeCount = messageSizeCount + 2 + cpa.assignment.size()*2 + cpa.lbReports.size()*2;
                //System.out.println("id:" + id + "  receive cpa from " + message.getIdSender());
                disposeCpaMessage();

        }
    }

    private void disposeCpaMessage(){
        lbReport.clear();
        receiveCost = cpa == null ? 0 : cpa.cpaCost;
        parentLbReports = cpa == null ? new HashMap<>() : new HashMap<>(cpa.lbReports);
        lastSentUb = ub = receiveUb = cpa == null ? (int) 1e8 : cpa.ub;
        subtreeLb = new HashMap<>();
        subtreeUB.clear();
        int lbSum = 0;
        for (int c : children){
            srchVal.put(c,NULL);
             int sum = 0;
             for (int pc : desc.get(c)){
                 if (parentLbReports.containsKey(pc)){
                     sum += parentLbReports.get(pc);
                 }
             }
             lbSum += sum;
             subtreeLb.put(c,sum);
             int[] tmp = new int[domain.length];
             Arrays.fill(tmp, sum);
             subtreeLB.put(c,tmp);
        }
        domainLb = new int[domain.length];
        for (int i = 0; i < domain.length; i ++){
            domainLb[i] = receiveCost + localCosts.get(parent)[i] + lbSum;
        }
        for (int c : children){
            assignNextVal(NULL,c);
        }
        if (isLeafAgent()){
            assignNextVal(NULL,NULL);
        }
    }

    private void assignNextVal(int val,int child){
        if (child != NULL) {
            for (int pc : pcb.get(child)) {
                lbReport.remove(pc);
            }
        }
        if (val == domain.length - 1)
            val = EMPTY;
        else
            while (++val < domain.length && domainLb[val] >= ub);
        if (val == domain.length){
            val = EMPTY;
        }
        boolean flag = false;
        if (child != NULL && val == EMPTY){
            srchVal.put(child,EMPTY);
            boolean tmpFlag = true;
            for (int c : children){
                if (srchVal.get(c) != EMPTY){
                    tmpFlag = false;
                    break;
                }
            }
            flag = tmpFlag;
        }
        if (child == NULL && val == EMPTY || flag){
            if (!isRootAgent())
                sendMessage(new Message(id,parent,MSG_BACKTRACK,null));
            else {
                System.out.println("ub:" + ub);
                for (int c : children){
                    sendMessage(new Message(id,c,MSG_TERMINATE,null));
                }
                stopProcess();
            }
        }
        else {
            if (isLeafAgent()){
                continueAssignment(val,child);
            }
            else if (val < EMPTY){
                for (int c : pcb.get(child)){
                    sendMessage(new Message(id,c,MSG_REQUEST,val));
                }
            }
        }
    }

    @Override
    public void runFinished() {
        super.runFinished();
        ResultWithPrivacy cycle = new ResultWithPrivacy();
        if (id == 1)
            cycle.setUb(ub);
        cycle.setAgentValues(id,0);
        cycle.setMessageSizeCount(messageSizeCount);
        mailer.setResultCycle(id,cycle);
    }

    private void continueAssignment(int val, int child){
        Map<Integer,Integer> newAssignment = new HashMap<>(cpa.assignment);
        newAssignment.put(id,val);
        if (isLeafAgent()){
            ub = receiveCost + localCosts.get(parent)[val];
            for (int ancestor : latestDividers){
                sendMessage(new Message(id,ancestor,MSG_UB,new UB(ub,newAssignment)));
            }
            assignNextVal(val,NULL);
        }
        else  {
            Map<Integer,Integer> newLbReports = new HashMap<>(parentLbReports);
            for (int id : lbAdjust.keySet()){
                if (newLbReports.containsKey(id)){
                    newLbReports.put(id,newLbReports.get(id) + lbAdjust.get(id));
                }
                else {
                    newLbReports.put(id,lbAdjust.get(id));
                }
            }
            if (children.size() == 1){
                int newCost = receiveCost + localCosts.get(parent)[val];
                lastSentUb = ub;
                sendMessage(new Message(this.id,child,MSG_CPA,new CPA(newAssignment,ub,newCost,newLbReports)));
            }
            else {
                int newCost = 0;
                int sum = 0;
                for (int c : children){
                    if (c == child || !subtreeLB.containsKey(c)){
                        continue;
                    }
                    sum += subtreeLB.get(c)[val];
                }
                int newUb = ub - receiveCost - localCosts.get(parent)[val] - sum;
                if (!subtreeUB.containsKey(child)){
                    int[] stUb = new int[domain.length];
                    Arrays.fill(stUb,(int)1e7);
                    subtreeUB.put(child,stUb);
                }
                subtreeUB.get(child)[val] = newUb;
                sendMessage(new Message(id,child,MSG_CPA,new CPA(newAssignment,newUb,newCost,newLbReports)));
            }
        }
    }

    private int[] updateLocalCost(int highId, int highVal){
        int eca = eca(highId);
        int[] costs = new int[domain.length];
        for (int i = 0; i < domain.length; i++){
            int base = eca == -1 ? 0 : localCosts.get(eca)[i];
            costs[i] = base + constraintCosts.get(highId)[i][highVal];
            ++ncccs;
        }
        localCosts.put(highId,costs);
        return costs;
    }

    private int eca(int pseudoParent){
        if (pseudoParentLevels.size() == 0){
            return -1;
        }
        if (pseudoParent == parent){
            return pseudoParentLevels.get(0).id;
        }
        for (int i = 1; i < pseudoParentLevels.size(); i++){
            if (pseudoParentLevels.get(i - 1).id == pseudoParent){
                return pseudoParentLevels.get(i).id;
            }
        }
        return -1;
    }

    private class LBResponse{
        int value;
        int estimate;

        public LBResponse(int value, int estimate) {
            this.value = value;
            this.estimate = estimate;
        }
    }

    private class UB{
        int ub;
        Map<Integer,Integer> assignment;

        public UB(int ub, Map<Integer, Integer> assignment) {
            this.ub = ub;
            this.assignment = new HashMap<>(assignment);
        }
    }

    private class CPA{
        Map<Integer,Integer> assignment;
        int ub;
        int cpaCost;
        Map<Integer,Integer> lbReports;

        public CPA(Map<Integer, Integer> assignment, int ub, int cpaCost, Map<Integer, Integer> lbReports) {
            this.assignment = new HashMap<>(assignment);
            this.ub = ub;
            this.cpaCost = cpaCost;
            this.lbReports = new HashMap<>(lbReports);
        }
    }

    private class NeighborLevel{
        int id;
        int level;

        public NeighborLevel(int id, int level) {
            this.id = id;
            this.level = level;
        }
    }
}
