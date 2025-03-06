/** AreaAgent
 *  Version: v0.6 (19.11.2024)
 *  changelog: merge
 *  @Author Marcel, Mahkam
 */
package io.github.agentsoz.ees.areaagent;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import io.github.agentsoz.ees.shared.Job;
import io.github.agentsoz.ees.shared.Message;
import io.github.agentsoz.ees.shared.SharedPlans;
import io.github.agentsoz.ees.JadexService.AreaTrikeService.AreaAgentService;
import io.github.agentsoz.ees.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.util.RingBuffer;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.clock.IClockService;
import jadex.micro.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.github.agentsoz.ees.shared.SharedUtils.getCurrentDateTime;


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


    //  JOB LISTS
    @Belief
    public List<Job> csvJobList = new ArrayList<>(); // job list for historic data
    @Belief
    public final List<Job> jobList = new ArrayList<>(); // job list for App data
    @Belief
    public List<Job> assignedJobs = new ArrayList<>();


    public LocatedAgentList locatedAgentList = new LocatedAgentList();

    public String cell;

    @Belief
    public String areaAgentId = null;
    public String myTag = null;

    public List<Message> requests = new ArrayList<>();  //requests are sorted by timestamp
    public Map<UUID, Long> receivedMessageIds = new HashMap<>(256);

    //  BUFFER
    public RingBuffer<Message> areaMessagesBuffer = new RingBuffer<>(256);
    public RingBuffer<Message> jobRingBuffer = new RingBuffer<>(256);
    public RingBuffer<Message> proposalBuffer = new RingBuffer<>(256);
    public RingBuffer<Message> messagesBuffer = new RingBuffer<>(256);


    public volatile boolean canDemand = true;

    public List<DelegateInfo> jobsToDelegate = new ArrayList<>();

    public List<String> neighbourIds = null;

    public Utils utils;
    public Plans plans;

    /** The agent body. */
    @OnStart
    private void body() {
        utils = new Utils(this);
        plans = new Plans(this, utils);
        utils.body();
        //bdiFeature.dispatchTopLevelGoal(new MaintainDistributeFirebaseJobs());
        bdiFeature.dispatchTopLevelGoal(new MaintainDistributeCSVJobs());
        //bdiFeature.dispatchTopLevelGoal(new MaintainDistributeAssignedJobs());
        //bdiFeature.dispatchTopLevelGoal(new JobBuffer());
        //bdiFeature.dispatchTopLevelGoal(new AreaMessagesBuffer());
        //bdiFeature.dispatchTopLevelGoal(new CheckProposals());
        //bdiFeature.dispatchTopLevelGoal(new DelegateJobs());
        bdiFeature.dispatchTopLevelGoal(new TrikeMessagesBuffer());
        bdiFeature.dispatchTopLevelGoal(new PrintSimTime());
        bdiFeature.dispatchTopLevelGoal(new CheckRequests());
        //bdiFeature.dispatchTopLevelGoal(new CheckDelegateInfo());
        bdiFeature.dispatchTopLevelGoal(new ReceivedMessages());
        //bdiFeature.dispatchTopLevelGoal(new TrikeCount());
    }


    @Goal(recur = true, recurdelay = 40 )
    private class AreaMessagesBuffer{}
    @Plan(trigger=@Trigger(goals=AreaMessagesBuffer.class))
    private void checkAreaMessagesBuffer(){
        plans.checkAreaMessagesBuffer();
    }


    @Goal(recur = true, recurdelay = 50 )
    private class CheckProposals{}
    @Plan(trigger=@Trigger(goals=CheckProposals.class))
    private void checkProposals(){
       plans.checkProposalBuffer();
    }


    @Goal(recur = true, recurdelay = 50 )
    private class CheckDelegateInfo{}
    @Plan(trigger=@Trigger(goals=CheckDelegateInfo.class))
    private void checkDelegateInfo(){
        plans.checkDelegateInfo();
    }


    @Goal(recur = true, recurdelay = 100)
    private class DelegateJobs{}
    @Plan(trigger=@Trigger(goals=DelegateJobs.class))
    private void delegateJobs(){
        plans.delegateJobs();
    }


    @Goal(recur = true, recurdelay = 3000 )
    private class PrintSimTime {}
    @Plan(trigger=@Trigger(goals=PrintSimTime.class))
    private void printTime()
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        System.out.println("Simulation Time: " + getCurrentDateTime().format(dateTimeFormatter));
        System.out.println(areaAgentId + ": "+ locatedAgentList.size() + " Trikes");
    }


    @Goal(recur = true, recurdelay = 100 )
    private class MaintainDistributeAssignedJobs
    {}
    @Plan(trigger=@Trigger(goals=MaintainDistributeAssignedJobs.class))
    private void sendAssignedJobs()
    {
        utils.sendJobToAgent(assignedJobs);
    }


    @Goal(recur = true, recurdelay = 100 )
    private class JobBuffer{}
    @Plan(trigger=@Trigger(goals=JobBuffer.class))
    private void checkJobBuffer() { plans.checkAssignedJobs(); }


    @Goal(recur = true, recurdelay = 1000 )
    private class MaintainDistributeFirebaseJobs
    {
        @GoalMaintainCondition
        boolean isListEmpty(){
            return jobList.isEmpty();
        }
    }
    @Plan(trigger=@Trigger(goals=MaintainDistributeFirebaseJobs.class))
    private void SendFirebaseJob()
    {
        utils.sendJobToAgent(jobList);
    }


    @Goal(recur = true, recurdelay = 15 )
    private class MaintainDistributeCSVJobs
    {
        @GoalMaintainCondition
        boolean isListEmpty(){
            return csvJobList.isEmpty();
        }
    }
    @Plan(trigger=@Trigger(goals=MaintainDistributeCSVJobs.class))
    private void SendCSVJob()
    {
        utils.sendJobToAgent(csvJobList);
    }


    @Goal(recur = true, recurdelay = 15 )
    private class TrikeMessagesBuffer{}
    @Plan(trigger=@Trigger(goals=TrikeMessagesBuffer.class))
    private void checkTrikeMessagesBuffer()
    {
       plans.checkTrikeMessagesBuffer();
    }


    @Goal(recur = true, recurdelay = 5000)
    private class CheckRequests{}
    @Plan(trigger=@Trigger(goals=CheckRequests.class))
    private void checkRequestTimeouts(){
        plans.checkRequestTimeouts();
    }


    @Goal(recur = true, recurdelay = 10000)
    private class ReceivedMessages{}
    @Plan(trigger=@Trigger(goals=ReceivedMessages.class))
    private void cleanupReceivedMessages(){
        SharedPlans.cleanupReceivedMessages(receivedMessageIds);
    }

    @Goal(recur = true, recurdelay = 1000)
    private class TrikeCount{}
    @Plan(trigger=@Trigger(goals=TrikeCount.class))
    private void checkTrikeCount(){
        plans.checkTrikeCount();
    }
}
