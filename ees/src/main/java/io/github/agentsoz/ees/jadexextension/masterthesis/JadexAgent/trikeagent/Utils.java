package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.trikeagent;

import com.google.api.core.ApiFuture;
import com.google.firebase.database.*;
import io.github.agentsoz.bdiabm.v3.AgentNotFoundException;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.ees.firebase.FirebaseHandler;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.*;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.util.Location;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.search.ServiceQuery;
import org.w3c.dom.Element;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService.messageToService;
import static io.github.agentsoz.ees.jadexextension.masterthesis.Run.XMLConfig.getClassField;


public class Utils {
    private final TrikeAgent trikeAgent;

    private String oldCellAddress = null;
    private String newCellAddress = null;
    
    public Utils(TrikeAgent trikeAgent){
        this.trikeAgent = trikeAgent;
    }

    public int selectNextAction(int index){
        int changes = 0;
        DecisionTask currentDecisionTask = trikeAgent.decisionTaskList.get(index);

        switch (currentDecisionTask.getStatus()) {
            case "new": {
                //  Execute Utillity here > "commit"|"delegate"

                double ownScore = calculateUtility(currentDecisionTask);
                currentDecisionTask.setUtillityScore(trikeAgent.agentID, ownScore);
                if (ownScore < TrikeConstants.commitThreshold && TrikeConstants.CNP_ACTIVE) {
                    currentDecisionTask.setStatus("delegate");
                } else {
                    currentDecisionTask.setStatus("commit");
                    String timeStampBooked = new SimpleDateFormat("HH.mm.ss.ms").format(new java.util.Date());
                    System.out.println("FINISHED Negotiation - JobID: " + currentDecisionTask.getJobID() + " TimeStamp: " + timeStampBooked);
                }
                changes += 1;
                break;
            }
            case "commit": {
                Trip newTrip = new Trip(currentDecisionTask, currentDecisionTask.getIDFromJob(), "CustomerTrip",
                        currentDecisionTask.getVATimeFromJob(), currentDecisionTask.getStartPositionFromJob(),
                        currentDecisionTask.getEndPositionFromJob(), "NotStarted");
                //TODO: create a unique tripID
                trikeAgent.tripList.add(newTrip);

                if(TrikeConstants.FIREBASE_ENABLED){
                    //  listen to the new child in firebase
                    ChildEventListener childEventListener = trikeAgent.firebaseHandler.childAddedListener("trips/"+newTrip.tripID, (dataSnapshot, previousChildName, list)->{
                        System.out.println(dataSnapshot);
                        // Iterate through messages under this trip
                        for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                            String message = (String) messageSnapshot.child("message").getValue();

                            // Check if the message is the specific question
                            if ("How many trips are scheduled before mine?".equals(message)) {
                                DatabaseReference parent = dataSnapshot.getRef().getParent();
                                String tripId = parent.getKey();
                                int numberOfTrips = 0;
                                System.out.println(list);
                                for(int i = 0; i < list.size(); i++){
                                    if(list.get(i).getTripID().equals(tripId)){
                                        numberOfTrips = i;
                                        break;
                                    }
                                }


                                // Push a new message node under the 'messages' node
                                DatabaseReference newMessageRef = dataSnapshot.getRef().push();

                                // Set the message content
                                newMessageRef.child("message").setValueAsync("Number of trips before yours: " + numberOfTrips);

                                // Add other necessary fields, e.g., sender and timestamp if needed
                                newMessageRef.child("sender").setValueAsync("agent");


                                // Remove the question message from Firebase
                                messageSnapshot.getRef().removeValue(new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            // Handle the error
                                            System.err.println("Error removing question message: " + databaseError.getMessage());
                                        }
                                    }
                                });
                            }
                        }
                    });

                    trikeAgent.listenerHashMap.put(newTrip.getTripID(), childEventListener);


                    //test
                    DatabaseReference tripRef = FirebaseDatabase.getInstance().getReference().child("trips").child(newTrip.tripID).child("messages");

                    // Push a new message node under the 'messages' node
                    DatabaseReference newMessageRef = tripRef.push();

