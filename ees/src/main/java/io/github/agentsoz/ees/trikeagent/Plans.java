package io.github.agentsoz.ees.trikeagent;

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

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.v3.AgentNotFoundException;
import io.github.agentsoz.ees.firebase.FirebaseHandler;
import io.github.agentsoz.ees.shared.*;
import io.github.agentsoz.ees.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.JadexService.NotifyService.INotifyService;
import io.github.agentsoz.ees.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.ees.Run.JadexModel;
import io.github.agentsoz.ees.Run.TrikeMain;
import io.github.agentsoz.ees.simagent.SimIDMapper;
import io.github.agentsoz.ees.util.csvLogger;
import io.github.agentsoz.util.Location;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.search.ServiceQuery;

import java.time.LocalDateTime;
import java.util.*;

import static io.github.agentsoz.ees.trikeagent.TrikeConstants.*;

public class Plans {
    private final Utils utils;
    private final TrikeAgent trikeAgent;

    public Plans(TrikeAgent trikeAgent, Utils utils){
        this.utils = utils;
        this.trikeAgent = trikeAgent;
    }

    public void reactToAgentIDAdded()
    {
        if (trikeAgent.agentID != null) // only react if the agentID exists
        {
            SharedUtils.trikeAgentMap.put(trikeAgent.agentID, trikeAgent);

            if (SimIDMapper.NumberSimInputAssignedID.size() == JadexModel.SimSensoryInputBrokernumber) // to make sure all SimInputBroker also receives its ID so vehicle agent could choose one SimInputBroker ID to register
                if (!trikeAgent.sent) { // to make sure the following part only executed once
                    trikeAgent.sent = true;
                    System.out.println("The agentid assigned for this vehicle agent is " + trikeAgent.agentID);
                    // setTag for itself to receive direct communication from SimSensoryInputBroker when service INotifyService is used.
                    IServiceIdentifier sid = ((IService) trikeAgent.agent.getProvidedService(INotifyService.class)).getServiceId();
                    trikeAgent.agent.setTags(sid, "" + trikeAgent.agentID);
                    //choosing one SimSensoryInputBroker to receive data from MATSIM
                    trikeAgent.currentSimInputBroker = utils.getRandomSimInputBroker();

                    // setTag for itself to receive direct communication from TripRequestControlAgent when service IsendTripService is used.
                    IServiceIdentifier sid2 = ((IService) trikeAgent.agent.getProvidedService(IAreaTrikeService.class)).getServiceId();
                    trikeAgent.agent.setTags(sid2, "" + trikeAgent.agentID);

                    //communicate with SimSensoryInputBroker when knowing the serviceTag of the SimSensoryInputBroker.
                    ServiceQuery<INotifyService2> query = new ServiceQuery<>(INotifyService2.class);
                    query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
                    query.setServiceTags("" + trikeAgent.currentSimInputBroker); // choose to communicate with the SimSensoryInputBroker that it registered befre
                    Collection<INotifyService2> service = trikeAgent.agent.getLocalServices(query);
                    for (INotifyService2 cs : service) {
                        cs.NotifyotherAgent(trikeAgent.agentID); // write the agentID into the list of the SimSensoryInputBroker that it chose before
                    }
                    System.out.println("agent "+ trikeAgent.agentID +"  registers at " + trikeAgent.currentSimInputBroker);
                    // Notify TripRequestControlAgent and JADEXModel
                    TrikeMain.TrikeAgentNumber = TrikeMain.TrikeAgentNumber+1;
                    JadexModel.flagMessage2();
                    //action perceive is sent to matsim only once in the initiation phase to register to receive events
                    utils.SendPerceivetoAdc();

                    trikeAgent.agentLocation = Cells.trikeRegisterLocations.get(trikeAgent.agentID);
                    utils.sendAreaAgentUpdate("register");

                    // Print the initial location for verification
                    System.out.println("Agent " + trikeAgent.agentID + " initial location: " + trikeAgent.agentLocation);

                    if(SharedConstants.FIREBASE_ENABLED){
                        //update the location of the agent
                        FirebaseHandler.updateAgentLocation(trikeAgent.agentID, trikeAgent.agentLocation);
                    }

                    //csvLogger csvLogger = new csvLogger(agentID);
                    csvLogger csvLogger = new csvLogger(trikeAgent.agentID, CNP_ACTIVE, THETA, ALLOW_CUSTOMER_MISS, CHARGING_THRESHOLD, commitThreshold, DISTANCE_FACTOR);

                }
        }
    }

