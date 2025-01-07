package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.trikeagent;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.ees.firebase.FirebaseHandler;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.*;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.INotifyService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.TrikeMain;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.search.ServiceQuery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.trikeagent.TrikeConstants.*;
import static io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService.messageToService;

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

                    if(FIREBASE_ENABLED){
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
                trikeAgent.tripList.add(chargingTrip);
                trikeAgent.chargingTripAvailable = "1";
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
            utils.currentTripStatus();
            switch (current.getProgress()) {
                case "NotStarted": {
                    trikeAgent.canExecute = false;
                    trikeAgent.isMatsimFree = false;
                    utils.sendDriveTotoAdc();
                    utils.updateCurrentTripProgress("DriveToStart");
                    break;
                }
                case "AtStartLocation": {
                    switch (current.getTripType()) {
                        case "ChargingTrip": {
                            trikeAgent.trikeBattery.loadBattery();
                            utils.updateCurrentTripProgress("Finished");
                            trikeAgent.chargingTripAvailable = "0";
                            executeTrips();
                            break;
                        }
                        case "CustomerTrip": {
                            if (utils.customerMiss(current)) { // customer not there
                                utils.updateCurrentTripProgress("Failed");
                            } else {
                                trikeAgent.canExecute = false;
                                trikeAgent.isMatsimFree = false;
                                utils.sendDriveTotoAdc();
                                utils.updateCurrentTripProgress("DriveToEnd");
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
                    utils.updateCurrentTripProgress("Finished");
                    executeTrips();
                    break;
                }
                case "Finished":
                case "Failed": {
                    trikeAgent.currentTrip.remove(0);
                    utils.currentTripStatus();
                    executeTrips();
                    break;
                }
            }
            utils.estimateBatteryAfterTIP();
        }
    }

    public void sensoryUpdate() {
        if(trikeAgent.actionContentRingBuffer.isEmpty()) return;
        ActionContent actionContent = trikeAgent.actionContentRingBuffer.read();
        if (trikeAgent.isMatsimFree && !trikeAgent.currentTrip.isEmpty()) {
            if (actionContent.getAction_type().equals("drive_to") && actionContent.getState() == ActionContent.State.PASSED) {
                System.out.println("Agent " + trikeAgent.agentID + " finished with the previous trip and now can take the next trip");
                System.out.println("AgentID: " + trikeAgent.agentID + actionContent.getParameters()[0]);
                double metersDriven = Double.parseDouble((String) actionContent.getParameters()[1]);
                utils.updateBeliefAfterAction(metersDriven);
                trikeAgent.canExecute = true;
                executeTrips();
                utils.updateAtInputBroker();
            }
        }
        utils.currentTripStatus();
    }

    public void checkMessagesBuffer() {
        //  asking area for trikes
        while (!trikeAgent.messagesBuffer.isEmpty()) {
            Message message = trikeAgent.messagesBuffer.read();

            ArrayList<String> neighbourList = message.getContent().getValues();
            String jobId = neighbourList.remove(0); //JobID
            neighbourList.remove(0); //#

            if(trikeAgent.decisionTasks.containsKey(jobId)){
                DecisionTask decisionTask = trikeAgent.decisionTasks.get(jobId);
                decisionTask.numResponses++;
                if (decisionTask.getStatus() == DecisionTask.Status.WAITING_NEIGHBOURS){
                    decisionTask.getAgentIds().addAll(neighbourList);
                }
            }
        }
    }

    public void checkCNPBuffer() {
        while (!trikeAgent.cnpBuffer.isEmpty()){
            Message message = trikeAgent.cnpBuffer.read();
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
                    decisionTask.numResponses++;

                    if(decisionTask.getStatus() == DecisionTask.Status.WAITING_PROPOSALS){
                        Double propose = Double.parseDouble(message.getContent().getValues().get(2));
                        String senderID = message.getSenderId();
                        decisionTask.setUtilityScore(senderID, propose);
                    }else{
                        //  optional: reject proposal
                    }
                    break;
                }
                case ACCEPT_PROPOSAL: {
                    String jobID = message.getContent().getValues().get(0);
                    DecisionTask decisionTask = trikeAgent.decisionTasks.get(jobID);
                    if(decisionTask.getStatus() == DecisionTask.Status.WAITING_MANAGER){
                        decisionTask.extra = message.getId().toString();
                        decisionTask.setStatus(DecisionTask.Status.CONFIRM_READY);
                    }else{
                        Message refuseMessage = Message.refuse(message);
                        IAreaTrikeService service = messageToService(trikeAgent.agent, refuseMessage);
                        service.sendMessage(refuseMessage.serialize());
                    }
                    break;
                }
                case REJECT_PROPOSAL: {
                    String jobID = message.getContent().getValues().get(0);
                    DecisionTask decisionTask = trikeAgent.decisionTasks.get(jobID);
                    if(decisionTask.getStatus() == DecisionTask.Status.WAITING_MANAGER) {
                        trikeAgent.decisionTasks.get(jobID).setStatus(DecisionTask.Status.NOT_ASSIGNED);
                    }
                    break;
                }
                case ACK: {
                    String jobID = message.getContent().getValues().get(0);
                    DecisionTask decisionTask = trikeAgent.decisionTasks.remove(jobID);
                    if(decisionTask.getStatus() == DecisionTask.Status.WAITING_CONFIRM){
                        if(trikeAgent.requests.stream().anyMatch(request -> request.getId().equals(message.getId()))){
                            trikeAgent.requests.removeIf(request -> request.getId().equals(message.getId()));
                            decisionTask.setStatus(DecisionTask.Status.DELEGATED);
                            trikeAgent.FinishedDecisionTaskList.add(decisionTask);
                        }
                    }else{
                        //  Todo: handle trike didn't confirm on time.
                        //  it will be delegated to someone else
                    }
                    break;
                }
                case REFUSE:{
                    String jobID = message.getContent().getValues().get(0);
                    DecisionTask decisionTask = trikeAgent.decisionTasks.get(jobID);
                    if(decisionTask.getStatus() == DecisionTask.Status.WAITING_CONFIRM){
                        decisionTask.setStatus(DecisionTask.Status.DELEGATE);
                    }
                    break;
                }
            }
        }
    }


    public void checkJobBuffer(){
        while (!trikeAgent.jobsBuffer.isEmpty()){
            Message message = trikeAgent.jobsBuffer.read();
            Job job = new Job(message.getContent().getValues());
            DecisionTask decisionTask = new DecisionTask(job, message.getSenderId(), DecisionTask.Status.NEW);
            trikeAgent.AddDecisionTask(decisionTask);

            Message response = Message.ack(message);
            IAreaTrikeService service = IAreaTrikeService.messageToService(trikeAgent.agent, response);
            service.sendMessage(message.serialize());
        }
    }

    public void checkRequestTimeouts(){
        Iterator<Message> iterator = trikeAgent.requests.iterator();
        long currentTimeStamp = Instant.now().toEpochMilli();

        while(iterator.hasNext()){
            Message message = iterator.next();
            if(currentTimeStamp >= message.getTimeStamp() + TrikeConstants.REQUEST_WAIT_TIME){
                iterator.remove();
                IAreaTrikeService service = IAreaTrikeService.messageToService(trikeAgent.agent, message);
                service.sendMessage(message.serialize());
                message.setTimeStamp(currentTimeStamp);
                trikeAgent.requests.add(message);
            }
        }
    }
}
