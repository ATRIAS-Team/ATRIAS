/* TrikeAgnet.java
 * Version: v0.12 (03.03.2024)
 * changelog: terminate tripList
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
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.IMappingAgentsService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.WritingIDService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.INotifyService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.TrikeAgentReceiveService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.TrikeAgentSendService;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.TrikeMain;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.GreedyScheduler;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.GreedyScheduler.enums.Strategy;
import io.github.agentsoz.ees.jadexextension.masterthesis.scheduler.SchedulerUtils;
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
import jadex.rules.parser.conditions.javagrammar.ArrayAccess;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private static final Logger log = LoggerFactory.getLogger(TrikeAgent.class);
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
    //vorher private wegen Battery moveToTarget public gemacht -oemer
    public Location agentLocation; // position of the agent TODO: init location with start location from matsim
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

    @Belief
    public List<String> chargingTripIDList = new ArrayList<>();

    @Belief    //contains the current Trip
    public List<Trip> currentTrip = new ArrayList<>();

    @Belief
    private List<ActionContent> SimActionList = new ArrayList<>();

    @Belief
    private List<PerceptContent> SimPerceptList = new ArrayList<>();

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
    public List<Double> estimateBatteryAfterTIP = Arrays.asList(trikeBattery.getMyChargestate());









    /**
     * Every DecisionTask with a score equal or higher will be commited
     * todo: should be initialized from a configFile()
     */


    public boolean informSimInput = false;

    public String currentSimInputBroker;
    private SimActuator SimActuator;

    //test variables
    //test variables
    public boolean isModified = false;

    @Belief
    public String chargingTripAvailable = "0"; //Battery -oemer

    public Double commitThreshold = 50.0;
    Double DRIVING_SPEED = 6.0;
    Boolean CNP_ACTIVE = Boolean.parseBoolean(System.getenv("cnp"));
    Double THETA = Double.parseDouble(System.getenv("theta")); //allowed waiting time for customers.
    int ITERS = Integer.parseInt(System.getenv("iters")); //allowed waiting time for customers.
    int RUNS = Integer.parseInt(System.getenv("runs")); //allowed waiting time for customers.
    Boolean ALLOW_CUSTOMER_MISS = true; // customer will miss when delay > THETA
    Double DISTANCE_FACTOR = 3.0; //multiply with distance estimations for energyconsumption, to avoid an underestimation

    Double CHARGING_THRESHOLD = 0.4; // Threshold to determine when a ChargingTrip should be generated

    //public List<Location> CHARGING_STATION_LIST = new ArrayList<>();

    public List<Location> CHARGING_STATION_LIST = Arrays.asList(new Location("", 476142.33,5553197.70), new Location("", 476172.65,5552839.64),new Location("", 476482.10,5552799.06),new Location("", 476659.13,5553054.12),new Location("", 476787.10,5552696.95),new Location("", 476689.45,5552473.11),new Location("", 476405.41,5552489.17),new Location("", 476100.86,5552372.79));



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

        // bdiFeature.dispatchTopLevelGoal(new AchieveMoveTo()); //Battery -oemer


        //sendMessage("area:0", "request", "");

        //csvLogger csvLogger;// = new csvLogger(agentID);
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

    @Belief
    public boolean erzeugt = false;


    @Goal(recur = true, recurdelay = 300)
    class testGoal {
        @GoalCreationCondition(factchanged = "estimateBatteryAfterTIP") //
        public testGoal() {
        }

        //@GoalTargetCondition
        //boolean senttoMATSIM() {
        //    return (erzeugt == true);
        //}
    }

    @Plan(trigger = @Trigger(goalfinisheds = testGoal.class))
    public void testPlan() {
        erzeugt = true;
    }

