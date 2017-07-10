package edu.cqu.test;

import edu.cqu.core.AsyncAgent;
import edu.cqu.core.AsyncMailer;
import edu.cqu.core.Message;

import java.util.Map;

/**
 * Created by dyc on 2017/6/16.
 */
public class TestAsyncAgent extends AsyncAgent {
    private static final int MAX_MESSAGES = 5;
    private int currentMessageId;
    public TestAsyncAgent(int id, int[] domain, int[] neighbours, Map<Integer, int[][]> constraintCosts, Map<Integer, int[]> neighbourDomains, AsyncMailer mailer) {
        super(id, domain, neighbours, constraintCosts, neighbourDomains, mailer);
    }

    @Override
    protected void initRun() {
        for (int neighbourId : neighbours){
            sendMessage(new Message(this.id,neighbourId,currentMessageId,null));
        }
    }

    @Override
    public void disposeMessage(Message message) {
        System.out.println(message);
        if (currentMessageId++ >= MAX_MESSAGES){
            stopProcess();
        }
        else {
            for (int neighbourId : neighbours){
                sendMessage(new Message(this.id,neighbourId,currentMessageId,null));
            }
        }
    }

    @Override
    public void runFinished() {

    }
}
