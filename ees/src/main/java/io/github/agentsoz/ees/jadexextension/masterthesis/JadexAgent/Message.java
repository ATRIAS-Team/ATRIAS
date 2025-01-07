package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import com.google.gson.Gson;

import java.util.UUID;

public class Message {
    private UUID id;
    private String senderId;
    private String receiverId;
    private ComAct comAct;

    private long timeStamp;

    private final MessageContent content;

    public Message(String senderId, String receiverId, ComAct comAct, long timeStamp, MessageContent content) {
        this.id = UUID.randomUUID();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.comAct = comAct;
        this.timeStamp = timeStamp;
        this.content = content;
    }

    public Message(String senderId, String receiverId, ComAct comAct, double timeStamp, MessageContent content) {
        this.id = UUID.randomUUID();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.comAct = comAct;
        this.timeStamp = (long) timeStamp;
        this.content = content;
    }

    public enum ComAct {
        INFORM, REQUEST, ACK, CALL_FOR_PROPOSAL, PROPOSE, REFUSE, ACCEPT_PROPOSAL, REJECT_PROPOSAL
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id){
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public ComAct getComAct() {
        return comAct;
    }

    public long getTimeStamp(){return timeStamp;}

    public MessageContent getContent() {
        return content;
    }

    public void setTimeStamp(long timeStamp){
        this.timeStamp = timeStamp;
    }

    public String serialize(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }
    public static Message deserialize(String messageJson){
        Gson gson = new Gson();
        return gson.fromJson(messageJson, Message.class);
    }

    public static Message ack(Message message){
        message.comAct = ComAct.ACK;
        String temp = message.senderId;
        message.senderId = message.receiverId;
        message.receiverId = temp;
        return message;
    }

    public static Message refuse(Message message){
        message.comAct = ComAct.REFUSE;
        String temp = message.senderId;
        message.senderId = message.receiverId;
        message.receiverId = temp;
        return message;
    }
}
