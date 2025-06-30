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
import io.github.agentsoz.ees.shared.*;
import io.github.agentsoz.ees.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.JadexService.AreaTrikeService.TrikeAgentService;
import io.github.agentsoz.ees.JadexService.MappingService.WritingIDService;
import io.github.agentsoz.ees.Run.JadexModel;
import io.github.agentsoz.ees.JadexService.MappingService.IMappingAgentsService;
import io.github.agentsoz.ees.JadexService.NotifyService.INotifyService;
import io.github.agentsoz.ees.JadexService.NotifyService.TrikeAgentReceiveService;
import io.github.agentsoz.ees.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.ees.JadexService.NotifyService2.TrikeAgentSendService;
import io.github.agentsoz.ees.simagent.SimIDMapper;
import io.github.agentsoz.ees.Run.XMLConfig;
import io.github.agentsoz.ees.util.Event;
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
import java.util.concurrent.*;

@Agent(type= BDIAgentFactory.TYPE)
@ProvidedServices({
        @ProvidedService(type= IMappingAgentsService.class, implementation=@Implementation(WritingIDService.class)),
        @ProvidedService(type= INotifyService.class, implementation=@Implementation(TrikeAgentReceiveService.class)),
        @ProvidedService(type= INotifyService2.class, implementation=@Implementation(TrikeAgentSendService.class)),
        @ProvidedService(type= IAreaTrikeService.class, implementation=@Implementation( TrikeAgentService.class)),
})
@RequiredServices({
        @RequiredService(name="clockservice", type= IClockService.class),
        @RequiredService(name ="sendareaagendservice", type = IAreaTrikeService.class),
        @RequiredService(name="mapservices", type= IMappingAgentsService.class),
        @RequiredService(name="broadcastingservices", type= INotifyService.class, scope= ServiceScope.PLATFORM),
        @RequiredService(name="notifywhenexecutiondoneservice", type= INotifyService2.class, scope= ServiceScope.PLATFORM),
})

public class TrikeAgent{

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
    public boolean canExecute = true;

    @Belief
    public boolean isCharging = false;

    public Location agentLocation;

    @Belief
    public Map<String, DecisionTask> decisionTasks = new ConcurrentHashMap<>();

    @Belief
    public List<DecisionTask> FinishedDecisionTaskList = new ArrayList<>();
    @Belief
    public List<Trip> tripList = Collections.synchronizedList(new ArrayList<>()); //contains all the trips
    @Belief    //contains the current Trip
    public List<Trip> currentTrip = Collections.synchronizedList(new ArrayList<>());

    public Map<UUID, Long> receivedMessageIds = new ConcurrentHashMap<>(2048);

    @Belief
    public List<Message> requests = Collections.synchronizedList(new ArrayList<>());  //requests are sorted by timestamp

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

    public volatile String cell = null;

    public boolean isAssigned = false;

    public List<Event<?>> events = new ArrayList<>();


    /**
     * The agent body.
     */
    @OnStart
    public void body() {
        utils = new Utils(this);
        plans = new Plans(this, utils);

        Element classElement = XMLConfig.getClassElement("TrikeAgent.java");
        TrikeConstants.configure(classElement);

        if(SharedConstants.FIREBASE_ENABLED){
            firebaseHandler = new FirebaseHandler<>(this, tripList);
            listenerHashMap = new HashMap<>();
        }

        System.out.println("TrikeAgent successfully started;");
        SimActuator = new SimActuator();
        SimActuator.setQueryPerceptInterface(JadexModel.storageAgent.getQueryPerceptInterface());
        AddAgentNametoAgentList(); // to get an AgentID later

        bdiFeature.dispatchTopLevelGoal(new ReactToAgentIDAdded());
        bdiFeature.dispatchTopLevelGoal(new MaintainManageJobs());
        bdiFeature.dispatchTopLevelGoal(new Log());
        bdiFeature.dispatchTopLevelGoal(new MaintainTripService());
        bdiFeature.dispatchTopLevelGoal(new UpdateLocation());

        bdiFeature.dispatchTopLevelGoal(new ReceivedMessages());
        bdiFeature.dispatchTopLevelGoal(new Requests());
    }

