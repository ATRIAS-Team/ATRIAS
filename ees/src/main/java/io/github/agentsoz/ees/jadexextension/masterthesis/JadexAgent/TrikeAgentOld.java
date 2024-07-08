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
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.WritingIDService;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.TrikeMain;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.IMappingAgentsService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.INotifyService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.TrikeAgentReceiveService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.TrikeAgentSendService;
import io.github.agentsoz.util.Location;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

public class TrikeAgentOld implements SendtoMATSIM{

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
    Boolean CNP_ACTIVE = true;
    Double THETA = 900.0; //allowed waiting time for customers.
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

    @Goal(recur = true, recurdelay = 100)
    public class MaintainBatteryLoaded {
        @GoalCreationCondition(factchanged = "estimateBatteryAfterTIP") //
        public MaintainBatteryLoaded() {
        }
    }

    @Plan(trigger = @Trigger(goals = MaintainBatteryLoaded.class))
    public void NewChargingTrip() {
        {
            if (estimateBatteryAfterTIP.get(0) < CHARGING_THRESHOLD && chargingTripAvailable.equals("0")){
                //estimateBatteryAfterTIP();
                //Location LocationCh = new Location("", 476530.26535798033, 5552438.979076344);
                //Location LocationCh = new Location("", 476224.26535798033, 5552487.979076344);
                chargingTripCounter+=1;
                String tripID = "CH";
                tripID = tripID.concat(Integer.toString(chargingTripCounter));
                Trip chargingTrip = new Trip(tripID, "ChargingTrip", getNextChargingStation(), "NotStarted");
                tripList.add(chargingTrip);
                tripIDList.add("1");
                chargingTripAvailable = "1";
            }
        }
    }

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




        /**
         * todo: will replace solution above
         */
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
            Trip newTrip = new Trip(decisionTaskList.get(position), dTaToTrip.getIDFromJob(), "CustomerTrip", dTaToTrip.getVATimeFromJob(), dTaToTrip.getStartPositionFromJob(), dTaToTrip.getEndPositionFromJob(), "NotStarted");
            //TODO: create a unique tripID
            tripList.add(newTrip);
            tripIDList.add("1");
            estimateBatteryAfterTIP();

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


        if (chargingTripAvailable.equals("0")) {


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
            Double vaTimeFirstTrip = null;
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
                    uPunctuality = 100.0 - ((100.0 * timeToNewTask - THETA)/THETA);
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
                        vaTimeFirstTrip = timeInSeconds(tripList.get(0).getVATime()); //fist VATime when there was no CurrentTrip
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
                    uPunctuality = 100.0 - ((100.0 * delayArrvialNewTask - THETA)/THETA);
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
            prepareLog(currentTrip.get(0), "0.0", "0.0", "false", "0.0");
            currentTrip.get(0).setProgress("Failed");
            currentTrip.remove(0);



        }
        if (tripList.size() > 0){
            while (tripList.size() > 0) {
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

    //TODO: @oemer check if it will work correctly

    //todo: access simulation time and determine if the customer has already left
    // SimZeit abfragen, gewünschte VATime vergleichen und wenn mehr als 5 min, dann missed, oder würfel, je länger desto wahrscheinlicher
    // statt estimated Duration, echte Duration verwenden, die Berechnung soll erfolgen, wenn der Trike Agent beim Kunden ankommt. --> Wichtig für den Reward
    // drive to customer, check simulation time and start time(vaTime) and define delta
    // 1. option: if delta > 5 minutes then --> missed else success
    // 2. option: if delta > 10 probability higher that it is missed




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

    /**
     *  handles the progress of the current Trip
     */
    public void ExecuteTrips() {
        System.out.println("DoNextTrip running");
        System.out.println("tripList of agent" +agentID+ " :"+ tripList.size());
        System.out.println("currentTrip: " + currentTrip.size());
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
                    trikeBattery.loadBattery();
                    updateCurrentTripProgress("Finished");
                    chargingTripAvailable = "0";
                }
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
        estimateBatteryAfterTIP();
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


}