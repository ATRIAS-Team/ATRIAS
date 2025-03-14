package io.github.agentsoz.ees.areaagent;

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

import io.github.agentsoz.ees.shared.*;
import io.github.agentsoz.ees.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.Run.JadexModel;
import io.github.agentsoz.util.Location;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class Plans {
    private AreaAgent areaAgent;
    private Utils utils;

    public Plans(AreaAgent areaAgent, Utils utils){
        this.areaAgent = areaAgent;
        this.utils = utils;
    }

    public void checkAssignedJobs(){
        while (!areaAgent.jobRingBuffer.isEmpty()){
            Message message = areaAgent.jobRingBuffer.read();
            areaAgent.assignedJobs.add(new Job(message.getContent().getValues()));

            Message responseMsg = Message.ack(message);
            responseMsg.getContent().values = new ArrayList<>();

            IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, responseMsg);
            service.sendMessage(responseMsg.serialize());
        }
    }

    public void checkAreaMessagesBuffer(){
        while(!areaAgent.areaMessagesBuffer.isEmpty()){
        Message bufferMessage = areaAgent.areaMessagesBuffer.read();
        String areaId = bufferMessage.getSenderId();

        switch (bufferMessage.getComAct()){
                case CALL_FOR_PROPOSAL:{
                    Message.ComAct responseAct;
                    if(areaAgent.locatedAgentList.size() <= AreaConstants.MIN_TRIKES){
                        responseAct = Message.ComAct.REFUSE;
                    }else{
                        responseAct = Message.ComAct.PROPOSE;
                    }
                    MessageContent messageContent = new MessageContent(responseAct.name());
                    messageContent.values.add(bufferMessage.getContent().getValues().get(1));
                    messageContent.values.add(areaAgent.cell);

                    Message message = new Message( areaAgent.areaAgentId, areaId, responseAct, SharedUtils.getSimTime(), messageContent);
                    IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);
                    service.sendMessage(message.serialize());
                    break;
                }
                case REJECT_PROPOSAL: {
                    break;
                }
            }
        }
    }

    public void checkProposalBuffer(){
        while (!areaAgent.proposalBuffer.isEmpty()){
            Message bufferMessage = areaAgent.proposalBuffer.read();
            String areaId = bufferMessage.getSenderId();
            String areaCell = bufferMessage.getContent().values.get(1);
            for (DelegateInfo delegateInfo: areaAgent.jobsToDelegate){
                if(delegateInfo.job.getID().equals(bufferMessage.getContent().values.get(0))){
                    if(bufferMessage.getComAct() == Message.ComAct.PROPOSE){
                        long hops = Cells.getHops(areaAgent.cell, areaCell);
                        delegateInfo.agentHops.put(areaId, hops);
                    }else{
                        delegateInfo.agentHops.put(areaId, Long.MAX_VALUE);
                    }
                }
            }
        }
    }

    public void delegateJobs(){
        for (DelegateInfo delegateInfo: areaAgent.jobsToDelegate) {
            if(delegateInfo.timeStamp != -1) return;
            for (String neighbourId: areaAgent.neighbourIds){
                MessageContent messageContent = new MessageContent("BROADCAST", delegateInfo.job.toArrayList());
                Message message = new Message(areaAgent.areaAgentId, neighbourId,
                        Message.ComAct.CALL_FOR_PROPOSAL, SharedUtils.getSimTime(), messageContent);
                IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);
                delegateInfo.timeStamp = SharedUtils.getSimTime();
                service.sendMessage(message.serialize());
            }
        }
    }

    public void checkDelegateInfo(){
        long currentTime = SharedUtils.getSimTime();
        Iterator<DelegateInfo> iterator = areaAgent.jobsToDelegate.iterator();

        while(iterator.hasNext()) {
            DelegateInfo delegateInfo = iterator.next();
          if(delegateInfo.timeStamp == -1) return;
          if(currentTime < delegateInfo.timeStamp + AreaConstants.NEIGHBOURS_WAIT_TIME
                  && delegateInfo.agentHops.size() != areaAgent.neighbourIds.size()) return;

          Iterator<Map.Entry<String, Long>> hopsIterator = delegateInfo.agentHops.entrySet().iterator();
          long bestHops = Long.MAX_VALUE;
          String bestAreaAgent = null;

          while (hopsIterator.hasNext()){
              Map.Entry<String, Long> entry = hopsIterator.next();
              if(entry.getValue() < bestHops){
                  bestHops = entry.getValue();
                  bestAreaAgent = entry.getKey();
              }
          }

          if(bestAreaAgent == null){
              return;
          }

            MessageContent messageContent = new MessageContent("ASSIGN");
            messageContent.values = delegateInfo.job.toArrayList();
            Message message = new Message( areaAgent.areaAgentId, bestAreaAgent, Message.ComAct.ACCEPT_PROPOSAL, SharedUtils.getSimTime(), messageContent);
            IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);
            service.sendMessage(message.serialize());
            areaAgent.requests.add(message);

            iterator.remove();
        }
    }

    public void checkTrikeMessagesBuffer(){
        while (!areaAgent.messagesBuffer.isEmpty()){
            Message bufferMessage = areaAgent.messagesBuffer.read();

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
                    if(areaAgent.locatedAgentList.size() > AreaConstants.MIN_TRIKES){
                        for (LocatedAgent locatedAgent: areaAgent.locatedAgentList.LocatedAgentList) {
                            if ((!locatedAgent.getAgentID().equals(requestID))) {
                                locatedAgentIds.add(locatedAgent.getAgentID());
                            }
                        }
                    }

                    MessageContent messageContent = new MessageContent("sendNeighbourList", locatedAgentIds);
                    //todo: crate a unique message id
                    Message message = new Message(bufferMessage.getReceiverId(), bufferMessage.getSenderId(),
                            Message.ComAct.INFORM, SharedUtils.getSimTime(), messageContent);
                    IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);

                    service.sendMessage(message.serialize());
                    break;
                }
                case ACK: {
                    areaAgent.requests.removeIf(request -> request.getId().equals(bufferMessage.getId()));
                    break;
                }
            }
        }
    }

    public void checkRequestTimeouts(){
        Iterator<Message> iterator = areaAgent.requests.iterator();
        long currentTimeStamp = SharedUtils.getSimTime();

        while(iterator.hasNext()){
            Message message = iterator.next();
            if(currentTimeStamp >= message.getTimeStamp() + AreaConstants.REQUEST_WAIT_TIME){
                iterator.remove();
                IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);
                service.sendMessage(message.serialize());
                message.setTimeStamp(currentTimeStamp);
                areaAgent.requests.add(message);
            }
        }
    }

    public void checkTrikeCount(){
        if(areaAgent.locatedAgentList.size() < AreaConstants.MIN_TRIKES && JadexModel.simulationtime > 0 && areaAgent.jobsToDelegate.size() < AreaConstants.MIN_TRIKES){
            Location cellLocation = Cells.getCellLocation(areaAgent.cell);
            LocalDateTime dt = SharedUtils.getCurrentDateTime();

            Job job = new Job(UUID.randomUUID().toString(), areaAgent.areaAgentId + "   " + UUID.randomUUID().toString(),
                    dt, dt, cellLocation, cellLocation);
            DelegateInfo delegateInfo = new DelegateInfo(job);
            areaAgent.jobsToDelegate.add(delegateInfo);
        }
    }
}