//    @Goal(recur = true, recurdelay = 100)
//    public class MaintainBatteryLoaded {
//        @GoalCreationCondition(factchanged = "estimateBatteryAfterTIP") //
//        public MaintainBatteryLoaded() {
//        }
//    }
//
//    @Plan(trigger = @Trigger(goals = MaintainBatteryLoaded.class))
//    public void NewChargingTrip() {
//        {
//            if (estimateBatteryAfterTIP.get(0) < CHARGING_THRESHOLD && chargingTripAvailable.equals("0")){
//                //estimateBatteryAfterTIP();
//                //Location LocationCh = new Location("", 476530.26535798033, 5552438.979076344);
//                //Location LocationCh = new Location("", 476224.26535798033, 5552487.979076344);
//                chargingTripCounter+=1;
//                String tripID = "CH";
//                tripID = tripID.concat(Integer.toString(chargingTripCounter));
//                Trip chargingTrip = new Trip(tripID, "ChargingTrip", getNextChargingStation(), "NotStarted");
//                tripList.add(chargingTrip);
//                tripIDList.add("1");
//                chargingTripAvailable = "1";
//            }
//        }
//    }

    public Location getNextChargingStation(){
        //CHARGING_STATION_LIST
        Location ChargingStation = CHARGING_STATION_LIST.get(0); //= new Location("", 476530.26535798033, 5552438.979076344);
        // last trip In pipe endlocation oder agentposition als ausgang nehmen
        Location startPosition;
        if (tripList.size() == 0 && currentTrip.size() == 0){
            startPosition = agentLocation;
        }
        else {
            startPosition = getLastTripInPipeline().getEndPosition();
        }
        Double lowestDistance = Double.MAX_VALUE;
        for (int i=0; i < CHARGING_STATION_LIST.size(); i++){
            Double compareDistance = Location.distanceBetween(startPosition, CHARGING_STATION_LIST.get(i));
            if (compareDistance<lowestDistance){
                lowestDistance = compareDistance;
                ChargingStation = CHARGING_STATION_LIST.get(i);
            }
        }


        //}

        return ChargingStation;
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
         * agentID
         * TODO: @Mariam Trike will commit a Trip here. write into firebase
         */

        //TODO: zwischenschritte (visio) fehlen utilliy usw.
        //tripIDList.add("1");
        //jobList.remove(0);
        //}



//        System.out.println("AgentID: " + agentID + " - EvaluateDecisionTask at simtime " + JadexModel.simulationtime);
        /**
         * todo: will replace solution above
         */
        test1();

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

    public void test1() {
        if (decisionTaskList.size()>0) {
            //System.out.println("EvaluateDecisionTask: new Version");
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
                if (changes == 0) {
                    finishedForNow = true;
                }
            }
        }
    }

    String mode = System.getenv("scheduler_mode");
    @Belief
    double timediff = JadexModel.simulationtime;
    long firstTime = System.currentTimeMillis();

    String lastScheduledTrip = "";
    public Integer selectNextAction(Integer position){
        Integer changes = 0;
        if (decisionTaskList.get(position).getStatus().equals("new")){
            /**
             *  Execute Utillity here > "commit"|"delegate"
             */
            Double ownScore = calculateUtility(decisionTaskList.get(position));
            //ownScore = 0.0; //todo: delete this line after the implementation of the cnp
            decisionTaskList.get(position).setUtillityScore(agentID, ownScore);
            if (ownScore < commitThreshold && CNP_ACTIVE){
                decisionTaskList.get(position).setStatus("delegate");
            }
            else{
                decisionTaskList.get(position).setStatus("commit");
                String timeStampBooked = new SimpleDateFormat("HH.mm.ss.ms").format(new java.util.Date());
                System.out.println("FINISHED Negotiation - JobID: " + decisionTaskList.get(position).getJobID() + " TimeStamp: "+ timeStampBooked);
            }

            changes += 1;
        }
        else if (decisionTaskList.get(position).getStatus().equals("commit")){
            /**
             *  create trip here
             */
            DecisionTask dTaToTrip = decisionTaskList.get(position);
            Trip newTrip = new Trip(
                    decisionTaskList.get(position),
                    dTaToTrip.getIDFromJob(),
                    "CustomerTrip",
                    dTaToTrip.getVATimeFromJob(),
                    dTaToTrip.getStartPositionFromJob(),
                    dTaToTrip.getEndPositionFromJob(),
                    "NotStarted",
                    dTaToTrip.getJob().getbookingTime());
            //TODO: create a unique tripID

            long start = System.currentTimeMillis();
            System.out.println("Agent " + agentID + " received Job " + dTaToTrip.getIDFromJob());

            tripList.add(newTrip);
            try {
//                if (!lastScheduledTrip.equals(newTrip.getTripID())) {
//                    lastScheduledTrip = newTrip.getTripID();

                    if (mode.equals("GREEDY")) {
                        GreedyScheduler greedyScheduler = new GreedyScheduler(
                                CHARGING_STATION_LIST,
                                trikeBattery.getMyChargestate(),
                                agentLocation,
                                getCurrentSimulationTimeAsDate(),
                                DRIVING_SPEED,
                                THETA,
                                agentID,
                                determineTimeTillEndpositionIsReach(),
                                currentTrip.size() > 0 ? currentTrip.get(0) : null
                        );
                        tripList = greedyScheduler.greedySchedule(tripList, Strategy.DRIVE_TO_CUSTOMER);
                    }

                    if (mode.equals("GENETIC")) {
                        TrikeAgent.GeneticScheduler geneticScheduler = new TrikeAgent.GeneticScheduler(initConfigurations());
                        tripList = geneticScheduler.start(tripList, ITERS, RUNS);
                    }

                    System.out.println("Agent " + agentID + " new trip list: " + tripList.stream().map(t -> t.getTripID()).collect(Collectors.toList()));
                    firstTime = System.currentTimeMillis();
                    timediff = JadexModel.simulationtime;
                    System.out.println("SCHEDULE RESULT " + tripList.stream().map(t -> t.getTripID()).collect(Collectors.toList()));
                    tripIDList.add("1");
//                }
            } catch (Exception e) {
                prepareLog(newTrip, "Exception", e.getCause().toString(), e.getMessage(), "");
                System.out.println("Exception caught from Scheduler");

                e.printStackTrace();
            }

            long end = System.currentTimeMillis();
            System.out.println("Slept about " + (end - start) + " milli sek");
            if (end - start > 1000) {
                System.out.println("Slept above 1000");
            }

//            estimateBatteryAfterTIP();

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
                    ArrayList<String> values = new ArrayList<>();
                    values.add(decisionTaskList.get(position).getJobID());
                    testTrikeToTrikeService(bidderID, tag, tag, values);
                    decisionTaskList.get(position).setStatus("waitingForConfirmations");
                }
                else if(tag.equals("RejectProposal")){
                    ArrayList<String> values = new ArrayList<>();
                    values.add(decisionTaskList.get(position).getJobID());
                    testTrikeToTrikeService(bidderID, tag, tag, values);
                }
                else if(tag.equals("AcceptSelf")){
                    //todo: selbst zusagen
                    decisionTaskList.get(position).setStatus("commit");
                    String timeStampBooked = new SimpleDateFormat("HH.mm.ss.ms").format(new java.util.Date());
                    System.out.println("FINISHED Negotiation - JobID: " + decisionTaskList.get(position).getJobID() + " TimeStamp: "+ timeStampBooked);


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
            Double ownScore = calculateUtility(decisionTaskList.get(position));
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
            // just a temporary solution for the paper
            // workaround for the not workign confirmation
            decisionTaskList.get(position).setStatus("delegated"); //todo: not shure if this is working corect
            FinishedDecisionTaskList.add(decisionTaskList.get(position)); //todo: not shure if this is working corect
            decisionTaskList.remove(position.intValue());//todo: not shure if this is working corect
        }
        else if (decisionTaskList.get(position).getStatus().equals("waitingForManager")){
            //todo: test timeout here
        }
        else if (decisionTaskList.get(position).getStatus().equals("committed")){
            System.out.println("should not exist: " + decisionTaskList.get(position).getStatus());
            //decisionTaskList.remove(0);
        }
        else {
            //System.out.println("invalid status: " + decisionTaskList.get(position).getStatus());
        }
        return changes;
    }

    private Double determineTimeTillEndpositionIsReach() {
        Trip trip = currentTrip.size() > 0 ? currentTrip.get(0) : null;
        if (trip == null) { return 0.0; }
        if (trip.getTripType().equals("ChargingTrip")) {
            return determineEndTimeOfChargingTrip(trip);
        }
        return determineEndTimeOfCustomerTrip(trip);
    }

    // @Tim
    private Double determineEndTimeOfChargingTrip(Trip trip) {
        Double time = 0.0;
        if (trip != null) {
            if (trip.getTripType().equals("ChargingTrip")) {
                if (trip.getProgress().equals("AtStartLocation")) {
                    if (trip.getChargingTime().equals(-1.0)) {
                        time = 0.0;
                    } else {
                        time += trip.getChargingTime();
                    }
                }
                if (trip.getProgress().equals("DriveToStart")) {
                    Double distance = Location.distanceBetween(agentLocation, trip.getStartPosition());
                    time += calculateTravelTime(distance);
                    if (trip.getChargingTime() == -1.0) {
                        time += MIN_CHARGING_TIME;
                    } else {
                        time += trip.getChargingTime();
                    }
                }
            }
        }
        return time;
    }

    // @Tim
    private Double determineEndTimeOfCustomerTrip(Trip trip) {
        Double distance = 0.0;
        if (trip != null) {
            String progress = trip.getProgress();
            switch (progress) {
                case "AtStartLocation":
                    distance += Location.distanceBetween(agentLocation, trip.getEndPosition());
                case "DriveToStart":
                    distance += Location.distanceBetween(agentLocation, trip.getStartPosition());
                    distance += Location.distanceBetween(trip.getStartPosition(), trip.getEndPosition());
                case "DriveToEnd":
                    distance += Location.distanceBetween(agentLocation, trip.getEndPosition());
            }
        }
        return calculateTravelTime(distance);
    }

    // @Tim
    private double calculateTravelTime(Double distance) {
        return ((distance / 1000) / DRIVING_SPEED) * 60 * 60;
    }

    @Goal(recur = true, recurdelay = 1000)
    public class HandleChargingTrips {
        @GoalCreationCondition(factadded = "chargingTripIDList") //
        public HandleChargingTrips() {
        }

        @GoalTargetCondition
        boolean finishCharging() {
            return currentTrip.size() == 0;
        }
    }

    // @New @Tim
    @Plan(trigger = @Trigger(goals = HandleChargingTrips.class))
    public void LoadBattery() {
        load();
    }

    Double CHARGING_THRESHHOLD = 0.05;
    // 3,5h bei 400 Watt
    // 12600 seconds for 0% - 100%
    Double COMPLETE_CHARGING_TIME = 12600.0;
    Double MIN_CHARGING_TIME = COMPLETE_CHARGING_TIME * CHARGING_THRESHHOLD;

    public TrikeAgent.Config initConfigurations() {
        // init configuration properties
        TrikeAgent.Config config = new TrikeAgent.Config();
        // ToDo: AgentLocation abhängig von laufenden Trip?
        config.setSimulationTime(getCurrentSimulationTimeAsDate());
        config.setAgentLocation(agentLocation);
        config.setBatteryLevel(trikeBattery.getMyChargestate());
        config.setDRIVING_SPEED(DRIVING_SPEED);
        config.setMIN_CHARGING_TIME(MIN_CHARGING_TIME);
        config.setMAX_CHARGING_TIME(COMPLETE_CHARGING_TIME);
        config.setCOMPLETE_CHARGING_TIME(COMPLETE_CHARGING_TIME);
        config.setTHETA(THETA);
        config.setChargingStations(CHARGING_STATION_LIST);
        config.setDISTANCE_FACTOR(3.0);
        config.setCHARGE_DECREASE(0.0001);
        config.setCHARGE_INCREASE(0.000079);
        return config;
    }

    public Double timeInSeconds(LocalDateTime time) {
        // Option 1: If the difference is greater than 300 seconds (5 minutes OR 300 seconds or 300000 millisec), then customer missed, -oemer

        double vaTimeSec = time.atZone(ZoneId.systemDefault()).toEpochSecond();
        //double vaTimeMilli = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        //double vaTimeSec2 = vaTimeMilli/1000;
        //double vaTimeMin = vaTimeMilli/1000/60;
        return vaTimeSec;
    }

    //test if there is at least one trip anywhere
    public Trip getLastTripInPipeline(){
        Trip lastTrip = null;
        if (tripList.size()>0){
            lastTrip = tripList.get(tripList.size()-1);
        }
        else if (tripList.size()==0 && currentTrip.size()>0){
            lastTrip = currentTrip.get(currentTrip.size()-1);

        }
        else{
            System.out.println("ERROR: getLastTripInPipeline() no trips available!");
        }
        return lastTrip;
    }



    /** Utillity Function
     * should be switchable between a regular and a learning attempt
     * todo: assumption bookingtime = vatime
     * todo: fortschritt von currenttrip berücksichtigen!
     * @return
     */
    Double calculateUtility(DecisionTask newTask){
        Double utillityScore = 0.0;

        try {
            // used for new fifo scheduling
            boolean noChargingTripInTripList = tripList.stream().filter(t -> t.getTripType().equals("ChargingTrip")).collect(Collectors.toList()).size() == 0;
            boolean noCurrentChargingTrip = currentTrip.size() == 0 || (currentTrip.size() > 0 && !currentTrip.get(0).getTripType().equals("ChargingTrip"));

            if (true) {

                newTask.getStartPositionFromJob();
                newTask.getEndPositionFromJob();
                newTask.getVATimeFromJob();

                Double a = 0.75; //1.0 / 3.0;
                Double b = 0.125; //1.0 / 3.0;
                Double c = 0.125; //1.0 / 3.0;

                Double uPunctuality = null;
                Double uBattery = null;
                Double uDistance = null;

                //###########################################################
                // punctuallity
                // arrival delay to arrive at the start position when started from the agentLocation
                //todo: number of comitted trips TIP über alle berechnen erwartete ankunft bei aktuellem bestimmen, dann delay bewerten ohne ladefahrten
                Double vaTimeFirstTrip = 0.0;
                //when there is no Trip before calculate the delay when started at the Agent Location
                if (currentTrip.size() == 0 && tripList.size() == 0) {
                    //agentLocation
                    Double distanceToStart = Location.distanceBetween(agentLocation, newTask.getStartPositionFromJob());
                    //Double vATimeNewTask = timeInSeconds(newTask.getVATimeFromJob());
                    Double timeToNewTask = ((distanceToStart/1000) / DRIVING_SPEED)*60*60; //in this case equals the delay as vatiem is bookingtime
                    // transforms the delay in seconds into as score beween 0 and 100 based of the max allowed delay of 900s
                    if (timeToNewTask<THETA){
                        uPunctuality = 100.0;
                    }
                    else if (THETA<= timeToNewTask && timeToNewTask<=2*THETA){
                        // @Tim adjusted
                        uPunctuality = 100.0 - (100.0 * ((timeToNewTask - THETA)/THETA));
                    }
                    else{
                        uPunctuality = 0.0;
                    }

                    //uPunctuality = Math.min(100.0, (100.0 - (((Math.min(THETA, timeToNewTask) - 0.0) / (THETA - 0.0)) * 100.0)));
                }
                else {
                    Double totalDistance_TIP = 0.0;
                    //todo: get va time of first job here or in an else case
                    if (currentTrip.size() == 1) { //distances driven from the agent location to the start of the current trip and to its end
                        totalDistance_TIP += Location.distanceBetween(agentLocation, currentTrip.get(0).getStartPosition());
                        if (currentTrip.get(0).getTripType().equals("CustomerTrip")) { //only drive to the end when it is a customerTrip
                            vaTimeFirstTrip = timeInSeconds(currentTrip.get(0).getVATime());
                            totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getStartPosition(), currentTrip.get(0).getEndPosition());
                        }
                    }
                    //  distance driven at tripList
                    if (tripList.size() > 0) {
                        if (currentTrip.size() > 0) { //journey to the first entry in the tripList from a currentTrip
                            if (currentTrip.get(0).getTripType().equals("CustomerTrip")) {
                                totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getEndPosition(), tripList.get(0).getStartPosition());
                            } else { // trips with only a start position
                                totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getStartPosition(), tripList.get(0).getStartPosition());
                            }
                        } else { //journey to the first entry in the tripList from the agentLocation
                            if (tripList.get(0).getTripType().equals("CustomerTrip")) {
                                vaTimeFirstTrip = timeInSeconds(tripList.get(0).getVATime()); //fist VATime when there was no CurrentTrip
                            }
                            totalDistance_TIP += Location.distanceBetween(agentLocation, tripList.get(0).getStartPosition());
                        }
                        // distance driven at TripList.get(0)
                        if (tripList.get(0).getTripType().equals("CustomerTrip")) {
                            totalDistance_TIP += Location.distanceBetween(tripList.get(0).getStartPosition(), tripList.get(0).getEndPosition());
                        }
                    } else {
                        // do nothing as all other Trips with only a startPosition will not contain any other movements;
                    }

                    // interates through all other Trips inside TripList
                    if (tripList.size() > 1){ //added to avoid crashes
                        for (int i = 1; i < tripList.size(); i++) {
                            if (tripList.get(i - 1).getTripType().equals("CustomerTrip")) {
                                totalDistance_TIP += Location.distanceBetween(tripList.get(i - 1).getEndPosition(), tripList.get(i).getStartPosition()); //triplist or currenttrip
                            } else { // Trips with only a startPosition
                                totalDistance_TIP += Location.distanceBetween(tripList.get(i - 1).getStartPosition(), tripList.get(i).getStartPosition()); //corrected! was to EndPosition before!
                            }
                            if (tripList.get(i).getTripType().equals("CustomerTrip")) { //triplist or currenttrip
                                totalDistance_TIP += Location.distanceBetween(tripList.get(i).getStartPosition(), tripList.get(i).getEndPosition());
                            }
                        }
                    }
                    //todo: drives to the start of the job that has to be evaluated
                    if (getLastTripInPipeline().getTripType().equals("CustomerTrip")) {
                        totalDistance_TIP += Location.distanceBetween(getLastTripInPipeline().getEndPosition(), newTask.getStartPositionFromJob());
                    }
                    else {
                        totalDistance_TIP += Location.distanceBetween(getLastTripInPipeline().getStartPosition(), newTask.getStartPositionFromJob());
                    }


                    Double vATimeNewTask = timeInSeconds(newTask.getVATimeFromJob());
                    Double timeToNewTask = ((totalDistance_TIP/1000) / DRIVING_SPEED)*60*60;
                    Double arrivalAtNewtask = vaTimeFirstTrip + timeToNewTask;

                    Double delayArrvialNewTask = Math.max((arrivalAtNewtask - vATimeNewTask), timeToNewTask);
                    System.out.println("vATimeNewTask: " + vATimeNewTask );
                    System.out.println("timeToNewTask: " + timeToNewTask );
                    System.out.println("arrivalAtNewtask: " + arrivalAtNewtask );
                    System.out.println("delayArrvialNewTask: " + delayArrvialNewTask );

                    if (delayArrvialNewTask<THETA){
                        uPunctuality = 100.0;
                    }
                    else if (THETA<= delayArrvialNewTask && delayArrvialNewTask <=2*THETA){
                        // @Tim adjusted
                        uPunctuality = 100.0 - (100.0 * ((delayArrvialNewTask - THETA)/THETA));
                    }
                    else{
                        uPunctuality = 0.0;
                    }

                    //uPunctuality = Math.min(100.0, (100.0 - (((Math.min(THETA, delayArrvialNewTask) - 0.0) / (THETA - 0.0)) * 100.0)));



                }
                //when there a trips iterate through all, starting at the va time of the first trip estimate your delay when arriving at the start location of
                // the Job that has to be evaluated


                //###########################################################
                // Battery
                //todo: battery from Ömer needed
                // differ between trips with and without customer???
                Double currentBatteryLevel = trikeBattery.getMyChargestate(); //todo: use real battery
                Double estBatteryLevelAfter_TIP = trikeBattery.getMyChargestate();
                Double estDistance = 0.0;
                Double estEnergyConsumption = 0.0;
                Double estEnergyConsumption_TIP = 0.0;
                Double totalDistance_TIP = 0.0;
                Double negativeInfinity = Double.NEGATIVE_INFINITY;
                Double bFactor = null;
                //todo ennergieverbrauch für zu evuluierenden job bestimmen

                //calculation of the estimatedEnergyConsumtion (of formertrips)


                if (currentTrip.size() == 1) { //battery relavant distance driven at currentTrip
                    //todo: fortschritt von currenttrip berücksichtigen
                    totalDistance_TIP += Location.distanceBetween(agentLocation, currentTrip.get(0).getStartPosition());
                    if (currentTrip.get(0).getTripType().equals("CustomerTrip")) { //only drive to the end when it is a customerTrip
                        totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getStartPosition(), currentTrip.get(0).getEndPosition());
                    }
                    if (currentTrip.get(0).getTripType().equals("ChargingTrip")) {
                        totalDistance_TIP = 0.0; //reset the distance until now because only the distance after a chargingTrip influences the battery
                    }
                }
                // battery relavant distance driven at tripList
                if (tripList.size() > 0) {
                    if (currentTrip.size() > 0) { //journey to the first entry in the tripList from a currentTrip
                        if (currentTrip.get(0).getTripType().equals("CustomerTrip")) {
                            totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getEndPosition(), tripList.get(0).getStartPosition());
                        } else { // trips with only a start position
                            totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getStartPosition(), tripList.get(0).getStartPosition());
                        }
                    } else { //journey to the first entry in the tripList from the agentLocation
                        totalDistance_TIP += Location.distanceBetween(agentLocation, tripList.get(0).getStartPosition());
                    }
                    // distance driven at TripList.get(0)
                    if (tripList.get(0).getTripType().equals("CustomerTrip")) {
                        totalDistance_TIP += Location.distanceBetween(tripList.get(0).getStartPosition(), tripList.get(0).getEndPosition());
                    }
                    if (tripList.get(0).getTripType().equals("ChargingTrip")) {
                        totalDistance_TIP = 0.0;
                    } else {
                        // do nothing as all other Trips with only a startPosition will not contain any other movements;
                    }


                    //todo: fahrt zum nächjsten start fehlt +-1 bei i???
                    // interates through all other Trips inside TripList
                    if (tripList.size() > 1){ //added to avoid crashes
                        for (int i = 1; i < tripList.size(); i++) {
                            if (tripList.get(i - 1).getTripType().equals("CustomerTrip")) {
                                totalDistance_TIP += Location.distanceBetween(tripList.get(i - 1).getEndPosition(), tripList.get(i).getStartPosition()); //triplist or currenttrip
                            } else { // Trips with only a startPosition
                                totalDistance_TIP += Location.distanceBetween(tripList.get(i - 1).getStartPosition(), tripList.get(i).getStartPosition()); //corrected! was to EndPosition before!
                            }
                            if (tripList.get(i).getTripType().equals("CustomerTrip")) { //triplist or currenttrip
                                totalDistance_TIP += Location.distanceBetween(tripList.get(i).getStartPosition(), tripList.get(i).getEndPosition());
                            }
                        }
                    }
                }
                //todo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! RICHTIGE WERTE ZUGREIFEN
                estEnergyConsumption_TIP = trikeBattery.SimulateDischarge(totalDistance_TIP * DISTANCE_FACTOR);//*2 because it would be critical to underestimate the distance
                estBatteryLevelAfter_TIP = currentBatteryLevel - estEnergyConsumption_TIP;

                //calculate teh estimated energy consumption of the new job


                //Distance from the agent location
                if (currentTrip.size() == 0 && tripList.size() == 0){
                    estDistance += Location.distanceBetween(agentLocation, newTask.getStartPositionFromJob());
                }
                //Distance from the Last Trip in Pipe
                else{
                    if (getLastTripInPipeline().getTripType().equals("CustomerTrip")){
                        estDistance += Location.distanceBetween(getLastTripInPipeline().getEndPosition(), newTask.getStartPositionFromJob());
                    }
                    else{
                        estDistance += Location.distanceBetween(getLastTripInPipeline().getStartPosition(), newTask.getStartPositionFromJob());
                    }
                }
                estDistance += Location.distanceBetween(newTask.getStartPositionFromJob(), newTask.getEndPositionFromJob());

                estEnergyConsumption = trikeBattery.SimulateDischarge(estDistance * DISTANCE_FACTOR);

                Double estBatterylevelTotal = estBatteryLevelAfter_TIP - estEnergyConsumption;


                //###########################################################
                // calculation of uBattery
                if (estBatterylevelTotal < 0.0) { //todo: estEnergyConsumption FEHLT!
                    uBattery = negativeInfinity;
                } else {
                    if (estBatterylevelTotal > 0.8) {
                        bFactor = 1.0;
                    } else if (estBatterylevelTotal >= 0.3) {
                        bFactor = 0.75;
                    } else if (estBatterylevelTotal < 0.3) {
                        bFactor = 0.1;
                    }
                    // ???? batteryLevelAfterTrips or 100?
                    uBattery = (bFactor * estBatterylevelTotal) * 100;

                }
                //###########################################################
                //Distance
                Double dmax = 3000.0;
                Double distanceToStart;

                if (tripList.size() == 0 && currentTrip.size() == 0) {
                    distanceToStart = Location.distanceBetween(agentLocation, newTask.getStartPositionFromJob());
                } else {
                    if (getLastTripInPipeline().getTripType().equals("CustomerTrip")) {
                        distanceToStart = Location.distanceBetween(getLastTripInPipeline().getEndPosition(), newTask.getStartPositionFromJob());
                    } else {
                        distanceToStart = Location.distanceBetween(getLastTripInPipeline().getStartPosition(), newTask.getStartPositionFromJob());
                    }
                }
                uDistance = Math.max(0, (100-distanceToStart / dmax));
                //uDistance = Math.max(0, Math.min(100, (100.0 - ((distanceToStart / dmax) * 100.0))));


                //###########################################################


                // calculate the total score

                utillityScore = Math.max(0.0, (a * uPunctuality + b * uBattery + c * uDistance));
            }
        } catch (Exception e) {
            System.out.println("Caught exception");
            e.printStackTrace();
        }

        System.out.println("agentID: " + agentID + "utillity: " + utillityScore);
        return utillityScore;
    }



    //estimates the batteryLevel after all Trips. Calculations a based on aerial line x1.5
    public Double estimateBatteryAfterTIP(){
        Double batteryChargeAfterTIP = trikeBattery.getMyChargestate();
        Double totalDistance_TIP = 0.0;
        if (currentTrip.size() == 1) { //battery relavant distance driven at currentTrip
            //todo: fortschritt von currenttrip berücksichtigen
            totalDistance_TIP += Location.distanceBetween(agentLocation, currentTrip.get(0).getStartPosition());
            if (currentTrip.get(0).getTripType().equals("CustomerTrip")) { //only drive to the end when it is a customerTrip
                totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getStartPosition(), currentTrip.get(0).getEndPosition());
            }
            if (currentTrip.get(0).getTripType().equals("ChargingTrip")) {
                totalDistance_TIP = 0.0; //reset the distance until now because only the distance after a chargingTrip influences the battery
            }
        }
        // battery relavant distance driven at tripList
        if (tripList.size() > 0) {
            if (currentTrip.size() > 0) { //journey to the first entry in the tripList from a currentTrip
                if (currentTrip.get(0).getTripType().equals("CustomerTrip")) {
                    totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getEndPosition(), tripList.get(0).getStartPosition());
                } else { // trips with only a start position
                    totalDistance_TIP += Location.distanceBetween(currentTrip.get(0).getStartPosition(), tripList.get(0).getStartPosition());
                }
            } else { //journey to the first entry in the tripList from the agentLocation
                totalDistance_TIP += Location.distanceBetween(agentLocation, tripList.get(0).getStartPosition());
            }
            // distance driven at TripList.get(0)
            if (tripList.get(0).getTripType().equals("CustomerTrip")) {
                totalDistance_TIP += Location.distanceBetween(tripList.get(0).getStartPosition(), tripList.get(0).getEndPosition());
            }
            if (tripList.get(0).getTripType().equals("ChargingTrip")) {
                totalDistance_TIP = 0.0;
            } else {
                // do nothing as all other Trips with only a startPosition will not contain any other movements;
            }

            //todo: fahrt zum nächjsten start fehlt +-1 bei i???
            // interates through all other Trips inside TripList
            if (tripList.size() > 1){ //added to avoid crashes
                for (int i = 1; i < tripList.size(); i++) {
                    if (tripList.get(i - 1).getTripType().equals("CustomerTrip")) {
                        totalDistance_TIP += Location.distanceBetween(tripList.get(i - 1).getEndPosition(), tripList.get(i).getStartPosition()); //triplist or currenttrip
                    } else { // Trips with only a startPosition
                        totalDistance_TIP += Location.distanceBetween(tripList.get(i - 1).getStartPosition(), tripList.get(i).getStartPosition()); //corrected! was to EndPosition before!
                    }
                    if (tripList.get(i).getTripType().equals("CustomerTrip")) {
                        totalDistance_TIP += Location.distanceBetween(tripList.get(i).getStartPosition(), tripList.get(i).getEndPosition());
                    }
                }
            }
        }
        Double estEnergyConsumption_TIP = trikeBattery.SimulateDischarge((totalDistance_TIP * DISTANCE_FACTOR));
        batteryChargeAfterTIP = batteryChargeAfterTIP - estEnergyConsumption_TIP;

        estimateBatteryAfterTIP.set(0, batteryChargeAfterTIP);
        return batteryChargeAfterTIP;
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
            if(currentTrip.size() == 0){
                ExecuteTrips();
                System.out.println("Set active to false of agentID " + agentID);
                activestatus = false;
            }

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

                    if (agentID.equals("0")){
                        agentLocation = new Location("", 476693.70,5553399.74);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("1")){
                        agentLocation = new Location("", 476411.90963429067, 5552419.709277404);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("2")){
                        agentLocation = new Location("", 476593.32115363394, 5553317.19412722);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("3")){
                        agentLocation = new Location("", 476438.79189037136, 5552124.30651799);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("4")){
                        agentLocation = new Location("", 476500.76932398824, 5552798.971484745);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("5")){
                        agentLocation = new Location("", 476538.9427888916, 5553324.827033389);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("6")){
                        agentLocation = new Location("", 476619.6161561999, 5552925.794018047);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("7")){
                        agentLocation = new Location("", 476606.7547, 5552369.86);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("8")){
                        agentLocation = new Location("", 476072.454, 5552737.847);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("9")){
                        agentLocation = new Location("", 476183.6117, 5552372.253);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("10")){
                        agentLocation = new Location("", 476897.6661, 5552908.159);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("11")){
                        agentLocation = new Location("", 476117.4177, 5552983.103);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("12")){
                        agentLocation = new Location("", 476206.3887, 5553181.409);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("13")){
                        agentLocation = new Location("", 476721.5633, 5553163.268);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("14")){
                        agentLocation = new Location("", 476504.8636, 5553075.586);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("15")){
                        agentLocation = new Location("", 476006.3971, 5552874.791);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("16")){
                        agentLocation = new Location("", 476896.9427, 5552809.207);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("17")){
                        agentLocation = new Location("", 476576.8201, 5552875.558);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("18")){
                        agentLocation = new Location("", 476659.5715, 5552264.147);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("19")){
                        agentLocation = new Location("", 476140.0289, 5552869.111);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("20")){
                        agentLocation = new Location("", 476459.8442, 5552766.704);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("21")){
                        agentLocation = new Location("", 476076.6989, 5552496.082);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("22")){
                        agentLocation = new Location("", 475950.8911, 5553012.783);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("23")){
                        agentLocation = new Location("", 476269.0866, 5553041.63);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("24")){
                        agentLocation = new Location("", 476574.3644, 5552706.306);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("25")){
                        agentLocation = new Location("", 476229.5433, 5553032.162);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("26")){
                        agentLocation = new Location("", 476182.5081, 5552736.953);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("27")){
                        agentLocation = new Location("", 476718.9972, 5552412.517);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("28")){
                        agentLocation = new Location("", 476088.6448, 5552928.079);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("29")){
                        agentLocation = new Location("", 476285.4132, 5552547.373);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("30")){
                        agentLocation = new Location("", 476257.686, 5553038.9);
                        sendAreaAgentUpdate("register");
                    }
                    else if (agentID.equals("31")){
                        agentLocation = new Location("", 476276.6184, 5553043.434);
                        sendAreaAgentUpdate("register");
                    }
                    //**/
                    /**
                     * TODO: @Mariam initiale anmeldung an firebase hier
                     */
                    //csvLogger csvLogger = new csvLogger(agentID);
                    csvLogger csvLogger = new csvLogger(agentID, CNP_ACTIVE, THETA, ALLOW_CUSTOMER_MISS, CHARGING_THRESHOLD, commitThreshold, DISTANCE_FACTOR);

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
                        System.out.println("AgentID: " + agentID + actionContent.getParameters()[0]);
                        //System.out.println("AgentID: " + agentID + actionContent.getParameters()[1]);
                        //System.out.println("AgentID: " + agentID + actionContent.getParameters()[2]);
                        //System.out.println("AgentID: " + agentID + actionContent.getParameters()[3]);
                        //System.out.println("AgentID: " + agentID + actionContent.getParameters()[4]);
                        //System.out.println("AgentID: " + agentID + actionContent.getParameters()[5]);
                        //System.out.println("AgentID: " + agentID + actionContent.getParameters()[6]);
                        //System.out.println("AgentID: " + agentID + actionContent.getParameters()[7]);


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

