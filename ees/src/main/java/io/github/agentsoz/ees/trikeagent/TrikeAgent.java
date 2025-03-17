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
import com.google.firebase.database.*;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.ees.firebase.FirebaseHandler;
import io.github.agentsoz.ees.shared.Message;
import io.github.agentsoz.ees.shared.SharedConstants;
import io.github.agentsoz.ees.shared.SharedPlans;
import io.github.agentsoz.ees.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.JadexService.AreaTrikeService.TrikeAgentService;
import io.github.agentsoz.ees.JadexService.MappingService.WritingIDService;
import io.github.agentsoz.ees.Run.JadexModel;
import io.github.agentsoz.ees.JadexService.MappingService.IMappingAgentsService;
import io.github.agentsoz.ees.JadexService.NotifyService.INotifyService;
import io.github.agentsoz.ees.JadexService.NotifyService.TrikeAgentReceiveService;
import io.github.agentsoz.ees.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.ees.JadexService.NotifyService2.TrikeAgentSendService;
import io.github.agentsoz.ees.shared.SimActuator;
import io.github.agentsoz.ees.simagent.SimIDMapper;
import io.github.agentsoz.ees.util.RingBuffer;
import io.github.agentsoz.ees.Run.XMLConfig;
import io.github.agentsoz.util.Location;

import jadex.bdiv3.BDIAgentFactory;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.clock.IClockService;
import jadex.micro.annotation.*;
import org.w3c.dom.Element;
import java.util.*;

@Agent(type = BDIAgentFactory.TYPE)
@ProvidedServices({
    @ProvidedService(type = IMappingAgentsService.class, implementation = @Implementation(WritingIDService.class)),
    @ProvidedService(type = INotifyService.class, implementation = @Implementation(TrikeAgentReceiveService.class)),
    @ProvidedService(type = INotifyService2.class, implementation = @Implementation(TrikeAgentSendService.class)),
    @ProvidedService(type = IAreaTrikeService.class, implementation = @Implementation(TrikeAgentService.class)),})
@RequiredServices({
    @RequiredService(name = "clockservice", type = IClockService.class),
    @RequiredService(name = "sendareaagendservice", type = IAreaTrikeService.class),
    @RequiredService(name = "mapservices", type = IMappingAgentsService.class),
    @RequiredService(name = "broadcastingservices", type = INotifyService.class, scope = ServiceScope.PLATFORM),
    @RequiredService(name = "notifywhenexecutiondoneservice", type = INotifyService2.class, scope = ServiceScope.PLATFORM),})

public class TrikeAgent {

    @Agent
    public IInternalAccess agent;
    @AgentFeature
    protected IBDIAgentFeature bdiFeature;
    @AgentFeature
    protected IExecutionFeature execFeature;
    @AgentFeature
    protected IRequiredServicesFeature requiredServicesFeature;

    // to indicate if the agent is available to take the new ride
    @Belief
    public volatile boolean isMatsimFree;
    @Belief
    public boolean canExecute = true;

    @Belief
    public Location agentLocation;

    @Belief
    public Map<String, DecisionTask> decisionTasks = new HashMap<>();

    @Belief
    public List<DecisionTask> FinishedDecisionTaskList = new ArrayList<>();
    @Belief
    public List<Trip> tripList = new ArrayList<>(); //contains all the trips
    @Belief    //contains the current Trip
    public List<Trip> currentTrip = new ArrayList<>();

    public RingBuffer<ActionContent> actionContentRingBuffer = new RingBuffer<>(256);
    public RingBuffer<PerceptContent> perceptContentRingBuffer = new RingBuffer<>(256);
    public RingBuffer<Message> messagesBuffer = new RingBuffer<>(256);
    public RingBuffer<Message> jobsBuffer = new RingBuffer<>(256);
    public RingBuffer<Message> cnpBuffer = new RingBuffer<>(256);

    public Map<UUID, Long> receivedMessageIds = new HashMap<>(256);
    public List<Message> requests = new ArrayList<>();  //requests are sorted by timestamp

    @Belief
    public String agentID = null; // store agent ID from the map
    @Belief
    public boolean sent = false;
    @Belief
    public String write = null;

    public int chargingTripCounter = 0;

    @Belief
    protected boolean daytime; //Battery -oemer
    @Belief
    public double sumLinkLength = 0.0;
    @Belief
    public BatteryModel trikeBattery = new BatteryModel();
    @Belief
    public List<Double> estimateBatteryAfterTIP = Arrays.asList(trikeBattery.getMyChargestate());

    public String currentSimInputBroker;
    public io.github.agentsoz.ees.shared.SimActuator SimActuator;

    @Belief
    public String chargingTripAvailable = "0"; //Battery -oemer

    //  FIREBASE
    public FirebaseHandler<TrikeAgent, Trip> firebaseHandler;
    public HashMap<String, ChildEventListener> listenerHashMap;

    public Utils utils;
    public Plans plans;

