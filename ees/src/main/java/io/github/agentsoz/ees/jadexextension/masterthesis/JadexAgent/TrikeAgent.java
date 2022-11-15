/* TrikeAgnet.java
 * v0.5
 * changelog: Thu agent + additions
 * @Author
 *
 *
 */
package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;


import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdiabm.v3.AgentNotFoundException;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.IJobDistributionService.IJobDistributionService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.WritingIDService;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.TrikeMain;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.ISendTripService.IsendTripService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.ISendTripService.ReceiveTripService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.IJobDistributionService.ReceiveJobDistributionService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.IMappingAgentsService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.INotifyService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.TrikeAgentReceiveService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.TrikeAgentSendService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.BatteryModel;

import io.github.agentsoz.util.Location;
import io.github.agentsoz.util.Time;
import jadex.bdiv3.BDIAgentFactory;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bdiv3.runtime.IPlan;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.ServiceQuery;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.*;
import org.matsim.core.mobsim.jdeqsim.Vehicle;

import java.time.LocalDateTime;
import java.util.*;


@Agent(type= BDIAgentFactory.TYPE)
@ProvidedServices({
        @ProvidedService(type= IMappingAgentsService.class, implementation=@Implementation(WritingIDService.class)),
        @ProvidedService(type= INotifyService.class, implementation=@Implementation(TrikeAgentReceiveService.class)),
        @ProvidedService(type= INotifyService2.class, implementation=@Implementation(TrikeAgentSendService.class)),
        @ProvidedService(type= IsendTripService.class, implementation=@Implementation(ReceiveTripService.class)),

})
@RequiredServices({
        @RequiredService(name="clockservice", type= IClockService.class),
        @RequiredService(name="sendtripservices", type= IsendTripService.class),
        @RequiredService(name="mapservices", type= IMappingAgentsService.class),
        @RequiredService(name="broadcastingservices", type= INotifyService.class, scope= ServiceScope.PLATFORM),
        @RequiredService(name="notifywhenexecutiondoneservice", type= INotifyService2.class, scope= ServiceScope.PLATFORM),

        // multiple=true,
})

/*This is the most actual one that is using for Testing the whole Run1 process*/

public class TrikeAgent implements SendtoMATSIM{

    /**
     * The bdi agent. Automatically injected
     */
    @Agent
    private IInternalAccess agent;
    @AgentFeature
    protected IBDIAgentFeature bdiFeature;
    @AgentFeature
    protected IExecutionFeature execFeature;
    @AgentFeature
    protected IRequiredServicesFeature requiredServicesFeature;

    @Belief
    protected int carriedcustomer;
    @Belief
    protected double my_numberofcharges = 200;
    @Belief
    protected double my_batteryhealth = 1.0 - 0.00025 * my_numberofcharges;
    @Belief
    protected double my_speed;
    @Belief
    protected boolean my_autopilot;
    @Belief
    protected double my_chargestate = 1.0;
    @Belief
    protected boolean daytime;
    @Belief
    public List<Location> chargingStation;
    @Belief
    //  public List <String> resultfromMATSIM = Arrays.asList("false");
    public String resultfromMATSIM = "false";
    // to indicate if the agent is available to take the new ride
    @Belief
    public boolean activestatus;
    //to do init from file
    @Belief
    private Location agentLocation; // position of the agent
    @Belief
    public List<Job> jobList = new ArrayList<>();
    @Belief    //contains all the trips
    public List<Trip> tripList = new ArrayList<>();
    @Belief
    public List<String> tripIDList = new ArrayList<>();
    @Belief    //contains the current Trip
    public List<Trip> currentTrip = new ArrayList<>();
    @Belief
    private List<ActionContent> SimActionList = new ArrayList<>();
    @Belief
    private List<PerceptContent> SimPerceptList = new ArrayList<>();
    @Belief
    private String agentID = null; // store agent ID from the map
    @Belief
    public boolean sent = false;

    public String write = null;
    public boolean informSimInput = false;
    public String currentSimInputBroker;
    private SimActuator SimActuator;
    public boolean TestSent = false;


