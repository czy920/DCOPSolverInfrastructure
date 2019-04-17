package edu.cqu.algorithms.dcop.incomplete;

import edu.cqu.core.Message;
import edu.cqu.core.SyncMailer;
import edu.cqu.ordering.BFSSyncAgent;
import edu.cqu.result.ResultCycle;

import java.util.*;

public class ACOAgent extends BFSSyncAgent {

    private static final int MSG_SUB_NODE_COUNT = 0;
    private static final int MSG_NODE_COUNT = 1;
    private static final int MSG_DETERMINE_LOWEST_NODE = 2;
    private static final int MSG_INFORM_LOWEST_NODE = 3;
    private static final int MSG_VALUE = 4;
    private static final int MSG_PHEROMONE = 5;

    private static final double alpha = 3;
    private static final double beta = 5;
    private static final double rho = 0.0025;
    private static final double tau0 = 3;
    private static final int K = 13;
    private static final int MAX_CYCLE = 1000;
    private static final double tauMax = 12;
    private static final double tauMin = 0.1;

    private Map<Integer,Ant> ants;
    private Map<Integer,double[][]> taus;
    private int subNodeCount;
    private int subtreeReceived;
    private int totalNodeCount;
    private Set<Integer> highPriorityNode;
    private Set<Integer> lowPriorityNode;
    private int lowestId;
    private int[] est;
    private int lambda = 241;

    //only for the root
    private int currentLowestId;
    private int currentLowestLevel;
    private boolean determineLowestNode;

    //need to be reset in the start of each cycle
    private int valueMessageReceivedCount;

    //only for lowest agent
    private int bestCost;
    private int currentCycle;


