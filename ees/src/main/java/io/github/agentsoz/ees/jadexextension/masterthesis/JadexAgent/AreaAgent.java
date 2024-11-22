/** AreaAgent
 *  Version: v0.6 (19.11.2024)
 *  changelog: merge
 *  @Author Marcel, Mahkam
 */
package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import io.github.agentsoz.ees.firebase.FirebaseHandler;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.AreaAgentService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.TrikeMain;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.XMLConfig;
import io.github.agentsoz.util.Location;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.clock.IClockService;
import jadex.micro.annotation.*;
import org.w3c.dom.Element;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.agentsoz.ees.jadexextension.masterthesis.Run.XMLConfig.getClassField;


@Agent(type = "bdi")

@ProvidedServices({
        @ProvidedService(type= IAreaTrikeService.class, implementation=@Implementation( AreaAgentService.class)),
})
@RequiredServices({
        @RequiredService(name="clockservice", type= IClockService.class),
        @RequiredService(name = "sendareaagendservice", type = IAreaTrikeService.class),
})

public class AreaAgent {

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
    private List<Job> csvJobList = new ArrayList<>(); // job list for historic data

    @Belief
    private final List<Job> jobList = new ArrayList<>(); // job list for App data

    public LocatedAgentList locatedAgentList = new LocatedAgentList();
    boolean done;

    LocalDate csvDate;

    String cell;

    @Belief
    private String areaAgentId = null;
    public String myTag = null;

    boolean FIREBASE_ENABLED = false;

    /** The agent body. */
    @OnStart
    private void body() throws InterruptedException {
        Element classElement = XMLConfig.getClassElement("AreaAgent.java");
        configure(classElement);
        Pattern pattern = Pattern.compile("[0-9]+");
        Matcher matcher = pattern.matcher(agent.getId().getLocalName());
        int index = 0;

        if (matcher.find()) {
            index = Integer.parseInt(matcher.group());
        }

        areaAgentId = "area: " + index;
        myTag = areaAgentId;
        cell = Cells.areaAgentCells.get(index);
        Cells.cellAgentMap.put(cell, areaAgentId);

        System.out.println("AreaAgent " + areaAgentId + " sucessfully started;");
        initJobs();

        IServiceIdentifier sid = ((IService) agent.getProvidedService(IAreaTrikeService.class)).getServiceId();
        agent.setTags(sid, areaAgentId);
        System.out.println("locatedAgentList size: " + locatedAgentList.size());

        if(FIREBASE_ENABLED) {
            //  fetch jobs from firebase
            FirebaseHandler<AreaAgent, Job> firebaseHandler = new FirebaseHandler<AreaAgent, Job>(this, jobList);
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

                String jobCell = Cells.locationToCellAddress(job.getStartPosition(), Cells.getCellResolution(cell));
                if (jobCell.equals(cell)) {
                    list.add(job);
                }
            });
        }
        bdiFeature.dispatchTopLevelGoal(new PrintSimTime());

        while (TrikeMain.TrikeAgentNumber != JadexModel.TrikeAgentnumber) {
            Thread.sleep(1000);
        }

        bdiFeature.dispatchTopLevelGoal(new MaintainDistributeFirebaseJobs());
        bdiFeature.dispatchTopLevelGoal(new MaintainDistributeCSVJobs());
    }

    @Goal(recur = true, recurdelay = 1000 )
    class PrintSimTime {}
    @Plan(trigger=@Trigger(goals=PrintSimTime.class))
    private void printTime()
    {
        System.out.println("Simulation time: "+JadexModel.simulationtime);
    }


    @Goal(recur = true, recurdelay = 10 )
    class MaintainDistributeCSVJobs
    {
        @GoalMaintainCondition
        boolean isListEmpty(){
            return csvJobList.isEmpty();
        }
    }

    @Goal(recur = true, recurdelay = 1000 )
    class MaintainDistributeFirebaseJobs
    {
        @GoalMaintainCondition
        boolean isListEmpty(){
            return jobList.isEmpty();
        }
    }

    @Plan(trigger=@Trigger(goals=MaintainDistributeFirebaseJobs.class))
    private void SendFirebaseJob()
    {
        sendJobToAgent(jobList);
    }


    @Plan(trigger=@Trigger(goals=MaintainDistributeCSVJobs.class))
    private void SendCSVJob()
    {
        sendJobToAgent(csvJobList);
    }

    /**
     * sending data to specific TrikeAgent by calling its serviceTag
     */
    private void sendJobToAgent(List<Job> jobList){
        //  current job
        Job job = jobList.get(0);

        //  convert
        LocalTime simTime = LocalTime.MIDNIGHT
                .withMinute((int) Math.floor((JadexModel.simulationtime % 3600) / 60))
                .withHour((int) Math.floor(JadexModel.simulationtime / 3600));

        LocalDateTime simDateTime = LocalDateTime.of(csvDate, simTime);
        if(job.getbookingTime().compareTo(simDateTime) >= 0) return;

        String closestAgent = locatedAgentList.calculateClosestLocatedAgent(job.getStartPosition());
        if (closestAgent.equals("NoAgentsLocated")){
            System.out.println("ERROR: No Agent located at this AreaAgent");
        }
        else{
            //message creation
            MessageContent messageContent = new MessageContent("", job.toArrayList());
            LocalTime bookingTime = LocalTime.now();
            System.out.println("START Negotiation - JobID: " + job.getID() + " Time: "+ bookingTime);
            Message message = new Message("0", areaAgentId, "" + closestAgent, "PROVIDE", JadexModel.simulationtime, messageContent);
            IAreaTrikeService service = IAreaTrikeService.messageToService(agent, message);
            service.trikeReceiveJob(message.serialize());
            //remove job from list
            jobList.remove(0);
            System.out.println("AREA AGENT: JOB was SENT");
        }
    }


    private void initJobs() {
        String csvFilePath = "ees/subsample_2.csv";
        char delimiter = ';';

        System.out.println("parse json from file:");
        List<Job> allJobs = Job.csvToJobs(csvFilePath, delimiter);
        for (Job job : allJobs) {
            String jobCell = Cells.locationToCellAddress(job.getStartPosition(), Cells.getCellResolution(cell));
            if(jobCell.equals(cell)){
                csvJobList.add(job);
            }
        }

        for (Job job: csvJobList) {
            System.out.println(job.getID());
        }
        if(!csvJobList.isEmpty()) {
            csvDate = csvJobList.get(0).getbookingTime().toLocalDate();
        }
    }

    private void configure(Element classElement) {
        this.FIREBASE_ENABLED = Boolean.parseBoolean(getClassField(classElement, "FIREBASE_ENABLED"));
    }
}