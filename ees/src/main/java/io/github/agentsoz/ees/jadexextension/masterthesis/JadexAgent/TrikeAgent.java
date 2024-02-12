/* TrikeAgnet.java
 * Version: v0.8 (19.01.2024)
 * changelog: working with decision tasks now
 * @Author Marcel (agent logic), Thu (BDI-ABM sync), Oemer (customer miss)
 *
 *
 */
package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdiabm.v3.AgentNotFoundException;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.TrikeAgentService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.WritingIDService;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.TrikeMain;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.IMappingAgentsService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.INotifyService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.TrikeAgentReceiveService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.TrikeAgentSendService;
import io.github.agentsoz.util.Location;
import io.github.agentsoz.util.Time;
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
import java.util.*;

import static io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService.messageToService;

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
    //ATTENTION @Mariam
    //IMPORTANT! right now location will be set after the first drive operation
    @Belief
    private Location agentLocation; // position of the agent TODO: init location with start location from matsim


    //todo: delete when replaced with decisionTaskList
    @Belief
    public List<Job> jobList = new ArrayList<>();
    @Belief
    public List<DecisionTask> decisionTaskList = new ArrayList<>();

    @Belief
    public List<DecisionTask> FinishedDecisionTaskList = new ArrayList<>();

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

    @Belief
    Double drivingSpeed = 6.0;


    /**
     * Every DecisionTask with a score equal or higher will be commited
     * todo: should be initialized from a configFile()
     */
    public Double commitThreshold = 50.0;

    public boolean informSimInput = false;

    public String currentSimInputBroker;
    private SimActuator SimActuator;

    //test variables
    //test variables
    public boolean isModified = false;
    boolean bool = true;


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
        bdiFeature.dispatchTopLevelGoal(new TimeTest());


        //sendMessage("area:0", "request", "");
    }

    @Goal(recur=true, recurdelay=1000) //in ms
    class ManageFlutter {
    }

    @Plan(trigger=@Trigger(goals=ManageFlutter.class))
    private void WriteFlutter()
    {
        /** TODO: @Mariam look for open questions in the firebase database and write answers into it
         *
         */
    }

    /**
     * This is just a debug Goal that will print many usefull information every 10s
     * TODO: find a better name
    */
    @Goal(recur=true, recurdelay=10000)
           class TimeTest {
    }

    @Plan(trigger=@Trigger(goals=TimeTest.class))
    private void TimeTestPrint()
    {
        System.out.println("agentID " + agentID +  " simtime" + JadexModel.simulationtime);
        Status();
    }

    /**
     * Will generate Trips from the Jobs sent by the Area Agent
     */

    @Goal(recur=true, recurdelay=1000) //standard = 1000
    class MaintainManageJobs
    {
        @GoalMaintainCondition	// The cleaner aims to maintain the following expression, i.e. act to restore the condition, whenever it changes to false.
        boolean jobListEmpty()
        {
            return decisionTaskList.size()==0; // Everything is fine as long as the charge state is above 20%, otherwise the cleaner needs to recharge.
        }
    }

    @Plan(trigger=@Trigger(goals=MaintainManageJobs.class))
    private void EvaluateDecisionTask()
    {
        /**
         * todo: remove this case when decisionTask is working
         */

        //if (jobList.size()>0) {
        //System.out.println("EvaluateDecisionTask: old Version Job");

        //Job jobToTrip = jobList.get(0);
        //Trip newTrip = new Trip(jobToTrip.getID(), "CustomerTrip", jobToTrip.getVATime(), jobToTrip.getStartPosition(), jobToTrip.getEndPosition(), "NotStarted");
        //TODO: create a unique tripID
        //tripList.add(newTrip);

        // TEST MESSAGE DELETE LATER
            //sendTestAreaAgentUpdate();
        //
            /**
             * agentID
             * TODO: @Mariam Trike will commit a Trip here. write into firebase
             */

            //TODO: zwischenschritte (visio) fehlen utilliy usw.
        //tripIDList.add("1");
        //jobList.remove(0);
    //}




        /**
         * todo: will replace solution above
         */
        if (decisionTaskList.size()>0) {
            System.out.println("EvaluateDecisionTask: new Version");
            boolean finishedForNow = false;
            while (finishedForNow == false) {
                Integer changes = 0;
                for (int i = 0; i < decisionTaskList.size(); i++) {
                    Integer currentChanges = selectNextAction(i);



                    //progress abgreifen
                    // funktion aufrufen
                    //finished decisiontask List anlegen?
                    //wenn durchlauf ohen Änderungen finishedForNow = true
                    changes += currentChanges;
                }
                if(changes==0){
                    finishedForNow = true;
                }
            }

            /**
            Trip newTrip = new Trip(jobToTrip.getID(), "CustomerTrip", jobToTrip.getVATime(), jobToTrip.getStartPosition(), jobToTrip.getEndPosition(), "NotStarted");
            //TODO: create a unique tripID
            tripList.add(newTrip);

            // TEST MESSAGE DELETE LATER
            sendTestAreaAgentUpdate();
            //
            /**
             * agentID
             * TODO: @Mariam Trike will commit a Trip here. write into firebase
             */

            /**

            //TODO: zwischenschritte (visio) fehlen utilliy usw.
            tripIDList.add("1");
            jobList.remove(0);
            */


        }

    }

    public Integer selectNextAction(Integer position){
        Integer changes = 0;
        if (decisionTaskList.get(position).getStatus().equals("new")){
            /**
             *  Execute Utillity here > "commit"|"delegate"
             */
            Double ownScore = calculateUtillity(decisionTaskList.get(position));
            ownScore = 0.0; //todo: delete this line after the implementation of the cnp
            decisionTaskList.get(position).setUtillityScore(agentID, ownScore);
            if (ownScore < commitThreshold){
                decisionTaskList.get(position).setStatus("delegate");
            }
            else{ decisionTaskList.get(position).setStatus("commit");}
            changes += 1;
        }
        else if (decisionTaskList.get(position).getStatus().equals("commit")){
            /**
             *  create trip here
             */
            DecisionTask dTaToTrip = decisionTaskList.get(position);
            Trip newTrip = new Trip(dTaToTrip.getIDFromJob(), "CustomerTrip", dTaToTrip.getVATimeFromJob(), dTaToTrip.getStartPositionFromJob(), dTaToTrip.getEndPositionFromJob(), "NotStarted");
            //TODO: create a unique tripID
            tripList.add(newTrip);
            tripIDList.add("1");

            decisionTaskList.get(position).setStatus("committed");
            FinishedDecisionTaskList.add(dTaToTrip);
            //decisionTaskList.remove(position); //geht nicht! warum? extra methode testen
            decisionTaskList.remove(position.intValue()); //geht nicht! warum? extra methode testen


            //decisionTaskList.remove(0);

            // TEST MESSAGE DELETE LATER //TODO: unsed Code from Mahkam
            //sendTestAreaAgentUpdate();
            //
            /**
             * agentID
             * TODO: @Mariam Trike will commit a Trip here. write into firebase
             */
            changes += 1;
        }
        else if (decisionTaskList.get(position).getStatus().equals("delegate")){
            /**
             *  start cnp here > "waitingForNeighbourlist"
             */
            //TODO: neighbour request here
            //TODO: adapt
            // TEST MESSAGE DELETE LATER
            //bool makes sure that the methods below are called only once

            ArrayList<String> values = new ArrayList<>();
            values.add(decisionTaskList.get(position).getJobID()); //todo move into a method
            decisionTaskList.get(position).setStatus("waitingForNeighbours");
            sendMessage("area:0", "request", "callForNeighbours", values);

                //sendTestAreaAgentUpdate();
                //testTrikeToTrikeService();
                //testAskForNeighbours();
            changes += 1;
        }
        else if (decisionTaskList.get(position).getStatus().equals("readyForCFP")){
            /**
             *  send cfp> "waitingForProposals"
             */
            Job JobForCFP = decisionTaskList.get(position).getJob();
            ArrayList<String> neighbourIDs = decisionTaskList.get(position).getNeighbourIDs();
            for( int i=0; i<neighbourIDs.size(); i++){
                //todo: klären message pro nachbar evtl mit user:
                //todo: action values definieren
                // values: gesammterJob evtl. bereits in area zu triek so vorhanden?
                //sendMessageToTrike(neighbourIDs.get(i), "CallForProposal", "CallForProposal", JobForCFP.toArrayList());
            testTrikeToTrikeService(neighbourIDs.get(i), "CallForProposal", "CallForProposal", JobForCFP.toArrayList());
            }


            decisionTaskList.get(position).setStatus("waitingForProposals");
            changes += 1;
        }
        else if (decisionTaskList.get(position).getStatus().equals("waitingForProposals")){
            //todo: überprüfen ob bereits alle gebote erhalten
            // falls ja ("readyForDecision")
            //todo:
            if (decisionTaskList.get(position).testAllProposalsReceived() == true){
                //System.out.println("");
                decisionTaskList.get(position).setStatus("readyForDecision");
            }
        }
        else if (decisionTaskList.get(position).getStatus().equals("readyForDecision")){
            /**
             *  send agree/cancel > "waitingForConfirmations"
             */
            decisionTaskList.get(position).tagBestScore(agentID);
            for (int i=0; i<decisionTaskList.get(position).getUTScoreList().size(); i++){
                String bidderID = decisionTaskList.get(position).getUTScoreList().get(i).getBidderID();
                String tag = decisionTaskList.get(position).getUTScoreList().get(i).getTag();
                if(tag.equals("AcceptProposal")){
                    //todo ablehnugn schicken
                    ArrayList<String> values = new ArrayList<>();
                    values.add(decisionTaskList.get(position).getJobID());
                    testTrikeToTrikeService(bidderID, tag, tag, values);
                    decisionTaskList.get(position).setStatus("waitingForConfirmations");
                }
                else if(tag.equals("RejectProposal")){
                    //todo: zusage schicken
                    ArrayList<String> values = new ArrayList<>();
                    values.add(decisionTaskList.get(position).getJobID());
                    testTrikeToTrikeService(bidderID, tag, tag, values);
                }
                else if(tag.equals("AcceptSelf")){
                    //todo: selbst zusagen
                    decisionTaskList.get(position).setStatus("commit");
                }
                else{
                    //todo: print ungültiger tag
                    System.out.println(agentID + ": invalid UTScoretag");
                }
                decisionTaskList.get(position).getUTScoreList();

            }
            //decisionTaskList.get(position).setStatus("waitingForConfirmations");
            changes += 1;
        }
        else if (decisionTaskList.get(position).getStatus().equals("readyForConfirmation")){
            /**
             *  send bid > "commit"
             */
            changes += 1;
        }
        else if (decisionTaskList.get(position).getStatus().equals("proposed")){
            /**
             *  send bid > "waitingForManager"
             */
            Double ownScore = calculateUtillity(decisionTaskList.get(position));
            //todo: eigene utillity speichern
            // send bid
            // ursprung des proposed job bestimmen
            ArrayList<String> values = new ArrayList<>();

            values.add(decisionTaskList.get(position).getJobID());
            values.add("#");
            values.add(String.valueOf(ownScore));

            //zb. values = jobid # score
            testTrikeToTrikeService(decisionTaskList.get(position).getOrigin(), "Propose", "Propose", values);
            decisionTaskList.get(position).setStatus("waitingForManager");

            changes += 1;
        }
        else if (decisionTaskList.get(position).getStatus().equals("notAssigned")){
            //todo in erledigt verschieben
            FinishedDecisionTaskList.add(decisionTaskList.get(position));
            decisionTaskList.remove(position.intValue());

        }
        else if (decisionTaskList.get(position).getStatus().equals("waitingForConfirmations")){
            //todo: test timeout here
        }
        else if (decisionTaskList.get(position).getStatus().equals("waitingForManager")){
            //todo: test timeout here
        }
        else if (decisionTaskList.get(position).getStatus().equals("committed")){
            System.out.println("should not exist: " + decisionTaskList.get(position).getStatus());
            //decisionTaskList.remove(0);
        }
        else {
            System.out.println("invalid status: " + decisionTaskList.get(position).getStatus());
        }
        return changes;
    }

    /** Utillity Function
     * should be switchable between a regular and a learning attempt
     *
     * @return
     */
    Double calculateUtillity(DecisionTask newTask){
        Double utillityScore = 100.0;
        /**

        newTask.getStartPositionFromJob();
        newTask.getEndPositionFromJob();
        newTask.getVATimeFromJob();


        Double a = 1.0/3.0;
        Double b = 1.0/3.0;
        Double c = 1.0/3.0;

        //todo get vaTime and simulation time in same format

        Integer lastEntry = Math.max(tripList.size(), 0);


        //todo: falls kunden fahrt

        String LastTripType = tripList.get(lastEntry).getTripType();



        tripList.get(lastEntry).getEndPosition();
        //falls andere
        tripList.get(lastEntry).getEndPosition();

        //distanz zu evaluierenden trip


        // punctuallity
        double VaTimeSeconds = Time.convertLocalDateTimeToDouble(newTask.getVATimeFromJob(), Time.TimestepUnit.SECONDS);


        drivingSpeed
        //todo: get start from last trip in triplist
        //todo: calculate arriving time at end location

        //todo: calculate distance
        //todo: estimate driving time in seconds by a speed of 6kmh
        //todo: estimate arriving time
        //todo: calculate a score 0.0 to 100.0 based of the delay



        //todo: global variable theta

        Double uPunctuallity = 100.0;





        // Battery
        Double uBattery = 100.0;
        Double curentBatteryLevel = 100.0;
        Double batteryLevelAfterTrips;
        Double estimatedEnergyConsumtion;
        Double negativeInfinity = Double.NEGATIVE_INFINITY;
        //todo ennergieverbrauch für zu evuluierenden job bestimmen
        //


        //todo: battery from Ömer needed
        // differ between trips with and without customer???
        if (currentTrip.size()>0){
            //currentTrip.get(0).getStartPosition();
            //currentTrip.get(0).getEndPosition();
            //batteryLevelAfterTrips =
        }
        for (int i=0; i<tripList.size(); i++){
            //tripList.get(i).getStartPosition();
            //tripList.get(i).getEndPosition();
            //batteryLevelAfterTrips =
        }
        if (batteryLevelAfterTrips<estimatedEnergyConsumtion){
            uBattery = negativeInfinity;
        }
        else {
            Double bFactor;
            if (curentBatteryLevel > 0.8){
                bFactor = 1.0;
            }
            else if (curentBatteryLevel >= 0.3){
                bFactor = 0.75;
            }
            else if (curentBatteryLevel < 0.3){
                bFactor = 0.1;
            }
            // ???? batteryLevelAfterTrips or 100?
            uBattery = bFactor * ((batteryLevelAfterTrips - estimatedEnergyConsumtion)/ batteryLevelAfterTrips);
        }

        //Distance
        //falls kunden fahrt
        tripList.get(Math.max(tripList.size(), 0)).getEndPosition();
        //falls andere
        tripList.get(Math.max(tripList.size(), 0)).getEndPosition();

        //distanz zu evaluierenden trip
        Integer lastEntry = Math.max(tripList.size(), 0);


        Double uDistance = 100.0;

        // endpos von letzten trip in triplist bestimmen
        // distence zu aktuellen zu evuluerenden berechnen
        Double distanceToStart;
        Double dmax;
        uDistance = (dmax - distanceToStart/dmax);



        utillityScore = a * uPunctuallity + b * uBattery + c * uDistance;

        if (utillityScore<0){
            utillityScore = 0.0;
        }
         **/

        return utillityScore;
    }


    /**
     *  MaintainTripService former SendDrivetoTooutAdc
     *
     *  desired behavior:
     *  start: when new trip is generated

     */

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
         //TODO: when able to remove ExecuteTrips from Sensory update the following lines are necessary
         //remove its agentID from the ActiveList of its SimSensoryInputBroker
         // updateAtInputBroker();
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
                    agent.setTags(sid, "" + agentID);
                    //choosing one SimSensoryInputBroker to receive data from MATSIM
                    currentSimInputBroker = getRandomSimInputBroker();

                    // setTag for itself to receive direct communication from TripRequestControlAgent when service IsendTripService is used.
                    IServiceIdentifier sid2 = ((IService) agent.getProvidedService(IAreaTrikeService.class)).getServiceId();
                    agent.setTags(sid2, "" + agentID);

                    //communicate with SimSensoryInputBroker when knowing the serviceTag of the SimSensoryInputBroker.
                    ServiceQuery<INotifyService2> query = new ServiceQuery<>(INotifyService2.class);
                    query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
                    query.setServiceTags("" + currentSimInputBroker); // choose to communicate with the SimSensoryInputBroker that it registered befre
                    Collection<INotifyService2> service = agent.getLocalServices(query);
                    for (INotifyService2 cs : service) {
                        cs.NotifyotherAgent(agentID); // write the agentID into the list of the SimSensoryInputBroker that it chose before
                    }
                    System.out.println("agent "+ this.agentID +"  registers at " + currentSimInputBroker);
                    // Notify TripRequestControlAgent and JADEXModel
                    TrikeMain.TrikeAgentNumber = TrikeMain.TrikeAgentNumber+1;
                    JadexModel.flagMessage2();
                    //action perceive is sent to matsim only once in the initiation phase to register to receive events
                    SendPerceivetoAdc();


                    agentLocation = new Location("", 238654.693529, 5886721.094209);

                    sendAreaAgentUpdate("register");
                    /**
                     * TODO: @Mariam initiale anmeldung an firebase hier
                     */

                }





        }
    }

    //#######################################################################
    //Goals and Plans : to print out something when the data from MATSIM is
    //written to its belief base by the SimSensoryInputBroker
    //#######################################################################

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
            //optional loop to print the SimActionList and the SimPerceptList
            //for (ActionContent actionContent : SimActionList) {
                //System.out.println("The result of action "+ actionContent.getAction_type()+ " for agent "+ agentID+ " is " + actionContent.getState());
                //         System.out.println("An example of a parameter in SimactionList of agent "+agentID +"is " + actionContent.getParameters()[0]);
            //}
            //for (PerceptContent perceptContent : SimPerceptList) {
              //  System.out.println("agent " +agentID +"receive percepts in SimPerceptList" );
            //}
            // reset for the next iteration
            setResultfromMASIM("false");
        }
        currentTripStatus();
        //updateBeliefAfterAction();
        if (informSimInput == false) //make sure it only sent once per iteration
        {   informSimInput = true;
            if (activestatus == true && (!(SimPerceptList.isEmpty()) || !(SimActionList.isEmpty()))) {
                for (ActionContent actionContent : SimActionList) {
                    if (actionContent.getAction_type().equals("drive_to")) {
                        System.out.println("Agent " + agentID + " finished with the previous trip and now can take the next trip");
                        updateBeliefAfterAction();
                        //TODO: ExecuteTrips should not be executes here, violates our VISIO diagram!
                        ExecuteTrips();
                        //tripIDList.add("0"); // TODO: this is an alternative for ExecutesTrips but it will not work deterministic!
                        //TODO: soemtimes teh agent will not execute all trips, further investigatin necessary
                        //TODO: mostly the error relates in someway to the activestatus which will not change back to true
                        //remove its agentID from the ActiveList of its SimSensoryInputBroker
                        updateAtInputBroker();
                    }
                }
            }
            currentTripStatus();
        }
    }

    /**
     * for the sny of the cycle
     */
    void updateAtInputBroker(){
        ServiceQuery<INotifyService2> query = new ServiceQuery<>(INotifyService2.class);
        query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
        query.setServiceTags("" + currentSimInputBroker);
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
             agentLocation = CurrentTripUpdate.getStartPosition();
         }
        if (CurrentTripUpdate.getProgress().equals("DriveToEnd")){
            updateCurrentTripProgress("AtEndLocation");
            agentLocation = CurrentTripUpdate.getEndPosition();
        }
        /**
         * TODO: @Mariam update firebase after every MATSim action: location of the agent
         */
        System.out.println("Neue Position: " + agentLocation);
        sendAreaAgentUpdate("update");
        //todo: action und perceive trennen! aktuell beides in beiden listen! löschen so nicht konsistent!
        //TODO: @Mahkam send Updates to AreaAgent
        currentTripStatus();
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

    //todo: remove for AddDecisionTask
    public void AddJobToJobList(Job Job)
    {
        jobList.add(Job);
    }

    public void AddDecisionTask(DecisionTask decisionTask)
    {
        decisionTaskList.add(decisionTask);
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
        System.out.println("\n currentTripStatus:");
        System.out.println("AgentID: " + agentID + " currentTripID: " + currentTrip.get(0).getTripID());
        System.out.println("AgentID: " + agentID + " currentTripType: " + currentTrip.get(0).getTripType());
        System.out.println("AgentID: " + agentID + " currentVaTime: " + currentTrip.get(0).getVATime());
        System.out.println("AgentID: " + agentID + " currentStartPosition: " + currentTrip.get(0).getStartPosition());
        System.out.println("AgentID: " + agentID + " currentEndPosition: " +currentTrip.get(0).getEndPosition());
        System.out.println("AgentID: " + agentID + " currentProgress: " + currentTrip.get(0).getProgress());
    }

    void Status(){
        //if (agentID.equals("0")){
            System.out.println("AgentID: " + agentID + " activestatus: " + activestatus);
            System.out.println("AgentID: " + agentID + " currentTrip.size: " + currentTrip.size());
            System.out.println("AgentID: " + agentID + " tripList.size: " + tripList.size());
            System.out.println("AgentID: " + agentID + " decisionTaskList.size: " + decisionTaskList.size());
            System.out.println("AgentID: " + agentID + " SimActionList: " + SimActionList.size());
            System.out.println("AgentID: " + agentID + " SimPerceptList: " + SimPerceptList.size());
            //for (ActionContent actionContent : SimActionList) {
                //System.out.println("AgentID: " + agentID + " actionType: "+ actionContent.getAction_type() + " actionState: " + actionContent.getState());
            //}
        for (int i=0; i<decisionTaskList.size(); i++){
            System.out.println("AgentID: " + agentID + " decisionTaskList status: " + decisionTaskList.get(i).getStatus());
        }


            currentTripStatus();
        //}
    }

    // why public static?
    public static boolean customerMiss(Trip trip) {
        //TODO: @oemer check if it will work correctly

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
     *  handles the progress of the current Trip
     */
    public void ExecuteTrips() {
        System.out.println("DoNextTrip running");
        System.out.println("tripList of agent" +agentID+ " :"+ tripList.size());
        System.out.println("currentTrip: " + currentTrip.size());
        //TODO: erst erledigtes löschen dann neue ausführen!
        newCurrentTrip(); // creates new current Trip if necessary and possible
        if (currentTrip.size() == 1) { //if there is a currentTrip
            currentTripStatus();
            // second part drive operations
            if (currentTrip.get(0).getProgress().equals("AtEndLocation")) {
                updateCurrentTripProgress("Finished");
            } else if (currentTrip.get(0).getProgress().equals("NotStarted")) {
                sendDriveTotoAdc();
                updateCurrentTripProgress("DriveToStart");
            } else if (currentTrip.get(0).getProgress().equals("AtStartLocation")) {
                // manage CustomerTrips that are AtStartLocation
                //TODO: @oemer add case for charging trip and execute charge operation
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
                currentTrip.remove(0); //dodo: check if it does really work or if position.intValue() needed
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
    ///////////////////////////////////////////////////////
    //  updates locatedagentlist of the area agent


    //  example of trike to trike communication
    void sendMessageToTrike(String receiverID, String comAct, String action, ArrayList<String> values){
        //message creation
        //ArrayList<String> values = new ArrayList<>();
        MessageContent messageContent = new MessageContent(action, values);
        Message testMessage = new Message("1", ""+agentID, receiverID, comAct, JadexModel.simulationtime,  messageContent);
        IAreaTrikeService service = messageToService(agent, testMessage);

        //calls trikeMessage methods of TrikeAgentService class
        service.trikeReceiveTrikeMessage(testMessage.serialize());
    }


    //  example of trike to trike communic ation
    public void testTrikeToTrikeService(String receiverID, String comAct, String action, ArrayList<String> values){
        //message creation
        //ArrayList<String> values = new ArrayList<>();
        MessageContent messageContent = new MessageContent(action, values);
        Message testMessage = new Message("1", agentID,""+receiverID, comAct, JadexModel.simulationtime,  messageContent);
        IAreaTrikeService service = messageToService(agent, testMessage);

        //calls trikeMessage methods of TrikeAgentService class
        service.trikeReceiveTrikeMessage(testMessage.serialize());
    }

    //
    public void sendMessage(String receiverID, String comAct, String action, ArrayList<String> values){
        //todo adapt for multiple area agents
        //todo use unique ids
        //message creation

        MessageContent messageContent = new MessageContent(action, values);
        Message testMessage = new Message("1", ""+agentID, receiverID, comAct, JadexModel.simulationtime,  messageContent);
        IAreaTrikeService service = messageToService(agent, testMessage);

        //calls trikeMessage methods of TrikeAgentService class
        service.receiveMessage(testMessage.serialize());

    }


    void sendAreaAgentUpdate(String action){
        //message creation
        //todo: decide if register or update here
        ArrayList<String> values = new ArrayList<>();

        values.add(Double.toString(agentLocation.getX()));
        values.add(Double.toString(agentLocation.getY()));
        MessageContent messageContent = new MessageContent(action, values);
        Message testMessage = new Message("0", agentID,"area:0", "inform", JadexModel.simulationtime,  messageContent);

        //query assigning
        IAreaTrikeService service = messageToService(agent, testMessage);
        //calls updateAreaAgent of AreaAgentService class
        service.areaReceiveUpdate(testMessage.serialize());

    }

    public void test(){
        ArrayList<String> values = new ArrayList<>();
        sendMessage("area:0", "request", "callForNeighbours", values);
        //sendMessage("area:0", "inform", "update");

    }

    //  if isModified=true, then testTrikeToTrikeService worked properly
    public void testModify(){
        isModified = true;
        System.out.println("isModified: " + isModified);


    }


}