    public ACOAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        ants = new HashMap<>();
        highPriorityNode = new HashSet<>();
        lowPriorityNode = new HashSet<>();
        taus = new HashMap<>();
        bestCost = Integer.MAX_VALUE;
    }

    private void initEst(){
        est = new int[domain.length];
        for (int i = 0; i < est.length; i++){
            int sum = 0;
            for (int lowId : lowPriorityNode){
                int min = Integer.MAX_VALUE;
                for (int j = 0; j < neighbourDomains.get(lowId).length; j++){
                    int cost = constraintCosts.get(lowId)[i][j];
                    if (cost < min){
                        min = cost;
                    }
                }
                sum += min;
            }
            est[i] = sum;
        }
    }

    @Override
    protected void pseudoTreeCreated() {
        highPriorityNode.addAll(pseudoParent);
        if (parent > 0)
        highPriorityNode.add(parent);
        lowPriorityNode.addAll(children);
        lowPriorityNode.addAll(pseudoChildren);
        for (int siblingId : siblings){
            if (siblingId > id){
                lowPriorityNode.add(siblingId);
            }
            else {
                highPriorityNode.add(siblingId);
            }
        }
        for (int highId : highPriorityNode){
            double[][] tau = new double[domain.length][neighbourDomains.get(highId).length];
            for (int i = 0; i < domain.length; i++){
                Arrays.fill(tau[i],tau0);
            }
            taus.put(highId,tau);
        }
        initEst();
        if (children.size() == 0){
            sendMessage(new Message(id,parent, MSG_SUB_NODE_COUNT,1));
        }

    }

    @Override
    public void runFinished() {
        ResultCycle resultCycle = new ResultCycle();
        resultCycle.setAgentValues(id,12);
        mailer.setResultCycle(id,resultCycle);
    }

    @Override
    public void disposeMessage(Message message) {
        super.disposeMessage(message);
        switch (message.getType()){
            case MSG_SUB_NODE_COUNT:
                subNodeCount += (int) message.getValue();
                if (++subtreeReceived == children.size()){
                    if (id != 1) {
                        sendMessage(new Message(id, parent, MSG_SUB_NODE_COUNT, subNodeCount + 1));
                    }
                    else {
                        totalNodeCount = subNodeCount + 1;
                        for (int i = 2; i <= totalNodeCount; i++){
                            sendMessage(new Message(id,i,MSG_NODE_COUNT,totalNodeCount));
                        }
                        // start algorithm

                    }
                }
                break;
            case MSG_NODE_COUNT:
                totalNodeCount = (int) message.getValue();
                if (lowPriorityNode.size() == 0){
                    sendMessage(new Message(id,1,MSG_DETERMINE_LOWEST_NODE,level));
                }
                break;
            case MSG_DETERMINE_LOWEST_NODE:
                int receivedLevel = (int) message.getValue();
                if (receivedLevel > currentLowestLevel){
                    currentLowestLevel = receivedLevel;
                    currentLowestId = message.getIdSender();
                }
                else if (receivedLevel == currentLowestLevel){
                    if (message.getIdSender() > currentLowestId){
                        currentLowestId = message.getIdSender();
                    }
                }
                determineLowestNode = true;
                break;
            case MSG_INFORM_LOWEST_NODE:
                lowestId = (int) message.getValue();
                if (id == 1){
                    startCycle();
                }
                break;
            case MSG_PHEROMONE:
                disposePheromone((Pheromone)message.getValue());
                break;
            case MSG_VALUE:
                disposeValueMessage((Map)message.getValue(),message.getIdSender());
                break;
        }
    }

    private void disposePheromone(Pheromone pheromone){
        if (pheromone.bestAnt != null){
            valueIndex = pheromone.bestAnt.solution.get(id);
        }
        for (int antId : pheromone.ants.keySet()){
            updatePheromone(pheromone.ants.get(antId));
        }
        evaporate();
        updateEstimate(pheromone.ants);
        ants.clear();
        valueMessageReceivedCount = 0;
        if (currentCycle++ < MAX_CYCLE){
            if (id == 1)
                startCycle();
        }
        else {
            stopProcess();
        }

    }

    //todo: later
    private void updateEstimate(Map<Integer,Ant> ants){
        for (int i = 0; i < domain.length; i++){
            double sum = 0;
            int count = 0;
            for (Ant ant : ants.values()){
                if (ant.solution.get(id) == i){
                    sum += subCost(ant);
                    count++;
                }
            }
            if (count != 0){
                est[i] = (int) ((est[i] + (sum / count)) / 2);
            }
        }
    }

    private int subCost(Ant ant){
        int sum = 0;
        for (int lowId : lowPriorityNode){
            sum += constraintCosts.get(lowId)[ant.solution.get(id)][ant.solution.get(lowId)];
        }
        return sum;
    }

    //todo: implement it next time
    private void updatePheromone(Ant ant){
        for (int highId : highPriorityNode){
            int myValue = ant.solution.get(id);
            int highValue = ant.solution.get(highId);
            int cost = constraintCosts.get(highId)[myValue][highValue];
            taus.get(highId)[myValue][highValue] += weightedDelta(ant,cost);
        }
    }

    private double weightedDelta(Ant ant,int cost){
        if (ant.cost < 0){
            return ant.delta * lambda * cost / ant.cost;
        }
        return ant.delta;
    }

    //todo : later
    private void evaporate(){
        for (int i = 0; i < domain.length; i++){
            for (int highId : highPriorityNode){
                for (int j = 0; j < neighbourDomains.get(highId).length; j++){
                    double old = taus.get(highId)[i][j];
                    old = (1 - rho) * old + rho * tau0;
                    if (old > tauMax){
                        old = tauMax;
                    }
                    else if (old < tauMin){
                        old = tauMin;
                    }
                    taus.get(highId)[i][j] = old;
                }
            }
        }
    }

    private void disposeValueMessage(Map<Integer,Ant> receivedAnts,int senderId){

        for (int antId : receivedAnts.keySet()){
            if (!ants.containsKey(antId)){
                ants.put(antId,receivedAnts.get(antId));
            }
            else {
                ants.get(antId).merge(receivedAnts.get(antId));
            }
        }
        if (highPriorityNode.contains(senderId)){
            valueMessageReceivedCount++;
            if (valueMessageReceivedCount == highPriorityNode.size()){
                for (int antId : ants.keySet()){
                    Ant ant = ants.get(antId);
                    int val = selectValue(ant);
                    ant.solution.put(id,val);
                    int sum = 0;
                    for (int highId : highPriorityNode){
                        sum += constraintCosts.get(highId)[val][ant.solution.get(highId)];
                    }
                    ant.cost += sum;
                }
                if (lowPriorityNode.size() != 0){
                    boolean firstChild = true;
                    for (int lowId : lowPriorityNode){
                        if (firstChild){
                            sendMessage(new Message(id,lowId,MSG_VALUE,cloneAnts(true)));
                            firstChild = false;
                        }
                        else {
                            sendMessage(new Message(id,lowId,MSG_VALUE,cloneAnts(false)));
                        }
                    }
                }
                else if (id != lowestId){
                    sendMessage(new Message(id,lowestId, MSG_VALUE,cloneAnts(true)));
                }
            }
        }
        if (id == lowestId){
            boolean allAntHaveSelectedValue = true;
            for (int antId : ants.keySet()){
                if (ants.get(antId).solution.size() < totalNodeCount){
                    allAntHaveSelectedValue = false;
                    break;
                }
            }
            if (allAntHaveSelectedValue){
                double sum = 0;
                Ant bestAnt = null;
                for (int antId : ants.keySet()){
                    Ant ant = ants.get(antId);
                    sum += ant.cost;
                    if (ant.cost < bestCost){
                        bestCost = ant.cost;
                        System.out.println(bestCost);
                        bestAnt = ant;
                    }
                }
                Map<Integer,Ant> clonedAnt = cloneAnts(true);
                for (int antId : ants.keySet()){
                    Ant ant = clonedAnt.get(antId);
                    ant.delta = calculateDelta(ant,sum / ants.size());
                }
                Pheromone pheromone = new Pheromone();
                pheromone.ants = clonedAnt;
                pheromone.bestAnt = bestAnt;
                for (int i = 1; i <= totalNodeCount; i++){
                    sendMessage(new Message(id, i, MSG_PHEROMONE, pheromone));
                }
            }
        }
    }

    private double calculateDelta(Ant ant,double averageCost){
        return 1 - (ant.cost - bestCost) / (averageCost - bestCost);
    }

    /**
     * select a value for an ant
     * @param ant the ant to be proceed
     * @return the value selected
     */
    private int selectValue(Ant ant){
        double[] theta = new double[domain.length];
        for (int i = 0; i < theta.length; i++){
            double sum = 0;
            for (int highId : highPriorityNode){
                sum += taus.get(highId)[i][ant.solution.get(highId)];
            }
            theta[i] = sum;
        }
        double[] eta = new double[domain.length];
        double lb = calculateLB();
        for (int i = 0; i < domain.length; i++){
            double sum = 0;
            for (int highId : highPriorityNode){
                sum += constraintCosts.get(highId)[i][ant.solution.get(highId)];
            }
            sum += est[i];
            sum -= lb;
            eta[i] = 1 / sum;
        }
        double[] p = new double[domain.length];
        double sum = 0;
        for (int i = 0; i < domain.length; i++){
            sum += Math.pow(theta[i],alpha) * Math.pow(eta[i],beta);
        }
        for (int i = 0; i < domain.length; i++){
            p[i] = Math.pow(theta[i],alpha) * Math.pow(eta[i],beta) / sum;
        }
        double[] pmf = new double[domain.length];
        double right = 0;
        for (int i = 0; i < pmf.length; i++){
            right += p[i];
            pmf[i] = right;
        }
        double random = Math.random();
        for (int i = 0; i < domain.length; i++){
            if (random < pmf[i]){
                return i;
            }
        }
        return -1;
    }

    private double calculateLB(){
        int LB = Integer.MAX_VALUE;
        for (int i = 0; i < domain.length; i++){
            int min = Integer.MAX_VALUE;
            int sum = 0;
            for (int highId : highPriorityNode){
                for (int j = 0; j < neighbourDomains.get(highId).length; j++){
                    int cost = constraintCosts.get(highId)[i][j];
                    if (cost < min){
                        min = cost;
                    }
                }
                sum += min;
            }
            sum += est[i];
            if (sum < LB){
                LB = sum;
            }
        }
        return LB - 1;

    }
    @Override
    public void allMessageDisposed() {
        super.allMessageDisposed();

        if (determineLowestNode){
            System.out.println("inform lowest node");
            for (int i = 1; i <= totalNodeCount; i++){
                sendMessage(new Message(id,i,MSG_INFORM_LOWEST_NODE,currentLowestId));
            }
            lowestId = currentLowestId;
            determineLowestNode = false;
        }
    }

    private void startCycle(){

        for (int antId = 0; antId < K; antId++){
            Ant ant = new Ant();
            ant.solution.put(id,(int)(Math.random() * domain.length));
            ants.put(antId,ant);
        }
        for (int lowId : lowPriorityNode){
            sendMessage(new Message(id,lowId,MSG_VALUE,cloneAnts(true)));
        }
    }

    private Map<Integer,Ant> cloneAnts(boolean withCost){
        Map<Integer,Ant> tmpAnts = new HashMap<>();
        for (int antId : ants.keySet()){
            if (withCost) {
                tmpAnts.put(antId, ants.get(antId).clone());
            }
            else {
                tmpAnts.put(antId, ants.get(antId).cloneWithoutCost());
            }
        }
        return tmpAnts;
    }

    private class Ant{
        Map<Integer,Integer> solution;
        int cost;
        double delta;
        public Ant(){
            solution = new HashMap<>();
        }
        public Ant clone(){
            Ant tmpAnt = new Ant();
            for (int id : solution.keySet()){
                tmpAnt.solution.put(id,solution.get(id));
            }
            tmpAnt.cost = cost;
            tmpAnt.delta = delta;
            return tmpAnt;
        }

        public Ant cloneWithoutCost(){
            Ant tmpAnt = new Ant();
            for (int id : solution.keySet()){
                tmpAnt.solution.put(id,solution.get(id));
            }
            tmpAnt.delta = delta;
            return tmpAnt;
        }

        public void merge(Ant otherAnt){
            for (int id : otherAnt.solution.keySet()){
                this.solution.put(id,otherAnt.solution.get(id));
            }
            this.cost += otherAnt.cost;
        }
    }

    private class Pheromone{
        Map<Integer,Ant> ants;
        Ant bestAnt;
    }
}