    /**
     * The agent body.
     */
    @OnStart
    public void body() {
        System.out.println("TrikeAgent sucessfully started;");
        SimActuator = new SimActuator();
        SimActuator.setQueryPerceptInterface(JadexModel.storageAgent.getQueryPerceptInterface());
        AddAgentNametoAgentList(); // to get an AgentID later
        activestatus = true;
        bdiFeature.dispatchTopLevelGoal(new ReactoAgentIDAdded());
        bdiFeature.dispatchTopLevelGoal(new MaintainManageJobs()); //evtl löschen
        //bdiFeature.dispatchTopLevelGoal(new TimeTest());
        //bdiFeature.dispatchTopLevelGoal(new TimeTrigger());
        Location Location1 = new Location("", 268674.543999, 5901195.908183);
        Trip Trip1 = new Trip("Trip1", "CustomerTrip", Location1, "NotStarted");
        Location Location2= new Location("", 288654.693529, 5286721.094209);

        Trip Trip2 = new Trip("Trip2", "CustomerTrip", Location2, "NotStarted");

        Location Location3= new Location("", 238654.693529, 5886721.094209);

        Trip Trip3 = new Trip("Trip3", "CustomerTrip", Location3, "NotStarted");

        Location Location4 = new Location("", 238674.543999, 5901195.908183);

        Trip Trip4 = new Trip("Trip4", "CustomerTrip", Location4, "NotStarted");
        //tripList.add(Trip1);
        //tripList.add(Trip2);
        //tripList.add(Trip3);
        //tripList.add(Trip4);

    }

    public void setMyLocation(Location location) {
    }

    //#######################################################################
    //Goals and Plans for Sending data to AgentDataContainer, for the Aktorik
    //#######################################################################

    @Goal(recur=true, recurdelay=1000)
    class TimeTest {
    }

    @Plan(trigger=@Trigger(goals=TimeTest.class))
    private void TimeTestPrint()
    {
        System.out.println("simtime" + JadexModel.simulationtime);
    }

    @Goal(recur=true, recurdelay=10000)
    class TimeTrigger {
    }

    @Plan(trigger=@Trigger(goals=TimeTest.class))
    private void TimeTriggerAusloesen()
    {
        SimActuator.setQueryPerceptInterface(JadexModel.storageAgent.getQueryPerceptInterface());
    }

    @Goal//(deliberation=@Deliberation(inhibits={}))
    public class MaintainBatteryLoaded
    {
        /**
         *  Author: Gürbüz, Ekin Kafkas
         *  ..................................................
         *  If it is at night or if the charge state is
         *  below 0.2, the trike agent will activate this goal.
         */
        @GoalMaintainCondition//(beliefs="my_chargestate")
        public boolean checkMaintain()
        {
            return daytime == true && my_chargestate > 0.2;
        }

        /**
         *  The target condition determines when
         *  the goal goes back to idle.
         */
        @GoalTargetCondition//(beliefs="my_chargestate")
        public boolean checkTarget()
        {
            return my_chargestate >= 1.0;
        }

        /**
         *  Author: Gürbüz, Ekin Kafkas
         *  ..................................................
         *  Suspend the goal when the trike agent
         *  carries a customer.
         */
//		@GoalContextCondition//(beliefs="carriedcustomer")
//		public boolean checkContext()
//		{
//			return carriedcustomer == null;
//		}
    }

    @Plan
    public class LoadBatteryPlan
    {
        @PlanBody
        public void body(TrikeAgent agentapi, IPlan planapi)
        {
            BatteryModel.loadBattery(agentapi, planapi);
        }
    }

    @Goal
    public class AchieveMoveTo
    {
        /** The location. */
        protected Location location;

        /**
         *  Create a new goal.
         *  @param location The location.
         */
        public AchieveMoveTo(Location location)
        {
//			System.out.println("created: "+location);
            this.location = location;
        }

        /**
         *  The goal is achieved when the position
         *  of the trike is near to the target position.
         */
        @GoalTargetCondition//(beliefs="my_location")
        public boolean checkTarget()
        {
            return agentLocation.isNear(location);
        }

        /**
         *  Author: Gürbüz, Ekin Kafkas
         *  ..................................................
         *  Suspend the goal if the trike agent
         *  is being charged at night, because the trikes
         *  are not going on any customer trips at night.
         */
        @GoalContextCondition//(beliefs="carriedcustomer")
        public boolean checkContext()
        {
            if (daytime == false)
            {
                return my_chargestate < 1.0;
            }

            else return my_chargestate <= 1.0;
        }

        /**
         *  Get the location.
         *  @return The location.
         */
        public Location getLocation()
        {
            return location;
        }
    }


    /**
     *  Move to a point.
     */
    @Plan
    public class MoveToLocationPlan {
        @PlanCapability
        protected TrikeAgent capa;
        @PlanAPI
        protected IPlan rplan;
        @PlanReason
        protected AchieveMoveTo goal;

        //-------- constructors --------

        /**
         * Create a new plan.
         */
        public MoveToLocationPlan() {
//		capa.getAgent().getLogger().info("Created: "+this);
        }
        //-------- methods --------

        /**
         * The plan body.
         */
        @PlanBody
        public IFuture<Void> body() {
            BatteryModel.moveToTarget();
            BatteryModel.oneStepToTarget();
            return BatteryModel.moveToTarget();
        }
        /**
         *
         */
    }

    @Goal(recur=true, recurdelay=1000)
    class MaintainManageJobs
    {
        @GoalMaintainCondition	// The cleaner aims to maintain the following expression, i.e. act to restore the condition, whenever it changes to false.
        boolean jobListEmpty()
        {
            return jobList.size()==0; // Everything is fine as long as the charge state is above 20%, otherwise the cleaner needs to recharge.
        }

