package io.github.agentsoz.ees.shared;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.google.gson.Gson;

import java.util.UUID;

public class Message {
    private UUID id;
    private String senderId;
    private String receiverId;
    private ComAct comAct;

    private long timeStamp;

    private int attempts = 1;

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
        INFORM, REQUEST, ACK, NACK, CALL_FOR_PROPOSAL, PROPOSE, REFUSE, ACCEPT_PROPOSAL, REJECT_PROPOSAL
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

    public static Message nack(Message message){
        message.comAct = ComAct.NACK;
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

    public Message reattempt(){
        this.attempts++;
        return this;
    }

    public int getAttempts(){
        return attempts;
    }
}
