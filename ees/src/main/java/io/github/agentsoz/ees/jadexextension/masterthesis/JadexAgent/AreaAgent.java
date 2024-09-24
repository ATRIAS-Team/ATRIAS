/** AreaAgent
 *  Version: v0.5 (24.09.2024)
 *  changelog: cleanup
 *  @Author Marcel, Mahkam
 */
package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.AreaAgentService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.TrikeMain;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


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

    @Belief
    private final String areaAgentId = "area:0";

    /** The agent body. */
    @OnStart
    private void body() {

        System.out.println("AreaAgent sucessfully started;");
        initJobs();

        IServiceIdentifier sid = ((IService) agent.getProvidedService(IAreaTrikeService.class)).getServiceId();
        agent.setTags(sid, areaAgentId);
        System.out.println("locatedAgentList size: " + locatedAgentList.size());

        bdiFeature.dispatchTopLevelGoal(new CheckNumberAgentAssignedID());
    }

    @Goal (recur = true, recurdelay = 3000)
    class CheckNumberAgentAssignedID {}

    @Plan(trigger = @Trigger(goals = CheckNumberAgentAssignedID.class))
    private void dispatchDistributionGoal() {
        if (TrikeMain.TrikeAgentNumber == JadexModel.TrikeAgentnumber)
            if (!done) {
                done = true;
                bdiFeature.dispatchTopLevelGoal(new MaintainDistributeFirebaseJobs());
                bdiFeature.dispatchTopLevelGoal(new MaintainDistributeCSVJobs());
            }
    }

    @Goal(recur = true, recurdelay = 500 )
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

    @Goal(recur = true, recurdelay = 500 )
    class MaintainDistributeCSVJobs
    {
        @GoalMaintainCondition
        boolean isListEmpty(){
            return csvJobList.isEmpty();
        }
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

        LocalDateTime simDateTime = LocalDateTime.of(LocalDate.now(), simTime);
        if(!job.getbookingTime().isBefore(simDateTime)) return;

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
        String csvFilePath = "ees/data-utm-1000.csv";
        char delimiter = ';';

        System.out.println("parse json from file:");
        csvJobList = Job.csvToJobs(csvFilePath, delimiter);

        for (Job job: csvJobList) {
            System.out.println(job.getID());
        }
    }
}