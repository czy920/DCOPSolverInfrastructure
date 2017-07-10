package edu.cqu.core;

/**
 * Created by dyc on 2017/6/16.
 */
public class Message {
    private int idSender;
    private int idReceiver;
    private int type;
    private Object value;

    public Message(int idSender, int idReceiver, int type, Object value) {
        this.idSender = idSender;
        this.idReceiver = idReceiver;
        this.type = type;
        this.value = value;
    }

    public int getIdSender() {
        return idSender;
    }

    public void setIdSender(int idSender) {
        this.idSender = idSender;
    }

    public int getIdReceiver() {
        return idReceiver;
    }

    public void setIdReceiver(int idReceiver) {
        this.idReceiver = idReceiver;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return idSender + "->" + idReceiver + ":" + type;
    }
}
