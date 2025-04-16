/** AreaAgent
 *  Version: v0.6 (19.11.2024)
 *  changelog: merge
 *
 * @Author Marcel, Mahkam
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
import io.github.agentsoz.ees.Run.JadexModel;
import io.github.agentsoz.ees.shared.Job;
import io.github.agentsoz.ees.shared.Message;
import io.github.agentsoz.ees.shared.SharedPlans;
import io.github.agentsoz.ees.JadexService.AreaTrikeService.AreaAgentService;
import io.github.agentsoz.ees.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.shared.SharedUtils;
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
import java.util.concurrent.ConcurrentHashMap;

import static io.github.agentsoz.ees.shared.SharedUtils.getCurrentDateTime;

@Agent(type = "bdi")

@ProvidedServices({
    @ProvidedService(type = IAreaTrikeService.class, implementation = @Implementation(AreaAgentService.class)),})
@RequiredServices({
    @RequiredService(name = "clockservice", type = IClockService.class),
    @RequiredService(name = "sendareaagendservice", type = IAreaTrikeService.class),})

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
    public List<Job> assignedJobs = Collections.synchronizedList(new ArrayList<>());

    public LocatedAgentList locatedAgentList = new LocatedAgentList();

    public String cell;

    @Belief
    public String areaAgentId = null;
    public String myTag = null;

    @Belief
    public List<Message> requests = Collections.synchronizedList(new ArrayList<>());  //requests are sorted by timestamp
    public Map<UUID, Long> receivedMessageIds = new ConcurrentHashMap<>(2048);

    //  BUFFER
    @Belief
    public RingBuffer<Message> areaMessagesBuffer = new RingBuffer<>(64);
    @Belief
    public RingBuffer<Message> jobRingBuffer = new RingBuffer<>(16);
    @Belief
    public RingBuffer<Message> proposalBuffer = new RingBuffer<>(64);
    @Belief
    public RingBuffer<Message> messagesBuffer = new RingBuffer<>(256);
    @Belief
    public List<DelegateInfo> jobsToDelegate = Collections.synchronizedList(new ArrayList<>());

    public List<String> neighbourIds = null;
    public int MIN_TRIKES = AreaConstants.MIN_TRIKES;

    public Utils utils;
    public Plans plans;

    public static final double NO_TRIKES_NO_TRIPS_LOAD = 100;

    public volatile double load = NO_TRIKES_NO_TRIPS_LOAD;
    public long lastDelegateRequestTS = 0;

    public long rebalanceInitTS = 600000;
    public long lastLoadUpdateTS = 0;

    /**
     * The agent body.
     */
    @OnStart
    private void body() {
        utils = new Utils(this);
        plans = new Plans(this, utils);
        utils.body();

        SharedUtils.areaAgentMap.put(areaAgentId, this);

        //bdiFeature.dispatchTopLevelGoal(new MaintainDistributeFirebaseJobs());
        bdiFeature.dispatchTopLevelGoal(new MaintainDistributeCSVJobs());
        bdiFeature.dispatchTopLevelGoal(new MaintainDistributeAssignedJobs());

        //bdiFeature.dispatchTopLevelGoal(new JobBuffer());
        //bdiFeature.dispatchTopLevelGoal(new AreaMessagesBuffer());
        //bdiFeature.dispatchTopLevelGoal(new CheckProposals());
        //bdiFeature.dispatchTopLevelGoal(new TrikeMessagesBuffer());
        //bdiFeature.dispatchTopLevelGoal(new DelegateJobs());
        bdiFeature.dispatchTopLevelGoal(new PrintSimTime());
        bdiFeature.dispatchTopLevelGoal(new CheckRequests());
        //bdiFeature.dispatchTopLevelGoal(new CheckDelegateInfo());
        bdiFeature.dispatchTopLevelGoal(new ReceivedMessages());
        //bdiFeature.dispatchTopLevelGoal(new TrikeCount());

        //bdiFeature.dispatchTopLevelGoal(new TripsLoad());
    }

    @Goal(recur = true, recurdelay = 700, randomselection = true)
    private class AreaMessagesBuffer {

        @GoalMaintainCondition
        private boolean isEmpty() {
            return areaMessagesBuffer.isEmpty();
        }
    }

    @Plan(trigger = @Trigger(goals = AreaMessagesBuffer.class))
    private void checkAreaMessagesBuffer() {
        plans.checkAreaMessagesBuffer();
    }

    @Goal(recur = true, recurdelay = 800, randomselection = true)
    private class CheckProposals {

        @GoalMaintainCondition
        private boolean isEmpty() {
            return proposalBuffer.isEmpty();
        }
    }

    @Plan(trigger = @Trigger(goals = CheckProposals.class))
    private void checkProposals() {
        plans.checkProposalBuffer();
    }

    @Goal(recur = true, recurdelay = 800, randomselection = true)
    private class CheckDelegateInfo {

        @GoalMaintainCondition
        private boolean isEmpty() {
            return jobsToDelegate.isEmpty();
        }
    }

    @Plan(trigger = @Trigger(goals = CheckDelegateInfo.class))
    private void checkDelegateInfo() {
        plans.checkDelegateInfo();
    }

    @Goal(recur = true, recurdelay = 800, randomselection = true)
    private class DelegateJobs {

        @GoalMaintainCondition
        private boolean isEmpty() {
            return jobsToDelegate.isEmpty();
        }
    }

    @Plan(trigger = @Trigger(goals = DelegateJobs.class))
    private void delegateJobs() {
        plans.delegateJobs();
    }

    @Goal(recur = true, recurdelay = 3000, randomselection = true)
    private class PrintSimTime {
    }

    @Plan(trigger = @Trigger(goals = PrintSimTime.class))
    private void printTime() {
        if (areaAgentId.equals("area: 0")) {
            System.out.println(JadexModel.simulationtime);
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            System.out.println("Simulation Time: " + getCurrentDateTime().format(dateTimeFormatter));
        }
        System.out.println(areaAgentId + ": " + locatedAgentList.size() + " Trikes");
        System.out.println(areaAgentId + ": " + load + " Load");
        synchronized (jobsToDelegate) {
            System.out.println(jobsToDelegate.size() + " jobs to delegate");
        }
        synchronized (requests) {
            System.out.println(requests.size() + " area requests");
            if (requests.size() > 5) {
                System.out.println("ALERT");
                for (Message request : requests) {
                    System.out.println("sender: " + request.getSenderId());
                    System.out.println("receiver: " + request.getReceiverId());
                    System.out.println("content: " + request.getContent().values);

                }
            }
        }
    }

    @Goal(recur = true, recurdelay = 500, randomselection = true)
    private class MaintainDistributeAssignedJobs {

        @GoalMaintainCondition
        private boolean isEmpty() {
            return assignedJobs.isEmpty();
        }
    }

    @Plan(trigger = @Trigger(goals = MaintainDistributeAssignedJobs.class))
    private void sendAssignedJobs() {
        utils.sendJobToAgent(assignedJobs);
    }

    @Goal(recur = true, recurdelay = 800, randomselection = true)
    private class JobBuffer {

        @GoalMaintainCondition
        private boolean isEmpty() {
            return jobRingBuffer.isEmpty();
        }
    }

    @Plan(trigger = @Trigger(goals = JobBuffer.class))
    private void checkJobBuffer() {
        plans.checkAssignedJobs();
    }

    @Goal(recur = true, recurdelay = 5000, randomselection = true)
    private class MaintainDistributeFirebaseJobs {

        @GoalMaintainCondition
        boolean isListEmpty() {
            return jobList.isEmpty();
        }
    }

    @Plan(trigger = @Trigger(goals = MaintainDistributeFirebaseJobs.class))
    private void SendFirebaseJob() {
        utils.sendJobToAgent(jobList);
    }

    @Goal(recur = true, recurdelay = 2000)
    private class MaintainDistributeCSVJobs {

        @Goal(recur = true, recurdelay = 500, randomselection = true)
        private class MaintainDistributeCSVJobs {

            @GoalMaintainCondition
            boolean isListEmpty() {
                return csvJobList.isEmpty();
            }
        }

        @Plan(trigger = @Trigger(goals = MaintainDistributeCSVJobs.class))
        private void SendCSVJob() {
            utils.sendJobToAgent(csvJobList);
        }

        @Goal(recur = true, recurdelay = 300, randomselection = true)
        private class TrikeMessagesBuffer {

            @GoalMaintainCondition
            private boolean isEmpty() {
                return messagesBuffer.isEmpty();
            }
        }

        @Plan(trigger = @Trigger(goals = TrikeMessagesBuffer.class))
        private void checkTrikeMessagesBuffer() {
            plans.checkTrikeMessagesBuffer();
        }

        @Goal(recur = true, recurdelay = 1000, randomselection = true)
        private class CheckRequests {

            @GoalMaintainCondition
            private boolean isEmpty() {
                return requests.isEmpty();
            }
        }

        @Plan(trigger = @Trigger(goals = CheckRequests.class))
        private void checkRequestTimeouts() {
            plans.checkRequestTimeouts();
        }

        @Goal(recur = true, recurdelay = 10000, randomselection = true)
        private class ReceivedMessages {
        }

        @Plan(trigger = @Trigger(goals = ReceivedMessages.class))
        private void cleanupReceivedMessages() {
            SharedPlans.cleanupReceivedMessages(receivedMessageIds);
        }

        @Goal(recur = true, recurdelay = 1000, randomselection = true)
        private class TrikeCount {
        }

        @Plan(trigger = @Trigger(goals = TrikeCount.class))
        private void checkTrikeCount() {
            plans.checkTrikeCount();
        }

        @Goal(recur = true, recurdelay = 3000, randomselection = true)
        private class TripsLoad {
        }

        @Plan(trigger = @Trigger(goals = TripsLoad.class))
        private void updateTripsLoad() {
            plans.updateTripsLoad();
        }

        public void sendMessage(String messageStr) {
            Message messageObj = Message.deserialize(messageStr);

            if (this.receivedMessageIds.containsKey(messageObj.getId())) {
                return;
            }
            this.receivedMessageIds.put(messageObj.getId(), SharedUtils.getSimTime());

            switch (messageObj.getComAct()) {
                case INFORM:
                case ACK:
                case NACK:
                    this.messagesBuffer.write(messageObj);
                    checkTrikeMessagesBuffer();
                    break;
                case REQUEST:
                    switch (messageObj.getContent().getAction()) {
                        case "trikesInArea":
                            this.messagesBuffer.write(messageObj);
                            plans.checkTrikeMessagesBuffer();
                            break;
                    }
                    break;
                case CALL_FOR_PROPOSAL:
                case REJECT_PROPOSAL:
                    this.areaMessagesBuffer.write(messageObj);
                    plans.checkAreaMessagesBuffer();
                    break;
                case PROPOSE:
                case REFUSE:
                    this.proposalBuffer.write(messageObj);
                    plans.checkProposalBuffer();
                    break;
                case ACCEPT_PROPOSAL:
                    this.jobRingBuffer.write(messageObj);
                    plans.checkAssignedJobs();
                    break;
            }
        }
    }