        //@GoalTargetCondition
        //boolean AllAgent() {
        //    return (TestSent == true);
        //}

    }

    @Plan(trigger=@Trigger(goals=MaintainManageJobs.class))
    private void EvaluateDecisionTask()
    {
        if (jobList.size()>0) {
            System.out.println("EvaluateDecisionTask");

            Job jobToTrip = jobList.get(0);
            Trip newTrip = new Trip(jobToTrip.getID(), "CustomerTrip", jobToTrip.getVATime(), jobToTrip.getStartPosition(), jobToTrip.getEndPosition(), "NotStarted");
            //TODO: create a unique tripID


            tripList.add(newTrip);
            //TODO: zwischenschritte (visio) fehlen
            tripIDList.add("1");
            jobList.remove(0);
            //hier job entfernen dann testen ob nur einmalig nach hinzufügen ausgeführt und dann nicht mehr
            //TestSent = true;
        }
    }

    /** OLD version: Thu
     @Goal(recur = true, recurdelay = 3000)
     class SendDrivetoTooutAdc {
     // Goal should be triggered when tripIDlist is not empty and activestatus = true. use tripIDlist
     //instead of TripList because there is both removal and addition happens inside the tripList
     //--> not stable for the triggering of factadded.
     @GoalCreationCondition(factadded = "tripIDList") //
     public SendDrivetoTooutAdc() {
     }

     @GoalTargetCondition
     boolean senttoMATSIM() {
     return !(activestatus == false);
     }
     }

     @Plan(trigger = @Trigger(goalfinisheds = SendDrivetoTooutAdc.class))
     public void PlansendDriveTotoOutAdc() {
     System.out.println( "New trip is added to agent " +agentID + " : Trip "+ tripIDList.get(tripIDList.size()-1));
     if (activestatus == true)
     // to control that the plan is not triggered because the Trip is removed from TripList
     //New trip should only be executed when the vehicle is available
     {
     System.out.println("New trip is assigned by TripReqControlAgent. Agent " + agentID + " is available to execute it");
     ExecuteTrips();
     } else {
     System.out.println("New trip is assigned by TripReqControlAgent. Agent " + agentID + " is busy and will execute later");
     }
     }
     **/

    /**
     *  MaintainTripService former SendDrivetoTooutAdc
     *
     *  desired behavior:
     *  start: when new trip is generated
     *
     *  try: to execute trip (activestatus == true)
     *
     *  finish: when tripList is empty
     */

    ///**
    @Goal(recur = true, recurdelay = 300)
    class MaintainTripService {
        @GoalCreationCondition(factadded = "tripIDList") //
        public MaintainTripService() {
        }

        @GoalTargetCondition
        boolean senttoMATSIM() {
            return !(activestatus == false);
        }
    }
//*/
    /**
     * DoNextTrip() former PlansendDriveTotoOutAdc()
     */
    ///**
    @Plan(trigger = @Trigger(goalfinisheds = MaintainTripService.class))
    public void DoNextTrip() {
        System.out.println( "New trip is added to agent " +agentID + " : Trip "+ tripIDList.get(tripIDList.size()-1));
        if (activestatus == true){

            ExecuteTrips();
            activestatus = false;

            //todo: if vorschieben nur ausführen wenn vorher keien fahroperation laufen!
            //TODO: in extra methode schieben
            //remove its agentID from the ActiveList of its SimSensoryInputBroker

            //updateAtInputBroker();

        } else {
            //System.out.println("New trip is assigned by TripReqControlAgent. Agent " + agentID + " is busy and will execute later");
        }
    }
    //*/
    public void ExecuteTripsNew() {
        newCurrentTrip(); // creates new current Trip if necessary and possible
        if (currentTrip.size() == 1) { //if there is a currentTrip
            //currentTripStatus();
            if (currentTrip.get(0).getProgress().equals("AtEndLocation")) {
                updateCurrentTripProgress("Finished");
            } else if (currentTrip.get(0).getProgress().equals("NotStarted")) {
                sendDriveTotoAdc();
                updateCurrentTripProgress("DriveToStart");
            } else if (currentTrip.get(0).getProgress().equals("AtStartLocation")) { // manage CustomerTrips that are AtStartLocation
                if (currentTrip.get(0).getTripType().equals("CustomerTrip")) {
                    if (customerMiss(currentTrip.get(0)) == true) { // customer not there
                        updateCurrentTripProgress("Failed");
                    } else if (customerMiss(currentTrip.get(0)) == false) { // customer still there
                        sendDriveTotoAdc();
                        updateCurrentTripProgress("DriveToEnd");
                    }
                }
                //add cases for other TripTypes here
                //else if(currentTrip.get(0).getTripType().equals("")) {
                //}
                // manage all other Trips that are AtStartLocation
                else {
                    updateCurrentTripProgress("Finished");
                }
            }
            // If the CurrentTrip is finished or failed > remove it
            if (currentTrip.get(0).getProgress().equals("Finished") || currentTrip.get(0).getProgress().equals("Failed")) {
                currentTrip.remove(0);
                //tripList.remove(0); //delete this line. is now isode of newCurrentTrip()
                if (tripList.size() > 0) { // if the tripList is not empty, depatch the next trip and send to data container
                    newCurrentTrip();
                    sendDriveTotoAdc();
                    //currentTripStatus();
                }
            }
        }
    }



