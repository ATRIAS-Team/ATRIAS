package io.github.agentsoz.ees.jadexextension.masterthesis.Backup;


import io.github.agentsoz.ees.jadexextension.jadexagent.TrikeMain2;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.clock.IClockService;
import jadex.micro.annotation.*;

import java.util.ArrayList;
import java.util.List;


@Agent(type = "bdi")

@ProvidedServices({
		@ProvidedService(type= ICheckIDCompleteService.class, implementation=@Implementation(TripReqControlService.class)),
		@ProvidedService(type= IsendTripService.class, implementation=@Implementation( SendtripService.class))
})
@RequiredServices({
		@RequiredService(name="clockservice", type= IClockService.class),
		@RequiredService(name = "notifyassignedidservices", type = ICheckIDCompleteService.class),
		@RequiredService(name = "sendtripservices", type = IsendTripService.class),
})




/** 	This is the simplified Version of Trip Request Control Agent that is responsible for broadcasting
 * the request of customers to the Trike Agent. A GUI is created to write the trip randomly to trike Agent.In this scope this Agent should assign random trip to
 * Trike Agent to test if the Trike Agents react correctly incase their trip lists are modified.
 * 		To make sure everything is running correctly, this agent should only start sending trip to trike agents
 * once all of the trike agents are assigned to an unique ID.
 * */

public class TripRequestControlAgent {

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

    // to check the number of agents that are assigned ID, when all of the agents receive their IDs, then they could start sending trips
    @Belief
    public List<String> NumberAgentAssignedID = new ArrayList<>();


    boolean done;


    /**
     * The agent body.
     */
    @OnStart
    public void body() {

        //	bdiFeature.dispatchTopLevelGoal(new WaitforIDAssigned());
        //	bdiFeature.dispatchTopLevelGoal(new ReactoAgentIDAdded());

    }

    public void addassignedIDAgent(String text) {
        NumberAgentAssignedID.add(text);
    }

    public List<String> getNumberAgentAssignedID() {
        return NumberAgentAssignedID;
    }

    @Goal
    class CheckNumberAgentAssignedID {
        @GoalCreationCondition(factadded = "NumberAgentAssignedID")
        public CheckNumberAgentAssignedID() {
        }

        @GoalTargetCondition
        boolean IDAgentListupdated() {
            return (NumberAgentAssignedID.size() == 3); // later will change to variable instead of a constant number like this
        }

    }

    @Plan(trigger = @Trigger(goalfinisheds = CheckNumberAgentAssignedID.class))
    public void PrintActiveAgent() {
        // when receive result from other agents, the plan somehow
        if (done == false) {
            done = true;
            TrikeMain2.CansendtoAdc = true;
            System.out.println("Can start sending trip now to agents");
        }
/*

 	//either we send trip here or we use GUI to send. with GUI we could add manually which trip we want which agent to receive
	public void sendTriptoAgent() {
			IFuture<Collection<IsendTripService>> sendservices = requiredServicesFeature.getServices("sendtripservices");
			sendservices.addResultListener(new DefaultResultListener<Collection<IsendTripService>>() {
				public void resultAvailable(Collection<IsendTripService> result) {
					for (Iterator<IsendTripService> it = result.iterator(); it.hasNext(); ) {
						IsendTripService cs = it.next();

						cs.sendTrip(BDIMATSIMAgentMap);
					}
				}
			});
			*/


    }

}