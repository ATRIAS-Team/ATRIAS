package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import com.google.gson.Gson;

public class Message {

    private String id;
    private String senderId;
    private String receiverId;

    private String comAct;
    private double simTime;
    private MessageContent content;

    // register/deregister/update;<name>,valX,valY
    // Constructor
    public Message(String id, String senderId, String receiverId, String comAct, double simTime, MessageContent content) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.comAct = comAct;
        this.simTime = simTime;
        this.content = content;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getComAct() {
        return comAct;
    }

    public Double getSimTime(){return simTime;}

    public MessageContent getContent() {
        return content;
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