    //#######################################################################
    //Goals and Plans : After the agentID is assigned to the Trike Agent,
    // Trike Agent should prepare everything for the synchronization process
    //#######################################################################

    @Goal(recur = true, recurdelay = 3000)
    class ReactoAgentIDAdded {
        public ReactoAgentIDAdded() {
        }
    }

    @Plan(trigger = @Trigger(goals = ReactoAgentIDAdded.class))
    private void ReacttoAgentIDAdded()
    {
        if (agentID != null) // only react if the agentID exists
        {

            if (SimIDMapper.NumberSimInputAssignedID.size() == JadexModel.SimSensoryInputBrokernumber) // to make sure all SimInputBroker also receives its ID so vehicle agent could choose one SimInputBroker ID to register
                if (sent == false) { // to make sure the following part only executed once
                    sent = true;
                    System.out.println("The agentid assigned for this vehicle agent is " + this.agentID);
                    // setTag for itself to receive direct communication from SimSensoryInputBroker when service INotifyService is used.
                    IServiceIdentifier sid = ((IService) agent.getProvidedService(INotifyService.class)).getServiceId();
                    agent.setTags(sid, "user:" + agentID);
                    //choosing one SimSensoryInputBroker to receive data from MATSIM
                    currentSimInputBroker = getRandomSimInputBroker();
                    // setTag for itself to receive direct communication from TripRequestControlAgent when service IsendTripService is used.
                    IServiceIdentifier sid2 = ((IService) agent.getProvidedService(IsendTripService.class)).getServiceId();
                    agent.setTags(sid2, "user:" + agentID);

                    //communicate with SimSensoryInputBroker when knowing the serviceTag of the SimSensoryInputBroker.
                    ServiceQuery<INotifyService2> query = new ServiceQuery<>(INotifyService2.class);
                    query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
                    query.setServiceTags("user:" + currentSimInputBroker); // choose to communicate with the SimSensoryInputBroker that it registered befre
                    Collection<INotifyService2> service = agent.getLocalServices(query);
                    for (Iterator<INotifyService2> iteration = service.iterator(); iteration.hasNext(); ) {
                        INotifyService2 cs = iteration.next();
                        cs.NotifyotherAgent(agentID); // write the agentID into the list of the SimSensoryInputBroker that it chose before
                    }

                    System.out.println("agent "+ this.agentID +"  registers at " + currentSimInputBroker);


                    // Notify TripRequestControlAgent and JADEXModel
                    TrikeMain.TrikeAgentNumber = TrikeMain.TrikeAgentNumber+1;
                    JadexModel.flagMessage2();
                    //action perceive is sent to matsim only once in the initiation phase to register to receive events
                    SendPerceivetoAdc();


                }
        }
    }

    //#######################################################################
    //Goals and Plans : to print out something when the data from MATSIM is
    //written to its belief base by the SimSensoryInputBroker
    //#######################################################################
//@Marcel Thus old code

    @Goal(recur = true,recurdelay = 300)
    class PerformSIMReceive {
        // Goal should be triggered when the simPerceptList or simActionList are triggered
        @GoalCreationCondition(beliefs = "resultfromMATSIM") //
        public PerformSIMReceive() {
        }
        @GoalTargetCondition
        boolean	PerceptorContentnotEmpty()
        {
            return ( !(SimPerceptList.isEmpty()) || !(SimActionList.isEmpty())|| (!(SimPerceptList.isEmpty()) && !(SimActionList.isEmpty())));
        }
    }

