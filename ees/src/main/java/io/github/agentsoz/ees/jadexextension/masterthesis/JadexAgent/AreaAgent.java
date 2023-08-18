package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;


import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.ISendTripService.IsendTripService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.ISendTripService.SendtripService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService.INotifyService;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.TrikeMain;
import io.github.agentsoz.util.Location;
import jadex.bdiv3.annotation.Belief;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.Trigger;
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

import java.util.*;


@Agent(type = "bdi")

@ProvidedServices({

})
@RequiredServices({
		@RequiredService(name="clockservice", type= IClockService.class),

})




/** 	This is the simplified Version of Trip Request Control Agent that is responsible for broadcasting
 * the request of customers to the Trike Agent. A GUI is created to write the trip randomly to trike Agent.In this scope this Agent should assign random trip to
 * Trike Agent to test if the Trike Agents react correctly incase their trip lists are modified.
 * 		To make sure everything is running correctly, this agent should only start sending trip to trike agents
 * once all of the trike agents are assigned to an unique ID.
 * */

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

    // to check the number of agents that are assigned ID, when all of the agents receive their IDs, then they could start sending trips
    @Belief
    public static int NumberAgentAssignedID;

    @Belief
    public static List<String> NumberSimInputAssignedID = new ArrayList<>();

    boolean done;


    Location Location1 = new Location("", 268674.543999, 5901195.908183);
    Trip Trip1 = new Trip("1", "CustomerTrip", Location1, "NotStarted");


    /**
     * The agent body.
     */
    @OnStart
    public void body() {

        System.out.println("TripRequestControlAgent sucessfully started;");
        bdiFeature.dispatchTopLevelGoal(new CheckNumberAgentAssignedID());

    }

/*
    @Goal (recur = true, recurdelay = 3000)
    class CheckNumberAgentAssignedID {
        public CheckNumberAgentAssignedID() {
        }
    }


    @Plan(trigger = @Trigger(goals = CheckNumberAgentAssignedID.class))
    public void PrintActiveAgent() {
        // when receive result from other agents, the plan somehow
        if (TrikeMain.TrikeAgentNumber== JadexModel.TrikeAgentnumber)
            if (done == false) {
                done = true;
                System.out.println("AREA Agent: Can start sending trip now to agents");
                System.out.println("Area Agent, Simulation Time: " + JadexModel.simulationtime);
            }
    }

*/


    @Goal(recur = true, orsuccess = false, recurdelay = 100)
    class DistributeTrips {
        public void DistributeTrips() {
        }

    }

    public String getClosestVehicleAgent(location startPosition){
        String closestAgent = "1";

        return closestAgent;
    };



    @Plan(trigger = @Trigger(goals = AreaAgent.DistributeTrips.class))
    private void SendTrip() {

        //getClosestVehicleAgent(Location1);
        // Trip erzeugen
        // an agent (agentid) aus getClosestVehicleAgent senden


        System.out.println(SensoryInputID + " start delivering data from MATSIM to "+agentId);
      ServiceQuery<INotifyService> query = new ServiceQuery<>(INotifyService.class);
      query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
      query.setServiceTags("user:" + agentId); // calling the tag of a trike agent
        Collection<INotifyService> service = agent.getLocalServices(query);
        for (Iterator<INotifyService> iteration = service.iterator(); iteration.hasNext(); ) {
            INotifyService cs = iteration.next();
            cs.NotifyotherAgent(ActionContentList, PerceptContentList, Activestatus); // assign data to vehicle agents via service
                            }


                    }






//    System.out.println(JadexModel.simulationtime);



}