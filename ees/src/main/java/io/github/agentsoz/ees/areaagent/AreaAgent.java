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

import static io.github.agentsoz.ees.areaagent.AreaConstants.NO_TRIKES_NO_TRIPS_LOAD;
import static io.github.agentsoz.ees.shared.SharedUtils.getCurrentDateTime;
import static io.github.agentsoz.ees.shared.SharedUtils.getSimTime;

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

    @Belief
    public List<DelegateInfo> jobsToDelegate = Collections.synchronizedList(new ArrayList<>());

    public List<String> neighbourIds = null;
    public int MIN_TRIKES = AreaConstants.MIN_TRIKES;

    public Utils utils;
    public Plans plans;

    public volatile double load = NO_TRIKES_NO_TRIPS_LOAD;
    public long lastDelegateRequestTS = -1;

    public long rebalanceInitTS = getSimTime() + 300000;
    public long lastLoadUpdateTS = -1;

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

        bdiFeature.dispatchTopLevelGoal(new PrintSimTime());
        bdiFeature.dispatchTopLevelGoal(new CheckRequests());
        bdiFeature.dispatchTopLevelGoal(new ReceivedMessages());

        //bdiFeature.dispatchTopLevelGoal(new CheckDelegateInfo());
        //bdiFeature.dispatchTopLevelGoal(new DelegateJobs());
        //bdiFeature.dispatchTopLevelGoal(new TrikeCount());
        //bdiFeature.dispatchTopLevelGoal(new TripsLoad());
    }

    @Goal(recur = true, recurdelay = 500)
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

    @Goal(recur = true, recurdelay = 500)
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

    @Goal(recur = true, recurdelay = 3000)
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
        System.out.println(areaAgentId + ": " + MIN_TRIKES + " mintrike");
    }

    @Goal(recur = true, recurdelay = 500)
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

    @Goal(recur = true, recurdelay = 5000)
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

    @Goal(recur = true, recurdelay = 300)
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

    @Goal(recur = true, recurdelay = 1000)
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

    @Goal(recur = true, recurdelay = 10000)
    private class ReceivedMessages {
    }

    @Plan(trigger = @Trigger(goals = ReceivedMessages.class))
    private void cleanupReceivedMessages() {
        SharedPlans.cleanupReceivedMessages(receivedMessageIds);
    }

    @Goal(recur = true, recurdelay = 1000)
    private class TrikeCount {
    }

    @Plan(trigger = @Trigger(goals = TrikeCount.class))
    private void checkTrikeCount() {
        plans.checkTrikeCount();
    }

    @Goal(recur = true, recurdelay = 3000)
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
                plans.checkTrikeMessagesBuffer(messageObj);
                break;
            case REQUEST:
                switch (messageObj.getContent().getAction()) {
                    case "trikesInArea":
                        plans.checkTrikeMessagesBuffer(messageObj);
                        break;
                }
                break;
            case CALL_FOR_PROPOSAL:
            case REJECT_PROPOSAL:
                plans.checkAreaMessagesBuffer(messageObj);
                break;
            case PROPOSE:
            case REFUSE:
                plans.checkProposalBuffer(messageObj);
                break;
            case ACCEPT_PROPOSAL:
                plans.checkAssignedJobs(messageObj);
                break;
        }
    }
}
