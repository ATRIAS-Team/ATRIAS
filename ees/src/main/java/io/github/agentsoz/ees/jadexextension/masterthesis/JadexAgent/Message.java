package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import com.google.gson.Gson;

import java.util.UUID;

public class Message {
    private final UUID id;
    private final String senderId;
    private final String receiverId;
    private final ComAct comAct;

    private long timeStamp;

    private final MessageContent content;

    // register/deregister/update;<name>,valX,valY
    // Constructor
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
        INFORM, REQUEST, ACK, CALL_FOR_PROPOSAL, PROPOSE, ACCEPT_PROPOSAL, REJECT_PROPOSAL
    }

    // Getters
    public UUID getId() {
        return id;
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
        //return "{id:" + getId() + ";" + "senderId:" + getSenderId() + ";" + "receiverId:" + getReceiverId() + ";" + "comAct:" + getComAct().toString() + ";" + "content:" + getContent() + ";" + "simTime:" + getSimTime();
        Gson gson = new Gson();
        //System.out.println(gson.toJson(this));
        return gson.toJson(this);
    }
    public static Message deserialize(String messageJson){
        Gson gson = new Gson();
        /*
        String[] parts = messageStr.split(";");
        String id = parts[0].split(":")[1];
        String senderId = parts[1].split(":")[1];
        String receiverId = parts[2].split(":")[1];
        String comAct = parts[3].split(":")[1];
        String content = parts[4].split(":")[1];
        double simTime = Double.parseDouble(parts[5].split(":")[1]);
        return new Message(id, senderId, receiverId, comAct, content, simTime);
         */
        return gson.fromJson(messageJson, Message.class);
    }
}