    @Plan(trigger = @Trigger(goalfinisheds = PerformSIMReceive.class))
    public void SensoryUpdate() {
        if (resultfromMATSIM.contains("true")) {
            System.out.println(agentID +" receives information from MATSIM");
            for (ActionContent actionContent : SimActionList) {
                System.out.println("The result of action "+ actionContent.getAction_type()+ " for agent "+ agentID+ " is " + actionContent.getState());
                //         System.out.println("An example of a parameter in SimactionList of agent "+agentID +"is " + actionContent.getParameters()[0]);
            }
            for (PerceptContent perceptContent : SimPerceptList) {
                System.out.println("agent " +agentID +"receive percepts in SimPerceptList" );
            }
            // reset for the next iteration
            setResultfromMASIM("false");
        //TODO: call decreaseBatteryHealthBasedOnDistance here from BatteryModel.java -oemer
            //BatteryModel.decreaseBatteryHealthBasedOnDistance(BatteryModel batteryModel, Id< Vehicle > specificVehicleId);
        }

        System.out.println("inside SensoryUpdate");
        currentTripStatus();
        //updateBeliefAfterAction();

        if (informSimInput == false) //make sure it only sent once per iteration
        {   informSimInput = true;
            if (activestatus == true && (!(SimPerceptList.isEmpty()) || !(SimActionList.isEmpty()))) {
                for (ActionContent actionContent : SimActionList) {
                    if (actionContent.getAction_type().equals("drive_to")) {
                        System.out.println("Agent " + agentID + " finished with the previous trip and now can take the next trip");
                        ExecuteTrips(); // can execute as soon as active status = true
                        //tripIDList.add("0");
                        //remove its agentID from the ActiveList of its SimSensoryInputBroker
                        updateAtInputBroker();
                        //bdiFeature.dispatchTopLevelGoal(new MaintainTripService());
                    }
                }
            }
            currentTripStatus();
        }


        /**
         if (resultfromMATSIM.contains("true")) {
         System.out.println(agentID +" receives information from MATSIM");
         for (ActionContent actionContent : SimActionList) {
         System.out.println("The result of action "+ actionContent.getAction_type()+ " for agent "+ agentID+ " is " + actionContent.getState());
         //         System.out.println("An example of a parameter in SimactionList of agent "+agentID +"is " + actionContent.getParameters()[0]);
         }

         for (PerceptContent perceptContent : SimPerceptList) {
         System.out.println("agent " +agentID +"receive percepts in SimPerceptList" );

         }
         // reset for the next iteration
         setResultfromMASIM("false");

         //@Marcel new code  added

         //actionContent.getAction_type()
         //actionContent.getState()
         //actionContent.getParameters()

         if (activestatus == true && (!(SimPerceptList.isEmpty()) || !(SimActionList.isEmpty()))) {
         for (ActionContent actionContent : SimActionList) {
         if (actionContent.getAction_type().equals("drive_to")) {
         System.out.println("Agent " + agentID + " finished with the previous trip and now can take the next trip");
         //ExecuteTrips(); // can execute as soon as active status = true
         tripIDList.add("0");
         }
         }
         //tripIDList.add("0");
         }
         }
         */


        // currentTripStatus();



        //updateBeliefAfterAction();

    }


    void updateAtInputBroker(){
        ServiceQuery<INotifyService2> query = new ServiceQuery<>(INotifyService2.class);
        query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
        query.setServiceTags("user:" + currentSimInputBroker);
        Collection<INotifyService2> service = agent.getLocalServices(query);
        for (Iterator<INotifyService2> iteration = service.iterator(); iteration.hasNext(); ) {
            INotifyService2 cs = iteration.next();
            cs.removeTrikeAgentfromActiveList(agentID);
            System.out.println(" Newly active Agent " + agentID + "notifies" + currentSimInputBroker + " that it finished deliberating");
        }
    }


    // After a succefull action in MATSIm: Updates the progreess of the current Trip and the Agent location
    //todo: better get the location from MATSim
    void updateBeliefAfterAction() {
        Trip CurrentTripUpdate = currentTrip.get(0);
        if (CurrentTripUpdate.getProgress().equals("DriveToStart")) {
            updateCurrentTripProgress("AtStartLocation");
            //SendPerceivetoAdc(); //TODO: only for tests delete if unsure
            agentLocation = CurrentTripUpdate.getStartPosition();
            //tripIDList.add("0"); //TODO: find better solution for example a goal trigger
        }

        if (CurrentTripUpdate.getProgress().equals("DriveToEnd")){
            updateCurrentTripProgress("AtEndLocation");
            agentLocation = CurrentTripUpdate.getEndPosition();
            //tripIDList.add("0"); //TODO: find better solution for example a goal trigger
        }
        //todo: action und perceive trennen! aktuell beides in beiden listen! löschen so nicht konsistent!
        SimActionList.remove(0); //TODO: hier genau drauf achten war beu Thu nicht so!
        //TODO: Send Updates to AreaAgent
        currentTripStatus();
        //System.out.println("simtime" + JadexModel.simulationtime); //
    }