    /**
     * The agent body.
     */
    @OnStart
    public void body() {
        utils = new Utils(this);
        plans = new Plans(this, utils);

        Element classElement = XMLConfig.getClassElement("TrikeAgent.java");
        TrikeConstants.configure(classElement);

        if (SharedConstants.FIREBASE_ENABLED) {
            firebaseHandler = new FirebaseHandler<>(this, tripList);
            listenerHashMap = new HashMap<>();
        }

        System.out.println("TrikeAgent successfully started;");
        SimActuator = new SimActuator();
        SimActuator.setQueryPerceptInterface(JadexModel.storageAgent.getQueryPerceptInterface());
        AddAgentNametoAgentList(); // to get an AgentID later
        isMatsimFree = true;

        bdiFeature.dispatchTopLevelGoal(new ReactToAgentIDAdded());
        bdiFeature.dispatchTopLevelGoal(new MaintainManageJobs());
        bdiFeature.dispatchTopLevelGoal(new Log());
        bdiFeature.dispatchTopLevelGoal(new PerformSIMReceive());
        bdiFeature.dispatchTopLevelGoal(new MaintainTripService());
        bdiFeature.dispatchTopLevelGoal(new CheckMessagesBuffer());
        bdiFeature.dispatchTopLevelGoal(new CNPBuffer());
        bdiFeature.dispatchTopLevelGoal(new JobBuffer());
        bdiFeature.dispatchTopLevelGoal(new ReceivedMessages());
        bdiFeature.dispatchTopLevelGoal(new Requests());
    }

    @Goal(recur = true, recurdelay = 40)
    private class JobBuffer {
    }

    @Plan(trigger = @Trigger(goals = JobBuffer.class))
    private void checkJobBuffer() {
        plans.checkJobBuffer();
    }

    /**
     * This is just a debug Goal that will print many usefull information every
     * 10s
     */
    @Goal(recur = true, recurdelay = 10000)
    private class Log {
    }

//    @Plan(trigger=@Trigger(goals=Log.class))
//    private void log() {
//        System.out.println("agentID " + agentID +  " simtime " + JadexModel.simulationtime);
//    }
    @Goal(recur = true, recurdelay = 50)
    private class MaintainBatteryLoaded {

        @GoalCreationCondition(factchanged = "estimateBatteryAfterTIP") //
        public MaintainBatteryLoaded() {
        }
    }

    @Plan(trigger = @Trigger(goals = MaintainBatteryLoaded.class))
    private void newChargingTrip() {
        plans.newChargingTrip();
    }

    /**
     * Will generate Trips from the Jobs sent by the Area Agent
     */
    @Goal(recur = true, recurdelay = 50)
    private class MaintainManageJobs {

        @GoalMaintainCondition
        boolean isDecisionEmpty() {
            return decisionTasks.isEmpty();
        }
    }

    @Plan(trigger = @Trigger(goals = MaintainManageJobs.class))
    private void evaluateDecisionTask() {
        plans.evaluateDecisionTask();
    }

    /**
     * MaintainTripService former SendDrivetoTooutAdc desired behavior: start:
     * when new trip is generated
     */
    @Goal(recur = true, recurdelay = 30)
    private class MaintainTripService {

        @GoalMaintainCondition
        boolean sentToMATSIM() {
            return !(isMatsimFree && canExecute);
        }
    }

    @Plan(trigger = @Trigger(goals = MaintainTripService.class))
    private void executeTrips() {
        plans.executeTrips();
    }

    //#######################################################################
    //Goals and Plans : After the agentID is assigned to the Trike Agent,
    // Trike Agent should prepare everything for the synchronization process
    //#######################################################################
    @Goal(recur = true, recurdelay = 3000)
    private class ReactToAgentIDAdded {
    }

    @Plan(trigger = @Trigger(goals = ReactToAgentIDAdded.class))
    private void ReactToAgentIDAdded() {
        plans.reactToAgentIDAdded();
    }

    //#######################################################################
    //Goals and Plans : to print out something when the data from MATSIM is
    //written to its belief base by the SimSensoryInputBroker
    //#######################################################################
    @Goal(recur = true, recurdelay = 50)
    private class PerformSIMReceive {
    }

    @Plan(trigger = @Trigger(goals = PerformSIMReceive.class))
    private void sensoryUpdate() {
        plans.sensoryUpdate();
    }

    @Goal(recur = true, recurdelay = 50)
    private class CheckMessagesBuffer {
    }

    @Plan(trigger = @Trigger(goals = CheckMessagesBuffer.class))
    private void checkMessagesBuffer() {
        plans.checkMessagesBuffer();
    }

    @Goal(recur = true, recurdelay = 20)
    private class CNPBuffer {
    }

    @Plan(trigger = @Trigger(goals = CNPBuffer.class))
    private void checkCNPBuffer() {
        plans.checkCNPBuffer();
    }

    @Goal(recur = true, recurdelay = 10000)
    private class Requests {
    }

    @Plan(trigger = @Trigger(goals = Requests.class))
    private void checkRequestTimeouts() {
        plans.checkRequestTimeouts();
    }

    @Goal(recur = true, recurdelay = 10000)
    private class ReceivedMessages {
    }

    @Plan(trigger = @Trigger(goals = ReceivedMessages.class))
    private void updateReceivedMessages() {
        SharedPlans.cleanupReceivedMessages(receivedMessageIds);
    }

    public void AddAgentNametoAgentList() {
        SimIDMapper.TrikeAgentNameList.add(agent.getId().getName());
    }

    public void AddTriptoTripList(Trip Trip) {
        tripList.add(Trip);
    }

    public void AddDecisionTask(DecisionTask decisionTask) {
        decisionTasks.put(decisionTask.getJobID(), decisionTask);
    }

    public void setAgentID(String agentid) {
        agentID = agentid;
    }

    public String getAgentID() {
        return agentID;
    }

    //Battery -oemer
    public void setMyLocation(Location location) {

    }
}
