package edu.cqu.algorithms.dcop.incomplete.maxsum.adssvp;

import edu.cqu.core.Message;


public interface HostAgent {
    void sendMessage(Message message);
    int getId();
    int[][] getConstraint(int oppositeId);
    int[] getNeighbours();
    int getNeighbourValue(int neighbourId);
    int getDomainLength();
    int getLocalCostCorr();
}
