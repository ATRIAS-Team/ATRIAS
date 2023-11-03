package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;



import io.github.agentsoz.ees.jadexextension.masterthesis.Run.TrikeMain;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.ISendTripService.IsendTripService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.ISendTripService.SendtripService;
import io.github.agentsoz.util.Location;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.*;

import java.time.LocalDateTime;
import java.util.*;


@Agent(type = "bdi")

@ProvidedServices({
        @ProvidedService(type= IsendTripService.class, implementation=@Implementation( SendtripService.class))
})
@RequiredServices({
        @RequiredService(name="clockservice", type= IClockService.class),
        @RequiredService(name = "sendtripservices", type = IsendTripService.class),
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

    //TODO: Some kind of area AgentID needed for the comunication when more areaagents are used


    @Belief
    public List<Job> jobList1 = new ArrayList<>(); //Joblist for historic data

    @Belief
    public List<Job> jobList2 = new ArrayList<>(); //JobList for App data



    // TODO problably not used anymore, try to delete
    // to check the number of agents that are assigned ID, when all of the agents receive their IDs, then they could start sending trips
    @Belief
    public static int NumberAgentAssignedID;
    @Belief
    public static List<String> NumberSimInputAssignedID = new ArrayList<>();


    boolean done;


    /** predefined trips for tests */
    Location Location3= new Location("", 238654.693529, 5886721.094209);
    Trip Trip3 = new Trip("Trip1", "CustomerTrip", Location3, "NotStarted");
    Location Location4 = new Location("", 238674.543999, 5901195.908183);
    Trip Trip4 = new Trip("Trip2", "CustomerTrip", Location4, "NotStarted");


    /** The agent body. */
    @OnStart
    public void body() {

        System.out.println("TripRequestControlAgent sucessfully started;");


        /** example code delete after testing */

        Job Job1 = new Job("1", "1", LocalDateTime.now(), LocalDateTime.now(), new Location("", 238654.693529, 5886721.094209), new Location("", 238674.543999, 5901195.908183));
        Job Job2 = new Job("2", "2", LocalDateTime.now(), LocalDateTime.now(), new Location("", 238674.543999, 5901195.908183), new Location("", 238654.693529, 5886721.094209));
        jobList1.add(Job1);
        jobList1.add(Job2);


        System.out.println(JadexModel.simulationtime);

        //String message = Job1.JobForTransfer();
        //System.out.println(message);

                /** ########*/

        	bdiFeature.dispatchTopLevelGoal(new CheckNumberAgentAssignedID());

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
                System.out.println("Can start sending trip now to agents");
                           sendTriptoAgent();
            }
    }
    /*

         //either we send trip here or we use GUI to send. with GUI we could add manually which trip we want which agent to receive


     */
    public void sendTriptoAgent() {
        IFuture<Collection<IsendTripService>> sendservices = requiredServicesFeature.getServices("sendtripservices");
        sendservices.addResultListener(new DefaultResultListener<Collection<IsendTripService>>() {
            public void resultAvailable(Collection<IsendTripService> result) {
                for (Iterator<IsendTripService> it = result.iterator(); it.hasNext(); ) {
                    IsendTripService cs = it.next();
                            //TODO: send Job
//                            cs.sendJob("1");




                    //TODO: servicetag missing only a broadcast?
                    //TODO select closest agent
                    //TODO: run multiple times till jobList is empty
                    //TODO: get time from Matsim and send only when >= bookingTime
                    //TODO: time format of matsim like 43220.0 how to compare?
                    System.out.println(JadexModel.simulationtime);

                    String message = jobList1.get(0).JobForTransfer();
                    jobList1.remove(0);

                    //String message ="";
                    cs.sendJob(message);



                    		//cs.sendTrip("1");
                }
            }
        });



    }

}