    /**
     //Thu alter code

     @Goal(recur = true,recurdelay = 3000)
     class PerformSIMReceive {
     // Goal should be triggered when the simPerceptList or simActionList are triggered
     @GoalCreationCondition(beliefs = "resultfromMATSIM") //
     public PerformSIMReceive() {
     }
     @GoalTargetCondition
     boolean	PerceptorContentnotEmpty()
     {
     return ( !(SimPerceptList.isEmpty()) || !(SimActionList.isEmpty())|| (!(SimPerceptList.isEmpty()) && !(SimActionList.isEmpty())));
     }
     }

     @Plan(trigger = @Trigger(goalfinisheds = PerformSIMReceive.class))
     public void UpdateSensory() {
     if (resultfromMATSIM.contains("true")) {
     System.out.println(agentID +" receives information from MATSIM");
     for (ActionContent actionContent : SimActionList) {
     System.out.println("The result of action "+ actionContent.getAction_type()+ " for agent "+ agentID+ " is " + actionContent.getState());
     //         System.out.println("An example of a parameter in SimactionList of agent "+agentID +"is " + actionContent.getParameters()[0]);
     }

     for (PerceptContent perceptContent : SimPerceptList) {
     System.out.println("agent " +agentID +"receive percepts in SimPerceptList" );

     }
     // reset for the next iteration
     setResultfromMASIM("false");

     //@Marcel new code  added

     //actionContent.getAction_type()
     //actionContent.getState()
     //actionContent.getParameters()

     }

     System.out.println("inside SensoryUpdate");
     currentTripStatus();
     //updateBeliefAfterAction();

     }

     // After a succefull action in MATSIm: Updates the progreess of the current Trip and the Agent location
     //todo: better get the location from MATSim
     void updateBeliefAfterAction() {
     Trip CurrentTripUpdate = currentTrip.get(0);
     if (CurrentTripUpdate.getProgress().equals("DriveToStart")) {
     updateCurrentTripProgress("AtStartLocation");
     //SendPerceivetoAdc(); //TODO: only for tests delete if unsure
     agentLocation = CurrentTripUpdate.getStartPosition();
     //tripIDList.add("0"); //TODO: find better solution for example a goal trigger
     }

     if (CurrentTripUpdate.getProgress().equals("DriveToEnd")){
     updateCurrentTripProgress("AtEndLocation");
     agentLocation = CurrentTripUpdate.getEndPosition();
     //tripIDList.add("0"); //TODO: find better solution for example a goal trigger
     }
     //todo: action und perceive trennen! aktuell beides in beiden listen! löschen so nicht konsistent!
     SimActionList.remove(0); //TODO: hier genau drauf achten war beu Thu nicht so!
     //TODO: Send Updates to AreaAgent
     currentTripStatus();
     //System.out.println("simtime" + JadexModel.simulationtime); //
     }



     //should take the first trip from the trip list
     @Plan(trigger = @Trigger(goalfinisheds =  PerformSIMReceive.class))
     public void ExecuteTripandInform()
     {
     if (informSimInput == false) //make sure it only sent once per iteration
     {   informSimInput = true;
     if (activestatus == true && (!(SimPerceptList.isEmpty()) || !(SimActionList.isEmpty()))) {
     for (ActionContent actionContent : SimActionList) {
     if (actionContent.getAction_type().equals("drive_to")) {
     System.out.println("Agent " + agentID + " finished with the previous trip and now can take the next trip");
     ExecuteTrips(); // can execute as soon as active status = true


     //remove its agentID from the ActiveList of its SimSensoryInputBroker
     ServiceQuery<INotifyService2> query = new ServiceQuery<>(INotifyService2.class);
     query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
     query.setServiceTags("user:" + currentSimInputBroker);
     Collection<INotifyService2> service = agent.getLocalServices(query);
     for (Iterator<INotifyService2> iteration = service.iterator(); iteration.hasNext(); ) {
     INotifyService2 cs = iteration.next();
     cs.removeTrikeAgentfromActiveList(agentID);
     System.out.println(" Newly active Agent " + agentID + "notifies" + currentSimInputBroker + " that it finished deliberating");
     }
     }
     }
     }

     }
     }


     */

    public void setResultfromMASIM(String Result) {
        this.resultfromMATSIM = Result;
    }

    public void AddAgentNametoAgentList()
    {
        SimIDMapper.TrikeAgentNameList.add(agent.getId().getName());
    }

    public void AddTriptoTripList(Trip Trip)
    {
        tripList.add(Trip);
    }

    public void AddTripIDTripList(String ID)
    {
        tripIDList.add(ID);
    }

    public void AddJobToJobList(Job Job)
    {
        jobList.add(Job);
    }

    public void setAgentID(String agentid) {
        agentID = agentid;
    }

    public String getAgentID() {
        System.out.println(agentID);

        return agentID;
    }

    public void setActionContentList(List<ActionContent> actionContentList) {
        SimActionList = actionContentList;
    }


    //just for a test delete after
    public void setTestList(String TextMessage) {
        //TestList.add(TextMessage);
        System.out.println("Service: new Trip received " + TextMessage);
    }

    public List<ActionContent> getActionContentList() {
        return SimActionList;
    }

