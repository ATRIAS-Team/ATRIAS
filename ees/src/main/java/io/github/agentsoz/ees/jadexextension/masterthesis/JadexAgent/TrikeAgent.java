/* TrikeAgnet.java
 * Version: v0.9 (16.02.2024)
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
import jadex.bdiv3.runtime.IPlan;
import jadex.commons.future.IFuture;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private String agentID = null; // store agent ID from the map
    @Belief
    public boolean sent = false;
    @Belief
    public String write = null;

    @Belief
    Double drivingSpeed = 6.0;

    @Belief
    Double theta = 900.0; //allowed waiting time for customers todo: use it iside customer miss
    @Belief
    protected boolean daytime; //Battery -oemer
    @Belief
    public double sumLinkLength =0.0;
    @Belief
    public BatteryModel trikeBattery = new BatteryModel();

    public csvLogger csvLogger = new csvLogger();


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
    @Belief
    public String my_chargestate = "1.0"; //Battery -oemer
    @Belief
    public String chargingTripAvailable = "0"; //Battery -oemer


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
        //bdiFeature.dispatchTopLevelGoal(new MaintainBatteryLoaded()); //Battery -oemer
        // bdiFeature.dispatchTopLevelGoal(new AchieveMoveTo()); //Battery -oemer


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
        //  my_chargestate = "0.5";
    }

    @Goal(recur = true)
    public class MaintainBatteryLoaded{
        //charging trip global prüfen, available dann 1, sonst 0 setzen.
        @GoalCreationCondition(factchanged = "my_chargestate") // ||  chargingTripAvailable
        public MaintainBatteryLoaded(){
        }
    }

    // @Goal(recur = true, recurdelay = 300)
    // public class MaintainBatteryLoaded{
    //charging trip global prüfen, available dann 1, sonst 0 setzen.
    //     @GoalCreationCondition(beliefs = "my_chargestate < 0.5" && "chargingTripAvailable == 0")
    // }

    //(Goal 1) Battery-oemer
    // @Goal(deliberation=@Deliberation(inhibits={}))
    // public class MaintainBatteryLoaded
    // {
    /**
     *  Author: Gürbüz, Ekin Kafkas
     *  ..................................................
     *  If it is at night or if the charge state is
     *  below 0.2, the trike agent will activate this goal.
     */
    //    @GoalMaintainCondition(beliefs="my_chargestate")
    //    public boolean checkMaintain()
    //    {
    //        //bool available charging trip hinzufügen
    //        return daytime == true && my_chargestate > 0.2;
    //    }

    /**
     *  The target condition determines when
     *  the goal goes back to idle.
     */
    //     @GoalTargetCondition(beliefs="my_chargestate")
    //     public boolean checkTarget()
    //     {
    //         return my_chargestate >= 1.0;
    //     }

    /**
     *  Author: Gürbüz, Ekin Kafkas
     *  ..................................................
     *  Suspend the goal when the trike agent
     *  carries a customer.
     */
