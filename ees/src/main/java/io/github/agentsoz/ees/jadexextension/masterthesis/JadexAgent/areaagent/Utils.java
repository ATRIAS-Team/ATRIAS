package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.areaagent;

import io.github.agentsoz.ees.firebase.FirebaseHandler;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.*;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.TrikeMain;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.XMLConfig;
import io.github.agentsoz.util.Location;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import org.w3c.dom.Element;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.agentsoz.ees.jadexextension.masterthesis.Run.XMLConfig.assignIfNotNull;
import static io.github.agentsoz.ees.jadexextension.masterthesis.Run.XMLConfig.getClassField;

public class Utils {
    AreaAgent areaAgent;

    public Utils(AreaAgent agent){
        this.areaAgent = agent;
    }

    public void body(){
        Element classElement = XMLConfig.getClassElement("AreaAgent.java");
        configure(classElement);
        Pattern pattern = Pattern.compile("[0-9]+");
        Matcher matcher = pattern.matcher(areaAgent.agent.getId().getLocalName());
        int index = 0;

        if (matcher.find()) {
            index = Integer.parseInt(matcher.group());
        }

        areaAgent.areaAgentId = "area: " + index;
        areaAgent.myTag = areaAgent.areaAgentId;
        areaAgent.cell = Cells.areaAgentCells.get(index);
        Cells.cellAgentMap.put(areaAgent.cell, areaAgent.areaAgentId);
        areaAgent.neighbourIds = Cells.getNeighbours(areaAgent.cell, 1);

        System.out.println("AreaAgent " + areaAgent.areaAgentId + " sucessfully started;");
        initJobs();

        IServiceIdentifier sid = ((IService) areaAgent.agent.getProvidedService(IAreaTrikeService.class)).getServiceId();
        areaAgent.agent.setTags(sid, areaAgent.areaAgentId);
        System.out.println("locatedAgentList size: " + areaAgent.locatedAgentList.size());

        if(areaAgent.FIREBASE_ENABLED) {
            //  fetch jobs from firebase
            FirebaseHandler<AreaAgent, Job> firebaseHandler = new FirebaseHandler<AreaAgent, Job>(areaAgent, areaAgent.jobList);
            firebaseHandler.childAddedListener("tripRequests", (dataSnapshot, previousChildName, list) -> {
                // A new child node has been added
                String tripRequestId = dataSnapshot.getKey();
                System.out.println("New trip request added: " + tripRequestId);

                // Access data of the new trip request
                //String assignedAgent = dataSnapshot.child("assignedAgent").getValue(String.class);
                String customerId = dataSnapshot.child("customerId").getValue(String.class);
                String startTimeStr = dataSnapshot.child("startTime").getValue(String.class);
                String timestampStr = dataSnapshot.child("timestamp").getValue(String.class);

                //System.out.println("Assigned Agent: " + assignedAgent);
                System.out.println("Customer ID: " + customerId);
                System.out.println("Start Time: " + startTimeStr);
                System.out.println("Timestamp: " + timestampStr);


                Location startPosition = new Location("", 0, 0);
                startPosition.x = dataSnapshot.child("startLocation").child("longitude").getValue(Double.class);
                startPosition.y = dataSnapshot.child("startLocation").child("latitude").getValue(Double.class);

                Location endPosition = new Location("", 0, 0);
                endPosition.x = dataSnapshot.child("endLocation").child("longitude").getValue(Double.class);
                endPosition.y = dataSnapshot.child("endLocation").child("latitude").getValue(Double.class);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS'Z'");
                System.out.println(startTimeStr);
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
                LocalTime localTime = LocalTime.parse(startTimeStr, dtf);
                LocalDateTime startTime = LocalDateTime.of(LocalDate.now(), localTime);
                LocalDateTime vaTime = LocalDateTime.parse(timestampStr, formatter);
                Job job = new Job(customerId, tripRequestId, startTime, vaTime, startPosition, endPosition);

                String jobCell = Cells.locationToCellAddress(job.getStartPosition(), Cells.getCellResolution(areaAgent.cell));
                if (jobCell.equals(areaAgent.cell)) {
                    list.add(job);
                }
            });
        }

        while (TrikeMain.TrikeAgentNumber != JadexModel.TrikeAgentnumber) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String CSV_SOURCE;

    public void sendJobToAgent(List<Job> jobList){
        //  current job
        Job job = jobList.get(0);

        //  convert
        LocalTime simTime = LocalTime.MIDNIGHT
                .withMinute((int) Math.floor((JadexModel.simulationtime % 3600) / 60))
                .withHour((int) Math.floor(JadexModel.simulationtime / 3600));

        LocalDateTime simDateTime = LocalDateTime.of(areaAgent.csvDate, simTime);
        if(!job.getbookingTime().isBefore(simDateTime)) return;

        String closestAgent = areaAgent.locatedAgentList.calculateClosestLocatedAgent(job.getStartPosition());
        if (closestAgent == null){
            areaAgent.jobsToDelegate.add(new DelegateInfo(job));
            System.out.println(job.getID() + " is delegated");
            jobList.remove(0);
        }
        else{
            //message creation
            MessageContent messageContent = new MessageContent("", job.toArrayList());
            LocalTime bookingTime = LocalTime.now();
            System.out.println("START Negotiation - JobID: " + job.getID() + " Time: "+ bookingTime);
            Message message = new Message(areaAgent.areaAgentId, closestAgent, Message.ComAct.REQUEST, JadexModel.simulationtime, messageContent);
            IAreaTrikeService service = IAreaTrikeService.messageToService(areaAgent.agent, message);
            service.sendMessage(message.serialize());

            areaAgent.requests.add(message);

            //remove job from list
            jobList.remove(0);
            System.out.println("AREA AGENT: JOB was SENT");
        }
    }


    private void initJobs() {
        String csvFilePath = this.CSV_SOURCE;
        char delimiter = ';';

        System.out.println("parse json from file:");
        List<Job> allJobs = Job.csvToJobs(csvFilePath, delimiter);
        for (Job job : allJobs) {
            String jobCell = Cells.locationToCellAddress(job.getStartPosition(), Cells.getCellResolution(areaAgent.cell));
            if(jobCell.equals(areaAgent.cell)){
                areaAgent.csvJobList.add(job);
            }
        }

        for (Job job: areaAgent.csvJobList) {
            System.out.println(job.getID());
        }

        if(!allJobs.isEmpty()) {
            areaAgent.csvDate = allJobs.get(0).getbookingTime().toLocalDate();
        }
    }

    private void configure(Element classElement) {
        assignIfNotNull(classElement,"FIREBASE_ENABLED", Boolean::parseBoolean,
                value -> areaAgent.FIREBASE_ENABLED = value);
        assignIfNotNull(classElement,"CSV_SOURCE", String::toString,
                value -> this.CSV_SOURCE = value);
        assignIfNotNull(classElement,"SEND_WAIT_TIME", Integer::parseInt,
                value -> AreaConstants.SEND_WAIT_TIME = value);
    }
}