                    // Set the message content synchronously
                    try {
                        ApiFuture<Void> messageFuture = newMessageRef.child("message").setValueAsync("How many trips are scheduled before mine?");
                        messageFuture.get(); // Wait for the setValueAsync operation to complete
                        System.out.println("Message set successfully.");
                    } catch (Exception e) {
                        System.err.println("Error setting message: " + e.getMessage());
                    }
                }

                estimateBatteryAfterTIP();

                currentDecisionTask.setStatus("committed");
                trikeAgent.FinishedDecisionTaskList.add(currentDecisionTask);
                trikeAgent.decisionTaskList.remove(index);

                if(TrikeConstants.FIREBASE_ENABLED){
                    FirebaseHandler.assignAgentToTripRequest(newTrip.getTripID(), trikeAgent.agentID);
                }

                changes += 1;
                break;
            }
            case "delegate": {
                // start cnp here > "waitingForNeighbourlist"
                //TODO: neighbour request here
                //TODO: adapt
                // TEST MESSAGE DELETE LATER
                //bool makes sure that the methods below are called only once

                ArrayList<String> values = new ArrayList<>();
                values.add(currentDecisionTask.getJobID()); //todo move into a method
                trikeAgent.decisionTaskList.get(index).setStatus("waitingForNeighbours");

                String areaAgentTag = Cells.cellAgentMap.get(newCellAddress);
                sendMessage(trikeAgent, areaAgentTag, "request", "trikesInArea", values);

                changes += 1;
                break;
            }
            case "readyForCFP": {
                //  send cfp> "waitingForProposals"
                Job JobForCFP = currentDecisionTask.getJob();
                ArrayList<String> neighbourIDs = currentDecisionTask.getNeighbourIDs();
                for (String neighbourID : neighbourIDs) {
                    //todo: klären message pro nachbar evtl mit user:
                    //todo: action values definieren
                    // values: gesammterJob evtl. bereits in area zu triek so vorhanden?
                    //sendMessageToTrike(neighbourIDs.get(i), "CallForProposal", "CallForProposal", JobForCFP.toArrayList());
                    testTrikeToTrikeService(neighbourID, "CallForProposal", "CallForProposal", JobForCFP.toArrayList());
                }

                currentDecisionTask.setStatus("waitingForProposals");
                changes += 1;
                break;
            }
            case "waitingForProposals": {
                //todo: überprüfen ob bereits alle gebote erhalten
                // falls ja ("readyForDecision")
                //todo:
                if (currentDecisionTask.testAllProposalsReceived()) {
                    trikeAgent.decisionTaskList.get(index).setStatus("readyForDecision");
                }
                break;
            }
            case "readyForDecision": {
                /**
                 *  send agree/cancel > "waitingForConfirmations"
                 */
                currentDecisionTask.tagBestScore(trikeAgent.agentID);
                for (int i = 0; i < currentDecisionTask.getUTScoreList().size(); i++) {
                    String bidderID = currentDecisionTask.getUTScoreList().get(i).getBidderID();
                    String tag = currentDecisionTask.getUTScoreList().get(i).getTag();
                    switch (tag) {
                        case "AcceptProposal": {
                            ArrayList<String> values = new ArrayList<>();
                            values.add(currentDecisionTask.getJobID());
                            testTrikeToTrikeService(bidderID, tag, tag, values);
                            currentDecisionTask.setStatus("waitingForConfirmations");
                            break;
                        }
                        case "RejectProposal": {
                            ArrayList<String> values = new ArrayList<>();
                            values.add(currentDecisionTask.getJobID());
                            testTrikeToTrikeService(bidderID, tag, tag, values);
                            break;
                        }
                        case "AcceptSelf": {
                            //todo: selbst zusagen
                            currentDecisionTask.setStatus("commit");
                            String timeStampBooked = new SimpleDateFormat("HH.mm.ss.ms").format(new java.util.Date());
                            System.out.println("FINISHED Negotiation - JobID: " + currentDecisionTask.getJobID() + " TimeStamp: " + timeStampBooked);
                            break;
                        }
                        default: {
                            //todo: print ungültiger tag
                            System.out.println(trikeAgent.agentID + ": invalid UTScoretag");
                            break;
                        }
                    }
                }
                changes += 1;
                break;
            }
            case "readyForConfirmation": {
                /**
                 *  send bid > "commit"
                 */
                changes += 1;
                break;
            }
            case "proposed": {
                /**
                 *  send bid > "waitingForManager"
                 */
                double ownScore = calculateUtility(currentDecisionTask);
                //todo: eigene utillity speichern
                // send bid
                // ursprung des proposed job bestimmen
                ArrayList<String> values = new ArrayList<>();

                values.add(currentDecisionTask.getJobID());
                values.add("#");
                values.add(String.valueOf(ownScore));

                //zb. values = jobid # score
                testTrikeToTrikeService(currentDecisionTask.getOrigin(), "Propose", "Propose", values);
                currentDecisionTask.setStatus("waitingForManager");

                changes += 1;
                break;
            }
            case "notAssigned": {
                //todo in erledigt verschieben
                trikeAgent.FinishedDecisionTaskList.add(currentDecisionTask);
                trikeAgent.decisionTaskList.remove(index);
                break;
            }
            case "waitingForConfirmations": {
                //todo: test timeout here
                // just a temporary solution for the paper
                // workaround for the not workign confirmation
                currentDecisionTask.setStatus("delegated"); //todo: not shure if this is working corect
                trikeAgent.FinishedDecisionTaskList.add(currentDecisionTask); //todo: not shure if this is working corect
                trikeAgent.decisionTaskList.remove(index);//todo: not shure if this is working corect
                break;
            }
            case "waitingForManager": {
                //todo: test timeout here
                break;
            }
            case "committed":{
                System.out.println("should not exist: " + trikeAgent.decisionTaskList.get(index).getStatus());
                //decisionTaskList.remove(0);
                break;
            }
            default: {
                //System.out.println("invalid status: " + decisionTaskList.get(position).getStatus());
                break;
            }
        }
        return changes;
    }

    public void updateAtInputBroker(){
        ServiceQuery<INotifyService2> query = new ServiceQuery<>(INotifyService2.class);
        query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
        query.setServiceTags(trikeAgent.currentSimInputBroker);
        Collection<INotifyService2> service = trikeAgent.agent.getLocalServices(query);
        for (INotifyService2 cs : service) {
            cs.removeTrikeAgentfromActiveList(trikeAgent.agentID);
            System.out.println(" Newly active Agent " + trikeAgent.agentID + "notifies" + trikeAgent.currentSimInputBroker + " that it finished deliberating");
        }
    }

    public void prepareLog(Trip trip, String batteryBefore, String batteryAfter,
                                  String arrivedAtLocation, String distance){
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
        csvLogger.addLog(trikeAgent.agentID, TrikeConstants.CNP_ACTIVE, TrikeConstants.THETA,
                TrikeConstants.ALLOW_CUSTOMER_MISS, TrikeConstants.CHARGING_THRESHOLD, TrikeConstants.commitThreshold,
                TrikeConstants.DISTANCE_FACTOR, "trike:" + trikeAgent.agentID, tripID, driveOperationNumber,
                tripType, batteryBefore, batteryAfter, arrivedAtLocation, distance, arrivalTime, origin);
    }

     // After a succefull action in MATSIm: Updates the progress of the current Trip and the Agent location
        //todo: better get the location from MATSim
     public void updateBeliefAfterAction(double metersDriven) {
        Trip currentTripUpdate = trikeAgent.currentTrip.get(0);
        //double metersDriven = 100.0;
        //Transport ohne Kunde
        String arrivedAtLocation = "true";

        if  (currentTripUpdate.getProgress().equals("DriveToStart")) {
            updateCurrentTripProgress("AtStartLocation");
            trikeAgent.agentLocation = currentTripUpdate.getStartPosition();
            String batteryBefore = Double.toString(trikeAgent.trikeBattery.getMyChargestate()); //todo: vorher schieben
            trikeAgent.trikeBattery.discharge(metersDriven, 0);
            String batteryAfter = Double.toString(trikeAgent.trikeBattery.getMyChargestate());
            //String arrivedAtLocation = "true";
            if (trikeAgent.trikeBattery.getMyChargestate() < 0.0){
                arrivedAtLocation = "false";
                updateCurrentTripProgress("Failed");

            }
            String distance = Double.toString(metersDriven);
            prepareLog (currentTripUpdate, batteryBefore, batteryAfter, arrivedAtLocation, distance);

            if (arrivedAtLocation.equals("false")){
                trikeAgent.currentTrip.remove(0);
                terminateTripList();
            }
        }


        //Transport mit Kunde
        if  (currentTripUpdate.getProgress().equals("DriveToEnd")){
            updateCurrentTripProgress("AtEndLocation");
            trikeAgent.agentLocation = currentTripUpdate.getEndPosition();
            String batteryBefore = Double.toString(trikeAgent.trikeBattery.getMyChargestate()); //todo: vorher schieben
            trikeAgent.trikeBattery.discharge(metersDriven, 1);
            String batteryAfter = Double.toString(trikeAgent.trikeBattery.getMyChargestate());
            if(TrikeConstants.FIREBASE_ENABLED){
                FirebaseHandler.removeChildEventListener("trips/"+trikeAgent.currentTrip.get(0).getTripID(),
                        trikeAgent.listenerHashMap.get (trikeAgent.currentTrip.get(0).getTripID()));
            }
            //String arrivedAtLocation = "true";
            if (trikeAgent.trikeBattery.getMyChargestate() < 0.0){
                arrivedAtLocation = "false";
                updateCurrentTripProgress("Failed");
            }
            String distance = Double.toString(metersDriven);
            prepareLog (currentTripUpdate, batteryBefore, batteryAfter, arrivedAtLocation, distance);

            if (arrivedAtLocation.equals("false")){
                trikeAgent.currentTrip.remove(0);
                terminateTripList();
            }
        }

        if(TrikeConstants.FIREBASE_ENABLED){
            // Update Firebase with the current progress
            FirebaseHandler.sendTripProgress (currentTripUpdate.getTripID(), currentTripUpdate.getProgress());
            /**
             * TODO: @Mariam update firebase after every MATSim action: location of the agent
             */


            System.out.println("Neue Position:" + trikeAgent.agentLocation);
            FirebaseHandler.updateAgentLocation(trikeAgent.agentID, trikeAgent.agentLocation);
        }

        System.out.println("Neue Position: " + trikeAgent.agentLocation);
        sendAreaAgentUpdate("update");


        //todo: action und perceive trennen! aktuell beides in beiden listen! löschen so nicht konsistent!
        //TODO: @Mahkam send Updates to AreaAgent
        currentTripStatus();
    }

    //remove all Trips from trikeAgent.tripList and currenTrip and write them with the logger
    public void terminateTripList(){
        if  (trikeAgent.currentTrip.size() > 1){
            prepareLog (trikeAgent.currentTrip.get(0), "0.0", "0.0", "false", "0.0");
            trikeAgent.currentTrip.get(0).setProgress("Failed");
            trikeAgent.currentTrip.remove(0);
        }
        if (!trikeAgent.tripList.isEmpty()){
            while (!trikeAgent.tripList.isEmpty()) {
                prepareLog(trikeAgent.tripList.get(0), "0.0", "0.0", "false", "0.0");
                trikeAgent.tripList.get(0).setProgress("Failed");
                trikeAgent.tripList.remove(0);
            }
        }
        trikeAgent.trikeBattery.loadBattery();
        trikeAgent.chargingTripAvailable = "0";

        System.out.println("AgentID: " + trikeAgent.agentID + "ALL TRIPS TERMINATED");
    }

    public void newCurrentTrip(){
        if(trikeAgent.currentTrip.isEmpty() && !trikeAgent.tripList.isEmpty()){
            trikeAgent.currentTrip.add(trikeAgent.tripList.remove(0));
        }
    }
    /** Updates the progress of the CurrentTrip
     *
     * @param newProgress
     */
    public void updateCurrentTripProgress(String newProgress) {
        Trip CurrentTripUpdate = trikeAgent.currentTrip.get(0);
        CurrentTripUpdate.setProgress(newProgress);
        trikeAgent.currentTrip.set(0, CurrentTripUpdate);
        currentTripStatus();
    }

    public void currentTripStatus() {
        if (!trikeAgent.currentTrip.isEmpty()){
            String agentID = trikeAgent.getAgentID();
            List<Trip> currentTrip = trikeAgent.currentTrip;
            System.out.println("\n currentTripStatus:");
            System.out.println("AgentID: " + agentID + " currentTripID: " + currentTrip.get(0).getTripID());
            System.out.println("AgentID: " + agentID + " currentTripType: " + currentTrip.get(0).getTripType());
            System.out.println("AgentID: " + agentID + " currentVaTime: " + currentTrip.get(0).getVATime());
            System.out.println("AgentID: " + agentID + " currentStartPosition: " + currentTrip.get(0).getStartPosition());
            System.out.println("AgentID: " + agentID + " currentEndPosition: " +currentTrip.get(0).getEndPosition());
            System.out.println("AgentID: " + agentID + " currentProgress: " + currentTrip.get(0).getProgress());
        }

    }

    public Location getNextChargingStation(){
        //CHARGING_STATION_LIST
        Location ChargingStation = TrikeConstants.CHARGING_STATION_LIST.get(0); //= new Location("", 476530.26535798033, 5552438.979076344);
        // last trip In pipe endlocation oder agentposition als ausgang nehmen
        Location startPosition;
        if (trikeAgent.tripList.isEmpty() && trikeAgent.currentTrip.isEmpty()){
            startPosition = trikeAgent.agentLocation;
        }
        else {
            startPosition = getLastTripInPipeline().getEndPosition();
        }
        double lowestDistance = Double.MAX_VALUE;
        for (int i=0; i < TrikeConstants.CHARGING_STATION_LIST.size(); i++){
            double compareDistance = Location.distanceBetween(startPosition, TrikeConstants.CHARGING_STATION_LIST.get(i));
            if (compareDistance<lowestDistance){
                lowestDistance = compareDistance;
                ChargingStation = TrikeConstants.CHARGING_STATION_LIST.get(i);
            }
        }

        return ChargingStation;
    }
    //  send message to AreaAgents to deregister from old and register to new
    public void changeArea(String originArea, String newArea) {
        //deregister from old
        MessageContent messageContent = new MessageContent("deregister", null);
        Message deregisterMessage = new Message("0", trikeAgent.agentID, originArea, "inform", JadexModel.simulationtime, messageContent);

        //query assigning
        IAreaTrikeService service = messageToService(trikeAgent.agent, deregisterMessage);

        //calls updateAreaAgent of AreaAgentService class
        service.areaReceiveUpdate(deregisterMessage.serialize());

        //register to new
        messageContent = new MessageContent("register", null);
        Message registerMessage = new Message("0", trikeAgent.agentID, newArea, "inform", JadexModel.simulationtime, messageContent);

        //query assigning
        service = messageToService(trikeAgent.agent, registerMessage);

        //calls updateAreaAgent of AreaAgentService class
        service.areaReceiveUpdate(registerMessage.serialize());
    }


    public double ArrivalTime(LocalDateTime vATime){
        long offset = (vATime
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

        long vaTimeMilli = vATime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        double curr = (JadexModel.simulationtime) * 1000;
        double diff = (curr - (vaTimeMilli - offset))/1000 ; //in seconds
        //double arrivalTime;
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
        //double vaTimeSec = timeInSeconds (trikeAgent.currentTrip.get(0).getVATime());
        double curr = (JadexModel.simulationtime) * 1000;
        double diff = curr - (vaTimeMilli - offset) ;
        if (diff > (TrikeConstants.THETA*1000) && TrikeConstants.ALLOW_CUSTOMER_MISS){
            return isMissed = true;
        }
        return isMissed;
    }

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


    public void sendDriveTotoAdc()
    {
        Object[] Endparams = new Object[7];
        // needs to get seperate parameter for different types of trip
        if  (trikeAgent.currentTrip.get(0).getProgress().equals("NotStarted"))
        {
            Endparams[0] = Constants.DRIVETO;
            Endparams[1] = trikeAgent.currentTrip.get(0).getStartPosition().getCoordinates();

        }
        if  (trikeAgent.currentTrip.get(0).getProgress().equals("AtStartLocation"))
        {
            Endparams[0] = Constants.DRIVETO;
            Endparams[1] = trikeAgent.currentTrip.get(0).getEndPosition().getCoordinates();
        }
        Endparams[2] = JadexModel.simulationtime;
        Endparams[3] = Constants.EvacRoutingMode.carFreespeed;
        Endparams[4] = "EvacPlace";
        Endparams[5] = trikeAgent.currentTrip.get(0).getTripID();
        //added oemer
        Endparams[6] = trikeAgent.sumLinkLength;
        trikeAgent.SimActuator.getEnvironmentActionInterface().packageAction(trikeAgent.agentID, "drive_to", Endparams, null);
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

        trikeAgent.SimActuator.getEnvironmentActionInterface().packageAction(trikeAgent.agentID, "perceive", params, "");
    }

    public double getDrivingDistanceTo(Location location) throws AgentNotFoundException { // EUclician Distanz
        double dist =
                (double)trikeAgent.SimActuator.getQueryPerceptInterface().queryPercept(
                        String.valueOf(trikeAgent.agentID),
                        Constants.REQUEST_DRIVING_DISTANCE_TO,
                        location.getCoordinates());
        return dist;
    }

    public Location getCurrentLocation() throws AgentNotFoundException {
        Location CurrentLocation = (Location) trikeAgent.SimActuator.getQueryPerceptInterface().queryPercept(String.valueOf(trikeAgent.agentID), Constants.REQUEST_LOCATION, null);

        return CurrentLocation;
    }
    ///////////////////////////////////////////////////////
    //  updates locatedagentlist of the area agent


    //  example of trike to trike communication
    public void sendMessageToTrike(String receiverID, String comAct, String action, ArrayList<String> values){
        //message creation
        //ArrayList<String> values = new ArrayList<>();
        MessageContent messageContent = new MessageContent(action, values);
        Message testMessage = new Message("1", ""+trikeAgent.agentID, receiverID, comAct, JadexModel.simulationtime,  messageContent);
        IAreaTrikeService service = messageToService(trikeAgent.agent, testMessage);

        //calls trikeMessage methods of TrikeAgentService class
        service.trikeReceiveTrikeMessage(testMessage.serialize());
    }


    //  example of trike to trike communic ation
    public void testTrikeToTrikeService(String receiverID, String comAct, String action, ArrayList<String> values){
        //message creation
        //ArrayList<String> values = new ArrayList<>();
        MessageContent messageContent = new MessageContent(action, values);
        Message testMessage = new Message("1", trikeAgent.agentID, receiverID, comAct, JadexModel.simulationtime,  messageContent);
        IAreaTrikeService service = messageToService(trikeAgent.agent, testMessage);

        //calls trikeMessage methods of TrikeAgentService class
        service.trikeReceiveTrikeMessage(testMessage.serialize());
    }

    //
    public static void sendMessage(TrikeAgent trikeAgent, String receiverID, String comAct, String action, ArrayList<String> values){
        //todo adapt for multiple area agents
        //todo use unique ids
        //message creation

        MessageContent messageContent = new MessageContent(action, values);
        Message testMessage = new Message("1", trikeAgent.agentID, receiverID, comAct, JadexModel.simulationtime,  messageContent);
        IAreaTrikeService service = messageToService(trikeAgent.agent, testMessage);

        //calls trikeMessage methods of TrikeAgentService class
        service.receiveMessage(testMessage.serialize());

    }

    public void sendAreaAgentUpdate(String action){
        //location
        ArrayList<String> values = new ArrayList<>();
        values.add(Double.toString(trikeAgent.agentLocation.getX()));
        values.add(Double.toString(trikeAgent.agentLocation.getY()));

        //update the cell based on location
        String foundKey = Cells.findKey(trikeAgent.agentLocation);
        newCellAddress = foundKey;

        //  init register of trikes
        if (action.equals("register")){
            oldCellAddress = newCellAddress;
        }

        // if the cell address has changed, change the area and leave the method
        if (!oldCellAddress.equals(newCellAddress)){
            String originArea = Cells.cellAgentMap.get(oldCellAddress);
            String newArea = Cells.cellAgentMap.get(newCellAddress);
            changeArea(originArea, newArea);
            oldCellAddress = newCellAddress;
            return;
        }

        //  get target AreaAgent tag based on the cell address
        String areaAgentTag = Cells.cellAgentMap.get(newCellAddress);

        //  update/register message
        MessageContent messageContent = new MessageContent(action, values);
        Message testMessage = new Message("0", trikeAgent.agentID, areaAgentTag, "inform", JadexModel.simulationtime,  messageContent);

        //query assigning
        IAreaTrikeService service = messageToService(trikeAgent.agent, testMessage);
        //calls updateAreaAgent of AreaAgentService class
        service.areaReceiveUpdate(testMessage.serialize());

    }

    public void print(String str){
        System.out.println(trikeAgent.agentID + ": " + str);
    }


    public void configure(Element classElement) {
        TrikeConstants.FIREBASE_ENABLED = Boolean.parseBoolean(getClassField(classElement, "FIREBASE_ENABLED"));
        trikeAgent.chargingTripAvailable = getClassField(classElement, "chargingTripAvailable");
        TrikeConstants.commitThreshold = Double.parseDouble(Objects.requireNonNull(getClassField(classElement, "commitThreshold")));
        TrikeConstants.DRIVING_SPEED = Double.parseDouble(Objects.requireNonNull(getClassField(classElement, "DRIVING_SPEED")));
        TrikeConstants.CNP_ACTIVE = Boolean.parseBoolean(getClassField(classElement, "CNP_ACTIVE"));
        TrikeConstants.THETA = Double.parseDouble(Objects.requireNonNull(getClassField(classElement, "THETA")));
        TrikeConstants.ALLOW_CUSTOMER_MISS = Boolean.parseBoolean(getClassField(classElement, "ALLOW_CUSTOMER_MISS"));
        TrikeConstants.DISTANCE_FACTOR = Double.parseDouble(Objects.requireNonNull(getClassField(classElement, "DISTANCE_FACTOR")));
        TrikeConstants.CHARGING_THRESHOLD = Double.parseDouble(Objects.requireNonNull(getClassField(classElement, "CHARGING_THRESHOLD")));
    }

    public String getRandomSimInputBroker() // choose random SimInputBroker to register in the begining
    {
        List<String> SimInputBrokerList = SimIDMapper.NumberSimInputAssignedID;
        Random rand = new Random();
        return SimInputBrokerList.get(rand.nextInt(SimInputBrokerList.size()));
    }

    //estimates the batteryLevel after all Trips. Calculations a based on aerial line x1.5
    public double estimateBatteryAfterTIP(){
        double batteryChargeAfterTIP = trikeAgent.trikeBattery.getMyChargestate();
        double totalDistance_TIP = 0.0;
        if  (trikeAgent.currentTrip.size() == 1) { //battery relavant distance driven at currentTrip
            //todo: fortschritt von currenttrip berücksichtigen
            totalDistance_TIP += Location.distanceBetween(trikeAgent.agentLocation, trikeAgent.currentTrip.get(0).getStartPosition());
            if  (trikeAgent.currentTrip.get(0).getTripType().equals("CustomerTrip")) { //only drive to the end when it is a customerTrip
                totalDistance_TIP += Location.distanceBetween (trikeAgent.currentTrip.get(0).getStartPosition(), trikeAgent.currentTrip.get(0).getEndPosition());
            }
            if  (trikeAgent.currentTrip.get(0).getTripType().equals("ChargingTrip")) {
                totalDistance_TIP = 0.0; //reset the distance until now because only the distance after a chargingTrip influences the battery
            }
        }
        // battery relavant distance driven at trikeAgent.tripList
        if (!trikeAgent.tripList.isEmpty()) {
            if  (!trikeAgent.currentTrip.isEmpty()) { //journey to the first entry in the trikeAgent.tripList from a currentTrip
                if  (trikeAgent.currentTrip.get(0).getTripType().equals("CustomerTrip")) {
                    totalDistance_TIP += Location.distanceBetween (trikeAgent.currentTrip.get(0).getEndPosition(), trikeAgent.tripList.get(0).getStartPosition());
                } else { // trips with only a start position
                    totalDistance_TIP += Location.distanceBetween (trikeAgent.currentTrip.get(0).getStartPosition(), trikeAgent.tripList.get(0).getStartPosition());
                }
            } else { //journey to the first entry in the trikeAgent.tripList from the agentLocation
                totalDistance_TIP += Location.distanceBetween(trikeAgent.agentLocation, trikeAgent.tripList.get(0).getStartPosition());
            }
            // distance driven at trikeAgent.tripList.get(0)
            if (trikeAgent.tripList.get(0).getTripType().equals("CustomerTrip")) {
                totalDistance_TIP += Location.distanceBetween(trikeAgent.tripList.get(0).getStartPosition(), trikeAgent.tripList.get(0).getEndPosition());
            }
            if (trikeAgent.tripList.get(0).getTripType().equals("ChargingTrip")) {
                totalDistance_TIP = 0.0;
            }

            //todo: fahrt zum nächjsten start fehlt +-1 bei i???
            // interates through all other Trips inside trikeAgent.tripList
            if (trikeAgent.tripList.size() > 1){ //added to avoid crashes
                for (int i = 1; i < trikeAgent.tripList.size(); i++) {
                    if (trikeAgent.tripList.get(i - 1).getTripType().equals("CustomerTrip")) {
                        totalDistance_TIP += Location.distanceBetween(trikeAgent.tripList.get(i - 1).getEndPosition(), trikeAgent.tripList.get(i).getStartPosition()); //triplist or currenttrip
                    } else { // Trips with only a startPosition
                        totalDistance_TIP += Location.distanceBetween(trikeAgent.tripList.get(i - 1).getStartPosition(), trikeAgent.tripList.get(i).getStartPosition()); //corrected! was to EndPosition before!
                    }
                    if (trikeAgent.tripList.get(i).getTripType().equals("CustomerTrip")) {
                        totalDistance_TIP += Location.distanceBetween(trikeAgent.tripList.get(i).getStartPosition(), trikeAgent.tripList.get(i).getEndPosition());
                    }
                }
            }
        }
        double estEnergyConsumption_TIP = trikeAgent.trikeBattery.SimulateDischarge((totalDistance_TIP * TrikeConstants.DISTANCE_FACTOR));
        batteryChargeAfterTIP = batteryChargeAfterTIP - estEnergyConsumption_TIP;

        trikeAgent.estimateBatteryAfterTIP.set(0, batteryChargeAfterTIP);
        return batteryChargeAfterTIP;
    }

    public double calculateUtility(DecisionTask newTask){
        double utillityScore = 0.0;


        if (trikeAgent.chargingTripAvailable.equals("0")) {


            newTask.getStartPositionFromJob();
            newTask.getEndPositionFromJob();
            newTask.getVATimeFromJob();

            double a = 1.0 / 3.0;
            double b = 1.0 / 3.0;
            double c = 1.0 / 3.0;

            double uPunctuality;
            double uBattery;
            double uDistance;

            //###########################################################
            // punctuallity
            // arrival delay to arrive at the start position when started from the agentLocation
            //todo: number of comitted trips TIP über alle berechnen erwartete ankunft bei aktuellem bestimmen, dann delay bewerten ohne ladefahrten
            double vaTimeFirstTrip = 0;
            //when there is no Trip before calculate the delay when started at the Agent Location
            if (trikeAgent.currentTrip.isEmpty() && trikeAgent.tripList.isEmpty()) {
                //agentLocation
                double distanceToStart = Location.distanceBetween(trikeAgent.agentLocation, newTask.getStartPositionFromJob());
                //double vATimeNewTask = timeInSeconds(newTask.getVATimeFromJob());
                double timeToNewTask = ((distanceToStart/1000) / TrikeConstants.DRIVING_SPEED)*60*60; //in this case equals the delay as vatiem is bookingtime
                // transforms the delay in seconds into as score beween 0 and 100 based of the max allowed delay of 900s
                if (timeToNewTask<TrikeConstants.THETA){
                    uPunctuality = 100.0;
                }
                else if (TrikeConstants.THETA<= timeToNewTask && timeToNewTask<=2*TrikeConstants.THETA){
                    uPunctuality = 100.0 - ((100.0 * timeToNewTask - TrikeConstants.THETA)/TrikeConstants.THETA);
                }
                else{
                    uPunctuality = 0.0;
                }

                //uPunctuality = Math.min(100.0, (100.0 - (((Math.min(THETA, timeToNewTask) - 0.0) / (THETA - 0.0)) * 100.0)));
            }
            else {
                double totalDistance_TIP = 0.0;
                //todo: get va time of first job here or in an else case
                if (trikeAgent.currentTrip.size() == 1) { //distances driven from the agent location to the start of the current trip and to its end
                    totalDistance_TIP += Location.distanceBetween(trikeAgent.agentLocation, trikeAgent.currentTrip.get(0).getStartPosition());
                    if (trikeAgent.currentTrip.get(0).getTripType().equals("CustomerTrip")) { //only drive to the end when it is a customerTrip
                        vaTimeFirstTrip = timeInSeconds(trikeAgent.currentTrip.get(0).getVATime());
                        totalDistance_TIP += Location.distanceBetween(trikeAgent.currentTrip.get(0).getStartPosition(), trikeAgent.currentTrip.get(0).getEndPosition());
                    }
                }
                //  distance driven at trikeAgent.tripList
                if (!trikeAgent.tripList.isEmpty()) {
                    if (!trikeAgent.currentTrip.isEmpty()) { //journey to the first entry in the trikeAgent.tripList from a currentTrip
                        if (trikeAgent.currentTrip.get(0).getTripType().equals("CustomerTrip")) {
                            totalDistance_TIP += Location.distanceBetween(trikeAgent.currentTrip.get(0).getEndPosition(), trikeAgent.tripList.get(0).getStartPosition());
                        } else { // trips with only a start position
                            totalDistance_TIP += Location.distanceBetween(trikeAgent.currentTrip.get(0).getStartPosition(), trikeAgent.tripList.get(0).getStartPosition());
                        }
                    } else { //journey to the first entry in the trikeAgent.tripList from the agentLocation
                        vaTimeFirstTrip = timeInSeconds(trikeAgent.tripList.get(0).getVATime()); //fist VATime when there was no CurrentTrip
                        totalDistance_TIP += Location.distanceBetween(trikeAgent.agentLocation, trikeAgent.tripList.get(0).getStartPosition());
                    }
                    // distance driven at trikeAgent.tripList.get(0)
                    if (trikeAgent.tripList.get(0).getTripType().equals("CustomerTrip")) {
                        totalDistance_TIP += Location.distanceBetween(trikeAgent.tripList.get(0).getStartPosition(), trikeAgent.tripList.get(0).getEndPosition());
                    }
                } else {
                    // do nothing as all other Trips with only a startPosition will not contain any other movements;
                }

                // interates through all other Trips inside trikeAgent.tripList
                if (trikeAgent.tripList.size() > 1){ //added to avoid crashes
                    for (int i = 1; i < trikeAgent.tripList.size(); i++) {
                        if (trikeAgent.tripList.get(i - 1).getTripType().equals("CustomerTrip")) {
                            totalDistance_TIP += Location.distanceBetween(trikeAgent.tripList.get(i - 1).getEndPosition(), trikeAgent.tripList.get(i).getStartPosition()); //triplist or currenttrip
                        } else { // Trips with only a startPosition
                            totalDistance_TIP += Location.distanceBetween(trikeAgent.tripList.get(i - 1).getStartPosition(), trikeAgent.tripList.get(i).getStartPosition()); //corrected! was to EndPosition before!
                        }
                        if (trikeAgent.tripList.get(i).getTripType().equals("CustomerTrip")) { //triplist or currenttrip
                            totalDistance_TIP += Location.distanceBetween(trikeAgent.tripList.get(i).getStartPosition(), trikeAgent.tripList.get(i).getEndPosition());
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


                double vATimeNewTask = timeInSeconds(newTask.getVATimeFromJob());
                double timeToNewTask = ((totalDistance_TIP/1000) / TrikeConstants.DRIVING_SPEED)*60*60;
                double arrivalAtNewtask = vaTimeFirstTrip + timeToNewTask;

                double delayArrvialNewTask = Math.max((arrivalAtNewtask - vATimeNewTask), timeToNewTask);
                System.out.println("vATimeNewTask: " + vATimeNewTask );
                System.out.println("timeToNewTask: " + timeToNewTask );
                System.out.println("arrivalAtNewtask: " + arrivalAtNewtask );
                System.out.println("delayArrvialNewTask: " + delayArrvialNewTask );

                if (delayArrvialNewTask<TrikeConstants.THETA){
                    uPunctuality = 100.0;
                }
                else if (TrikeConstants.THETA<= delayArrvialNewTask && delayArrvialNewTask <=2*TrikeConstants.THETA){
                    uPunctuality = 100.0 - ((100.0 * delayArrvialNewTask - TrikeConstants.THETA)/TrikeConstants.THETA);
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
            double currentBatteryLevel = trikeAgent.trikeBattery.getMyChargestate(); //todo: use real battery
            double estBatteryLevelAfter_TIP = trikeAgent.trikeBattery.getMyChargestate();
            double estDistance = 0.0;
            double estEnergyConsumption = 0.0;
            double estEnergyConsumption_TIP = 0.0;
            double totalDistance_TIP = 0.0;
            double negativeInfinity = Double.NEGATIVE_INFINITY;
            double bFactor = 0;
            //todo ennergieverbrauch für zu evuluierenden job bestimmen

            //calculation of the estimatedEnergyConsumtion (of formertrips)


            if  (trikeAgent.currentTrip.size() == 1) { //battery relavant distance driven at currentTrip
                //todo: fortschritt von currenttrip berücksichtigen
                totalDistance_TIP += Location.distanceBetween(trikeAgent.agentLocation, trikeAgent.currentTrip.get(0).getStartPosition());
                if  (trikeAgent.currentTrip.get(0).getTripType().equals("CustomerTrip")) { //only drive to the end when it is a customerTrip
                    totalDistance_TIP += Location.distanceBetween (trikeAgent.currentTrip.get(0).getStartPosition(), trikeAgent.currentTrip.get(0).getEndPosition());
                }
                if  (trikeAgent.currentTrip.get(0).getTripType().equals("ChargingTrip")) {
                    totalDistance_TIP = 0.0; //reset the distance until now because only the distance after a chargingTrip influences the battery
                }
            }
            // battery relavant distance driven at trikeAgent.tripList
            if (!trikeAgent.tripList.isEmpty()) {
                if  (!trikeAgent.currentTrip.isEmpty()) { //journey to the first entry in the trikeAgent.tripList from a currentTrip
                    if  (trikeAgent.currentTrip.get(0).getTripType().equals("CustomerTrip")) {
                        totalDistance_TIP += Location.distanceBetween (trikeAgent.currentTrip.get(0).getEndPosition(), trikeAgent.tripList.get(0).getStartPosition());
                    } else { // trips with only a start position
                        totalDistance_TIP += Location.distanceBetween (trikeAgent.currentTrip.get(0).getStartPosition(), trikeAgent.tripList.get(0).getStartPosition());
                    }
                } else { //journey to the first entry in the trikeAgent.tripList from the agentLocation
                    totalDistance_TIP += Location.distanceBetween(trikeAgent.agentLocation, trikeAgent.tripList.get(0).getStartPosition());
                }
                // distance driven at trikeAgent.tripList.get(0)
                if (trikeAgent.tripList.get(0).getTripType().equals("CustomerTrip")) {
                    totalDistance_TIP += Location.distanceBetween(trikeAgent.tripList.get(0).getStartPosition(), trikeAgent.tripList.get(0).getEndPosition());
                }
                if (trikeAgent.tripList.get(0).getTripType().equals("ChargingTrip")) {
                    totalDistance_TIP = 0.0;
                }  // do nothing as all other Trips with only a startPosition will not contain any other movements;


                //todo: fahrt zum nächjsten start fehlt +-1 bei i???
                // interates through all other Trips inside trikeAgent.tripList
                if (trikeAgent.tripList.size() > 1){ //added to avoid crashes
                    for (int i = 1; i < trikeAgent.tripList.size(); i++) {
                        if (trikeAgent.tripList.get(i - 1).getTripType().equals("CustomerTrip")) {
                            totalDistance_TIP += Location.distanceBetween(trikeAgent.tripList.get(i - 1).getEndPosition(), trikeAgent.tripList.get(i).getStartPosition()); //triplist or currenttrip
                        } else { // Trips with only a startPosition
                            totalDistance_TIP += Location.distanceBetween(trikeAgent.tripList.get(i - 1).getStartPosition(), trikeAgent.tripList.get(i).getStartPosition()); //corrected! was to EndPosition before!
                        }
                        if (trikeAgent.tripList.get(i).getTripType().equals("CustomerTrip")) { //triplist or currenttrip
                            totalDistance_TIP += Location.distanceBetween(trikeAgent.tripList.get(i).getStartPosition(), trikeAgent.tripList.get(i).getEndPosition());
                        }
                    }
                }
            }
            //todo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! RICHTIGE WERTE ZUGREIFEN
            estEnergyConsumption_TIP = trikeAgent.trikeBattery.SimulateDischarge(totalDistance_TIP * TrikeConstants.DISTANCE_FACTOR);//*2 because it would be critical to underestimate the distance
            estBatteryLevelAfter_TIP = currentBatteryLevel - estEnergyConsumption_TIP;

            //calculate teh estimated energy consumption of the new job


            //Distance from the agent location
            if  (trikeAgent.currentTrip.isEmpty() && trikeAgent.tripList.isEmpty()){
                estDistance += Location.distanceBetween(trikeAgent.agentLocation, newTask.getStartPositionFromJob());
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

            estEnergyConsumption = trikeAgent.trikeBattery.SimulateDischarge(estDistance * TrikeConstants.DISTANCE_FACTOR);

            double estBatterylevelTotal = estBatteryLevelAfter_TIP - estEnergyConsumption;


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
            double dmax = 3000.0;
            double distanceToStart;

            if (trikeAgent.tripList.isEmpty() && trikeAgent.currentTrip.isEmpty()) {
                distanceToStart = Location.distanceBetween(trikeAgent.agentLocation, newTask.getStartPositionFromJob());
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
        System.out.println("agentID: " + trikeAgent.agentID + "utillity: " + utillityScore);
        return utillityScore;
    }

    public double timeInSeconds(LocalDateTime time) {
        // Option 1: If the difference is greater than 300 seconds (5 minutes OR 300 seconds or 300000 millisec), then customer missed, -oemer

        double vaTimeSec = time.atZone(ZoneId.systemDefault()).toEpochSecond();
        return vaTimeSec;
    }

    //test if there is at least one trip anywhere
    public Trip getLastTripInPipeline(){
        Trip lastTrip = null;
        if (!trikeAgent.tripList.isEmpty()){
            lastTrip = trikeAgent.tripList.get(trikeAgent.tripList.size()-1);
        }
        else if (!trikeAgent.currentTrip.isEmpty()){
            lastTrip = trikeAgent.currentTrip.get(trikeAgent.currentTrip.size()-1);

        }
        else{
            System.out.println("ERROR: getLastTripInPipeline() no trips available!");
        }
        return lastTrip;
    }
}