//	    @GoalContextCondition (beliefs="carriedcustomer")
//		public boolean checkContext()
//		{
//			return carriedcustomer == null;
//		}
    // }

    //(Plan 1) Battery- oemer
    @Plan(trigger=@Trigger(goals=MaintainBatteryLoaded.class))
    public class NewChargingTrip
    {
        @PlanBody
        public void body()
        //TrikeAgent agentapi, IPlan planapi
        {
            if(chargingTripAvailable.equals("0")){
                Location LocationCh= new Location("", 476530.26535798033, 5552438.979076344);
                Trip chargingTrip = new Trip("CH01", "ChargingTrip", LocationCh, "NotStarted");
                tripList.add(chargingTrip);
                chargingTripAvailable = "1";
            }
        }
    }


    //(Goal 2) Battery -oemer
    //  @Goal
    //  public class AchieveMoveTo
    //  {
    /** The location. */
    //  protected Location location;

    /**
     *  Create a new goal.
     *  @param location The location.
     */
    //    public AchieveMoveTo(Location location)
    //   {
    //		System.out.println("created: "+location);
    //        this.location = location;
    //    }
    /**
     *  The goal is achieved when the position
     *  of the trike is near to the target position.
     */
    // @GoalTargetCondition//(beliefs="my_location")
    // public boolean checkTarget()
    // {
    //   return agentLocation.isNear(location);
    //}

    /**
     *  Author: Gürbüz, Ekin Kafkas
     *  ..................................................
     *  Suspend the goal if the trike agent
     *  is being charged at night, because the trikes
     *  are not going on any customer trips at night.
     */
    //  @GoalContextCondition//(beliefs="carriedcustomer")
    //  public boolean checkContext()
    //  {
    //     if (daytime == false)
    //      {
    //          return my_chargestate < 1.0;
    //      }
    //      else return my_chargestate <= 1.0;
    //  }

    /**
     *  Get the location.
     *  @return The location.
     */
    //     public Location getLocation()
    //     {
    //          return location;
    //      }
    // }

    //(Plan 2) Battery -oemer
    //   @Plan(trigger=@Trigger(goals=AchieveMoveTo.class))
    //  public class MoveToLocationPlan {
    //   @PlanCapability
    //  protected TrikeAgent capa;
    //   @PlanAPI
    //   protected IPlan rplan;
    //    @PlanReason
    //    protected AchieveMoveTo goal;

    //-------- constructors --------

    /**
     * Create a new plan.
     */
    //   public MoveToLocationPlan() {
    //capa.getAgent().getLogger().info("Created: "+this);
    //   }
    //-------- methods --------
    /**
     * The plan body.
     */
    //  @PlanBody
    //   public IFuture<Void> body() {

    //     trikeBattery.moveToTarget();
    //     trikeBattery.oneStepToTarget();
    //      return trikeBattery.moveToTarget();
    //     }
    // }

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
            Double ownScore = calculateUtillity(decisionTaskList.get(position));
            //ownScore = 0.0; //todo: delete this line after the implementation of the cnp
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
            // just a temporary solution for the paper
            //decisionTaskList.get(position).setStatus("delegated"); //todo: delete this line
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
    Double calculateUtillity(DecisionTask newTask){
        Double utillityScore = 100.0;
        /**


         newTask.getStartPositionFromJob();
         newTask.getEndPositionFromJob();
         newTask.getVATimeFromJob();

         Double a = 1.0/3.0;
         Double b = 1.0/3.0;
         Double c = 1.0/3.0;
         Double uPunctuallity = null;
         Double uBattery = null;
         Double uDistance = null;

         //###########################################################
         // punctuallity
         // arrival delay to arrive at the start position when started from the agentLocation
         if (currentTrip.size()==0 && tripList.size()==0){
            //agentLocation
            Double distanceToStart = Location.distanceBetween(agentLocation, newTask.getStartPositionFromJob());
            //Double vATimeNewTask = timeInSeconds(newTask.getVATimeFromJob());
            Double timeToNewTask = distanceToStart/drivingSpeed*1000; //in this case equals the delay as vatiem is bookingtime
            // transforms the delay in seconds into as score beween 0 and 100 based of the max allowed delay of 900s
            uPunctuallity = Math.min(100.0,(100.0-(((Math.min(theta, timeToNewTask)-0.0)/(theta-0.0))*100.0)) );
         }
         // arrival delay to arrive at the start position when performed the last trip in pipe (LTIP) before starting at its start position at its vaTime
         else if (currentTrip.size()>0 || tripList.size()>0) {
             Double distanceToEnd_LTIP = 0.0;
             Double timeToEnd_LTIP = null;
         //todo: hier nach trip type unterscheiden
             if (getLastTripInPipeline().getTripType().equals("CustomerTrip")){
                 distanceToEnd_LTIP = Location.distanceBetween(getLastTripInPipeline().getStartPosition(), getLastTripInPipeline().getEndPosition());
                 timeToEnd_LTIP = distanceToEnd_LTIP / drivingSpeed * 1000;
             }
             else if (getLastTripInPipeline().getTripType().equals("chargingTrip")){
                 Double ChargingTime = 0.0; //TODO: @Ömer Ladezeit addieren
                 timeToEnd_LTIP = (distanceToEnd_LTIP / drivingSpeed * 1000) + ChargingTime;
             }
             //Double timeToEnd_LTIP = distanceToEnd_LTIP / drivingSpeed * 1000;
             Double vATime_LTIP = timeInSeconds(getLastTripInPipeline().getVATime());
             //todo: was tun wenn charging trip oder ähnliches? keien VA time
             Double timeAtEnd_LTIP = vATime_LTIP + timeToEnd_LTIP;
             //todo: maybe boarding time?
             Double distanceToStart = Location.distanceBetween(getLastTripInPipeline().getEndPosition(), newTask.getStartPositionFromJob());
             Double timeToNewTask = distanceToStart / drivingSpeed * 1000;
             Double vATimeNewTask = timeInSeconds(newTask.getVATimeFromJob());
             Double delayArrvialNewTask = Math.max((timeAtEnd_LTIP + timeToNewTask - vATimeNewTask), timeToNewTask);

             uPunctuallity = Math.min(100.0, (100.0 - (((Math.min(theta, delayArrvialNewTask) - 0.0) / (theta - 0.0)) * 100.0)));

         // unterscheidung charging trip

         //if (currentTrip.get(0).getTripType().equals("customerTrip)")){
         //if (currentTrip.get(0).getTripType().equals("chargingTrip)")){
         }


         //###########################################################
         // Battery
         //todo: battery from Ömer needed
         // differ between trips with and without customer???
         Double currentBatteryLevel = 100.0; //todo: use real battery
         Double estBatteryLevelAfter_TIP = trikeBattery.getMyChargestate();
         Double estEnergyConsumption = 0.0;
         Double estEnergyConsumption_TIP = 0.0;
         Double totalDistance_TIP = 0.0;
         Double negativeInfinity = Double.NEGATIVE_INFINITY;
         Double bFactor = null;
         //todo ennergieverbrauch für zu evuluierenden job bestimmen

         //calculation of the estimatedEnergyConsumtion (of formertrips)


         if (currentTrip.size()==1){ //battery relavant distance driven at currentTrip
         //todo: fortschritt von currenttrip berücksichtigen
            totalDistance_TIP +=Location.distanceBetween(agentLocation, currentTrip.get(0).getStartPosition());
            if (currentTrip.get(0).getTripType().equals("customerTrip")){ //only drive to the end when it is a customerTrip
                totalDistance_TIP +=Location.distanceBetween(currentTrip.get(0).getStartPosition(), currentTrip.get(0).getEndPosition());
            }
            if (currentTrip.get(0).getTripType().equals("ChargingTrip")){
                totalDistance_TIP = 0.0; //reset the distance until now because only the distance after a chargingTrip influences the battery
            }
         }
         // battery relavant distance driven at tripList
         if (tripList.size() > 0) {
            if (currentTrip.size()>0){ //journey to the first entry in the tripList from a currentTrip
                if (currentTrip.get(0).getTripType().equals("customerTrip")){
                    totalDistance_TIP +=Location.distanceBetween(currentTrip.get(0).getEndPosition(), tripList.get(0).getStartPosition());
                }
                else{ // trips with only a start position
                    totalDistance_TIP +=Location.distanceBetween(currentTrip.get(0).getStartPosition(), tripList.get(0).getStartPosition());
                }
            }
            else{ //journey to the first entry in the tripList from the agentLocation
            totalDistance_TIP +=Location.distanceBetween(agentLocation, tripList.get(0).getStartPosition());
            }
            // distance driven at TripList.get(0)
            if (tripList.get(0).getTripType().equals("customerTrip")) {
                totalDistance_TIP += Location.distanceBetween(tripList.get(0).getStartPosition(), tripList.get(0).getEndPosition());
            }
            if (tripList.get(0).getTripType().equals("chargingTrip")) {
                 totalDistance_TIP = 0.0;
            }
            else{
                 // do nothing as all other Trips with only a startPosition will not contain any other movements;
            }


            //todo: fahrt zum nächjsten start fehlt +-1 bei i???
             // interates through all other Trips inside TripList
            for (int i=1; i<tripList.size()-1; i++){
                if (currentTrip.get(i-1).getTripType().equals("customerTrip")) {
                 totalDistance_TIP += Location.distanceBetween(tripList.get(i-1).getEndPosition(), tripList.get(i).getStartPosition());
                }
                else{ // Trips with only a startPosition
                    totalDistance_TIP += Location.distanceBetween(tripList.get(i-1).getStartPosition(), tripList.get(i).getEndPosition());
                }
                if (currentTrip.get(i).getTripType().equals("customerTrip")) {
                    totalDistance_TIP += Location.distanceBetween(tripList.get(i).getStartPosition(), tripList.get(i).getEndPosition());
                }
            }
         }
         //todo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! RICHTIGE WERTE ZUGREIFEN
         estEnergyConsumption_TIP = trikeBattery.SimulateDischarge(totalDistance_TIP);//todo: @Ömer ist es das was wir hier brauchen?
         estBatteryLevelAfter_TIP = currentBatteryLevel - estEnergyConsumption_TIP;//todo: @Ömer ist es das was wir hier brauchen?

         //###########################################################
         // calculation of uBattery
         if (estBatteryLevelAfter_TIP<estEnergyConsumption){
         uBattery = negativeInfinity;
         }
         else {

         if (estBatteryLevelAfter_TIP > 0.8){
         bFactor = 1.0;
         }
         else if (estBatteryLevelAfter_TIP >= 0.3){
         bFactor = 0.75;
         }
         else if (estBatteryLevelAfter_TIP < 0.3){
         bFactor = 0.1;
         }
         // ???? batteryLevelAfterTrips or 100?
         uBattery = bFactor * ((estBatteryLevelAfter_TIP - estEnergyConsumption)/ estBatteryLevelAfter_TIP);

         }
         //###########################################################
         //Distance
         Double dmax = 3000.0; //todo: @Ömer RICHTIGEN WERT EINTRAGEN!!!!!!!!!!!!!!!
         Double distanceToStart;

         if (tripList.size()==0 && currentTrip.size()==0){
         distanceToStart = Location.distanceBetween(agentLocation, newTask.getStartPositionFromJob());
         }
         else{
             if(getLastTripInPipeline().getTripType().equals("customerTrip")){
                 distanceToStart = Location.distanceBetween(getLastTripInPipeline().getEndPosition(), newTask.getStartPositionFromJob());
             }
             else{
                 distanceToStart = Location.distanceBetween(getLastTripInPipeline().getStartPosition(), newTask.getStartPositionFromJob());
             }
         }
         uDistance = dmax - distanceToStart/dmax;

         //###########################################################



         // calculate the total score

        utillityScore = Math.max(0.0, (a * uPunctuallity + b * uBattery + c * uDistance));**/
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


                    agentLocation = new Location("", 476721.007399257, 5552121.23354965);

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
        double metersDriven = Double.parseDouble((String) SimActionList.get(0).getParameters()[1]);

        //Transport ohne Kunde
        if (CurrentTripUpdate.getProgress().equals("DriveToStart")) {
            updateCurrentTripProgress("AtStartLocation");
            agentLocation = CurrentTripUpdate.getStartPosition();
            trikeBattery.discharge(metersDriven, 0);
        }


        //Transport mit Kunde
        if (CurrentTripUpdate.getProgress().equals("DriveToEnd")){
            updateCurrentTripProgress("AtEndLocation");
            agentLocation = CurrentTripUpdate.getEndPosition();
            trikeBattery.discharge(metersDriven, 1);
        }
        /**
         * TODO: @Mariam update firebase after every MATSim action: location of the agent
         */
        System.out.println("Neue Position: " + agentLocation);
        sendAreaAgentUpdate("update");

        csvLogger.logTrip();

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

    //TODO: @oemer check if it will work correctly

    //todo: access simulation time and determine if the customer has already left
    // SimZeit abfragen, gewünschte VATime vergleichen und wenn mehr als 5 min, dann missed, oder würfel, je länger desto wahrscheinlicher
    // statt estimated Duration, echte Duration verwenden, die Berechnung soll erfolgen, wenn der Trike Agent beim Kunden ankommt. --> Wichtig für den Reward
    // drive to customer, check simulation time and start time(vaTime) and define delta
    // 1. option: if delta > 5 minutes then --> missed else success
    // 2. option: if delta > 10 probability higher that it is missed


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
    };

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
                //added -oemer
                if (currentTrip.get(0).getTripType().equals("ChargingTrip")) {
                    trikeBattery.loadBattery();
                    updateCurrentTripProgress("Finished");
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
            if (currentTrip.get(0).getProgress().equals("Finished") || currentTrip.get(0).getProgress().equals("Failed")) {
                currentTrip.remove(0); //dodo: check if it does really work or if position.intValue() needed
                //todo: commetn in if error
                //tripList.remove(0);
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








