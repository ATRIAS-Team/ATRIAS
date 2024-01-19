/** AreaAgent
 *  Version: v0.4 (19.01.2024)
 *  changelog: universal message service
 *  @Author Marcel, Mahkam
 */
package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaAgentService.IAreaAgentService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaAgentService.SendAreaAgentService;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.TrikeMain;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.util.Location;
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
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.time.ZoneId;
import java.util.*;


@Agent(type = "bdi")

@ProvidedServices({
        @ProvidedService(type= IAreaAgentService.class, implementation=@Implementation( SendAreaAgentService.class)),
})
@RequiredServices({
        @RequiredService(name="clockservice", type= IClockService.class),
        @RequiredService(name = "sendareaagendservice", type = IAreaAgentService.class),
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

    public LocatedAgentList locatedAgentList = new LocatedAgentList();

    // TODO problably not used anymore, try to delete
    // to check the number of agents that are assigned ID, when all of the agents receive their IDs, then they could start sending trips
    @Belief
    public static int NumberAgentAssignedID;
    @Belief
    public static List<String> NumberSimInputAssignedID = new ArrayList<>();

    boolean done;
    long offset;
    long milli;

    @Belief
    private String areaAgentId = "area:0";

    /** The agent body. */
    @OnStart
    public void body() throws ParserConfigurationException, IOException, SAXException {

        System.out.println("AreaAgent sucessfully started;");
        initJobs();

        IServiceIdentifier sid = ((IService) agent.getProvidedService(IAreaAgentService.class)).getServiceId();
        //agent.getId().getName() instead of 0
        agent.setTags(sid, areaAgentId);

        /** example code delete after testing */
        //Job Job1 = new Job("1", "1", LocalDateTime.now(), LocalDateTime.now(), new Location("", 238654.693529, 5886721.094209), new Location("", 238674.543999, 5901195.908183));
        //Job Job2 = new Job("2", "2", LocalDateTime.now(), LocalDateTime.now(), new Location("", 238674.543999, 5901195.908183), new Location("", 238654.693529, 5886721.094209));
        //jobList1.add(Job1);
        //jobList1.add(Job2);

        // add hardcoded agents for the locatedAgentList like this!
        LocatedAgent newAgent = new LocatedAgent("0", new Location("", 238654.693529, 5886721.094209), JadexModel.simulationtime);
        locatedAgentList.updateLocatedAgentList(newAgent, JadexModel.simulationtime, "register");
        System.out.println("locatedAgentList size: " + locatedAgentList.size());

        /** ########*/
        bdiFeature.dispatchTopLevelGoal(new CheckNumberAgentAssignedID());
        //bdiFeature.dispatchTopLevelGoal(new PrintTime1());
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
                bdiFeature.dispatchTopLevelGoal(new MaintainDistributeJobs());
            }
    }

    @Goal(recur = true, recurdelay = 1000 )
    class MaintainDistributeJobs
    {
        //Compare time with matsim and send only if simulationtiem > booking time
        //bdiFeature.dispatchTopLevelGoal(new CheckNumberAgentAssignedID());
        @GoalMaintainCondition
        boolean jobListNotEmpty(){
            return jobList1.isEmpty();
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

        while(!jobList1.isEmpty()) {
            milli = jobList1.get(0).getbookingTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            boolean isReady = milli - offset <= JadexModel.simulationtime * 1000;
            //System.out.println("job time: " + (milli - offset) / 1000);
            //System.out.println("sim time: " + JadexModel.simulationtime);
            if(!isReady) break;

            ServiceQuery<IAreaAgentService> query = new ServiceQuery<>(IAreaAgentService.class); //# Service Baustein
            query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL    //# Service Baustein
            //System.out.println("locatedAgentList size: " + locatedAgentList.size());

            Job toHandle = jobList1.get(0);

            //toHandle.getStartPosition();
            String closestAgent = locatedAgentList.calculateClosestLocatedAgent(toHandle.getStartPosition());

            if (closestAgent.equals("NoAgentsLocated")){
                System.out.println("ERROR: No Agent located at this AreaAgent");
            }
            else{
                String trikeTag = "user:" + closestAgent;
                MessageContent messageContent = new MessageContent("", toHandle.toArrayList());
                Message message = new Message("0", areaAgentId, trikeTag, "PROVIDE", JadexModel.simulationtime, messageContent);
                query.setServiceTags(trikeTag); // calling the tag of a trike agent   //# Service Baustein
                IAreaAgentService service = agent.getLocalService(query);               //# Service Baustein
                service.sendJob(message.serialize());
                jobList1.remove(0);

                System.out.println("AREA AGENT: JOB was SENT");
            }
        }
    }

    public void initJobs() throws ParserConfigurationException, IOException, SAXException {
        String csvFilePath = "C:\\Users\\Marcel\\Desktop\\data.csv";
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

     /*
     // parse simulation start time
     Document document = new XMLParser(("C:\\Users\\Mahkamjon\\Desktop\\ees\\ees\\scenarios\\mount-alexander-shire\\maldon-example\\ees.xml")).getDocument();
     Element globalElement = (Element) document.getElementsByTagName("global").item(0);
     String[] timeArr = globalElement.getElementsByTagName("opt").item(2)
             .getTextContent()
             .split(":");
     int hour = Integer.parseInt(timeArr[0]);
     int minute = Integer.parseInt(timeArr[1]);

    */

        offset = jobList1.get(0).getbookingTime()
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}