    public void setPerceptContentList(List<PerceptContent> perceptContentList) {
        SimPerceptList = perceptContentList;
    }

    public List<PerceptContent> getPerceptContentList() {
        return SimPerceptList;
    }

    public String getRandomSimInputBroker() // choose random SimInputBroker to register in the begining
    {
        List<String> SimInputBrokerList = SimIDMapper.NumberSimInputAssignedID;
        Random rand = new Random();
        String randomSimInputBroker = SimInputBrokerList.get(rand.nextInt(SimInputBrokerList.size()));
        return randomSimInputBroker;
    }


    //#######################################################################
    //Methods uses for sending trip info to data container
    //#######################################################################

    void newCurrentTrip(){
        System.out.println("Test if new currentTrip can bea created");
        if(currentTrip.size()==0 && tripList.size()>0 ){
            System.out.println("no currentTrip available");
            System.out.println("getting nextTrip from TripList");
            currentTrip.add(tripList.get(0));
            //tripList.remove(0);
            //       currentTrip.get(0).setProgress("NotStarted"); //because when SImSensoryInput sends back the result, it sets the progress to finished.
        }
    }

    /** Updates the progress of the CurrentTrip
     *
     * @param newProgress
     */
    void updateCurrentTripProgress(String newProgress) {
        Trip CurrentTripUpdate = currentTrip.get(0);
        CurrentTripUpdate.setProgress(newProgress);
        currentTrip.set(0, CurrentTripUpdate);
        currentTripStatus();
    }

    void currentTripStatus() {
        String currentTripID = currentTrip.get(0).getTripID();
        String currentTripType = currentTrip.get(0).getTripType();
        LocalDateTime currentVaTime = currentTrip.get(0).getVATime();
        Location currentStartPosition = currentTrip.get(0).getStartPosition();
        Location currentEndPosition = currentTrip.get(0).getEndPosition();
        String currentProgress = currentTrip.get(0).getProgress();
        System.out.println("\n currentTripStatus:");
        System.out.println("agentID: " + agentID);
        System.out.println("currentTripID: " + currentTripID);
        System.out.println("currentTripType: " + currentTripType);
        System.out.println("currentVaTime: " + currentVaTime);
        System.out.println("currentStartPosition: " + currentStartPosition);
        System.out.println("currentEndPosition: " + currentEndPosition);
        System.out.println("currentProgress: " + currentProgress);
    }

    // why public static?
    public static boolean customerMiss(Trip trip) {

        //todo: access simulation time and determine if the customer has already left
        // SimZeit abfragen, gewünschte VATime vergleichen und wenn mehr als 5 min, dann missed, oder würfel, je länger desto wahrscheinlicher
        // statt estimated Duration, echte Duration verwenden, die Berechnung soll erfolgen, wenn der Trike Agent beim Kunden ankommt. --> Wichtig für den Reward
        // drive to customer, check simulation time and start time(vaTime) and define delta
        // 1. option: if delta > 5 minutes then --> missed else success
        // 2. option: if delta > 10 probability higher that it is missed


        //Trip trip = new Trip("1", tripType, vaTime, startPosition, endPosition, progress);


        //  Time difference calculation using Time.java
        double timeDifferenceSeconds = Time.convertLocalDateTimeToDouble(trip.vaTime, Time.TimestepUnit.SECONDS); // get simulation time hjere?

        // Option 1: If the difference is greater than 300 seconds (5 minutes), then customer missed
        if (timeDifferenceSeconds > 300) {
            return true;
        }
        else {
            // Option 2: Probabilistic approach
            Random random = new Random();
            double probability = timeDifferenceSeconds * 0.05;
            double randomValue = random.nextDouble();
            return randomValue < probability;
            //System.out.println();
        }

        //double simulationtime = JadexModel.simulationtime;

    }

    public boolean customerMissProb(Trip trip) {
        boolean missed = false;
        double timeDifferenceSeconds = Time.convertLocalDateTimeToDouble(trip.vaTime, Time.TimestepUnit.SECONDS);

        if (timeDifferenceSeconds > 0) {
            double probability = 0.05 * timeDifferenceSeconds; //Probabilistic value
            missed = new Random().nextDouble() < probability;
        }
        return missed;
    }


