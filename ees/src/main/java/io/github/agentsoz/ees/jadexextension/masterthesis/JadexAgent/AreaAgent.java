/** AreaAgent
 *  Version: v0.1
 *
 *  changelog: able to send multiple Jobs from its JobList1
 */


package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.INotifyService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.TrikeMain;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.ISendTripService.IsendTripService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.ISendTripService.SendtripService;
import io.github.agentsoz.util.Location;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.ServiceQuery;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;


@Agent(type = "bdi")

@ProvidedServices({
        @ProvidedService(type= IsendTripService.class, implementation=@Implementation( SendtripService.class))
})
@RequiredServices({
        @RequiredService(name="clockservice", type= IClockService.class),
        @RequiredService(name = "sendtripservices", type = IsendTripService.class),
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

    //TODO: Some kind of area AgentID needed for the comunication when more areaagents are used

    @Belief
    public List<Job> jobList1 = new ArrayList<>(); //Joblist for historic data

    @Belief
    public List<Job> jobList2 = new ArrayList<>(); //JobList for App data

    LocatedAgentList locatedAgentList = new LocatedAgentList();

    // TODO problably not used anymore, try to delete
    // to check the number of agents that are assigned ID, when all of the agents receive their IDs, then they could start sending trips
    @Belief
    public static int NumberAgentAssignedID;
    @Belief
    public static List<String> NumberSimInputAssignedID = new ArrayList<>();

    boolean done;

    /** The agent body. */
    @OnStart
    public void body() {

        System.out.println("AreaAgent sucessfully started;");
        initJobs();

        /** example code delete after testing */


        Job Job1 = new Job("1", "1", LocalDateTime.now(), LocalDateTime.now(), new Location("", 238654.693529, 5886721.094209), new Location("", 238674.543999, 5901195.908183));
        Job Job2 = new Job("2", "2", LocalDateTime.now(), LocalDateTime.now(), new Location("", 238674.543999, 5901195.908183), new Location("", 238654.693529, 5886721.094209));
        //jobList1.add(Job1);
        //jobList1.add(Job2);

        LocatedAgent newAgent = new LocatedAgent("0", new Location("", 238654.693529, 5886721.094209), LocalDateTime.now());
        locatedAgentList.updateLocatedAgentList(newAgent, "register");
        System.out.println("locatedAgentList size: " + locatedAgentList.size());

        System.out.println(JadexModel.simulationtime);
        /** ########*/
        bdiFeature.dispatchTopLevelGoal(new AreaAgent.CheckNumberAgentAssignedID());
    }

    @Goal (recur = true, recurdelay = 3000)
    class CheckNumberAgentAssignedID {
        public CheckNumberAgentAssignedID() {
        }
    }

    @Plan(trigger = @Trigger(goals = CheckNumberAgentAssignedID.class))
    public void PrintActiveAgent() {
        // when receive result from other agents, the plan somehow
        //TODO need time from MATSim
        if (TrikeMain.TrikeAgentNumber== JadexModel.TrikeAgentnumber)
            if (done == false) {
                done = true;
                ///**
                try {
                    Thread.sleep(20000); // pause for 1 second
                    // do something
                } catch (InterruptedException e) {
                    // handle exception
                }
                 //*/
                System.out.println("SLEEPING FINISHED");
                bdiFeature.dispatchTopLevelGoal(new MaintainDistributeJobs());
            }
    }

    @Goal(recur = true, recurdelay = 5000 )
    class MaintainDistributeJobs
    {
        //Compare time with matsim and send only if simulationtiem > booking time
        //bdiFeature.dispatchTopLevelGoal(new CheckNumberAgentAssignedID());
        @GoalMaintainCondition
        boolean jobListNotEmpty(){
            return (jobList1.size()==0);
        }
    }

    @Plan(trigger=@Trigger(goals=MaintainDistributeJobs.class))
    private void SendJob()
    {
        sendJobToAgent();
    }

    public void sendJobToAgent(){

        // sending data to specific TrikeAgent by calling its serviceTag
        //TODO: get time from Matsim and send only when >= bookingTime
        //TODO: time format of matsim like 43220.0 how to compare?
        //System.out.println(JadexModel.simulationtime);
        //TODO: run multiple times till jobList is empty
        ServiceQuery<IsendTripService> query = new ServiceQuery<>(IsendTripService.class); //# Service Baustein
        query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL    //# Service Baustein
        System.out.println("locatedAgentList size: " + locatedAgentList.size());

        Job toHandle = jobList1.get(0);

        //toHandle.getStartPosition();
        String closestAgent = locatedAgentList.calculateClosestLocatedAgent(toHandle.getStartPosition());
        String message = toHandle.JobForTransfer();
        if (closestAgent.equals("NoAgentsLocated")){
            System.out.println("ERROR: No Agent located at this AreaAgent");
        }
        else{
            query.setServiceTags("user:" + closestAgent); // calling the tag of a trike agent   //# Service Baustein
            Collection<IsendTripService> service = agent.getLocalServices(query);               //# Service Baustein
            for (Iterator<IsendTripService> iteration = service.iterator(); iteration.hasNext(); ) { //# Service Baustein
                IsendTripService cs = iteration.next();                                              //# Service Baustein
                cs.sendJob(message);                                                               //# Service Baustein
            }
            jobList1.remove(0);
        }
    }


     public void initJobs(){
     String csvFilePath =  "C:\\Users\\Oemer\\Desktop\\Github Repositories\\ees-läuft\\ees\\data.csv";
     String jsonFilePath = "output.json";
     char delimiter = ';';

     //parse csv and create json output
     JSONParser.csvToJSON(csvFilePath, jsonFilePath, delimiter);

     System.out.println("parse json from file:");
     jobList1 = Job.JSONFileToJobs("output.json");

     for (Job job: jobList1) {
     System.out.println(job.getID());
     }

     //  jobs directly from json
     System.out.println("parse json from app:");

     //jobList2 = Job.JSONToJobs("");

     for (Job job: jobList2) {
     System.out.println(job.getID());
     }
     }

}