    public void newChargingTrip() {
        {
            if (trikeAgent.estimateBatteryAfterTIP.get(0) < CHARGING_THRESHOLD && trikeAgent.chargingTripAvailable.equals("0")){
                //utils.estimateBatteryAfterTIP();
                trikeAgent.chargingTripCounter+=1;
                String tripID = "CH";
                tripID = tripID.concat(Integer.toString(trikeAgent.chargingTripCounter));
                Trip chargingTrip = new Trip(tripID, "ChargingTrip", utils.getNextChargingStation(), "NotStarted");
                chargingTrip.setVaTime(SharedUtils.getCurrentDateTime());

                double distToStation;
                LocalDateTime prevEndTime;

                if(!trikeAgent.tripList.isEmpty()){
                    int lastTripIndex = trikeAgent.tripList.size() - 1;
                    Trip lastTrip = trikeAgent.tripList.get(lastTripIndex);

                    distToStation = utils.getDistanceBetween(lastTrip.getEndPosition(), chargingTrip.getStartPosition());
                    prevEndTime = lastTrip.getEndTime();

                }else if(!trikeAgent.currentTrip.isEmpty()){
                    Trip lastTrip = trikeAgent.currentTrip.get(0);

                    distToStation = utils.getDistanceBetween(lastTrip.getEndPosition(), chargingTrip.getStartPosition());
                    prevEndTime = lastTrip.getEndTime();
                }else{
                    distToStation = utils.getDrivingDistanceTo(chargingTrip.getStartPosition());
                    prevEndTime = SharedUtils.getCurrentDateTime();
                }

                long drivingTimeInSec = (long) (((distToStation / 1000.0) / DRIVING_SPEED)*60*60);

                chargingTrip.setEndTime(prevEndTime.plusSeconds(drivingTimeInSec + 1800));
                chargingTrip.setArriveTime(prevEndTime.plusSeconds(drivingTimeInSec));


                trikeAgent.tripList.add(chargingTrip);
                trikeAgent.chargingTripAvailable = "1";

                utils.eventTracker.TripList_BeliefUpdated(trikeAgent);
                utils.eventTracker.ChargingTripCreation(trikeAgent, chargingTrip);
            }
        }
    }

    public void evaluateDecisionTask()
    {
        Iterator<DecisionTask> iterator = trikeAgent.decisionTasks.values().iterator();
        while (iterator.hasNext()){
            utils.selectNextAction(iterator);
        }
    }