    /**
     *  old version will be deprecated ater reorganisation
     */
    public void ExecuteTrips() {
        System.out.println("DoNextTrip running");
        System.out.println("tripList of agent" +agentID+ " :"+ tripList.size());
        System.out.println("currentTrip: " + currentTrip.size());


        newCurrentTrip(); // creates new current Trip if necessary and possible
        if (currentTrip.size() == 1) { //if there is a currentTrip
            currentTripStatus();

            if (currentTrip.get(0).getProgress().equals("AtEndLocation")) {
                updateCurrentTripProgress("Finished");
            } else if (currentTrip.get(0).getProgress().equals("NotStarted")) {
                sendDriveTotoAdc();
                //##########################################
                updateCurrentTripProgress("DriveToStart");
            } else if (currentTrip.get(0).getProgress().equals("AtStartLocation")) {
                // manage CustomerTrips that are AtStartLocation
                if (currentTrip.get(0).getTripType().equals("CustomerTrip")) {
                    if (customerMiss(currentTrip.get(0)) == true) { // customer not there
                        updateCurrentTripProgress("Failed");
                    } else if (customerMiss(currentTrip.get(0)) == false) { // customer still there
                        //##########################################
                        sendDriveTotoAdc();
                        //##########################################
                        updateCurrentTripProgress("DriveToEnd");
                    }
                }
                //add cases for other TripTypes here
                //else if(currentTrip.get(0).getTripType().equals("")) {
                //}
                // manage all other Trips that are AtStartLocation
                else {
                    updateCurrentTripProgress("Finished");
                }

            }
            // If the CurrentTrip is finished or failed > remove it
            if (currentTrip.get(0).getProgress().equals("Finished") || currentTrip.get(0).getProgress().equals("Failed")) {
                currentTrip.remove(0);
                tripList.remove(0);
                if (tripList.size() > 0) { // if the tripList is not empty, depatch the next trip and send to data container
                    newCurrentTrip();
                    sendDriveTotoAdc();
                    currentTripStatus();
                }
            }
        }
    }

    public void sendDriveTotoAdc()
    {
        Object[] Endparams = new Object[6];

        // needs to get seperate parameter for different types of trip
        if (currentTrip.get(0).getProgress().equals("NotStarted"))
        {
            Endparams[0] = Constants.DRIVETO;
            Endparams[1] = currentTrip.get(0).getStartPosition().getCoordinates();

        }
        if (currentTrip.get(0).getProgress().equals("AtStartLocation"))
        {

            Endparams[0] = Constants.DRIVETO;
            Endparams[1] = currentTrip.get(0).getEndPosition().getCoordinates();
        }
        Endparams[2] = JadexModel.simulationtime;
        Endparams[3] = Constants.EvacRoutingMode.carFreespeed;
        Endparams[4] = "EvacPlace";
        Endparams[5] = currentTrip.get(0).getTripID();

        SimActuator.getEnvironmentActionInterface().packageAction(agentID, "drive_to", Endparams, null);
        activestatus = false; // to mark that this trike agent is not available to take new trip

    }

    public void SendPerceivetoAdc() // needs to send in the begining to subscribe to events in MATSIM
    {
        Object[] params = new Object[7];
        params[0] = "blocked";
        params[1] = "congestion";
        params[2] = "arrived"; // five secs from now;
        params[3] = "departed";
        params[4] = "activity_started";
        params[5] = "activity_ended"; // add replan activity to mark location/time of replanning
        params[6] = "stuck";

        SimActuator.getEnvironmentActionInterface().packageAction(agentID, "perceive", params, "");
    }

    public double getDrivingDistanceTo(Location location) throws AgentNotFoundException { // EUclician Distanz
        double dist =
                (double)SimActuator.getQueryPerceptInterface().queryPercept(
                        String.valueOf(agentID),
                        Constants.REQUEST_DRIVING_DISTANCE_TO,
                        location.getCoordinates());
        return dist;
    }

    public  Location getCurrentLocation() throws AgentNotFoundException {
        Location CurrentLocation = (Location) SimActuator.getQueryPerceptInterface().queryPercept(String.valueOf(agentID), Constants.REQUEST_LOCATION, null);

        return CurrentLocation;
    }

    //added from Ekins' trike agent -oemer
    public double getMyChargestate()
    {
        return my_chargestate;
    }
    public double getMyBatteryHealth() {
        return my_batteryhealth;
    }
    public double getMyNumberOfCharges() {
        return my_numberofcharges;
    }
    public void setMyNumberOfCharges(double my_numberofcharges) {
        this.my_numberofcharges = my_numberofcharges;
    }
    public void setMyBatteryHealth(double my_batteryhealth) {
        this.my_batteryhealth = my_batteryhealth;
    }
    public void setMyChargestate(double my_chargestate)
    {
        this.my_chargestate = my_chargestate;
    }
    public int getCarriedCustomer()
    {
        return carriedcustomer;
    }
    public boolean getMyAutopilot()
    {
        return my_autopilot;
    }
    public void setMyAutopilot(boolean my_autopilot)
    {
        this.my_autopilot = my_autopilot;
    }
    public double getMySpeed()
    {
        return my_speed;
    }
    public void setMySpeed(double my_speed)
    {
        this.my_speed = my_speed;
    }
    public Location getLocation()
    {
        return agentLocation;
    }
    public void setLocation(Location agentLocation)
    {
        this.agentLocation = agentLocation;
    }
}