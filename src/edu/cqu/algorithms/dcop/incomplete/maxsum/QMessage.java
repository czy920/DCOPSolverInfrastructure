package edu.cqu.algorithms.dcop.incomplete.maxsum;

public class QMessage {
    public double[] utility;
    public int target;
    public void average(QMessage qMessage,double scale){
        for (int i = 0; i < utility.length; i++){
            utility[i] = (1 - scale) * utility[i] + scale * qMessage.utility[i];
        }
    }
}