    public void executeTrips() {
        utils.newCurrentTrip();

        Trip current = null;
        if(!trikeAgent.currentTrip.isEmpty()){
            current = trikeAgent.currentTrip.get(0);
        }

        if (current != null) {
            switch (current.getProgress()) {
                case "NotStarted": {
                    utils.currentTripStatus();
                    trikeAgent.canExecute = false;
                    utils.sendDriveTotoAdc();
                    utils.updateCurrentTripProgress("DriveToStart");
                    utils.currentTripStatus();
                    try {
                        Location[] locations = utils.getCurrentLocation();
                        System.out.println("AgentID: " + trikeAgent.agentID + " location: " +
                                Arrays.toString(locations));


                        System.out.println("AgentID: " + trikeAgent.agentID + " vanilla distance to start: " + (Location.distanceBetween(locations[0], trikeAgent.currentTrip.get(0).startPosition)));

                        System.out.println("AgentID: " + trikeAgent.agentID + " distance to start: " +
                                utils.getDrivingDistanceTo(trikeAgent.currentTrip.get(0).startPosition));
                    } catch (AgentNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
                case "AtStartLocation": {
                    utils.currentTripStatus();
                    switch (current.getTripType()) {
                        case "ChargingTrip": {
                            if(!trikeAgent.isCharging){
                                trikeAgent.isCharging = true;
                            }
                            if(current.getEndTime().isAfter(SharedUtils.getCurrentDateTime())) return;

                            trikeAgent.trikeBattery.loadBattery();
                            utils.updateCurrentTripProgress("Finished");
                            trikeAgent.chargingTripAvailable = "0";
                            trikeAgent.isCharging = false;
                            executeTrips();
                            break;
                        }
                        case "CustomerTrip": {
                            if (utils.customerMiss(current)) { // customer not there
                                utils.updateCurrentTripProgress("Failed");
                                executeTrips();
                            } else {
                                trikeAgent.canExecute = false;
                                utils.sendDriveTotoAdc();
                                utils.updateCurrentTripProgress("DriveToEnd");
                                utils.currentTripStatus();
                                try {
                                    Location[] locations = utils.getCurrentLocation();
                                    System.out.println("AgentID: " + trikeAgent.agentID + " location: " +
                                            Arrays.toString(locations));


                                    System.out.println("AgentID: " + trikeAgent.agentID + " vanilla distance to end: " + (Location.distanceBetween(locations[0], trikeAgent.currentTrip.get(0).endPosition)));


                                    System.out.println("AgentID: " + trikeAgent.agentID + " distance to end: " +
                                            utils.getDrivingDistanceTo(trikeAgent.currentTrip.get(0).endPosition));
                                } catch (AgentNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            break;
                        }
                        default: {
                            utils.updateCurrentTripProgress("Finished");
                            executeTrips();
                            break;
                        }
                    }
                    break;
                }
                case "AtEndLocation": {
                    utils.currentTripStatus();
                    utils.updateCurrentTripProgress("Finished");
                    executeTrips();
                    break;
                }
                case "Finished":
                case "Failed": {
                    utils.currentTripStatus();
                    trikeAgent.currentTrip.remove(0);
                    executeTrips();
                    break;
                }
            }
            utils.estimateBatteryAfterTIP();
        }
    }

    public void sensoryUpdate(List<ActionContent> actionContents) {
        for (ActionContent actionContent:actionContents) {
            if (!trikeAgent.currentTrip.isEmpty()) {
                if (actionContent.getAction_type().equals("drive_to") && actionContent.getState() == ActionContent.State.PASSED) {
                    System.out.println("Agent " + trikeAgent.agentID + " finished with the previous trip and now can take the next trip");
                    System.out.println("AgentID: " + trikeAgent.agentID + actionContent.getParameters()[0]);
                    double metersDriven = Double.parseDouble((String) actionContent.getParameters()[1]);
                    utils.updateBeliefAfterAction(metersDriven);
                    utils.updateAtInputBroker();
                    trikeAgent.canExecute = true;
                }
            }
        }
    }

    public void updateLocation(){
        try{
            Location currentLocation = utils.getCurrentLocation()[0];
            trikeAgent.agentLocation = currentLocation;

            if(SharedConstants.FIREBASE_ENABLED){
                FirebaseHandler.updateAgentLocation(trikeAgent.agentID, currentLocation);
            }

            utils.sendAreaAgentUpdate("update");
            utils.eventTracker.AgentPosition_BeliefUpdated(trikeAgent);
        }catch (Exception e){
            System.err.println(e.getMessage());
        }
    }
    public void checkMessagesBuffer(Message message) {
        //  asking area for trikes
        ArrayList<String> neighborList = message.getContent().getValues();
        String jobID = neighborList.remove(0); //JobID
        neighborList.remove(0); //#

        DecisionTask decisionTask = trikeAgent.decisionTasks.get(jobID);
        if (decisionTask == null) return;

        if (decisionTask.getStatus() == DecisionTask.Status.WAITING_FOR_NEIGHBOURLIST){
            synchronized (trikeAgent.requests){
                trikeAgent.requests.removeIf(request -> request.getId().equals(message.getId()));
            }
            Collections.shuffle(neighborList);
            synchronized (decisionTask.getAgentIds()){
                int counter = 0;
                for(String neighbor: neighborList){
                    decisionTask.getAgentIds().add(neighbor);
                    if(++counter == MAX_CNP_TRIKES){
                        break;
                    }
                }
            }
            decisionTask.numResponses.addAndGet(1);
        }
    }

    public void checkCNPBuffer(Message message) {
        switch (message.getComAct()){
            case CALL_FOR_PROPOSAL: {
                Job job = new Job(message.getContent().getValues());
                DecisionTask decisionTask = new DecisionTask(job, message.getSenderId(), DecisionTask.Status.PROPOSED);
                trikeAgent.AddDecisionTask(decisionTask);
                break;
            }
            case PROPOSE: {
                String jobID = message.getContent().getValues().get(0);
                DecisionTask decisionTask = trikeAgent.decisionTasks.get(jobID);
                if (decisionTask == null) break;

                if(decisionTask.getStatus() == DecisionTask.Status.WAITING_FOR_PROPOSALS){
                    Double propose = Double.parseDouble(message.getContent().getValues().get(2));
                    String senderID = message.getSenderId();
                    decisionTask.setUtilityScore(senderID, propose);
                    decisionTask.numResponses.addAndGet(1);
                }else{
                    System.out.println("ERROR1");
                }
                break;
            }
            case ACCEPT_PROPOSAL: {
                String jobID = message.getContent().getValues().get(0);
                DecisionTask decisionTask = trikeAgent.decisionTasks.get(jobID);
                if (decisionTask == null){
                    System.out.println("ERROR2");
                    break;
                }
                if (decisionTask.getStatus() == DecisionTask.Status.WAITING_FOR_MANAGER) {
                    decisionTask.extra = message.getId().toString();
                    decisionTask.setStatus(DecisionTask.Status.READY_FOR_CONFIRMATION);
                }
                else if(decisionTask.getStatus() == DecisionTask.Status.READY_FOR_CONFIRMATION){
                    //  do nothing
                }
                else {
                    Message refuseMessage = Message.refuse(message);
                    //IAreaTrikeService service = messageToService(trikeAgent.agent, refuseMessage);
                    SharedUtils.sendMessage(refuseMessage.getReceiverId(), refuseMessage.serialize());
                }
                break;
            }
            case REJECT_PROPOSAL: {
                String jobID = message.getContent().getValues().get(0);
                DecisionTask decisionTask = trikeAgent.decisionTasks.get(jobID);
                if (decisionTask == null) break;

                if (decisionTask.getStatus() == DecisionTask.Status.WAITING_FOR_MANAGER) {
                    trikeAgent.decisionTasks.get(jobID).setStatus(DecisionTask.Status.NOT_ASSIGNED);
                }
                break;
            }
            case ACK: {
                String jobID = message.getContent().getValues().get(0);
                DecisionTask decisionTask = trikeAgent.decisionTasks.get(jobID);
                if (decisionTask == null) break;
                if (decisionTask.getStatus() == DecisionTask.Status.WAITING_FOR_CONFIRMATIONS) {
                    decisionTask.setStatus(DecisionTask.Status.DELEGATED);
                    synchronized (trikeAgent.requests){
                        trikeAgent.requests.removeIf(request -> request.getId().equals(message.getId()));
                    }
                }
                break;
            }
            case REFUSE:{
                String jobID = message.getContent().getValues().get(0);
                DecisionTask decisionTask = trikeAgent.decisionTasks.get(jobID);
                if (decisionTask == null) break;
                if (decisionTask.getStatus() == DecisionTask.Status.WAITING_FOR_CONFIRMATIONS) {
                    decisionTask.setStatus(DecisionTask.Status.COMMIT);
                    synchronized (trikeAgent.requests){
                        trikeAgent.requests.removeIf(request -> request.getId().equals(message.getId()));
                    }
                }
                break;
            }
        }
    }

    public void  checkJobBuffer(Message message){
        if(!message.getSenderId().equals(Cells.cellAgentMap.get(trikeAgent.cell))){
            Message response = Message.nack(message);
            //IAreaTrikeService service = messageToService(trikeAgent.agent, response);
            SharedUtils.sendMessage(response.getReceiverId(), response.serialize());
            return;
        }

        Job job = new Job(message.getContent().getValues());
        DecisionTask decisionTask = new DecisionTask(job, message.getSenderId(), DecisionTask.Status.NEW);
        System.out.println("Job " + job.getID() + " accepted!");
        trikeAgent.AddDecisionTask(decisionTask);

        Message response = Message.ack(message);
        //IAreaTrikeService service = messageToService(trikeAgent.agent, response);
        SharedUtils.sendMessage(response.getReceiverId(), response.serialize());
    }

    public void checkRequestTimeouts(){
        synchronized (trikeAgent.requests){
            Iterator<Message> iterator = trikeAgent.requests.iterator();
            long currentTimeStamp = SharedUtils.getSimTime();

            while(iterator.hasNext()){
                Message message = iterator.next();
                if(currentTimeStamp >= message.getTimeStamp() + TrikeConstants.REQUEST_WAIT_TIME){
                    if(message.getAttempts() < 1){
                        //IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);
                        message.reattempt();
                        message.setTimeStamp(currentTimeStamp);
                        SharedUtils.sendMessage(message.getReceiverId(), message.serialize());
                    }else{
                        iterator.remove();
                    }
                }
            }
        }
    }
}