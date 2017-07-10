package edu.cqu.test;

import edu.cqu.core.Message;
import edu.cqu.core.SyncAgent;
import edu.cqu.core.SyncMailer;

import java.util.Map;

/**
 * Created by dyc on 2017/6/17.
 */
public class TestSyncAgent extends SyncAgent {

    private static final int MAX_CYCLE = 2;

    private int cycle;

    public TestSyncAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, SyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
    }

    @Override
    protected void initRun() {
        int target = neighbours[(int) (Math.random() * neighbours.length)];
        sendMessage(new Message(this.id,target,cycle,null));
        System.out.println("agent " + id + " INIT");
    }

    @Override
    public void disposeMessage(Message message) {
        System.out.println(message);
        int target = neighbours[(int) (Math.random() * neighbours.length)];
        sendMessage(new Message(this.id,target,cycle,null));
    }

    @Override
    public void allMessageDisposed() {
        super.allMessageDisposed();
        System.out.println("Agent " + id + " cycle:" + cycle);
        sendMessage(new Message(id,id,cycle++,null));
        if (cycle == MAX_CYCLE){
            stopProcess();
        }
    }

    @Override
    public void runFinished() {

    }
}
