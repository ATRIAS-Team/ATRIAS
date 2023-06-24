package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;


import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdiabm.v3.AgentNotFoundException;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.TrikeMain;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.IDValidateService.IDValidateSevice;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.IDValidateService.NotifyControlAgentService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.ISendTripService.IsendTripService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.ISendTripService.ReceiveTripService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.IMappingAgentsService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.WrittingIDService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.INotifyService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.TrikeAgentReceiveService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.TrikeAgentSendService;
import io.github.agentsoz.util.Location;
import jadex.bdiv3.BDIAgentFactory;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.ServiceQuery;
import jadex.bridge.service.types.clock.IClockService;
import jadex.micro.annotation.*;

import java.time.LocalDateTime;
import java.util.*;


@Agent(type= BDIAgentFactory.TYPE)
@ProvidedServices({
        @ProvidedService(type= IMappingAgentsService.class, implementation=@Implementation(WrittingIDService.class)),
        @ProvidedService(type= INotifyService.class, implementation=@Implementation(TrikeAgentReceiveService.class)),
        @ProvidedService(type= IDValidateSevice.class, implementation=@Implementation(NotifyControlAgentService.class)),
        @ProvidedService(type= INotifyService2.class, implementation=@Implementation(TrikeAgentSendService.class)),
        @ProvidedService(type= IsendTripService.class, implementation=@Implementation(ReceiveTripService.class)),

})
@RequiredServices({
        @RequiredService(name="clockservice", type= IClockService.class),
        @RequiredService(name="sendtripservices", type= IsendTripService.class),
        @RequiredService(name="mapservices", type= IMappingAgentsService.class),
        @RequiredService(name="broadcastingservices", type= INotifyService.class, scope= ServiceScope.PLATFORM),
        @RequiredService(name="notifyassignedidservices", type= IDValidateSevice.class, scope= ServiceScope.PLATFORM),
        @RequiredService(name="notifywhenexecutiondoneservice", type= INotifyService2.class, scope= ServiceScope.PLATFORM),

        // multiple=true,
})


/*@Arguments({
		@Argument(name="keyword", clazz=String.class, defaultvalue="\"nerd\"", description="The keyword to react to."),
		@Argument(name="reply", clazz=String.class, defaultvalue="\"Watch your language\"", description="The reply message."),
		@Argument(name="thread", clazz=Thread.class, description="The reply message.")

})

 */

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
  //  public List <String> resultfromMATSIM = Arrays.asList("false");
    public String resultfromMATSIM = "false";

    // to indicate if the agent is available to take the new ride
    @Belief
    public boolean activestatus;

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
    public String actionID;
    public Object[] actionparameters;
    @Belief
    public boolean sent = false;
    public String write = null;
    public boolean informSimInput = false;
    public String agentName;

    public String currentSimInputBroker;
    private SimActuator SimActuator;





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

    }

    //#######################################################################
    //Goals and Plans for Sending data to AgentDataContainer, for the Aktorik
    //#######################################################################

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
        if (activestatus == true) // to control that the plan is not triggered because the Trip is removed from TripList
        {
            System.out.println("New trip is assigned by TripReqControlAgent. Agent " + agentID + " is available to execute it");
            ExecuteTrips();
        } else {
            System.out.println("New trip is assigned by TripReqControlAgent. Agent " + agentID + " is busy and will execute later");
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
        if (agentID != null) {

            if (SimIDMapper.NumberSimInputAssignedID.size() == JadexModel.SimSensoryInputBrokernumber) // to make sure all SimInputBroker also receives its ID
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
                    query.setServiceTags("user:" + currentSimInputBroker);
                    Collection<INotifyService2> service = agent.getLocalServices(query);
                    for (Iterator<INotifyService2> iteration = service.iterator(); iteration.hasNext(); ) {
                        INotifyService2 cs = iteration.next();
                        cs.NotifyotherAgent(agentID);
                    }

                    System.out.println("agent "+ this.agentID +"  registers at " + currentSimInputBroker);


                    // Notify TripRequestControlAgent and JADEX
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

    //should take the first trip from the trip list
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
        }
    }

    //#######################################################################
    //Goals and Plans : To react when drive_to action result is available
    //#######################################################################
    /*
    @Goal(recur = true,recurdelay = 3000)
    class InformwhenFinished {
        // when the DRIVE_TO Action is successful, trikeagent will become newly active agents in this cycle
        // and is added to the list of activeagent in the sensoryAgent
        //In this BDI-MATSIM cycle, these active TrikeAgent needs to execute the next trip in the
        // trip list if available and inform the SimSensory Agent.
        // The BDI cycle is finished when SimSensory receives information from all of these newlyactiveAgents.
        @GoalCreationCondition(beliefs = "activestatus") //
        public InformwhenFinished() {
        }
        @GoalTargetCondition
        boolean	activestatusupdated()
        {
            return (activestatus == true || activestatus == false);
        }
    }

     */

    //should take the first trip from the trip list
    @Plan(trigger = @Trigger(goalfinisheds =  PerformSIMReceive.class))
    public void ExecuteTripandInform()
    {
        if (informSimInput == false)
        // will need to rewrite the code for this
        {   informSimInput = true;
            if (activestatus == true && (!(SimPerceptList.isEmpty()) || !(SimActionList.isEmpty()))) {
                for (ActionContent actionContent : SimActionList) {
                    if (actionContent.getAction_type().equals("drive_to")) {
                        System.out.println("Agent " + agentID + " finished with the previous trip and now can take the next trip");
                        // only when action = passed vs fail thi moi execute phan nay
                        ExecuteTrips();


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

    public List<ActionContent> getActionContentList() {
        return SimActionList;
    }

    public void setPerceptContentList(List<PerceptContent> perceptContentList) {
        SimPerceptList = perceptContentList;
    }

    public List<PerceptContent> getPerceptContentList() {
        return SimPerceptList;
    }

    public String getRandomSimInputBroker()
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
    }

    void currentTripStatus() {
        String currentTripType = currentTrip.get(0).getTripType();
        LocalDateTime currentVaTime = currentTrip.get(0).getVaTime();
        Location currentStartPosition = currentTrip.get(0).getStartPosition();
        Location currentEndPosition = currentTrip.get(0).getEndPosition();
        String currentProgress = currentTrip.get(0).getProgress();
        System.out.println("\n currentTripStatus:");
        System.out.println("currentTripType: " + currentTripType);
        System.out.println("currentVaTime: " + currentVaTime);
        System.out.println("currentStartPosition: " + currentStartPosition);
        System.out.println("currentEndPosition: " + currentEndPosition);
        System.out.println("currentProgress: " + currentProgress);
    }

    boolean customerMiss() {
        //##########################################
        //todo: access time information and determine if the customer have alraedy leaved
        //##########################################
        boolean customerMiss = true;

        return customerMiss;
    }

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
                    if (customerMiss() == true) { // customer not there
                        updateCurrentTripProgress("Failed");
                    } else if (customerMiss() == false) { // customer still there
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
            // If the CurrentTrip is finshied or failed > remove it
            if (currentTrip.get(0).getProgress().equals("Finished") || currentTrip.get(0).getProgress().equals("Failed")) {
                currentTrip.remove(0);
                tripList.remove(0);
                if (tripList.size() > 0) {
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

    public void SendPerceivetoAdc()
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



}