//    List<String> logCheck = new ArrayList<>();

    void prepareLog(Trip trip, String batteryBefore, String batteryAfter, String arrivedAtLocation, String distance){
        String tripID = trip.getTripID();
        String tripType = trip.getTripType();
        String driveOperationNumber = "1";
        String origin = "";
        if (trip.getProgress().equals("AtEndLocation")){
            driveOperationNumber = "2";
        }
        String arrivalTime = "0.0"; //when it was not a CustomerTrip
        if (trip.getTripType().equals("CustomerTrip")){
            arrivalTime = Double.toString(ArrivalTime(trip.getVATime()));
            origin = "trike:" + trip.getDecisionTaskD().getOrigin();
        }
        csvLogger.addLog(agentID, CNP_ACTIVE, THETA, ALLOW_CUSTOMER_MISS, CHARGING_THRESHOLD, commitThreshold, DISTANCE_FACTOR, "trike:" + agentID, tripID, driveOperationNumber, tripType, batteryBefore, batteryAfter, arrivedAtLocation, distance, arrivalTime, origin);
    }



    // After a succefull action in MATSIm: Updates the progreess of the current Trip and the Agent location
    //todo: better get the location from MATSim
    void updateBeliefAfterAction() {
        Trip CurrentTripUpdate = currentTrip.get(0);
        double metersDriven = Double.parseDouble((String) SimActionList.get(0).getParameters()[1]);
        //double metersDriven = 100.0;
        //Transport ohne Kunde
        String arrivedAtLocation = "true";

        if (CurrentTripUpdate.getProgress().equals("DriveToStart")) {
            updateCurrentTripProgress("AtStartLocation");
            agentLocation = CurrentTripUpdate.getStartPosition();
            String batteryBefore = Double.toString(trikeBattery.getMyChargestate()); //todo: vorher schieben
            trikeBattery.discharge(metersDriven, 0);
            String batteryAfter = Double.toString(trikeBattery.getMyChargestate());
            //String arrivedAtLocation = "true";
            if (trikeBattery.getMyChargestate() < 0.0){
                arrivedAtLocation = "false";
                updateCurrentTripProgress("Failed");

            }
            String distance = Double.toString(metersDriven);
//            logCheck.add(CurrentTripUpdate.getTripID());
            prepareLog(CurrentTripUpdate, batteryBefore, batteryAfter, arrivedAtLocation, distance);

            if (arrivedAtLocation.equals("false")){
                currentTrip.remove(0);
                terminateTripList();
            }
        }


        //Transport mit Kunde
        if (CurrentTripUpdate.getProgress().equals("DriveToEnd")){
            updateCurrentTripProgress("AtEndLocation");
            agentLocation = CurrentTripUpdate.getEndPosition();
            String batteryBefore = Double.toString(trikeBattery.getMyChargestate()); //todo: vorher schieben
            trikeBattery.discharge(metersDriven, 1);
            String batteryAfter = Double.toString(trikeBattery.getMyChargestate());
            //String arrivedAtLocation = "true";
            if (trikeBattery.getMyChargestate() < 0.0){
                arrivedAtLocation = "false";
                updateCurrentTripProgress("Failed");
            }
            String distance = Double.toString(metersDriven);
//            logCheck.add(CurrentTripUpdate.getTripID());
            prepareLog(CurrentTripUpdate, batteryBefore, batteryAfter, arrivedAtLocation, distance);

            if (arrivedAtLocation.equals("false")){
                currentTrip.remove(0);
                terminateTripList();
            }
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

    //remove all Trips from tripList and currenTrip and write them with the logger
    public void terminateTripList(){
        if (currentTrip.size() > 1){
//            logCheck.add(currentTrip.get(0).getTripID());
            prepareLog(currentTrip.get(0), "0.0", "0.0", "false", "0.0");
            currentTrip.get(0).setProgress("Failed");
            currentTrip.remove(0);



        }
        if (tripList.size() > 0){
            while (tripList.size() > 0) {
//                logCheck.add(tripList.get(0).getTripID());
                prepareLog(tripList.get(0), "0.0", "0.0", "false", "0.0");
                tripList.get(0).setProgress("Failed");
                tripList.remove(0);
            }
        }
        trikeBattery.loadBattery();
        chargingTripAvailable = "0";

        System.out.println("AgentID: " + agentID + "ALL TRIPS TERMINATED");
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
        System.out.println("Test if new currentTrip can be created");
        if(currentTrip.size()==0 && tripList.size()>0 ){
            System.out.println("no currentTrip available");
            System.out.println("getting nextTrip from TripList");
            currentTrip.add(tripList.get(0));
            tripList.remove(0);

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
        if (currentTrip.size() > 0){
            System.out.println("\n currentTripStatus:");
            System.out.println("AgentID: " + agentID + " currentTripID: " + currentTrip.get(0).getTripID());
            System.out.println("AgentID: " + agentID + " currentTripType: " + currentTrip.get(0).getTripType());
            System.out.println("AgentID: " + agentID + " currentVaTime: " + currentTrip.get(0).getVATime());
            System.out.println("AgentID: " + agentID + " currentStartPosition: " + currentTrip.get(0).getStartPosition());
            System.out.println("AgentID: " + agentID + " currentEndPosition: " +currentTrip.get(0).getEndPosition());
            System.out.println("AgentID: " + agentID + " currentProgress: " + currentTrip.get(0).getProgress());
        }

    }

    void Status(){
        //if (agentID.equals("0")){
        System.out.println("AgentID: " + agentID + " activestatus: " + activestatus);
        System.out.println("AgentID: " + agentID + " currentTrip.size: " + currentTrip.size());
        System.out.println("AgentID: " + agentID + " tripList.size: " + tripList.size() + " -> " + tripList.stream().map(t -> t.getTripID()).collect(Collectors.toList()));
        System.out.println("AgentID: " + agentID + " decisionTaskList.size: " + decisionTaskList.size());
        System.out.println("AgentID: " + agentID + " SimActionList: " + SimActionList.size());
        System.out.println("AgentID: " + agentID + " SimPerceptList: " + SimPerceptList.size());
        //for (ActionContent actionContent : SimActionList) {
        //System.out.println("AgentID: " + agentID + " actionType: "+ actionContent.getAction_type() + " actionState: " + actionContent.getState());
        //}
        for (int i=0; i<decisionTaskList.size(); i++){
            System.out.println("AgentID: " + agentID + " decisionTaskList status: " + decisionTaskList.get(i).getStatus() + " - " + decisionTaskList.get(i).getJob().getID() + " - Origin: " + decisionTaskList.get(i).getOrigin());
        }


        currentTripStatus();
        //}
    }

    // why public static?

    //TODO: @oemer check if it will work correctly

    //todo: access simulation time and determine if the customer has already left
    // SimZeit abfragen, gewünschte VATime vergleichen und wenn mehr als 5 min, dann missed, oder würfel, je länger desto wahrscheinlicher
    // statt estimated Duration, echte Duration verwenden, die Berechnung soll erfolgen, wenn der Trike Agent beim Kunden ankommt. --> Wichtig für den Reward
    // drive to customer, check simulation time and start time(vaTime) and define delta
    // 1. option: if delta > 5 minutes then --> missed else success
    // 2. option: if delta > 10 probability higher that it is missed


//    @Goal(recur = true, recurdelay = 100)
//    public class HandleChargingTrips {
//    }

    // @New @Tim
//    @Plan(trigger = @Trigger(goals = TrikeAgentNew.HandleChargingTrips.class))
//    public void LoadBattery() {
//        load();
//    }

    private void finishChargingTripAndLog(Trip trip) {
        System.out.println("FINISHED CHARGING TRIP " + trip.getTripID() + " FROM AGENT "+ agentID + " ChargingTime: " + (JadexModel.simulationtime - startTimeCharging));
        trip.setChargingTime(0.0);
        updateCurrentTripProgress("Finished");
        try {
            prepareLog(
                    trip,
                    previousBatteryLevel == null ? "-" : previousBatteryLevel.toString(),
                    String.valueOf(trikeBattery.getMyChargestate()),
                    "true",
                    "0.0"
            );
            previousBatteryLevel = null;
            startOfCharging = false;
        } catch (Exception e) {
            System.out.println("FinishAndCharging Exception");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        ExecuteTrips();
    }

    private LocalDateTime getCurrentSimulationTimeAsDate() {
        long secondsAfterMidnight = (long) JadexModel.simulationtime;
        // ToDo: Use correct date otherwise calculations in greedy scheduler are wrong
        return LocalDateTime.of(
                LocalDate.of(2016, 7, 31),
                LocalTime.of(
                        (int) secondsAfterMidnight / 3600,
                        (int) (secondsAfterMidnight % 3600) / 60,
                        (int) secondsAfterMidnight % 60
                )
        );
    }


    public Double ArrivalTime(LocalDateTime vATime){
        long offset = (vATime
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

        long vaTimeMilli = vATime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        double curr = (JadexModel.simulationtime) * 1000;
        double diff = (curr - (vaTimeMilli - offset))/1000 ; //in seconds
        //Double arrivalTime;
        return diff;
    };



    public boolean customerMiss(Trip trip) {
        long offset = (trip.getVATime()
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());



        // Option 1: If the difference is greater than 300 seconds (5 minutes OR 300 seconds or 300000 millisec), then customer missed, -oemer
        boolean isMissed = false;
        long vaTimeMilli = trip.getVATime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        //Double vaTimeSec = timeInSeconds(currentTrip.get(0).getVATime());
        double curr = (JadexModel.simulationtime) * 1000;
        double diff = curr - (vaTimeMilli - offset) ;
        if (diff > (THETA*1000) && ALLOW_CUSTOMER_MISS){
            return isMissed = true;
        }
        return isMissed;
    }
    /** old version
     public boolean customerMiss(Trip trip) {
     // Option 1: If the difference is greater than 300 seconds (5 minutes OR 300 seconds or 300000 millisec), then customer missed, -oemer
     boolean isMissed = false;
     double vaTimeMilli = trip.getVATime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
     double curr = JadexModel.simulationtime;
     double diff = (vaTimeMilli - (curr * 30000000));
     if (diff >= 1.6000000E13){
     return isMissed = true;
     }
     return isMissed;
     }
     **/

    public boolean customerMissProb(Trip trip) {
        // Option 2: If the difference is greater than 600 seconds (10 minutes OR 600 seconds or 600000 millisec), then customer probably missed, -oemer
        boolean isMissed = false;
        double vaTimeMilli = trip.getVATime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        double curr = JadexModel.simulationtime;
        double diff = (vaTimeMilli - (curr * 100000000));
        if (diff >= 600000000) {
            double probability = 0.05 * vaTimeMilli;
            isMissed = new Random().nextDouble() < probability;
        }
        return isMissed;
    }

    Double oldSimTime = 0.0;
    Double previousBatteryLevel = null;
    boolean startOfCharging = false;
    double startTimeCharging = JadexModel.simulationtime;

    /**
     *  handles the progress of the current Trip
     */
    public void ExecuteTrips() {
        System.out.println(String.format("AgentId - %s: Called ExecuteTrips at simtime %s", agentID, JadexModel.simulationtime));
        System.out.println("DoNextTrip running");
//        System.out.println("tripList of agent" +agentID+ " :"+ tripList.size());
//        System.out.println("currentTrip: " + currentTrip.size());
        //TODO: erst erledigtes löschen dann neue ausführen!
        newCurrentTrip();
        if (currentTrip.size() == 1){
            if (currentTrip.get(0).getProgress().equals("AtEndLocation")) {
                updateCurrentTripProgress("Finished");
            }
            if (currentTrip.get(0).getProgress().equals("Finished") || currentTrip.get(0).getProgress().equals("Failed")) {
                currentTrip.remove(0); //dodo: check if it does really work or if position.intValue() needed
            }
        }

        newCurrentTrip(); // creates new current Trip if necessary and possible
        if (currentTrip.size() == 1) { //if there is a currentTrip
            currentTripStatus();
            // second part drive operations

            if (currentTrip.get(0).getProgress().equals("NotStarted")) {
                sendDriveTotoAdc();
                updateCurrentTripProgress("DriveToStart");
            }
            else if (currentTrip.get(0).getProgress().equals("AtEndLocation")) {
                updateCurrentTripProgress("Finished");
            }
            else if (currentTrip.get(0).getProgress().equals("AtStartLocation")) {
                // manage CustomerTrips that are AtStartLocation
                //TODO: @oemer add case for charging trip and execute charge operation
                //added -oemer
                if (currentTrip.get(0).getTripType().equals("ChargingTrip")) {
                    startTimeCharging = JadexModel.simulationtime;
                    System.out.println("AgentID: " + agentID + "started charging at " + startTimeCharging);
                    chargingTripIDList.add("1");

//                    load();
//                    trikeBattery.loadBattery();
//                    updateCurrentTripProgress("Finished");
//                    chargingTripAvailable = "0";
                }
                else if (currentTrip.get(0).getTripType().equals("CustomerTrip")) {
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
            ///**
            if (currentTrip.get(0).getProgress().equals("Finished") || currentTrip.get(0).getProgress().equals("Failed")) {
                currentTrip.remove(0); //dodo: check if it does really work or if position.intValue() needed
                //todo: commetn in if error
                //tripList.remove(0);
                if (tripList.size() > 0) { // if the tripList is not empty, depatch the next trip and send to data container
                    newCurrentTrip(); //hier??? ExecuteTrips()
                    ExecuteTrips(); //because you have to start
                    //sendDriveTotoAdc();
                    currentTripStatus();
                }
            }
            //**/
        }
//        estimateBatteryAfterTIP();
    }

    public void load() {
//        logCheck.add("L-Start");
        if (oldSimTime != JadexModel.simulationtime) {

            if (currentTrip.size() > 0 && currentTrip.get(0).getTripType().equals("ChargingTrip")
                    && currentTrip.get(0).getProgress().equals("AtStartLocation")) {

                if (!startOfCharging) {
                    startOfCharging = true;
                    oldSimTime = JadexModel.simulationtime;
                }

                Double delta = JadexModel.simulationtime - oldSimTime;

                Trip trip = currentTrip.get(0);
                // currentTrip contains a maximum of one element
                if (trip.getChargingTime() > 0.0) {

                    if (previousBatteryLevel == null) {
                        previousBatteryLevel = trikeBattery.getMyChargestate();
                    }
                    trikeBattery.charge(delta);

                    if (trikeBattery.getMyChargestate() == 1.0) {
                        // Finish charging because new trips are available
                        finishChargingTripAndLog(trip);
                    } else {
                        // charging time is in seconds
                        Double newChargingTime = (trip.getChargingTime() - delta) <= 0.0
                                ? 0.0
                                : trip.getChargingTime() - delta;
                        trip.setChargingTime(newChargingTime);
                        System.out.println("Agent " + agentID + "" +
                                " - Remaining charging time: " + trip.getChargingTime() +
                                " - Battery Level: " + trikeBattery.getMyChargestate() +
                                " - JadexSimTime: " + JadexModel.simulationtime);

                        if (trip.getChargingTime() == 0.0) {
                            finishChargingTripAndLog(trip);
                        }
                    }
                }

                // ChargingTime equals -1 means charge until new trips are available
                else if (trip.getChargingTime() == -1.0 && tripList.size() == 0) {
                    if (previousBatteryLevel == null) {
                        previousBatteryLevel = trikeBattery.getMyChargestate();
                    }
                    trikeBattery.charge(delta);
                    System.out.println("Agent " + agentID + "" +
                            " - Remaining charging time: " + trip.getChargingTime() +
                            " - Battery Level: " + trikeBattery.getMyChargestate() +
                            " - JadexSimTime: " + JadexModel.simulationtime);

                    if (trikeBattery.getMyChargestate() == 1.0) {
                        // Finish charging because new trips are available
                        finishChargingTripAndLog(trip);
                    }

                }

                else if (trip.getChargingTime() == -1.0 && tripList.size() > 0) {
                    // Finish charging because new trips are available
                    finishChargingTripAndLog(trip);
                }

                oldSimTime = JadexModel.simulationtime;
            }
        }
//        logCheck.add("L-End");
    }

    public void sendDriveTotoAdc()
    {
        Object[] Endparams = new Object[7];
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
        //added oemer
        Endparams[6] = sumLinkLength;
        SimActuator.getEnvironmentActionInterface().packageAction(agentID, "drive_to", Endparams, null);
        System.out.println("Set active to false of agentID " + agentID);
        activestatus = false; // to mark that this trike agent is not available to take new trip

    }

    public void SendPerceivetoAdc() // needs to send in the begining to subscribe to events in MATSIM
    {
        Object[] params = new Object[8];
        params[0] = "blocked";
        params[1] = "congestion";
        params[2] = "arrived"; // five secs from now;
        params[3] = "departed";
        params[4] = "activity_started";
        params[5] = "activity_ended"; // add replan activity to mark location/time of replanning
        params[6] = "stuck";
        params[7] = "sum_link_length"; //added -oemer

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

    // @Tim
    Double getDrivingDistanceBetweenToNodes(Location start, Location end, Double startTime) throws AgentNotFoundException {
        List<Double> args = Arrays.asList(start.getX(), start.getY(), end.getX(), end.getY(), startTime);
        LeastCostPathCalculator.Path path = (LeastCostPathCalculator.Path) SimActuator.getQueryPerceptInterface().queryPercept(
                String.valueOf(agentID),
                Constants.REQUEST_DRIVING_DISTANCE_BETWEEN_TWO_NODES,
                args);
        double res = 0.0;
        for (Link link: path.links) {
            res += link.getLength();
        }
        return res;
    }

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
    //Battery -oemer
    public void setMyLocation(Location location) {
    }

    public boolean isDaytime()
    {
        return this.daytime;
    }

    /**
     * Set the daytime of this Vision.
     * @param daytime the value to be set
     */
    public void setDaytime(boolean daytime)
    {
        this.daytime = daytime;

    }







    public class GeneticScheduler {

        private final Config config;

        public GeneticScheduler(Config config) {
            this.config = config;
        }

        // Es sollen Kanten und Punkte miteinander verbunden werden
        // Kanten stellen Kundenfahrten dar, die von einem Start zu einem Endpunkt verlaufen
        // Einzelne Punkte sind Ladestationen

        // Wird ein Trip nicht mit in die Lösung aufgenommen ist das nicht schlimm odr würde steigen?

        // Step 1 Generiere zuerste eine Menge von Chromosomen (mögliche Lösungen)
        // Step 2 Crossover/ Mutation/ Inversion + evtl. weiter k Random erstelle Chromosome
        // Evtl. starten mit Kruskal MST?


        // Zuerst benötigt man eine initiale population (1), dann eine fitness funktion (2) die diese bewertet. Dann werden die
        // besten x Chromosome aus der Population ausgewählt (3) und mit Hilfe von Crossover und Mutation verändert (4)
        // Schritt 3 - 4 werden wiederholt bis eine akzeptabele Lösung gefunden wurde.

        // Eine Aktion ist ein Gen, eine Menge von Genen ein Chromosom
        Population population;
        private int chargingStationsCounter = 0;

        public List<Trip> start(List<Trip> tripsToSchedule, int iterations, int runs) {
            try {
                List<Double> scores = new ArrayList<>();
                List<Chromosome> chromosomes = new ArrayList<>();
                for (int i = 0; i < runs; i++) {
                    // initial population size in abhängigkeit von der anzahl der trip -> max (2n + 1)! Permutation
                    int n = tripsToSchedule.size() * 2 + 1;
                    int totalPermutations = 1;
                    for (int k = 1; k <= n; k++) {
                        totalPermutations *= k;
                    }
                    // Get 10% of amount but maximum of 500
                    int initialPopulationSize = (totalPermutations / 10) < 100 ? 100 : Math.min(totalPermutations / 10, 300);

//                    int initialPopulationSize = 100 * Double.valueOf(Math.pow(2, i * 1)).intValue();

                    System.out.println("Gen Sched Trip to schedule: " + tripsToSchedule.stream().map(t -> t.getTripID()).collect(Collectors.toList()).toString());
                    System.out.println("Popsize " + initialPopulationSize);

                    population = new Population(initialPopulationSize, tripsToSchedule, this.config);

                    for (int j = 0; j < iterations; j++) {
//            System.out.println("Iteration " + i);
                        population.update();
                    }

                    Chromosome best = population.getBestChromosome();
//                    List<Trip> result = mapGenesBackToListOfTrip(genes, tripsToSchedule);
                    scores.add(best.fitnessOld());
                    chromosomes.add(best);
                }

                int index = 0;
                double maxVal = 0.0;
                for (int i = 0; i < scores.size(); i++) {
                    if (scores.get(i) > maxVal) {
                        index = i;
                        maxVal = scores.get(i);
                    }
                }
                return mapGenesBackToListOfTrip(chromosomes.get(index).mergeGenes(), tripsToSchedule);
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
                return null;
            }
        }

        private List<Trip> mapGenesBackToListOfTrip(List<Gene> genes, List<Trip> trips) {
            try {

                List<Trip> result = new ArrayList<>();
                for (Gene gene : genes) {
                    // charging gene
                    if (gene.getEnd() == null) {
                        Trip trip = new Trip(
                                "CH" + (chargingStationsCounter),
                                "ChargingTrip",
                                gene.getStart(),
                                "NotStarted",
                                gene.getChargingTime());
                        chargingStationsCounter++;
                        result.add(trip);
                    } else {
                        // customer trip
                        Trip trip = trips.stream().filter(t -> t.getTripID() == gene.getId()).collect(Collectors.toList()).get(0);
                        result.add(trip);
                    }
                }

                // set chargingTime to -1 if lastTrip ist chargingTrip
                Trip lastTrip = result.get(result.size() - 1);
                if (lastTrip.getTripType().equals("ChargingTrip")) {
                    lastTrip.setChargingTime(-1.0);
                }

                System.out.println("Result of Scheduler " + result.stream().map(t -> t.getTripID()).collect(Collectors.toList()));
                System.out.println("ChargingTime " + result.stream().map(t -> t.getChargingTime() == null ? 0 : t.getChargingTime()).collect(Collectors.toList()));
                return result;
            } catch (Exception e) {
                System.out.println("Caught exception when creating result");
                e.printStackTrace();
                return null;
            }
        }
    }

    public class Population {

        private List<Chromosome> population;
        private final int initialPopulationSize;
        private Gene[] customerGene;
        private Gene[] chargingGene;
        private final GeneticUtils geneticUtils;
        private final Config config;
        private final Random random = new Random();
        private Set<String> representationSet;

        public Population(int initialPopulationSize, List<Trip> trips, Config config) {
            this.initialPopulationSize = initialPopulationSize;
            this.geneticUtils = new GeneticUtils(config);
            deconstructInputTripListIntoGenes(trips);
            this.config = config;
            this.representationSet = new HashSet<>();
            this.population = init(initialPopulationSize);


            // füge anfängliches chromosom in die population ein => könnte bereits eine gute Lösung sein
//            Chromosome origin = new Chromosome(Arrays.asList(customerGene), Arrays.asList(chargingGene), config);
//            if (representationSet.add(origin.getRepresentation())) {
//                this.population.add(origin);
//            }
        }

        public void updateRepresentationSet() {
            this.representationSet = new HashSet<>();
            for (Chromosome c: population) {
                this.representationSet.add(c.getRepresentation());
            }
        }

        private void deconstructInputTripListIntoGenes(List<Trip> trips) {
            List<Gene> chargingGenes = new ArrayList<>();
            List<Gene> customerGenes = new ArrayList<>();
            if (trips.size() == 1) {
                customerGenes.add(geneticUtils.mapTripToGene(trips.get(0)));
                chargingGenes.add(null);
                chargingGenes.add(null);
            } else {
                Trip prevTrip = trips.get(0);
                if (prevTrip.getTripType().equals("ChargingTrip")) {
                    chargingGenes.add(geneticUtils.mapTripToGene(prevTrip));
                } else {
                    chargingGenes.add(null);
                    customerGenes.add(geneticUtils.mapTripToGene(prevTrip));
                }

                for (int i = 1; i < trips.size(); i++) {
                    Trip currTrip = trips.get(i);
                    if (currTrip.getTripType().equals("ChargingTrip")) {
                        chargingGenes.add(geneticUtils.mapTripToGene(currTrip));
                    } else {
                        if (prevTrip.getTripType().equals("CustomerTrip")
                                && currTrip.getTripType().equals("CustomerTrip")) {
                            chargingGenes.add(null);
                        }
                        customerGenes.add(geneticUtils.mapTripToGene(currTrip));
                    }
                    prevTrip = currTrip;
                }

                if (trips.get(trips.size() - 1).getTripType().equals("CustomerTrip")) {
                    chargingGenes.add(null);
                }
            }

            if ((customerGenes.size() + 1) != chargingGenes.size()) {
                System.out.println("Caught Exception in Deconstruct " + trips.stream().map(t -> t.getTripID()).collect(Collectors.toList()));
            }

            this.customerGene = new Gene[customerGenes.size()];
            this.chargingGene = new Gene[chargingGenes.size()];
            this.customerGene = customerGenes.toArray(new Gene[0]);
            this.chargingGene = chargingGenes.toArray(new Gene[0]);
        }

        public Chromosome getBestChromosome() {
            // is sorted and therefore the best chromosome is at first place in the list
            return this.population.get(0);
        }

        private List<Chromosome> init(int initialPopulationSize) {
            try {
                Gene[] chargingGene = geneticUtils.generateChargingGenes();

                representationSet = new HashSet<>();
                List<Chromosome> initPopulation = new ArrayList<>();
                if (config.batteryLevel > 0.90 && customerGene.length == 1) {
                    // popsize kann nicht erreicht werden bzw. nur schwer
                    while (initialPopulationSize < 20) {
                        Chromosome newChromosome = geneticUtils.create(customerGene, chargingGene);
                        if (newChromosome.fallsNotBelowThreshhold() && representationSet.add(newChromosome.getRepresentation())) {
                            initPopulation.add(newChromosome);
                        }
                    }
                } else {
                    while (initPopulation.size() < initialPopulationSize) {
                        Chromosome newChromosome = geneticUtils.create(customerGene, chargingGene);
                        if (newChromosome.fallsNotBelowThreshhold() && representationSet.add(newChromosome.getRepresentation())) {
                            initPopulation.add(newChromosome);
                        }
                    }
                }
                return initPopulation;
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
                return null;
            }
        }

        public void update() {
            try {
//        System.out.println("CrossOver");
                crossover();
//        System.out.println("Mutation");
                mutation();
//        System.out.println("Spawn");
                noveltySearch();
//        System.out.println("Selection");
                selection();
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
            }
        }

        private void tournamentSelection() {

        }

        private void selection() {
            try {
//                System.out.println("Population size before selection: " + population.size());
//                this.population.sort(Comparator.comparingDouble(Chromosome::fitness).reversed());
//                // keep population size constant but keep the fittest individuals
//                this.population = this.population.stream().limit(this.initialPopulationSize).collect(Collectors.toList());
//                if (Boolean.parseBoolean(System.getenv("csv"))) {
//                    System.out.println("tripIds,chargingTimes,coords,distance,waitingTimesSum,odr,battery,distanceFraction,waitingTimesSumFraction,odrFraction,batteryFraction,fitness");
//                }
                this.population = selectTopNChromosomes();
//                updateRepresentationSet();
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
            }
        }

        public List<Chromosome> selectTopNChromosomes() {
            PriorityQueue<Chromosome> minHeap = new PriorityQueue<>(this.initialPopulationSize, Comparator.comparingDouble(Chromosome::fitnessOld));

            for (Chromosome chromosome : this.population) {
                if (minHeap.size() < this.initialPopulationSize) {
                    minHeap.add(chromosome);
                } else if (chromosome.fitnessOld() > minHeap.peek().fitnessOld()) {
                    minHeap.poll();
                    minHeap.add(chromosome);
                }
            }


            List<Chromosome> topChromosomes = new ArrayList<>(minHeap);
            topChromosomes.sort(Comparator.comparingDouble(Chromosome::fitnessOld).reversed());
            List<Chromosome> withoutDuplicats = new ArrayList<>();
            representationSet = new HashSet<>();
            for (Chromosome chromosome: topChromosomes) {
                if (representationSet.add(chromosome.getRepresentation())) {
                    withoutDuplicats.add(chromosome);
                }
            }
            return withoutDuplicats;
        }

        private void noveltySearch() {
            try {
                IntStream.range(0, 20)
                        .forEach(e -> {
                            Chromosome c = geneticUtils.create(customerGene, geneticUtils.generateChargingGenes());
                            if (representationSet.add(c.getRepresentation())) {
                                this.population.add(c);
                            }
                        });
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
            }
        }

        private void mutation() {
            try {
                int size = population.size();
                for (int i = 0; i < size / 5; i++) {
                    if (population.get(0).getCustomerChromosome().size() > 1) {
                        Chromosome mutated  = this.population.get(random.nextInt(population.size())).mutate();
                        if (mutated.fallsNotBelowThreshhold() && representationSet.add(mutated.getRepresentation())) {
                            population.add(mutated);
                        }
                    }
                    Chromosome chargingChromosome = this.population.get(random.nextInt(population.size()));
                    // get new charging chromosome if there are no charging genes
                    while (chargingChromosome.mergeGenes().size() == chargingChromosome.customerChromosome.size()) {
                        chargingChromosome = this.population.get(random.nextInt(population.size()));
                    }
                    Chromosome mutated2 = chargingChromosome.mutateChargingTimes();
                    if (mutated2.fallsNotBelowThreshhold() && representationSet.add(mutated2.getRepresentation())) {
                        population.add(mutated2);
                    }
                }
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
            }
        }

        private void crossoverWithPopulationReplacement() {
            try {
                List<Chromosome> cache = new ArrayList<>();
                // no candidates exists for a crossover
                if (population.size() == 1) {
                    return;
                }
                for (Chromosome chromosome : this.population) {
                    Chromosome partner = getCrossOverPartner(chromosome);
                    cache.addAll(Arrays.asList(chromosome.crossover(partner)));
                }

                for (Chromosome c : cache) {
                    representationSet = new HashSet<>();
                    this.population = new ArrayList<>();
                    if (c.fallsNotBelowThreshhold() & representationSet.add(c.getRepresentation())) {
                        this.population.add(c);
                    }
                }
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
            }
        }

        private void crossover() {
            try {
                List<Chromosome> cache = new ArrayList<>();
                // no candidates exists for a crossover
                if (population.size() == 1) {
                    return;
                }
                for (int i = 0; i < population.size() / 2; i++) {
                    Chromosome ith = population.get(i);
                    Chromosome partner = getCrossOverPartner(ith);
                    cache.addAll(Arrays.asList(ith.crossover(partner)));
                }

                for (Chromosome c : cache) {
                    if (c.fallsNotBelowThreshhold() && representationSet.add(c.getRepresentation())) {
                        this.population.add(c);
                    }
                }
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
            }
        }

        private Chromosome getCrossOverPartner(Chromosome chromosome) {
            try {
                Chromosome partner = this.population.get(random.nextInt(population.size()));
                while (chromosome.representation == partner.representation) {
                    partner = this.population.get(random.nextInt(population.size()));
                }
                return partner;
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
                return null;
            }
        }
    }

    public class Chromosome {

        private Config config;
        private final List<Gene> customerChromosome;
        private final List<Gene> chargingChromosome;
        private final Random random;
        private final GeneticUtils geneticUtils;
        private String representation;

        public Chromosome(List<Gene> customerChromosome, List<Gene> chargingChromosome, Config config) {
            this.customerChromosome = Collections.unmodifiableList(customerChromosome);
            this.chargingChromosome = Collections.unmodifiableList(chargingChromosome);
            this.config = config;
            this.random = new Random();
            this.geneticUtils = new GeneticUtils(config);
            this.representation = "";
            updateRepresentation();
        }

        public List<Gene> mergeGenes() {
            List<Gene> resultGene = new ArrayList<>();
            try {
                for (int i = 0; i < customerChromosome.size(); i++) {
                    if (chargingChromosome.get(i) != null) {
                        resultGene.add(chargingChromosome.get(i));
                    }
                    resultGene.add(customerChromosome.get(i));
                }
                // add last charging chromosome if not null
                if (chargingChromosome.get(customerChromosome.size()) != null) {
                    resultGene.add(chargingChromosome.get(customerChromosome.size()));
                }
                return resultGene;
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                System.out.println("what the...");
            }
            return resultGene;
        }

        String filePath = "genetic3-3-3-1.csv";
        String header = "ids,chTimes,distance,waitingTime,odr,battery,disFrac,waitingFrac,odrFrac,battFrac,fitness";

        double fitnessOld() {
            // calculate fitness for merged genes
            List<Gene> chromosomeToEvaluate = mergeGenes();

            boolean vehicleBreaksDownRisk = false;
            BatteryModel model = new BatteryModel();
            model.setMyChargestate(config.getBatteryLevel() + getCurrentTripChargingTime() * config.getCHARGE_INCREASE());
            // distance to agentlocation of first gene => create gene for agentLocation
            // if end location is null the start location is used to determine the distance between two genes
            Gene agentGene = new Gene(null, config.getAgentLocation(), null, null, null, config);
            double distance = agentGene.distance(chromosomeToEvaluate.get(0));
            double currWaitingTime = calculateTravelTime(distance, config.getDRIVING_SPEED());
            List<Double> waitingTimes = new ArrayList<>();
            if (chromosomeToEvaluate.get(0).getEnd() != null) {
                double diffToBookingTime = calculateWaitingTime(chromosomeToEvaluate.get(0).bookingTime, config.getSimulationTime());
                waitingTimes.add(currWaitingTime + diffToBookingTime);
            }
            for (int i = 0; i < chromosomeToEvaluate.size() - 1; i++) {
                // calc distance of trip (start to end)
                double geneDistance = getGeneDistance(chromosomeToEvaluate.get(i));

                // gene is charging gene if distance equals 0.0
                if (geneDistance == 0.0) {
                    double chargingTime = chromosomeToEvaluate.get(i).getChargingTime();
                    currWaitingTime += chargingTime;
                    model.charge(chargingTime);
                }

                // calc distance to next trip
                double distanceToNext = chromosomeToEvaluate.get(i).distance(chromosomeToEvaluate.get(i + 1)) ;
                model.discharge(geneDistance + distanceToNext, 0, false);
                // battery level shouldn't get near 20%
                if (model.getMyChargestate() <= 0.3) {
                    vehicleBreaksDownRisk = true;
                    break;
                }
                distance += geneDistance + distanceToNext;

                // add time needed for the trip itself and time needed for the next trip
                currWaitingTime += calculateTravelTime(geneDistance + distanceToNext, config.getDRIVING_SPEED());

//                currWaitingTime += chromosomeToEvaluate.get(i).calculateWaitingTime(chromosomeToEvaluate.subList(0, i + 1));

                // add waiting time if next trip is customer trip
                if (chromosomeToEvaluate.get(i + 1).getEnd() != null) {
                    double diffToBookingTime = calculateWaitingTime(chromosomeToEvaluate.get(i + 1).bookingTime, config.getSimulationTime());
                    waitingTimes.add(currWaitingTime + diffToBookingTime);
                }
            }

            // last trip has not yet been considered
            double lastGeneDistance =  getGeneDistance(chromosomeToEvaluate.get(chromosomeToEvaluate.size() - 1));
            distance += lastGeneDistance;
            if (lastGeneDistance == 0.0) {
                double chargingTime = chromosomeToEvaluate.get(chromosomeToEvaluate.size() - 1).getChargingTime();
                model.charge(chargingTime);
            }
            model.discharge(lastGeneDistance, 0, false);
            // battery level shouldn't get near 20%
            if (model.getMyChargestate() <= 0.3) {
                vehicleBreaksDownRisk = true;
            }

//            Double chargingTime = chromosomeToEvaluate.stream()
//                    .map(g -> g.getChargingTime() == null ? 0.0 : g.getChargingTime())
//                    .collect(Collectors.summingDouble(Double::doubleValue));
//            model.charge(chargingTime);

            // overall waiting time minimieren?
            int odr = waitingTimes.stream().filter(wt -> wt > config.getTHETA()).collect(Collectors.toList()).size();
            double waitingTimeSum = waitingTimes.stream().mapToDouble(Double::doubleValue).sum();

            if (vehicleBreaksDownRisk) {
                return 0.0;
            }

            // Becomes smaller the greater the distance
            double avgCusTripDistance = 1000;
            double avgChaTripDistance = 500;
            double tripsTimesAvgDistance = avgCusTripDistance * this.customerChromosome.size()
                    + avgChaTripDistance * (chromosomeToEvaluate.size() - this.customerChromosome.size());

            Double fitnessFractionDistance = (distance / tripsTimesAvgDistance) > 1.0 ? 1.0 : (tripsTimesAvgDistance / 10000);
            fitnessFractionDistance = 1 - fitnessFractionDistance;
            // 0.1 was added, as otherwise the fraction is equal to 1 with an ODR value of 1. However, this should only be
            // 1 if the ODR is 0.
            Double fitnessFractionODR = 1 - (odr / Double.valueOf(this.customerChromosome.size()));

            double thetaTimeCusTrips = this.customerChromosome.size() * config.getTHETA();
            Double fitnessFractionWaitingTimeSum = (waitingTimeSum / thetaTimeCusTrips) > 1.0 ? 1.0 : (thetaTimeCusTrips / 10000);
            fitnessFractionWaitingTimeSum =  1 - fitnessFractionWaitingTimeSum;

            // The higher the fitness, the better it is
            // fitnessFractionDistance, fitnessFractionODR, model.getMyChargeState € [0,1]

//            double distanceWeight = 0.2192;
//            double waitingTimeSumWeight = 0.3781;
//            double odrWeight = 0.3707;
//            double batteryWeight = 0.0321;

            double distanceWeight = 0.3;
            double waitingTimeSumWeight = 0.3;
            double odrWeight = 0.3;
            double batteryWeight = 0.1;

            Double considerFuture = 0.0;
            if (isChargingGene(chromosomeToEvaluate.get(chromosomeToEvaluate.size() - 1))) {
                considerFuture = 0.1;
            }

            double fitness = distanceWeight * (fitnessFractionDistance)
                    + waitingTimeSumWeight * fitnessFractionWaitingTimeSum
                    + odrWeight * fitnessFractionODR
                    + batteryWeight * model.getMyChargestate()
                    + considerFuture;


            if (false) {
                File file = new File(filePath);
                boolean dateiExistiert = file.exists();

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                    if (!dateiExistiert) {
                        writer.write(header);
                        writer.newLine();
                    }

                    writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                            representation,
                            chromosomeToEvaluate.stream().map(g -> g.getChargingTime() == null ? 0 : g.getChargingTime()).collect(Collectors.toList()).toString().replace(",", "-"),
//                                chromosomeToEvaluate.stream()
//                                        .map(g -> {
//                                            if (g.getEnd() == null) {
//                                                return Arrays.asList(g.getStart());
//                                            } else {
//                                                return Arrays.asList(g.getStart(), g.getEnd());
//                                            }
//                                        })
//                                        .flatMap(List::stream)
//                                        .collect(Collectors.toList()).toString().replace(",", "-"),
                            distance,
                            waitingTimeSum,
                            odr,
                            model.getMyChargestate(),
                            fitnessFractionDistance,
                            fitnessFractionWaitingTimeSum,
                            fitnessFractionODR,
                            model.getMyChargestate(),
                            fitness
                    ));
                    writer.newLine();

                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
//                System.out.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
//                                chromosomeToEvaluate.stream().map(g -> g.getId()).collect(Collectors.toList()).toString().replace(",", "-"),
//                                chromosomeToEvaluate.stream().map(g -> g.getChargingTime() == null ? 0 : g.getChargingTime()).collect(Collectors.toList()).toString().replace(",", "-"),
//                                chromosomeToEvaluate.stream()
//                                        .map(g -> {
//                                            if (g.getEnd() == null) {
//                                                return Arrays.asList(g.getStart());
//                                            } else {
//                                                return Arrays.asList(g.getStart(), g.getEnd());
//                                            }
//                                        })
//                                        .flatMap(List::stream)
//                                        .collect(Collectors.toList()).toString().replace(",", "-"),
//                                distance,
//                                waitingTimeSum,
//                                odr,
//                                model.getMyChargestate(),
//                                fitnessFractionDistance,
//                                fitnessFractionWaitingTimeSum,
//                                fitnessFractionODR,
//                                model.getMyChargestate(),
//                                fitness
//                        )
//                );
            }

            return fitness;
        }

        double fitnessRefactored() {
            try {
                // calculate fitness for merged genes
                List<Gene> chromosomeToEvaluate = mergeGenes();

                Location vaLocation = getStartLocation();

                Double batteryAfterList;
                List<Double> distancesToTrips = new ArrayList<>();
                Double batteryLevel = config.batteryLevel;
                Double batteryThreshhold = 0.3;
                Boolean batteryFallsBelowThreshhold = false;
                Double distance = 0.0;
                for (int i = 0; i < chromosomeToEvaluate.size(); i++) {
                    Gene currentGene = chromosomeToEvaluate.get(i);
                    List<Location> locationsOfGene = getLocationsOfGene(currentGene);

                    // drive to start of current trip
                    Double currentDistance = getDistanceWithDistanceFactor(vaLocation, locationsOfGene.get(0));
                    distance += currentDistance;
                    distancesToTrips.add(distance);
                    vaLocation = locationsOfGene.get(0);

                    if (isChargingGene(currentGene)) {
                        Double chargingTime = currentGene.getChargingTime() == null ? 0.0 : currentGene.getChargingTime();
                        batteryLevel += chargingTime * config.getCHARGE_INCREASE();
                    } else {
                        // first trip is customer - drive to end
                        distance += getDistanceWithDistanceFactor(vaLocation, locationsOfGene.get(1));
                        vaLocation = locationsOfGene.get(1);
                    }
                }

                List<Double> waitingTimesOfGenes = getWaitingTimeOfGenes(distancesToTrips, chromosomeToEvaluate);

                // add distance of last trip from start to end
                Double totalDistance = distancesToTrips.get(distancesToTrips.size() - 1);
                Gene lastGene = chromosomeToEvaluate.get(chromosomeToEvaluate.size() - 1);
                if (!isChargingGene(lastGene)) {
                    totalDistance += getDistanceWithDistanceFactor(lastGene.getStart(), lastGene.getEnd());
                }

                // berücksichtige ob current trip charging trip und dann chargingtime bei Bestimmung der ODR
                batteryAfterList = batteryLevel - (totalDistance * config.getCHARGE_DECREASE()) + getCurrentTripChargingTime() * config.getCHARGE_INCREASE();

                // check if battery threshhold is undercut
                if (batteryAfterList < batteryThreshhold) {
                    batteryFallsBelowThreshhold = true;
                }

                // rating
                if (batteryFallsBelowThreshhold) {
                    return 0.0;
                }

                int odr = 0;
                for (int i = 0; i < chromosomeToEvaluate.size(); i++) {
                    if (!isChargingGene(chromosomeToEvaluate.get(i)) && waitingTimesOfGenes.get(i) > THETA) {
                        odr++;
                    }
                }

                // weighted sum consisting of totalDistance, odr, batteryAfterAll

                // Becomes smaller the greater the distance
                // analyzed from data.csv
                double avgCusTripDistance = 1100;
                double avgChaTripDistance = 450;
                double tripsTimesAvgDistance = avgCusTripDistance * this.customerChromosome.size()
                        + avgChaTripDistance * (chromosomeToEvaluate.size() - this.customerChromosome.size());

                Double fitnessFractionDistance = (totalDistance / tripsTimesAvgDistance) > 1.0
                        ? 1.0
                        : (tripsTimesAvgDistance / 10000);
                fitnessFractionDistance = 1 - fitnessFractionDistance;

                // 0.1 was added, as otherwise the fraction is equal to 1 with an ODR value of 1. However, this should only be
                // 1 if the ODR is 0.
                Double fitnessFractionODR = 1 - (odr / Double.valueOf(this.customerChromosome.size()));

                Double waitingTimeSum = waitingTimesOfGenes.stream().mapToDouble(Double::doubleValue).sum();
                double thetaTimeCusTrips = this.customerChromosome.size() * config.getTHETA();
                Double fitnessFractionWaitingTimeSum = (waitingTimeSum / thetaTimeCusTrips) > 1.0
                        ? 1.0
                        : (waitingTimeSum / thetaTimeCusTrips);
                fitnessFractionWaitingTimeSum = 1 - fitnessFractionWaitingTimeSum;

                // The higher the fitness, the better it is
                // fitnessFractionDistance, fitnessFractionODR, model.getMyChargeState € [0,1]

//            double distanceWeight = 0.2192;
//            double waitingTimeSumWeight = 0.3781;
//            double odrWeight = 0.3707;
//            double batteryWeight = 0.0321;


                double distanceWeight = 0.3;
                double waitingTimeSumWeight = 0.3;
                double odrWeight = 0.3;
                double batteryWeight = 0.1;

                Double considerFuture = 0.0;
                if (isChargingGene(chromosomeToEvaluate.get(chromosomeToEvaluate.size() - 1))) {
                    considerFuture = 0.1;
                }

                double fitness = distanceWeight * (fitnessFractionDistance)
                        + waitingTimeSumWeight * fitnessFractionWaitingTimeSum
                        + odrWeight * fitnessFractionODR
                        + batteryWeight * batteryAfterList
                        + considerFuture;

                if (false) {
                    File file = new File(filePath);
                    boolean dateiExistiert = file.exists();

                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                        if (!dateiExistiert) {
                            writer.write(header);
                            writer.newLine();
                        }

                        writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                                chromosomeToEvaluate.stream().map(g -> g.getId()).collect(Collectors.toList()).toString().replace(",", "-"),
                                chromosomeToEvaluate.stream().filter(g -> isChargingGene(g)).map(g -> g.getChargingTime()).mapToDouble(Double::doubleValue).sum(),
                                totalDistance,
                                waitingTimeSum,
                                odr,
                                batteryAfterList,
                                fitnessFractionDistance,
                                fitnessFractionWaitingTimeSum,
                                fitnessFractionODR,
                                batteryAfterList,
                                fitness
                        ));
                        writer.newLine();

                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }

                return fitness;
            } catch (Exception e) {
                e.printStackTrace();
                return 0.0;
            }
        }

        Boolean fallsNotBelowThreshhold() {
            try {
                Double threshhold = 0.3;
                List<Location> locations = new ArrayList<>();
                locations.add(getStartLocation());
                locations.addAll(getLocations());
                Double currChargingTime = getCurrentTripChargingTime();

                Double totalDistance = geneticUtils.getTotalDistanceOfLocationList(locations);
                Double battery = config.getBatteryLevel() + currChargingTime * config.getCHARGE_INCREASE();
                battery -= totalDistance * config.getCHARGE_DECREASE();

                List<Gene> genesWithoutLast = chargingChromosome.subList(0, chargingChromosome.size() - 1);
                Double chargingTimeInChromosom = genesWithoutLast.stream()
                        .filter(g -> g != null)
                        .map(g -> g.getChargingTime())
                        .mapToDouble(Double::doubleValue)
                        .sum();
                battery += chargingTimeInChromosom * config.getCHARGE_INCREASE();

                return battery > threshhold;
            } catch (Exception e) {
                e.printStackTrace();
                return true;
            }
        }

        private List<Location> getLocations() {
            List<Location> result = new ArrayList<>();
            for (int i = 0; i < chargingChromosome.size(); i++) {
                if (chargingChromosome.get(i) != null) {
                    result.add(chargingChromosome.get(i).getStart());
                }
                if (i != chargingChromosome.size() - 1) {
                    result.add(customerChromosome.get(i).getStart());
                    result.add(customerChromosome.get(i).getEnd());
                }
            }
            return result;
        }

        private List<Double> getWaitingTimeOfGenes(List<Double> distancesToTrips, List<Gene> chromosome) {
            List<Double> travelTimesToEachStart = distancesToTrips.stream()
                    .map(d -> SchedulerUtils.calculateTravelTime(d, config.DRIVING_SPEED))
                    .collect(Collectors.toList());

            for (int i = 0; i < travelTimesToEachStart.size(); i++) {
                Gene gene = chromosome.get(i);
                if (!isChargingGene(gene)) {
                    double travelAndWaitingTime = travelTimesToEachStart.get(i) + SchedulerUtils.calculateWaitingTime(
                            gene.bookingTime,
                            config.getSimulationTime()
                    );

                    Double chargingTimeBeforeTrip = getCurrentTripChargingTime();
                    for (int j = 0; j < i; j++) {
                        if (isChargingGene(chromosome.get(j))) {
                            chargingTimeBeforeTrip += chromosome.get(j).getChargingTime();
                        }
                    }

                    // add MIN_CHARGING_TIME for every charging station on the way
                    travelAndWaitingTime += chargingTimeBeforeTrip;
                    travelTimesToEachStart.set(i, travelAndWaitingTime);
                }
            }
            return travelTimesToEachStart;
        }

        private Double getCurrentTripChargingTime() {
            if (currentTrip.size() == 0) { return 0.0; }
            else {
                return currentTrip.get(0).getTripType().equals("ChargingTrip")
                        ? currentTrip.get(0).getChargingTime()
                        : 0.0;
            }
        }

        private boolean isChargingGene(Gene currentGene) {
            return currentGene.getEnd() == null;
        }

        private Double getDistanceWithDistanceFactor(Location startLocation, Location location) {
            return Location.distanceBetween(startLocation, location) * DISTANCE_FACTOR;
        }

        private List<Location> getLocationsOfGene(Gene gene) {
            if (gene.getEnd() == null) {
                return Arrays.asList(gene.getStart());
            } else {
                return Arrays.asList(gene.getStart(), gene.getEnd());
            }
        }

        private Location getStartLocation() {
            Location location = agentLocation;
            if (currentTrip.size() > 0) {
                switch (currentTrip.get(0).getTripType()) {
                    case "AtStartLocation":
                        location = currentTrip.get(0).getStartPosition();
                        break;
                    case "DriveToEnd":
                        // überabschätzung
                        location = currentTrip.get(0).getStartPosition();
                        break;
                    case "AtEndLocation":
                        location = currentTrip.get(0).getEndPosition();
                        break;
                    default:
                        location = agentLocation;
                        break;
                }
            }
            return location;
        }

        private double getGeneDistance(Gene gene) {
            return gene.getEnd() != null
                    ? Location.distanceBetween(gene.getStart(), gene.getEnd()) * config.DISTANCE_FACTOR
                    : 0.0;
        }

        private void updateRepresentation() {
            String result = "";
            List<Gene> mergedGenes = mergeGenes();
            for (Gene gene: mergedGenes) {
                result += gene.id;
                if (gene.getChargingTime() != null) {
                    result += gene.getChargingTime();
                }
            }
            this.representation = result;
        }

        Chromosome[] crossover(final Chromosome otherChromosome) {
            try {
                // halbiere das Chromosom und füge die zweite hälfte des ersten an die erste hälfte des zweiten ein und umgekehrt
                List<List<Gene>> customerCrossOver = geneticUtils.makeCrossoverCustomer(customerChromosome, otherChromosome.customerChromosome);
                List<List<Gene>> chargingCrossOver = geneticUtils.makeCrossoverCharging(chargingChromosome, otherChromosome.chargingChromosome);

                if (customerCrossOver.get(0).size() != customerChromosome.size()
                        || customerCrossOver.get(1).size() != customerChromosome.size()
                        || chargingCrossOver.get(0).size() != chargingChromosome.size()
                        || chargingCrossOver.get(1).size() != chargingChromosome.size()) {
                    System.out.println("Caught Exception in Crossover: \n  this.customer - " + this.customerChromosome
                            + " | this.charging - " + this.chargingChromosome
                            + "\n  Other customer - " + otherChromosome.customerChromosome + " | Other charging - " + otherChromosome.chargingChromosome);
                }

                return new Chromosome[] {
                        new Chromosome(customerCrossOver.get(0), chargingCrossOver.get(0), config),
                        new Chromosome(customerCrossOver.get(1), chargingCrossOver.get(1), config)
                };
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
                return null;
            }
        }

        private List<Gene> copyList(List<Gene> genes) {
            List<Gene> copy = new ArrayList<>();
            for (Gene gene: genes) {
                if (gene == null) {
                    copy.add(null);
                } else {
                    copy.add(gene.createDeepCopy());
                }
            }
            return copy;
        }

        Chromosome mutate() {
            try {
                // mutate customer trips and insert or delete charging trips

                // case customer trip
                final List<Gene> customerCopy = new ArrayList<>(this.customerChromosome);
                if (this.customerChromosome.size() > 1) {
                    int size = this.customerChromosome.size();
                    int indexA = random.nextInt(size);
                    int indexB = random.nextInt(size);
                    while (indexA == indexB) {
                        indexA = random.nextInt(size);
                        indexB = random.nextInt(size);
                    }
                    Collections.swap(customerCopy, indexA, indexB);
                }

                // case charging trip
                // swap random or change the charging times of random charging trips
                Random rand = new Random();

                List<Gene> chargingCopy = copyList(this.chargingChromosome);

                if (chargingCopy.size() == 2) {
                    Collections.swap(chargingCopy, 0, 1);
                } else {
                    int size = this.chargingChromosome.size();
                    int indexACharging = random.nextInt(size);
                    int indexBCharging = random.nextInt(size);
                    while (indexACharging == indexBCharging) {
                        indexACharging = random.nextInt(size);
                        indexBCharging = random.nextInt(size);
                    }
                    Collections.swap(chargingCopy, indexACharging, indexBCharging);
                }

                return new Chromosome(customerCopy, chargingCopy, config);
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
                return null;
            }
        }

        Chromosome mutateChargingTimes() {
            final List<Gene> customerCopy = new ArrayList<>(this.customerChromosome);
            List<Gene> chargingCopy = new ArrayList<>(this.chargingChromosome);
            List<Gene> mutated = geneticUtils.mutateChargingTimesInRelationToBatteryLevelConsideringPreviousTrips(chargingCopy, customerCopy);
            return new Chromosome(customerCopy, mutated, config);
        }

        public List<Gene> getCustomerChromosome() {
            return customerChromosome;
        }

        public List<Gene> getChargingChromosome() {
            return chargingChromosome;
        }

        public String getRepresentation() {
            return representation;
        }
    }

    public class GeneticUtils {

        private final Config config;
        private final Random random = new Random();

        public GeneticUtils(Config config) {
            this.config = config;
        }

        public List<List<Gene>> split(List<Gene> chromosome) {
            List<List<Gene>> result = new ArrayList<>();
            result.add(chromosome.subList(0, chromosome.size() / 2));
            result.add(chromosome.subList(chromosome.size() / 2, chromosome.size()));
            return result;
        }

        public  Chromosome create(final Gene[] customerGeneInput, Gene[] chargingGeneInput) {
            // create random solutions for initial population
            // create chromosome for customerTrips
            final List<Gene> customerGene = Arrays.asList(Arrays.copyOf(customerGeneInput, customerGeneInput.length));
            Collections.shuffle(customerGene);

            // create chromosome for chargingTrips
            // get random number of chargingStations between 0 and sie of customerTrip + 1
            List<Gene> chargingGenes = getRandomAmountOfChargingStations(
                    chargingGeneInput,
                    customerGeneInput.length + 1
            );

            List<Gene> copyChargingGenes = copyList(chargingGenes);
            return new Chromosome(customerGene, copyChargingGenes, config);
        }

        public Gene mapTripToGene(Trip trip) {
            try {
                return new Gene(
                        trip.tripID,
                        trip.startPosition,
                        trip.endPosition,
                        trip.bookingTime,
                        trip.getChargingTime(),
                        config
                );
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
                return null;
            }
            // ToDo: Extend with booking time and more?
        }

        public List<Gene> getRandomAmountOfChargingStations(Gene[] chargingGene, int resultSize) {
            try {
                // create list consisting of null values
                List<Gene> result = new ArrayList<>();
                for (int i = 0; i < resultSize; i++) {
                    result.add(null);
                }

                // set at random position random charging stations
                int count = random.nextInt(resultSize);
                for (int i = 0; i < count; i++) {
                    int randomIndex = random.nextInt(chargingGene.length);
                    int indexToSet = random.nextInt(resultSize);
                    while (result.get(indexToSet) != null) {
                        indexToSet = random.nextInt(resultSize);
                    }
                    Gene gene = chargingGene[randomIndex];
                    Gene copyOfGene = gene.createDeepCopy();

                    int min = config.getMIN_CHARGING_TIME().intValue();
                    Double temp = (config.getMAX_CHARGING_TIME() * (1 - config.getBatteryLevel())) > config.getMIN_CHARGING_TIME() + 1.0
                            ? (config.getMAX_CHARGING_TIME() * (1 - config.getBatteryLevel()))
                            : config.getMIN_CHARGING_TIME() + 1.0;
                    int max = temp.intValue();
                    int randomInt = random.nextInt((max - min) + 1) + min;

                    copyOfGene.setChargingTime(Double.valueOf(randomInt));
                    result.set(indexToSet, copyOfGene);
                }
                return result;
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
                return null;
            }
        }

        public List<List<Gene>> makeCrossoverCustomer(List<Gene> first, List<Gene> second) {
            // case there is only one customer trip
            try {

                if (first.size() == 1) {
                    return Arrays.asList(first, first);
                }

                final List<List<Gene>> thisDNA = split(first);
                final List<List<Gene>> otherDNA = split(second);

                final List<Gene> firstCrossOver = new ArrayList<>(thisDNA.get(0));

                for (int i = 0; i < otherDNA.get(0).size(); i++) {
                    if (!firstCrossOver.contains(otherDNA.get(0).get(i))) {
                        firstCrossOver.add(otherDNA.get(0).get(i));
                    }
                }

                for (int i = 0; i < otherDNA.get(1).size(); i++) {
                    if (!firstCrossOver.contains(otherDNA.get(1).get(i))) {
                        firstCrossOver.add(otherDNA.get(1).get(i));
                    }
                }

                final List<Gene> secondCrossOver = new ArrayList<>(otherDNA.get(1));

                for (int i = 0; i < thisDNA.get(0).size(); i++) {
                    if (!secondCrossOver.contains(thisDNA.get(0).get(i))) {
                        secondCrossOver.add(thisDNA.get(0).get(i));
                    }
                }

                for (int i = 0; i < thisDNA.get(1).size(); i++) {
                    if (!secondCrossOver.contains(thisDNA.get(1).get(i))) {
                        secondCrossOver.add(thisDNA.get(1).get(i));
                    }
                }

                if (firstCrossOver.size() > first.size()) {
                    removeElements(firstCrossOver, first.size());
                }

                if (secondCrossOver.size() > first.size()) {
                    removeElements(secondCrossOver, first.size());
                }

                return Arrays.asList(firstCrossOver, secondCrossOver);
            } catch (Exception e) {
                System.out.println("Caught exception: " + e);
                e.printStackTrace();
            }
            return null;
        }

        public List<List<Gene>> makeCrossoverCharging(List<Gene> first, List<Gene> second) {
            try {
                final List<List<Gene>> thisDNA = split(first);
                final List<List<Gene>> otherDNA = split(second);

                List<List<Gene>> copyThisDNA = makeCopyList(thisDNA);
                List<List<Gene>> copyOtherDNA = makeCopyList(otherDNA);

                final List<Gene> firstCrossOver = new ArrayList<>(copyThisDNA.get(0));
                firstCrossOver.addAll(copyOtherDNA.get(1));

                final List<Gene> secondCrossOver = new ArrayList<>(copyOtherDNA.get(0));
                secondCrossOver.addAll(copyThisDNA.get(1));

                return Arrays.asList(firstCrossOver, secondCrossOver);
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
            }
            return null;
        }

        private List<List<Gene>> makeCopyList(List<List<Gene>> thisDNA) {
            List<List<Gene>> result = new ArrayList<>();
            for (List<Gene> intermediate: thisDNA) {
                List<Gene> copy = copyList(intermediate);
                result.add(copy);
            }
            return result;
        }

        private List<Gene> copyList(List<Gene> genes) {
            List<Gene> copy = new ArrayList<>();
            for (Gene gene: genes) {
                if (gene == null) {
                    copy.add(null);
                } else {
                    copy.add(gene.createDeepCopy());
                }
            }
            return copy;
        }

        public Gene[] generateChargingGenes() {
            try {
                List<Location> chargingStations = config.getChargingStations();
                Double minChargingTime = config.getMIN_CHARGING_TIME();
                Gene[] chargingGene = new Gene[chargingStations.size()];
                for (int i = 0; i < chargingStations.size(); i++) {
                    chargingGene[i] = mapTripToGene(
                            new Trip(
                                    "CH" + (i + 1),
                                    "ChargingTrip",
                                    chargingStations.get(i),
                                    "NotStarted",
                                    minChargingTime)
                    );
                }
                return chargingGene;
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
                return null;
            }
        }


        public List<Gene> mutateChargingTimesInRelationToBatteryLevelConsideringPreviousTrips(List<Gene> genes, List<Gene> customergenes) {
            try {
                genes = copyList(genes);
                boolean atLeastOneWasMutated = false;
                while (!atLeastOneWasMutated) {
                    for (int i = 0; i < genes.size(); i++) {
                        Double batteryLevel = getBatteryLevelConsideringPreviousGenes(customergenes, genes, i);
                        Double chargingTimeTillThreshhold = 0.0;
                        if (batteryLevel < 0.2) {
                            chargingTimeTillThreshhold = (0.3 - batteryLevel) * config.getCOMPLETE_CHARGING_TIME();
                        }
                        if (genes.get(i) != null) {
                            Double minChargingTime = config.getMIN_CHARGING_TIME() < chargingTimeTillThreshhold ? chargingTimeTillThreshhold : config.getMIN_CHARGING_TIME();
                            boolean rand = random.nextBoolean();
                            if (rand) {
                                int min = config.getMIN_CHARGING_TIME().intValue();
                                Double temp = (config.getMAX_CHARGING_TIME() * (1 - batteryLevel)) > minChargingTime + 1.0
                                        ? (config.getMAX_CHARGING_TIME() * (1 - batteryLevel))
                                        : minChargingTime + 1.0;
                                int max = temp.intValue();
                                int randomInt = random.nextInt((max - min) + 1) + min;

                                genes.get(i).setChargingTime(Double.valueOf(randomInt));
                                atLeastOneWasMutated = true;
                            }
                        }
                    }
                }


//                int startSize = genes.size();
//                List<Integer> chargingIndeces = findChargingStations(genes);
//                if (chargingIndeces.size() == 0) {
//                    return genes;
//                }
//                int changeAmount = random.nextInt(chargingIndeces.size());
//                for (int i = 0; i < changeAmount; i++) {
//                    // get random charging gene
//                    int index = chargingIndeces.get(random.nextInt(chargingIndeces.size()));
//                    Gene chargingGene = genes.get(index);
//
//                    Gene copy = new Gene(
//                            chargingGene.getId(),
//                            chargingGene.getStart(),
//                            chargingGene.getEnd(),
//                            null,
//                            chargingGene.getChargingTime(),
//                            config
//                    );
//                    // get random charging time between mincharging time and max charging time
//                    Double randomChargingTime = ThreadLocalRandom.current().nextDouble(
//                            config.getMIN_CHARGING_TIME(),
//                            (config.getMAX_CHARGING_TIME() * (1 - config.getBatteryLevel())) > config.getMIN_CHARGING_TIME() + 1.0 ? (config.getMAX_CHARGING_TIME() * (1 - config.getBatteryLevel())) : config.getMIN_CHARGING_TIME() + 1.0
//                    );
//                    copy.setChargingTime(randomChargingTime);
//                    genes.set(index, copy);
//                }
//
//                if (genes.size() != startSize) {
//                    System.out.println("Caught Exception in MutateChargingTimes");
//                }
//
                return genes;
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
            }
            return null;
        }

        private Double getBatteryLevelConsideringPreviousGenes(List<Gene> customerGenes, List<Gene> chargingGenes, int i) {
            // i = 0 no previous trip, only to start location, i = 1 one charging one customer possible, i = 2 two charging to customer
            Double startBattery = config.getBatteryLevel();
            if (i == 0 && chargingGenes.get(i) != null) {
                Double distance = (Location.distanceBetween(getStartLocation(), chargingGenes.get(i).start) * config.DISTANCE_FACTOR);
                startBattery -= distance * config.getCHARGE_DECREASE();
            } else {
                // get locations list calculate distance
                List<Location> locations = getLocationsOfGenes(customerGenes, chargingGenes, i);
                Double distance = getTotalDistanceOfLocationList(locations);
                startBattery -= distance * config.getCHARGE_DECREASE();

                // get charging times till i
                Double chargingTimes = 0.0;
                for (int j = 0; j < i; j++) {
                    if (chargingGenes.get(j) != null)
                    chargingTimes += chargingGenes.get(j).getChargingTime();
                }
                startBattery += chargingTimes * config.getCHARGE_INCREASE();
            }
            return startBattery;
        }

        private List<Location> getLocationsOfGenes(List<Gene> customerGenes, List<Gene> chargingGenes, int i) {
            List<Location> locations = new ArrayList<>();
            for (int j = 0; j < i; j++) {
                if (chargingGenes.get(j) != null) {
                    locations.add(chargingGenes.get(j).getStart());
                }
                locations.add(customerGenes.get(j).getStart());
                locations.add(customerGenes.get(j).getEnd());
            }
            if (chargingGenes.get(i) != null) {
                locations.add(chargingGenes.get(i).getStart());
            }
            return locations;
        }

        public Double getTotalDistanceOfLocationList(List<Location> locs) {
            Double result = 0.0;
            for (int i = 0; i < locs.size() - 1; i++) {
                result += Location.distanceBetween(locs.get(i), locs.get(i + 1)) * DISTANCE_FACTOR;
            }
            return result;
        }

        private Location getStartLocation() {
            Location location = agentLocation;
            if (currentTrip.size() > 0) {
                switch (currentTrip.get(0).getTripType()) {
                    case "AtStartLocation":
                        location = currentTrip.get(0).getStartPosition();
                        break;
                    case "DriveToEnd":
                        // überabschätzung
                        location = currentTrip.get(0).getStartPosition();
                        break;
                    case "AtEndLocation":
                        location = currentTrip.get(0).getEndPosition();
                        break;
                    default:
                        location = agentLocation;
                        break;
                }
            }
            return location;
        }

        public List<Gene> mutateChargingTimes(List<Gene> genes) {
            try {
                boolean atLeastOneWasMutated = false;
                while (!atLeastOneWasMutated) {
                    for (int i = 0; i < genes.size(); i++) {
                        if (genes.get(i) != null) {
                            boolean rand = random.nextBoolean();
                            if (rand) {
                                int min = config.getMIN_CHARGING_TIME().intValue();
                                Double temp = (config.getMAX_CHARGING_TIME() * (1 - config.getBatteryLevel())) > config.getMIN_CHARGING_TIME() + 1.0 ? (config.getMAX_CHARGING_TIME() * (1 - config.getBatteryLevel())) : config.getMIN_CHARGING_TIME() + 1.0;
                                int max = temp.intValue();
                                int randomInt = random.nextInt((max - min) + 1) + min;

                                genes.get(i).setChargingTime(Double.valueOf(randomInt));
                                atLeastOneWasMutated = true;
                            }
                        }
                    }
                }


//                int startSize = genes.size();
//                List<Integer> chargingIndeces = findChargingStations(genes);
//                if (chargingIndeces.size() == 0) {
//                    return genes;
//                }
//                int changeAmount = random.nextInt(chargingIndeces.size());
//                for (int i = 0; i < changeAmount; i++) {
//                    // get random charging gene
//                    int index = chargingIndeces.get(random.nextInt(chargingIndeces.size()));
//                    Gene chargingGene = genes.get(index);
//
//                    Gene copy = new Gene(
//                            chargingGene.getId(),
//                            chargingGene.getStart(),
//                            chargingGene.getEnd(),
//                            null,
//                            chargingGene.getChargingTime(),
//                            config
//                    );
//                    // get random charging time between mincharging time and max charging time
//                    Double randomChargingTime = ThreadLocalRandom.current().nextDouble(
//                            config.getMIN_CHARGING_TIME(),
//                            (config.getMAX_CHARGING_TIME() * (1 - config.getBatteryLevel())) > config.getMIN_CHARGING_TIME() + 1.0 ? (config.getMAX_CHARGING_TIME() * (1 - config.getBatteryLevel())) : config.getMIN_CHARGING_TIME() + 1.0
//                    );
//                    copy.setChargingTime(randomChargingTime);
//                    genes.set(index, copy);
//                }
//
//                if (genes.size() != startSize) {
//                    System.out.println("Caught Exception in MutateChargingTimes");
//                }
//
                return genes;
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
            }
            return null;
        }

        // helper functions
        private List<Integer> findChargingStations(List<Gene> genes) {
            try {
                List<Integer> indeces = new ArrayList<>();
                for (int i = 0; i < genes.size(); i++) {
                    if (genes.get(i) != null) {
                        indeces.add(i);
                    }
                }
                return indeces;
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
                return null;
            }
        }

        private void removeElements(List<Gene> genes, int size) {
            try {
                while (genes.size() > size) {
                    System.out.println("While");
                    int randomIndex = random.nextInt(genes.size());
                    int currentIndex = 0;
                    Iterator iterator = genes.iterator();
                    while (iterator.hasNext()) {
                        iterator.next();
                        if (currentIndex == randomIndex) {
                            iterator.remove();
                            break;
                        }
                        currentIndex++;
                    }
                }
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
            }

        }
    }

    public class Gene {

        private final String id;
        // represents a trip (customer or charging trip)
        private final Location start;
        // end is null in case of charging trip
        private final Location end;
        private final LocalDateTime bookingTime;
        private Double chargingTime = null;
        private Config config;

        public Gene(String id, Location start, Location end, LocalDateTime bookingTime, Double chargingTime, Config config) {
            this.id = id;
            this.start = start;
            this.end = end;
            this.bookingTime = bookingTime;
            this.chargingTime = chargingTime;
            this.config = config;
        }

        // take type of trip into account

        public double distance(Gene gene) {
            try {
                if (end == null) {
//                    Double test = getDrivingDistanceBetweenToNodes(start, gene.start, JadexModel.simulationtime);
                    return Location.distanceBetween(start, gene.start) * config.DISTANCE_FACTOR;
                } else {
//                    Double test = getDrivingDistanceBetweenToNodes(end, gene.start, JadexModel.simulationtime);
                    return Location.distanceBetween(end, gene.start) * config.DISTANCE_FACTOR;
                }
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
                return 0.0;
            }
        }

        public Gene createDeepCopy() {
            return new Gene(
                    id,
                    start,
                    end,
                    bookingTime,
                    chargingTime,
                    config
            );
        }

        public Location getStart() {
            return start;
        }

        public Location getEnd() {
            return end;
        }

        public Double getChargingTime() {
            return chargingTime;
        }

        public void setChargingTime(Double chargingTime) {
            this.chargingTime = chargingTime;
        }

        public Double calculateWaitingTime(List<Gene> otherGenes) {
            try {
                Gene agentGene = new Gene(null, config.getAgentLocation(), null, null, null, config);
                Double waitingTime = calculateTravelTime(distance(agentGene), config.getDRIVING_SPEED());;
                if (chargingTime == null) { return waitingTime; }
                // travel time
                for (Gene gene : otherGenes) {
                    waitingTime += calculateTravelTime(distance(gene), config.getDRIVING_SPEED());
                    if (gene.getChargingTime() != null) {
                        waitingTime = gene.getChargingTime();
                    }
                }
                return waitingTime;
            } catch (Exception e) {
                System.out.println("Caught exception");
                e.printStackTrace();
                return 0.0;
            }
        }

        public String getId() {
            return id;
        }
    }

    public class Config {
        private LocalDateTime simulationTime;
        private Double DRIVING_SPEED;
        private Double MIN_CHARGING_TIME;
        private Double MAX_CHARGING_TIME;

        public Double getCOMPLETE_CHARGING_TIME() {
            return COMPLETE_CHARGING_TIME;
        }

        public void setCOMPLETE_CHARGING_TIME(Double COMPLETE_CHARGING_TIME) {
            this.COMPLETE_CHARGING_TIME = COMPLETE_CHARGING_TIME;
        }

        private Double COMPLETE_CHARGING_TIME;
        private Double batteryLevel;
        private Location agentLocation;
        private Double THETA;
        private List<Location> chargingStations;
        private Double DISTANCE_FACTOR;
        private Double CHARGE_INCREASE;

        public Double getCHARGE_DECREASE() {
            return CHARGE_DECREASE;
        }

        public void setCHARGE_DECREASE(Double CHARGE_DECREASE) {
            this.CHARGE_DECREASE = CHARGE_DECREASE;
        }

        public Double getCHARGE_INCREASE() {
            return CHARGE_INCREASE;
        }

        public void setCHARGE_INCREASE(Double CHARGE_INCREASE) {
            this.CHARGE_INCREASE = CHARGE_INCREASE;
        }

        private Double CHARGE_DECREASE;


        public Config() { }

        public Double getDISTANCE_FACTOR() {
            return DISTANCE_FACTOR;
        }

        public void setDISTANCE_FACTOR(Double DISTANCE_FACTOR) {
            this.DISTANCE_FACTOR = DISTANCE_FACTOR;
        }
        // to prevent instantiation

        public LocalDateTime getSimulationTime() {
            return simulationTime;
        }

        public void setSimulationTime(LocalDateTime simulationTime) {
            this.simulationTime = simulationTime;
        }

        public Double getDRIVING_SPEED() {
            return DRIVING_SPEED;
        }

        public void setDRIVING_SPEED(Double DRIVING_SPEED) {
            this.DRIVING_SPEED = DRIVING_SPEED;
        }

        public Double getMIN_CHARGING_TIME() {
            return MIN_CHARGING_TIME;
        }

        public void setMIN_CHARGING_TIME(Double MIN_CHARGING_TIME) {
            this.MIN_CHARGING_TIME = MIN_CHARGING_TIME;
        }

        public Double getBatteryLevel() {
            return batteryLevel;
        }

        public void setBatteryLevel(Double batteryLevel) {
            this.batteryLevel = batteryLevel;
        }

        public Location getAgentLocation() {
            return agentLocation;
        }

        public void setAgentLocation(Location agentLocation) {
            this.agentLocation = agentLocation;
        }

        public Double getTHETA() {
            return THETA;
        }

        public void setTHETA(Double THETA) {
            this.THETA = THETA;
        }

        public List<Location> getChargingStations() {
            return chargingStations;
        }

        public void setChargingStations(List<Location> chargingStations) {
            this.chargingStations = chargingStations;
        }

        public Double getMAX_CHARGING_TIME() {
            return MAX_CHARGING_TIME;
        }

        public void setMAX_CHARGING_TIME(Double MAX_CHARGING_TIME) {
            this.MAX_CHARGING_TIME = MAX_CHARGING_TIME;
        }
    }

    public long calculateWaitingTime(LocalDateTime bookingTime, LocalDateTime simulationTime) {
        return Duration
                .between(bookingTime, simulationTime)
                .getSeconds();
    }

    public double calculateTravelTime(Double distance, Double DRIVING_SPEED) {
        // kmh / 100 => meter => meter / driving speed = h => h / 3600 = sec
        return ((distance / 1000) / DRIVING_SPEED) * 60 * 60;
    }
}