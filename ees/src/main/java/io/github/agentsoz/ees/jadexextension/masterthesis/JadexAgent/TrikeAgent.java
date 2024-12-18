package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;
import com.google.firebase.database.*;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.ees.firebase.FirebaseHandler;
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
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.annotation.OnStart;
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
    /**
     * The bdi agent. Automatically injected
     */
    @Agent
    public IInternalAccess agent;
    @AgentFeature
    protected IBDIAgentFeature bdiFeature;

    // to indicate if the agent is available to take the new ride
    @Belief
    public volatile boolean isMatsimFree;
    @Belief
    public boolean canExecute = true;

    public Location agentLocation;

    @Belief
    public List<DecisionTask> decisionTaskList = new ArrayList<>();

    @Belief
    public List<DecisionTask> FinishedDecisionTaskList = new ArrayList<>();

    @Belief
    public List<Trip> tripList = new ArrayList<>(); //contains all the trips

    @Belief    //contains the current Trip
    public List<Trip> currentTrip = new ArrayList<>();

    public RingBuffer<ActionContent> actionContentRingBuffer = new RingBuffer<>(16);

    public RingBuffer<PerceptContent> perceptContentRingBuffer = new RingBuffer<>(16);


    @Belief
    public String agentID = null; // store agent ID from the map
    @Belief
    public boolean sent = false;
    @Belief
    public String write = null;

    public Integer chargingTripCounter = 0;

    @Belief
    protected boolean daytime; //Battery -oemer
    @Belief
    public double sumLinkLength = 0.0;
    @Belief
    public BatteryModel trikeBattery = new BatteryModel();
    @Belief
    public List<Double> estimateBatteryAfterTIP = List.of(trikeBattery.getMyChargestate());

    public String currentSimInputBroker;
    public SimActuator SimActuator;

    @Belief
    public String chargingTripAvailable = "0"; //Battery -oemer

    public MyLogger logger;

    private Plans plans;

    //  FIREBASE
    public FirebaseHandler<TrikeAgent, Trip> firebaseHandler;
    public HashMap<String, ChildEventListener> listenerHashMap;

    /**
     * The agent body.
     */
    @OnStart
    private void body() {
        Element classElement = XMLConfig.getClassElement("TrikeAgent.java");
        Utils utils = new Utils(this);
        plans = new Plans(this, utils);
        
        utils.configure(classElement);

        if(TrikeConstants.FIREBASE_ENABLED){
            firebaseHandler = new FirebaseHandler<>(this, tripList);
            listenerHashMap = new HashMap<>();
        }

        System.out.println("TrikeAgent sucessfully started;");
        SimActuator = new SimActuator();
        SimActuator.setQueryPerceptInterface(JadexModel.storageAgent.getQueryPerceptInterface());
        AddAgentNameToAgentList(); // to get an AgentID later
        isMatsimFree = true;
        bdiFeature.dispatchTopLevelGoal(new ReactToAgentIDAdded());
        bdiFeature.dispatchTopLevelGoal(new MaintainManageJobs());
        bdiFeature.dispatchTopLevelGoal(new PerformSIMReceive());
        bdiFeature.dispatchTopLevelGoal(new MaintainTripService());
        bdiFeature.dispatchTopLevelGoal(new MaintainBatteryLoaded());
    }

    @Goal(recur = true, recurdelay = 100)
    private class MaintainBatteryLoaded {}

    @Plan(trigger = @Trigger(goals = MaintainBatteryLoaded.class))
    private void newChargingTrip() {
        plans.newChargingTrip();
    }


    /**
     * Will generate Trips from the Jobs sent by the Area Agent
     */
    @Goal(recur=true, recurdelay=1000)
    private class MaintainManageJobs
    {
        @GoalMaintainCondition
        boolean isDecisionEmpty() {return decisionTaskList.isEmpty();}
    }

    @Plan(trigger=@Trigger(goals=MaintainManageJobs.class))
    private void evaluateDecisionTask() {plans.evaluateDecisionTask();}


    @Goal(recur = true, recurdelay = 100)
    private class MaintainTripService {
        @GoalMaintainCondition
        boolean sentToMATSIM() {
            return !(isMatsimFree && canExecute);
        }
    }

    @Plan(trigger = @Trigger(goals = MaintainTripService.class))
    private void doNextTrip() {plans.executeTrips();}


    //#######################################################################
    //Goals and plans : After the agentID is assigned to the Trike Agent,
    // Trike Agent should prepare everything for the synchronization process
    //#######################################################################

    @Goal(recur = true, recurdelay = 3000)
    private class ReactToAgentIDAdded {}

    @Plan(trigger = @Trigger(goals = ReactToAgentIDAdded.class))
    private void reactToAgentIDAdded() {plans.reactToAgentIDAdded();}

    //#######################################################################
    //Goals and plans : to print out something when the data from MATSIM is
    //written to its belief base by the SimSensoryInputBroker
    //#######################################################################

    @Goal(recur = true,recurdelay = 300)
    private class PerformSIMReceive {}

    @Plan(trigger = @Trigger(goals = PerformSIMReceive.class))
    private void sensoryUpdate() {plans.sensoryUpdate();}



    public void AddAgentNameToAgentList()
    {
        SimIDMapper.TrikeAgentNameList.add(agent.getId().getName());
    }

    public void AddDecisionTask(DecisionTask decisionTask)
    {
        decisionTaskList.add(decisionTask);
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