    /**
     * This is just a debug Goal that will print many usefull information every 10s
     */
    @Goal(recur=true, recurdelay=3000)
    private class Log {}

    @Plan(trigger=@Trigger(goals=Log.class))
    private void log() {
    }

    @Goal(recur=true, recurdelay = 10000)
    private class UpdateLocation{}

    @Plan(trigger=@Trigger(goals=UpdateLocation.class))
    private void updateLocation(){

        if(this.cell != null && !isAssigned){
            isAssigned = true;
            return;
        }

        if(isAssigned){
            plans.updateLocation();
        }
    }


    @Goal(recur = true, recurdelay = 1000)
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
    @Goal(recur=true, recurdelay= 50)
    private class MaintainManageJobs {
        @GoalMaintainCondition
        private boolean isEmpty(){
            return decisionTasks.isEmpty();
        }
    }

    @Plan(trigger=@Trigger(goals=MaintainManageJobs.class))
    private void evaluateDecisionTask() {
        plans.evaluateDecisionTask();
    }

    /**
     *  MaintainTripService former SendDrivetoTooutAdc
     *  desired behavior:
     *  start: when new trip is generated
     */
    @Goal(recur = true, recurdelay = 1000)
    private class MaintainTripService {
        @GoalMaintainCondition
        private boolean check(){
            return !(canExecute || isCharging);
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
    private class ReactToAgentIDAdded {}

    @Plan(trigger = @Trigger(goals = ReactToAgentIDAdded.class))
    private void ReactToAgentIDAdded()
    {
        plans.reactToAgentIDAdded();
    }

    //#######################################################################
    //Goals and Plans : to print out something when the data from MATSIM is
    //written to its belief base by the SimSensoryInputBroker
    //#######################################################################

    @Goal(recur = true, recurdelay = 1000)
    private class Requests{
        @GoalMaintainCondition
        private boolean isEmpty(){
            return requests.isEmpty();
        }
    }

    @Plan(trigger=@Trigger(goals= Requests.class))
    private void checkRequestTimeouts(){
        plans.checkRequestTimeouts();
    }

    @Goal(recur = true, recurdelay = 10000)
    private class ReceivedMessages{}

    @Plan(trigger=@Trigger(goals= ReceivedMessages.class))
    private void updateReceivedMessages(){
        SharedPlans.cleanupReceivedMessages(receivedMessageIds);
    }


    public void AddAgentNametoAgentList()
    {
        SimIDMapper.TrikeAgentNameList.add(agent.getId().getName());
    }

    public void AddTriptoTripList(Trip Trip)
    {
        tripList.add(Trip);
    }

    public void AddDecisionTask(DecisionTask decisionTask)
    {
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

    public void sendMessage(String messageStr){
        Message messageObj = Message.deserialize(messageStr);

        if(this.receivedMessageIds.containsKey(messageObj.getId())) return;
        this.receivedMessageIds.put(messageObj.getId(), SharedUtils.getSimTime());

        switch (messageObj.getComAct()){
            case CALL_FOR_PROPOSAL:
            case PROPOSE:
            case ACCEPT_PROPOSAL:
            case REJECT_PROPOSAL:
            case REFUSE:
                plans.checkCNPBuffer(messageObj);
                break;
            case INFORM:{
                plans.checkMessagesBuffer(messageObj);
                break;
            }
            case REQUEST:
                plans.checkJobBuffer(messageObj);
                break;
            case ACK:
                switch (messageObj.getContent().getAction()){
                    case "confirmAccept": {
                        plans.checkCNPBuffer(messageObj);
                        break;
                    }
                }
                break;
        }
    }

    public void NotifyotherAgent(List<ActionContent> ActionContentList, List<PerceptContent> PerceptContentList, boolean activestatus) {
        SharedUtils.executorService.submit(()->{
            if (activestatus)
            {
                this.plans.sensoryUpdate(ActionContentList);
            }
        });
    }
}
