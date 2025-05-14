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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public class Plans {
    private AreaAgent areaAgent;
    private Utils utils;

    public Plans(AreaAgent areaAgent, Utils utils){
        this.areaAgent = areaAgent;
        this.utils = utils;
    }

    public void checkAssignedJobs(Message message){
        areaAgent.assignedJobs.add(new Job(message.getContent().getValues()));

        Message responseMsg = Message.ack(message);
        responseMsg.getContent().values = new ArrayList<>();

        SharedUtils.sendMessage(responseMsg.getReceiverId(), responseMsg.serialize());
    }

    public void checkAreaMessagesBuffer(Message bufferMessage){
        String areaId = bufferMessage.getSenderId();

        switch (bufferMessage.getComAct()){
            case CALL_FOR_PROPOSAL:{
                Message.ComAct responseAct;
                if(areaAgent.locatedAgentList.size() <= areaAgent.MIN_TRIKES){
                    responseAct = Message.ComAct.REFUSE;
                }else{
                    responseAct = Message.ComAct.PROPOSE;
                }
                MessageContent messageContent = new MessageContent(responseAct.name());
                messageContent.values.add(bufferMessage.getContent().getValues().get(1));
                messageContent.values.add(areaAgent.cell);

                messageContent.values.add("" + areaAgent.getLoad());

                Message message = new Message(areaAgent.areaAgentId, areaId, responseAct, SharedUtils.getSimTime(), messageContent);
                SharedUtils.sendMessage(message.getReceiverId(), message.serialize());
                break;
            }
            case REJECT_PROPOSAL: {
                break;
            }
        }
    }

    public void checkProposalBuffer(Message bufferMessage){
        String areaId = bufferMessage.getSenderId();
        String areaCell = bufferMessage.getContent().values.get(1);
        double load = Double.parseDouble(bufferMessage.getContent().values.get(2));

        synchronized (areaAgent.jobsToDelegate){
            for (DelegateInfo delegateInfo: areaAgent.jobsToDelegate){
                if(delegateInfo.job.getID().equals(bufferMessage.getContent().values.get(0))){
                    if(bufferMessage.getComAct() == Message.ComAct.PROPOSE){
                        long hops = Cells.getHops(areaAgent.cell, areaCell);
                        delegateInfo.agentHops.put(areaId, hops);
                        BigDecimal bigDecimal = BigDecimal.valueOf(load).setScale(3, RoundingMode.HALF_UP);
                        delegateInfo.agentLoad.put(areaId, bigDecimal.doubleValue());
                    }else{
                        delegateInfo.agentHops.put(areaId, Long.MAX_VALUE);
                        delegateInfo.agentLoad.put(areaId, Double.MAX_VALUE);
                    }
                }
            }
        }
    }

    public void delegateJobs(){
        synchronized (areaAgent.jobsToDelegate){
            Iterator<DelegateInfo> iterator = areaAgent.jobsToDelegate.iterator();
            while(iterator.hasNext()) {
                DelegateInfo delegateInfo = iterator.next();

                if (delegateInfo.timeStamp != -1) return;

                if(areaAgent.neighbourIds.isEmpty()){
                    iterator.remove();
                    return;
                }

                for (String neighbourId : areaAgent.neighbourIds) {
                    MessageContent messageContent = new MessageContent("BROADCAST", delegateInfo.job.toArrayList());
                    Message message = new Message(areaAgent.areaAgentId, neighbourId,
                            Message.ComAct.CALL_FOR_PROPOSAL, SharedUtils.getSimTime(), messageContent);
                    delegateInfo.timeStamp = SharedUtils.getSimTime();
                    SharedUtils.sendMessage(message.getReceiverId(), message.serialize());
                }
            }
        }
    }

    public void checkDelegateInfo(){
        long currentTime = SharedUtils.getSimTime();
        synchronized (areaAgent.jobsToDelegate){
            Iterator<DelegateInfo> iterator = areaAgent.jobsToDelegate.iterator();

            while(iterator.hasNext()) {
                DelegateInfo delegateInfo = iterator.next();
                if(delegateInfo.timeStamp == -1) return;
                if(currentTime < delegateInfo.timeStamp + AreaConstants.NEIGHBOURS_WAIT_TIME
                        && delegateInfo.agentHops.size() != areaAgent.neighbourIds.size()) return;


                String bestAreaAgent = null;
                long lowestHops = Long.MAX_VALUE;
                double lowestLoad = AreaConstants.NO_TRIKES_NO_TRIPS_LOAD;

                //  check if it is a rebalance trip
                if(delegateInfo.job.getID().startsWith("area")) {
                    Map<String, Long> choices = areaAgent.utils.findLowestChoices(delegateInfo.agentLoad,
                            delegateInfo.agentHops, lowestLoad, 0.0);


                    //  find best agent by lowest hops
                    for (Map.Entry<String, Long> entry : choices.entrySet()) {
                        if (entry.getValue() < lowestHops) {
                            lowestHops = entry.getValue();
                            bestAreaAgent = entry.getKey();
                        }
                    }
                }
                else{
                    long stop = 1L;
                    do{
                        Map<String, Double> choices = areaAgent.utils.findLowestChoices(delegateInfo.agentHops,
                                delegateInfo.agentLoad, lowestHops, stop);

                        //  find best agent by lowest load
                        for (Map.Entry<String, Double> entry : choices.entrySet()) {
                            if (entry.getValue() < lowestLoad) {
                                lowestLoad = entry.getValue();
                                bestAreaAgent = entry.getKey();
                            }
                        }
                        stop++;
                    }while (stop < 4 && lowestLoad >= AreaConstants.NO_TRIKES_NO_TRIPS_LOAD);

                    if(lowestLoad >= AreaConstants.NO_TRIKES_NO_TRIPS_LOAD){
                        iterator.remove();
                        return;
                    }
                }

                //  if no best found, cancel CNP
                if(bestAreaAgent == null){
                    iterator.remove();
                    return;
                }

                areaAgent.lastDelegateRequestTS = SharedUtils.getSimTime();

                MessageContent messageContent = new MessageContent("ASSIGN");
                messageContent.values = delegateInfo.job.toArrayList();
                Message message = new Message( areaAgent.areaAgentId, bestAreaAgent, Message.ComAct.ACCEPT_PROPOSAL, SharedUtils.getSimTime(), messageContent);
                //IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);
                areaAgent.requests.add(message);
                SharedUtils.sendMessage(message.getReceiverId(), message.serialize());

                iterator.remove();
            }
        }
    }

    public void checkTrikeMessagesBuffer(Message bufferMessage){
        switch (bufferMessage.getComAct()){
            case INFORM:{
                ArrayList<String> locationParts = bufferMessage.getContent().getValues();
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
                synchronized (areaAgent.locatedAgentList.LocatedAgentList){
                    boolean isPart = false;
                    for (LocatedAgent locatedAgent: areaAgent.locatedAgentList.LocatedAgentList) {
                        if ((!locatedAgent.getAgentID().equals(requestID))) {
                            locatedAgentIds.add(locatedAgent.getAgentID());
                        }else{
                            isPart = true;
                        }
                    }

                    /*
                    if(areaAgent.locatedAgentList.size() <= areaAgent.MIN_TRIKES && !isPart && !isMine){
                        locatedAgentIds = new ArrayList<>();
                        locatedAgentIds.add(bufferMessage.getContent().getValues().get(0));
                        locatedAgentIds.add("#");
                    }
                     */
                }

                MessageContent messageContent = new MessageContent("sendNeighbourList", locatedAgentIds);
                Message message = new Message(bufferMessage.getReceiverId(), bufferMessage.getSenderId(),
                        Message.ComAct.INFORM, SharedUtils.getSimTime(), messageContent);
                SharedUtils.sendMessage(message.getReceiverId(), message.serialize());
                break;
            }
            case ACK: {
                synchronized (areaAgent.requests){
                    areaAgent.requests.removeIf(request -> request.getId().equals(bufferMessage.getId()));
                }
                break;
            }
            case NACK:{
                synchronized (areaAgent.requests){
                    areaAgent.requests.removeIf(request -> request.getId().equals(bufferMessage.getId()));
                }
                Job job = new Job(bufferMessage.getContent().getValues());
                areaAgent.utils.sendJobToAgent(List.of(job));
                break;
            }
        }
    }

    public void checkRequestTimeouts(){
        synchronized (areaAgent.requests){
            Iterator<Message> iterator = areaAgent.requests.iterator();
            long currentTimeStamp = SharedUtils.getSimTime();

            while(iterator.hasNext()){
                Message message = iterator.next();
                if(currentTimeStamp >= message.getTimeStamp() + AreaConstants.REQUEST_WAIT_TIME){
                    if(message.getAttempts() < 1){
                        iterator.remove();
                        //IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);
                        message.setTimeStamp(currentTimeStamp);
                        areaAgent.requests.add(message.reattempt());
                        SharedUtils.sendMessage(message.getReceiverId(), message.serialize());
                    }else{
                        iterator.remove();
                    }
                }
            }
        }
    }

    public void checkTrikeCount(){
        if(areaAgent.MIN_TRIKES == -1 && JadexModel.simulationtime > 0){
            areaAgent.MIN_TRIKES = (int) Math.floor(areaAgent.locatedAgentList.size() * 0.8);
        }

        long currentTime = SharedUtils.getSimTime();

        synchronized (areaAgent.loadLock){
        if(currentTime >= areaAgent.rebalanceInitTS && currentTime >= areaAgent.lastMinTrikesUpdateTS + 60000) {
            if (areaAgent.getLoad() <= AreaConstants.UNLOAD_TRIGGER) {
                areaAgent.lastMinTrikesUpdateTS = currentTime;
                areaAgent.MIN_TRIKES = (int) Math.floor(areaAgent.locatedAgentList.size() * AreaConstants.UNLOAD_FACTOR);
            }

            double normalizedLoad = areaAgent.getLoad();
            if(normalizedLoad >= AreaConstants.NO_TRIKES_NO_TRIPS_LOAD){
                normalizedLoad -= AreaConstants.NO_TRIKES_NO_TRIPS_LOAD;
            }

            boolean isOverloadTrigger =
                    (normalizedLoad >= AreaConstants.OVERLOAD_TRIGGER);

            boolean isEmpty = areaAgent.MIN_TRIKES == 0 && areaAgent.getLoad() == AreaConstants.NO_TRIKES_NO_TRIPS_LOAD;

            if (isOverloadTrigger && !isEmpty) {
                areaAgent.lastMinTrikesUpdateTS = currentTime;
                areaAgent.MIN_TRIKES = (int) Math.floor(areaAgent.locatedAgentList.size() * AreaConstants.OVERLOAD_FACTOR);
                if (areaAgent.MIN_TRIKES == 0) {
                    areaAgent.MIN_TRIKES = 1;
                }
            }
        }
        }

        boolean isMin = areaAgent.jobsToDelegate.size() + areaAgent.locatedAgentList.size() < areaAgent.MIN_TRIKES;
        synchronized (areaAgent.jobsToDelegate){
            if(isMin && currentTime >= areaAgent.lastDelegateRequestTS + 60000){
                Location cellLocation = Cells.getCellLocation(areaAgent.cell);
                LocalDateTime dt = SharedUtils.getCurrentDateTime();

                Job job = new Job(UUID.randomUUID().toString(), areaAgent.areaAgentId + "   " + UUID.randomUUID(),
                        dt, dt, cellLocation, cellLocation);
                DelegateInfo delegateInfo = new DelegateInfo(job);
                System.out.println(areaAgent.areaAgentId + " ask for trikes!");
                areaAgent.jobsToDelegate.add(delegateInfo);
                areaAgent.lastDelegateRequestTS = currentTime;
            }
        }
    }

    public void updateTripsLoad(){
        long currentTime = SharedUtils.getSimTime();
        if(currentTime >= areaAgent.lastLoadUpdateTS + 60000) {
            areaAgent.lastLoadUpdateTS = currentTime;
            synchronized (areaAgent.loadLock) {
                if (areaAgent.getLoad() < AreaConstants.NO_TRIKES_NO_TRIPS_LOAD) {
                    double newLoad = areaAgent.getLoad() * AreaConstants.LOAD_DECAY_FACTOR;

                    BigDecimal bigDecimal = BigDecimal.valueOf(newLoad).setScale(4, RoundingMode.HALF_UP);
                    areaAgent.setLoad(bigDecimal.doubleValue());
                } else if (areaAgent.getLoad() > AreaConstants.NO_TRIKES_NO_TRIPS_LOAD) {
                    double deltaDecay = (areaAgent.getLoad() - AreaConstants.NO_TRIKES_NO_TRIPS_LOAD) * AreaConstants.LOAD_DECAY_FACTOR;
                    double newLoad = deltaDecay + AreaConstants.NO_TRIKES_NO_TRIPS_LOAD;

                    BigDecimal bigDecimal = BigDecimal.valueOf(newLoad).setScale(4, RoundingMode.HALF_UP);
                    areaAgent.setLoad(bigDecimal.doubleValue());
                }
            }
        }
    }
}
