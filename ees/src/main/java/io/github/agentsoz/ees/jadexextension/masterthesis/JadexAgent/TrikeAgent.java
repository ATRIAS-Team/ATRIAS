package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;
import com.google.firebase.database.*;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.ees.firebase.FirebaseHandler;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.shared.SharedPlans;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.trikeagent.Plans;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.trikeagent.TrikeConstants;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.trikeagent.Utils;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.TrikeAgentService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.WritingIDService;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.IMappingAgentsService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.INotifyService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.TrikeAgentReceiveService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.TrikeAgentSendService;
import io.github.agentsoz.ees.util.RingBuffer;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.XMLConfig;
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

    public RingBuffer<ActionContent> actionContentRingBuffer = new RingBuffer<>(16);
    public RingBuffer<PerceptContent> perceptContentRingBuffer = new RingBuffer<>(16);
    public RingBuffer<Message> messagesBuffer = new RingBuffer<>(32);
    public RingBuffer<Message> jobsBuffer = new RingBuffer<>(32);
    public RingBuffer<Message> cnpBuffer = new RingBuffer<>(64);

    public Map<UUID, Long> receivedMessageIds = new HashMap<>(64);
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
    public SimActuator SimActuator;

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
        utils.configure(classElement);

        if(TrikeConstants.FIREBASE_ENABLED){
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

    @Goal(recur=true, recurdelay=100)
    private class JobBuffer {}

    @Plan(trigger=@Trigger(goals=JobBuffer.class))
    private void checkJobBuffer() {
       plans.checkJobBuffer();
    }


    /**
     * This is just a debug Goal that will print many usefull information every 10s
     */
    @Goal(recur=true, recurdelay=10000)
    private class Log {}

    @Plan(trigger=@Trigger(goals=Log.class))
    private void log() {
        System.out.println("agentID " + agentID +  " simtime " + JadexModel.simulationtime);
    }

    @Goal(recur = true, recurdelay = 100)
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
    @Goal(recur=true, recurdelay=20)
    private class MaintainManageJobs {
        @GoalMaintainCondition
        boolean isDecisionEmpty()
        {
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
    @Goal(recur = true, recurdelay = 100)
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

    @Goal(recur = true,recurdelay = 300)
    private class PerformSIMReceive {}

    @Plan(trigger = @Trigger(goals = PerformSIMReceive.class))
    private void sensoryUpdate() {
        plans.sensoryUpdate();
    }

    @Goal(recur = true, recurdelay = 10)
    private class CheckMessagesBuffer{}

    @Plan(trigger = @Trigger(goals = CheckMessagesBuffer.class))
    private void checkMessagesBuffer(){
        plans.checkMessagesBuffer();
    }

    @Goal(recur = true, recurdelay = 10)
    private class CNPBuffer{}

    @Plan(trigger = @Trigger(goals = CNPBuffer.class))
    private void checkCNPBuffer(){
        plans.checkCNPBuffer();
    }

    @Goal(recur = true, recurdelay = 10000)
    private class Requests{}

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
}