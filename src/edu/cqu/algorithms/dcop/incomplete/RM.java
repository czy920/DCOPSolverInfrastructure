package edu.cqu.algorithms.dcop.incomplete;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;
import edu.cqu.result.ResultCycle;

import java.util.HashMap;
import java.util.Map;

public class RM extends SyncAgent {

    private final static int MSG_VALUE = 1;
    private final static int MSG_TERMINATE = 2;

    private Map<Integer, Integer> neighboursValue;
    private final static double P = 0.9;
    private double[] averageRegret;
    private int valueMsgCnt = 0;


    public RM(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
        averageRegret = new double[domain.length];
        neighboursValue = new HashMap<>();
    }

    @Override
    protected void initRun() {
        valueIndex = (int) (domain.length * Math.random());
        broadcastValue();
    }


    @Override
    public void runFinished() {
        ResultCycle resultCycle = new ResultCycle();
        resultCycle.setAgentValues(id,valueIndex);
        mailer.setResultCycle(id,resultCycle);
    }

    @Override
    public void disposeMessage(Message message) {
        switch (message.getType()) {
            case MSG_VALUE:{
                int neighbourId = message.getIdSender();
                int value = (int) message.getValue();
                neighboursValue.put(neighbourId, value);
                updateLocalView(neighbourId,value);

                break;
            }
            case MSG_TERMINATE:{
                break;
            }
        }
    }

    @Override
    public void allMessageDisposed() {
        super.allMessageDisposed();
        if (valueMsgCnt >= 1000){
            stopProcess();
            return;
        }
        if (neighboursValue.size() == neighbours.length) {
            ++valueMsgCnt;
            int currentValue = 0;
            for (int i : neighbours) {
                currentValue += constraintCosts.get(i)[valueIndex][neighboursValue.get(i)];
            }
            double[] stateRegret = new double[domain.length];
            for (int j = 0; j < domain.length; ++j) {
                for (int i : neighbours) {
                    stateRegret[j] += constraintCosts.get(i)[j][neighboursValue.get(i)];
                }
                averageRegret[j] = (1.0/valueMsgCnt) * (currentValue - stateRegret[j] + (valueMsgCnt - 1) * averageRegret[j]);
                stateRegret[j] = Math.max(0, averageRegret[j]);
            }
            double[] mixStrategyCDF = cdf(stateRegret);

            int candidateState = 0;
            double rnd = Math.random();
            for (int i = 0; i < domain.length; ++i) {
                if (mixStrategyCDF[i] >= rnd) {
                    candidateState = i;
                    break;
                }
            }
            if (Math.random() <= P) {
                if (valueIndex != candidateState) {
                    valueIndex = candidateState;
                    broadcastValue();
                }
            }
        }
    }

    private void broadcastValue() {
        for (int i : neighbours){
            sendMessage(new Message(id, i, MSG_VALUE, valueIndex));
        }
    }

    double[] cdf(double[] stateRegret) {
        double[] mixStrategyCDF = new double[domain.length];
        double acc = 0;
        for (int i = 0; i < domain.length; ++i) {
            acc += stateRegret[i];
            mixStrategyCDF[i] = acc;
        }
        for (int i = 0; i < domain.length; ++i) {
            if (acc != 0)
                mixStrategyCDF[i] /= acc;
            else {
                mixStrategyCDF[i] = (1.0 / mixStrategyCDF.length) * i;
            }
        }
        return mixStrategyCDF;
    }
}
