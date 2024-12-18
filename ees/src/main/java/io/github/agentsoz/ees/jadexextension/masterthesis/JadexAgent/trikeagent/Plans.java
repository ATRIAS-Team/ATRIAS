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

import java.util.Collection;

public class Plans {
    private final Utils utils;
    private final TrikeAgent agent;

    public Plans(TrikeAgent agent, Utils utils){
        this.utils = utils;
        this.agent = agent;
    }

    public void evaluateDecisionTask()
    {
        boolean finishedForNow = false;
        while (!finishedForNow) {
            int changes = 0;
            for (int i = 0; i < agent.decisionTaskList.size(); i++) {
                int currentChanges = utils.selectNextAction(i);
                changes += currentChanges;
            }
            if(changes==0){
                finishedForNow = true;
            }
        }
    }

    public void newChargingTrip() {
        {
            if (agent.estimateBatteryAfterTIP.get(0) < TrikeConstants.CHARGING_THRESHOLD && agent.chargingTripAvailable.equals("0")){
                //estimateBatteryAfterTIP();
                agent.chargingTripCounter+=1;
                String tripID = "CH";
                tripID = tripID.concat(Integer.toString(agent.chargingTripCounter));
                Trip chargingTrip = new Trip(tripID, "ChargingTrip", utils.getNextChargingStation(), "NotStarted");
                agent.tripList.add(chargingTrip);
                agent.chargingTripAvailable = "1";
            }
        }
    }

    public void sensoryUpdate() {
        utils.currentTripStatus();
        if(agent.actionContentRingBuffer.isEmpty()) return;
        ActionContent actionContent = agent.actionContentRingBuffer.read();
        if (agent.isMatsimFree && !agent.currentTrip.isEmpty()) {
            if (actionContent.getAction_type().equals("drive_to") && actionContent.getState() == ActionContent.State.PASSED) {
                System.out.println("Agent " + agent.agentID + " finished with the previous trip and now can take the next trip");
                System.out.println("AgentID: " + agent.agentID + actionContent.getParameters()[0]);
                double metersDriven = Double.parseDouble((String) actionContent.getParameters()[1]);
                utils.updateBeliefAfterAction(metersDriven);
                agent.canExecute = true;
                executeTrips();
                utils.updateAtInputBroker();
            }
        }
        utils.currentTripStatus();
    }

    /**
     *  handles the progress of the current Trip
     */
    public  void executeTrips() {
        utils.newCurrentTrip();

        Trip current = null;
        if(!agent.currentTrip.isEmpty()){
            current = agent.currentTrip.get(0);
        }

        if (current != null) {
            utils.currentTripStatus();
            switch (current.getProgress()) {
                case "NotStarted": {
                    agent.canExecute = false;
                    agent.isMatsimFree = false;
                    utils.sendDriveTotoAdc();
                    utils.updateCurrentTripProgress("DriveToStart");
                    break;
                }
                case "AtStartLocation": {
                    switch (current.getTripType()) {
                        case "ChargingTrip": {
                            agent.trikeBattery.loadBattery();
                            utils.updateCurrentTripProgress("Finished");
                            agent.chargingTripAvailable = "0";
                            executeTrips();
                            break;
                        }
                        case "CustomerTrip": {
                            if (utils.customerMiss(current)) { // customer not there
                                utils.updateCurrentTripProgress("Failed");
                            } else {
                                agent.canExecute = false;
                                agent.isMatsimFree = false;
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
                    agent.currentTrip.remove(0);
                    utils.currentTripStatus();
                    executeTrips();
                    break;
                }
            }
            utils.estimateBatteryAfterTIP();
        }
    }
    public void reactToAgentIDAdded()
    {
        if (agent.agentID != null) // only react if the agentID exists
        {
            if (SimIDMapper.NumberSimInputAssignedID.size() == JadexModel.SimSensoryInputBrokernumber) // to make sure all SimInputBroker also receives its ID so vehicle agent could choose one SimInputBroker ID to register
                if (!agent.sent) { // to make sure the following part only executed once
                    agent.sent = true;
                    System.out.println("The agent id assigned for this vehicle agent is " + agent.agentID);
                    // setTag for itself to receive direct communication from SimSensoryInputBroker when service INotifyService is used.
                    IServiceIdentifier sid = ((IService) agent.agent.getProvidedService(INotifyService.class)).getServiceId();
                    agent.agent.setTags(sid,  agent.agentID);
                    //choosing one SimSensoryInputBroker to receive data from MATSIM
                    agent.currentSimInputBroker = utils.getRandomSimInputBroker();

                    // setTag for itself to receive direct communication from TripRequestControlAgent when service ISendTripService is used.
                    IServiceIdentifier sid2 = ((IService) agent.agent.getProvidedService(IAreaTrikeService.class)).getServiceId();
                    agent.agent.setTags(sid2, agent.agentID);

                    //communicate with SimSensoryInputBroker when knowing the serviceTag of the SimSensoryInputBroker.
                    ServiceQuery<INotifyService2> query = new ServiceQuery<>(INotifyService2.class);
                    query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
                    query.setServiceTags(agent.currentSimInputBroker); // choose to communicate with the SimSensoryInputBroker that it registered before
                    Collection<INotifyService2> service = agent.agent.getLocalServices(query);
                    for (INotifyService2 cs : service) {
                        cs.NotifyotherAgent(agent.agentID); // write the agentID into the list of the SimSensoryInputBroker that it chose before
                    }
                    System.out.println("agent "+ agent.agentID +"  registers at " + agent.currentSimInputBroker);
                    // Notify TripRequestControlAgent and JADEXModel
                    TrikeMain.TrikeAgentNumber = TrikeMain.TrikeAgentNumber+1;
                    JadexModel.flagMessage2();
                    //action perceive is sent to matsim only once in the initiation phase to register to receive events
                    utils.SendPerceivetoAdc();

                    agent.agentLocation = Cells.trikeRegisterLocations.get(agent.agentID);
                    utils.sendAreaAgentUpdate("register");

                    // Print the initial location for verification
                    System.out.println("Agent " + agent.agentID + " initial location: " + agent.agentLocation);

                    if(TrikeConstants.FIREBASE_ENABLED){
                        //update the location of the agent
                        FirebaseHandler.updateAgentLocation(agent.agentID, agent.agentLocation);
                    }

                    //csvLogger csvLogger = new csvLogger(agentID);
                    csvLogger csvLogger = new csvLogger(agent.agentID, TrikeConstants.CNP_ACTIVE, TrikeConstants.THETA,
                            TrikeConstants.ALLOW_CUSTOMER_MISS, TrikeConstants.CHARGING_THRESHOLD, TrikeConstants.commitThreshold, TrikeConstants.DISTANCE_FACTOR);
                }
        }
    }
}
