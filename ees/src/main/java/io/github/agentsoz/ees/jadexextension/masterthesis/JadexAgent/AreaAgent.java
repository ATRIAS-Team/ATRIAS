/** AreaAgent
 *  Version: v0.6 (19.11.2024)
 *  changelog: merge
 *  @Author Marcel, Mahkam
 */
package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.areaagent.AreaConstants;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.areaagent.DelegateInfo;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.areaagent.Plans;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.areaagent.Utils;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.AreaAgentService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.util.RingBuffer;
import io.github.agentsoz.util.Location;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.clock.IClockService;
import jadex.micro.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;


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
    public IInternalAccess agent;
    @AgentFeature
    public IBDIAgentFeature bdiFeature;
    @AgentFeature
    protected IExecutionFeature execFeature;
    @AgentFeature
    protected IRequiredServicesFeature requiredServicesFeature;

    @Belief
    public List<Job> csvJobList = new ArrayList<>(); // job list for historic data

    @Belief
    public final List<Job> jobList = new ArrayList<>(); // job list for App data

    public List<Job> delegatedJobs = new ArrayList<>();

    public LocatedAgentList locatedAgentList = new LocatedAgentList();
    boolean done;

    public LocalDate csvDate;

    public String cell;

    @Belief
    public String areaAgentId = null;
    public String myTag = null;

    public List<Message> sentMessages = new ArrayList<>();

    public Map<UUID, Long> receivedMessageIds = new HashMap<>(64);

    //  BUFFER
    public RingBuffer<Message> areaMessagesBuffer = new RingBuffer<>(4);
    public RingBuffer<Job> jobRingBuffer = new RingBuffer<>(8);
    public RingBuffer<Message> proposalBuffer = new RingBuffer<>(32);
    public RingBuffer<Message> trikeMessagesBuffer = new RingBuffer<>(16);


    public boolean FIREBASE_ENABLED = false;

    public int MIN_TRIKES = 12;



    public long waitTime = 3000;  //3 sec

    public List<DelegateInfo> jobsToDelegate = new ArrayList<>();

    public List<String> neighbourIds = null;

    public Utils utils;
    public Plans plans;

    /** The agent body. */
    @OnStart
    private void body() throws InterruptedException {
        utils = new Utils(this);
        plans = new Plans(this, utils);
        utils.body();
        bdiFeature.dispatchTopLevelGoal(new MaintainDistributeFirebaseJobs());
        bdiFeature.dispatchTopLevelGoal(new MaintainDistributeCSVJobs());
        bdiFeature.dispatchTopLevelGoal(new MaintainDistributeBufferJobs());
        bdiFeature.dispatchTopLevelGoal(new CheckAreaMessagesBuffer());
        bdiFeature.dispatchTopLevelGoal(new CheckProposals());
        bdiFeature.dispatchTopLevelGoal(new DelegateJobs());
        bdiFeature.dispatchTopLevelGoal(new CheckTrikeMessagesBuffer());
        bdiFeature.dispatchTopLevelGoal(new PrintSimTime());
        //bdiFeature.dispatchTopLevelGoal(new CheckAcks());
        //bdiFeature.dispatchTopLevelGoal(new ReceivedMessages());
    }

    @Goal(recur = true, recurdelay = 100 )
    private class CheckAreaMessagesBuffer{}

    @Plan(trigger=@Trigger(goals=CheckAreaMessagesBuffer.class))
    private void checkAreaMessagesBuffer(){
        plans.checkAreaMessagesBuffer();
    }

    @Goal(recur = true, recurdelay = 100 )
    private class CheckProposals{}

    @Plan(trigger=@Trigger(goals=CheckProposals.class))
    private void checkProposals(){
       plans.checkProposals();
    }


    @Goal(recur = true, recurdelay = 200)
    private class DelegateJobs{}

    @Plan(trigger=@Trigger(goals=DelegateJobs.class))
    private void delegateJobs(){
        for (DelegateInfo delegateInfo: jobsToDelegate) {
            if(delegateInfo.ts != -1) return;
            for (String neighbourId: neighbourIds){
                MessageContent messageContent = new MessageContent("BROADCAST", delegateInfo.job.toArrayList());
                Message message = new Message(areaAgentId, neighbourId, Message.ComAct.REQUEST, JadexModel.simulationtime, messageContent);
                IAreaTrikeService service = IAreaTrikeService.messageToService(agent, message);
                delegateInfo.ts = Instant.now().toEpochMilli();
                service.sendMessage(message.serialize());
            }
        }
    }


    @Goal(recur = true, recurdelay = 1000 )
    private class PrintSimTime {}
    @Plan(trigger=@Trigger(goals=PrintSimTime.class))
    private void printTime()
    {
        System.out.println("Simulation time: "+JadexModel.simulationtime);
        if(areaAgentId.equals("area: 1")){
            System.out.println(locatedAgentList.size());
        }
    }


    @Goal(recur = true, recurdelay = 20 )
    private class MaintainDistributeCSVJobs
    {
        @GoalMaintainCondition
        boolean isListEmpty(){
            return csvJobList.isEmpty();
        }
    }



    @Goal(recur = true, recurdelay = 1000 )
    private class MaintainDistributeFirebaseJobs
    {
        @GoalMaintainCondition
        boolean isListEmpty(){
            return jobList.isEmpty();
        }
    }

    @Goal(recur = true, recurdelay = 100 )
    private class MaintainDistributeBufferJobs{}

    @Plan(trigger=@Trigger(goals=MaintainDistributeBufferJobs.class))
    private void maintainDistributeBufferJobs()
    {
        if(jobRingBuffer.isEmpty()) return;
        delegatedJobs.add(jobRingBuffer.read());
        utils.sendJobToAgent(delegatedJobs);
    }


    @Plan(trigger=@Trigger(goals=MaintainDistributeFirebaseJobs.class))
    private void SendFirebaseJob()
    {
        utils.sendJobToAgent(jobList);
    }


    @Plan(trigger=@Trigger(goals=MaintainDistributeCSVJobs.class))
    private void SendCSVJob()
    {
        utils.sendJobToAgent(csvJobList);
    }


    @Goal(recur = true, recurdelay = 50 )
    private class CheckTrikeMessagesBuffer{}

    @Plan(trigger=@Trigger(goals=CheckTrikeMessagesBuffer.class))
    private void checkTrikeMessagesBuffer()
    {
       plans.checkTrikeMessagesBuffer();
    }

    @Goal(recur = true, recurdelay = 5000)
    private class CheckAcks{}

    @Plan(trigger=@Trigger(goals=CheckAcks.class))
    private void checkAcks(){
        plans.checkAcks();
    }

    @Goal(recur = true, recurdelay = 10000)
    private class ReceivedMessages{}

    @Plan(trigger=@Trigger(goals=ReceivedMessages.class))
    private void updateReceivedMessages(){
        Iterator<Long> iterator = receivedMessageIds.values().iterator();
        long currentTimeStamp = Instant.now().toEpochMilli();
        while (iterator.hasNext()){
            long timeStamp = iterator.next();
            if(currentTimeStamp >= timeStamp + 30000){
                iterator.remove();
            }
        }
    }
}