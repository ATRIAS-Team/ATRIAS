package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.areaagent;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.*;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.util.Location;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;

public class Plans {
    private AreaAgent areaAgent;
    private Utils utils;
    public Plans(AreaAgent areaAgent, Utils utils){
        this.areaAgent = areaAgent;
        this.utils = utils;
    }
    public void checkAreaMessagesBuffer(){
        if(areaAgent.areaMessagesBuffer.isEmpty()) return;
        Message bufferMessage = areaAgent.areaMessagesBuffer.read();
        String areaId = bufferMessage.getSenderId();

        switch (bufferMessage.getComAct()){
            case REQUEST:{
                if(areaAgent.locatedAgentList.size() < areaAgent.MIN_TRIKES) return;
                MessageContent messageContent = new MessageContent("PROPOSE");
                messageContent.values.addAll(bufferMessage.getContent().getValues());
                messageContent.values.add(areaAgent.cell);

                Message message = new Message( areaAgent.areaAgentId, areaId, Message.ComAct.REQUEST, JadexModel.simulationtime, messageContent);
                IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);
                service.sendMessage(message.serialize());
            }
        }
    }

    public void checkProposals(){
        for (DelegateInfo delegateInfo: areaAgent.jobsToDelegate) {
            long currentTime = Instant.now().toEpochMilli();
            if(delegateInfo.ts == -1 || currentTime < delegateInfo.ts + areaAgent.waitTime) return;
            if(areaAgent.proposalBuffer.isEmpty()){
                throw new RuntimeException("FAILED TRIP");
            }

            long minHops = 10;
            String bestAreaAgent = null;

            while (!areaAgent.proposalBuffer.isEmpty()){
                Message bufferMessage = areaAgent.proposalBuffer.read();
                String areaId = bufferMessage.getSenderId();
                String areaCell = bufferMessage.getContent().values.get(10);

                long hops = Cells.getHops(areaAgent.cell, areaCell);
                if(hops < minHops){
                    minHops = hops;
                    bestAreaAgent = areaId;
                }
            }

            MessageContent messageContent = new MessageContent("ASSIGN");
            messageContent.values = delegateInfo.job.toArrayList();
            Message message = new Message( areaAgent.areaAgentId, bestAreaAgent, Message.ComAct.REQUEST, JadexModel.simulationtime, messageContent);
            IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);
            service.sendMessage(message.serialize());

            areaAgent.jobsToDelegate.remove(delegateInfo);
        }
    }

    public void checkTrikeMessagesBuffer(){
        if(areaAgent.trikeMessagesBuffer.isEmpty()) return;
        Message bufferMessage = areaAgent.trikeMessagesBuffer.read();

        switch (bufferMessage.getComAct()){
            case INFORM:{
                ArrayList<String> locationParts = bufferMessage.getContent().getValues();
                //Location location = new Location(locationParts.get(0), Double.parseDouble(locationParts.get(1)), Double.parseDouble(locationParts.get(2)));
                Location location = null;
                if(locationParts != null){
                    location = new Location("", Double.parseDouble(locationParts.get(0)), Double.parseDouble(locationParts.get(1)));
                }
                LocatedAgent locatedAgent = new LocatedAgent(bufferMessage.getSenderId(), location);
                areaAgent.locatedAgentList.updateLocatedAgentList(locatedAgent, bufferMessage.getTimeStamp(), bufferMessage.getContent().getAction());
                break;
            }
            case REQUEST: {
                ArrayList<String> locatedAgentIds = new ArrayList<>();
                locatedAgentIds.add(bufferMessage.getContent().getValues().get(0));
                locatedAgentIds.add("#");

                //todo: when everywhere just the ID and not user: is used remove this
                String requestID = bufferMessage.getSenderId();

                if(areaAgent.locatedAgentList.size() > areaAgent.MIN_TRIKES){
                    for (LocatedAgent locatedAgent: areaAgent.locatedAgentList.LocatedAgentList) {
                        if ((!locatedAgent.getAgentID().equals(requestID))) {
                            locatedAgentIds.add(locatedAgent.getAgentID());
                        }
                    }
                }

                MessageContent messageContent = new MessageContent("sendNeighbourList", locatedAgentIds);
                //todo: crate a unique message id
                Message message = new Message(bufferMessage.getReceiverId(), bufferMessage.getSenderId(),
                        Message.ComAct.INFORM, JadexModel.simulationtime, messageContent);
                IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);

                service.sendMessage(message.serialize());
                break;
            }
        }
    }

    public void checkAcks(){
        Iterator<Message> iterator = areaAgent.sentMessages.iterator();

        while(iterator.hasNext()){
            long currentTimeStamp = Instant.now().toEpochMilli();
            Message message = iterator.next();
            if(currentTimeStamp >= message.getTimeStamp() + AreaConstants.SEND_WAIT_TIME){
                iterator.remove();
                IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);
                service.sendMessage(message.serialize());
                message.setTimeStamp(currentTimeStamp);
                areaAgent.sentMessages.add(message);
            }
        }
    }
}
