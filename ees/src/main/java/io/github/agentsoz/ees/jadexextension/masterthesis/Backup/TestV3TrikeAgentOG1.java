package io.github.agentsoz.ees.jadexextension.masterthesis.Backup;


import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.ees.jadexextension.jadexagent.TrikeMain;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexModel.JadexModel;
import io.github.agentsoz.util.Location;
import jadex.bdiv3.BDIAgentFactory;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.clock.IClockService;
import jadex.micro.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


@Agent(type= BDIAgentFactory.TYPE)
@ProvidedServices({
		@ProvidedService(type= IMappingAgentsService.class, implementation=@Implementation(WrittingIDService.class)),
		@ProvidedService(type= INotifyService.class, implementation=@Implementation(TrikeAgentDataReceiveService.class)),
		@ProvidedService(type= ICheckIDCompleteService.class, implementation=@Implementation(TrikeAgenttoControlAgentService.class))

})
@RequiredServices({
		@RequiredService(name="clockservice", type= IClockService.class),
		@RequiredService(name="chatservices", type= IMappingAgentsService.class),
		@RequiredService(name="broadcastingservices", type= INotifyService.class, scope= ServiceScope.PLATFORM),
		@RequiredService(name="notifyassignedidservices", type= ICheckIDCompleteService.class, scope= ServiceScope.PLATFORM),

		// multiple=true,
})


/*@Arguments({
		@Argument(name="keyword", clazz=String.class, defaultvalue="\"nerd\"", description="The keyword to react to."),
		@Argument(name="reply", clazz=String.class, defaultvalue="\"Watch your language\"", description="The reply message."),
		@Argument(name="thread", clazz=Thread.class, description="The reply message.")

})

 */

/*This is the most actual one that is using for Testing the whole Run1 process*/

public class TestV3TrikeAgentOG1 {

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
	protected boolean resultfromMASIM;

	// to indicate if the agent is available to take the new ride
	@Belief
	public boolean activestatus;

	@Belief
	public List<String> Triplist;

	@Belief
	private static String vehicleID;

	@Belief
	private static Location agentLocation; // position of the agent

	@Belief    //contains all the trips
	public List<Trip> tripList = new ArrayList<>();

	@Belief    //contains the current Trip
	public List<Trip> currentTrip = new ArrayList<>();

	@Belief
	private List<ActionContent> ActionContentList;

	@Belief
	private List<PerceptContent> PerceptContentList;

	@Belief
	private String agentID = null; // store agent ID from the map
	public String actionID;
	public Object[] actionparameters;
	@Belief
	public boolean sent = false;
	public String write = null;
	public String agentName;
	public String currentAreaAgent;
	//public TestV2EnvironmentActionAgent storageagent;


	Location Location1 = new Location("", 238654.693529, 5886721.094209);

	Trip Trip1 = new Trip("Trip1", "CustomerTrip", Location1, "NotStarted");

	Location Location2 = new Location("", 238674.543999, 5901195.908183);

	Trip Trip2 = new Trip("Trip2", "CustomerTrip", Location2, "NotStarted");


	/**
	 * The agent body.
	 */
	@OnStart
	public void body() {

		/**31.01.2022: Adding mapping to Jadex Agent because of shitty name that could not be changed**/


		AddAgentNametoAgentList();
		activestatus = true;
		currentAreaAgent = getRandomAreaAgent();


		//	bdiFeature.dispatchTopLevelGoal(new WaitforIDAssigned());
		//	bdiFeature.dispatchTopLevelGoal(new ReactoAgentIDAdded());
		System.out.println("TrikeAgent sucessfully started;");
		//	bdiFeature.dispatchTopLevelGoal(new ReactoNotificationfromEnvActAgent());
	}


	//#######################################################################
	//Goals and Plans for Sending data to AgentDataContainer, for the Aktorik
	//#######################################################################

	@Goal(recur = true, recurdelay = 3000)
	class SendTooutAdc {
		// Goal should be trigger when triplist is not empty and activestatus = true
		//Need to check both because in the begining, the activestatus is true as default, so the change in triplist should be
		//triggered for trip list to be sent to adc. Later in cycle, the change in activestatus will trigger for trip list to be send to adc.
		@GoalCreationCondition(factadded = "tripList", beliefs = "activestatus") //
		public SendTooutAdc() {
			System.out.println("Send to outadc");
		}

		@GoalTargetCondition
		boolean senttoMATSIM() {
			return !(activestatus == false);
		}
	}

	//should take the first trip from the trip list
	// three scenarios need to be considered:
	// New Trip, agent is available when MATSIM is takecontrol -> Trip is added to OutADC, no remove from potentialActiveAgentList
	// New Trip, agent is just available, BDI is takecontrol -> Trip is added to OutADC, remove agent from potentialactiveAgentList
	// No New Trip, agent is available, BDI is takecontrol -> No trip is added, remove agent


	@Plan(trigger = @Trigger(goalfinisheds = SendTooutAdc.class))
	public void SendtoAgentDataContainer() {
		System.out.println("DoNextTrip running");
		System.out.println("tripList: " + tripList.size());
		System.out.println("currentTrip: " + currentTrip.size());


		newCurrentTrip(); // creates new current Trip if necessary and possible
		if (currentTrip.size() == 1) { //if there is a currentTrip
			currentTripStatus();

			if (currentTrip.get(0).getProgress().equals("AtEndLocation")) {
				updateCurrentTripProgress("Finished");
			} else if (currentTrip.get(0).getProgress().equals("NotStarted")) {

				Object[] Startparams = new Object[2];
				Startparams[0] = Constants.DRIVETO;
				Startparams[1] = currentTrip.get(0).getStartPosition();

				//##########################################
				//send drive to startPosition to Matsim
				JadexModel.storageAgent.getEnvironmentActionInterface().packageAction(agentID, "drive_to", Startparams, null);
				activestatus = false;

				//##########################################
				updateCurrentTripProgress("DriveToStart");
			} else if (currentTrip.get(0).getProgress().equals("AtStartLocation")) {
				// manage CustomerTrips that are AtStartLocation
				if (currentTrip.get(0).getTripType().equals("CustomerTrip")) {
					if (customerMiss() == true) { // customer not there
						updateCurrentTripProgress("Failed");
					} else if (customerMiss() == false) { // customer still there

						Object[] Endparams = new Object[2];
						Endparams[0] = Constants.DRIVETO;
						Endparams[1] = currentTrip.get(0).getEndPosition();
						//##########################################
						//todo send drive to endPosition to Matsim
						JadexModel.storageAgent.getEnvironmentActionInterface().packageAction(agentID, "drive_to", Endparams, null);
						activestatus = false;

						//##########################################
						updateCurrentTripProgress("DriveToEnd");
					}
				}
				//add cases for other TripTypes here
				//else if(currentTrip.get(0).getTripType().equals("")) {
				//}
				// manage all other Trips that are AtStartLocation
				else {
					updateCurrentTripProgress("Finished");
				}

			}
			// If the CurrentTrip is finshied or failed > remove it
			if (currentTrip.get(0).getProgress().equals("Finished") || currentTrip.get(0).getProgress().equals("Failed")) {
				currentTrip.remove(0);
			}
		}
		currentTripStatus();
	}

	//#######################################################################
	//Goals and Plans : After the agentID is assigned to the Trike Agent,
	//TrikeAgent notifies TripRequestControlAgent by writting in its list NumberAgentAssignedID
	//#######################################################################

	@Goal(recur = true, recurdelay = 3000)
	class ReactoAgentIDAdded {
		@GoalCreationCondition(beliefs = "agentID")
		public ReactoAgentIDAdded() {
			System.out.println("Goal react if agentID is added ");
		}

		@GoalTargetCondition
		boolean IDupdated() {
			return !(agentID == null);
		}
	}

	@Plan(trigger = @Trigger(goalfinisheds = ReactoAgentIDAdded.class))
	private void ReacttoAgentIDAdded()
/*
	{	System.out.println("check how many time plans is triggered1");
			if (sent == null) {
				sent = "1"; // in case the plan is executed too much
				IFuture<Collection<ICheckIDCompleteService>> checkIDCompleteservices = requiredServicesFeature.getServices("notifyassignedidservices");
				checkIDCompleteservices.addResultListener(new DefaultResultListener<Collection<ICheckIDCompleteService>>() {
					public void resultAvailable(Collection<ICheckIDCompleteService> result) {
						for (Iterator<ICheckIDCompleteService> it = result.iterator(); it.hasNext(); ) {
							ICheckIDCompleteService cs = it.next();
							cs.WriteinIDAssignedList("AssignedIDDone");

						}
					}
				});

 */
	// After an Agent the the ID, start execute the trip
	{
		if (agentID != null) {
			if (sent == false) {
				sent = true;
				if (currentAreaAgent == "AreaAgent1") {
					AreaAgent1.addTrikeAgent(agentID);
					TrikeMain.TrikeAgentNumber = TrikeMain.TrikeAgentNumber + 1;

				}

				if (currentAreaAgent == "AreaAgent2") {
					AreaAgent2.addTrikeAgent(agentID);
					TrikeMain.TrikeAgentNumber = TrikeMain.TrikeAgentNumber + 1;

				}


				tripList.add(getRandomTrip());
				System.out.println("plan trigger when this agent id is assigned" + this.agentID);
				agent.getFeature(IBDIAgentFeature.class).dropGoal(ReactoAgentIDAdded.class); // this needs to be tested


			}
		}
	}


	@Goal(recur = true,recurdelay = 3000)
	class PerformSIMReceive {
		// Goal should be triggered when the simPerceptList or simContentList are triggered
		//Need to check both because in the begining, the activestatus is true as default, so the change in triplist should be
		//triggered for trip list to be sent to adc. Later in cycle, the change in activestatus will trigger for trip list to be send to adc.
		@GoalCreationCondition(beliefs = "resultfromMASIM") //
		public PerformSIMReceive() {
			System.out.println("We receive information from MATSIM");
		}
		@GoalTargetCondition
		boolean	PerceptorContentnotEmpty()
		{
			return ( !(PerceptContentList.isEmpty())|| !(ActionContentList.isEmpty()));
		}
	}

	//should take the first trip from the trip list
	@Plan(trigger = @Trigger(goalfinisheds = PerformSIMReceive.class))
	public void UpdateSensory()
	{
		for (ActionContent actionContent : ActionContentList) {
			System.out.println(actionContent.getState());
			System.out.println(actionContent.getParameters());
		}

		for (PerceptContent perceptContent: PerceptContentList) {

			System.out.println(perceptContent.getParameters());
		}

	}





	/****/
	public void ReactToMATSIM() {
		System.out.println("sucessfully dispatch goals");
	}

	public boolean getResultfromMASIM() {
		System.out.println("sucessfully update data from data from MATSIM" + resultfromMASIM);
		return resultfromMASIM;
	}


	public void setResultfromMASIM(boolean Result) {
		this.resultfromMASIM = Result;
	}

	public void AddAgentNametoAgentList()
	{
		BDIMappingAgent.JadexAgentNameList.add(agent.getId().getName());
	}

	public void AddTriptoTripList(String Trip)
	{
		Triplist.add(Trip);
	}

	public void setAgentID(String agentid) {
		agentID = agentid;
	}

	public String getAgentID() {
		System.out.println(agentID);

		return agentID;
	}

	public void setActionContentList(List<ActionContent> actionContentList) {
		ActionContentList = actionContentList;
	}

	public List<ActionContent> getActionContentList() {
		return ActionContentList;
	}

	public void setPerceptContentList(List<PerceptContent> perceptContentList) {
		PerceptContentList = perceptContentList;
	}

	public List<PerceptContent> getPerceptContentList() {
		return PerceptContentList;
	}

	public String getRandomAreaAgent() {
		List<String> AreaAgentList = Arrays.asList("AreaAgent1","AreaAgent2");
		Random rand = new Random();
		String randomAreaAgent = AreaAgentList.get(rand.nextInt(AreaAgentList.size()));
		return randomAreaAgent;
	}

	public Trip getRandomTrip() {
		List<Trip> RandomTripList = Arrays.asList(Trip1,Trip2);
		Random rand = new Random();
		Trip randomTrip = RandomTripList.get(rand.nextInt(RandomTripList.size()));
		return randomTrip;
	}

	void newCurrentTrip(){
		System.out.println("Test if new currentTrip can bea created");
		if(currentTrip.size()==0 && tripList.size()>0 ){
			System.out.println("no currentTrip available");
			System.out.println("getting nextTrip from TripList");
			currentTrip.add(tripList.get(0));
			tripList.remove(0);
		}
	}

	/** Updates the progress of the CurrentTrip
	 *
	 * @param newProgress
	 */
	void updateCurrentTripProgress(String newProgress) {
		Trip CurrentTripUpdate = currentTrip.get(0);
		CurrentTripUpdate.setProgress(newProgress);
		currentTrip.set(0, CurrentTripUpdate);
	}

	void currentTripStatus() {
		String currentTripType = currentTrip.get(0).getTripType();
		LocalDateTime currentVaTime = currentTrip.get(0).getVaTime();
		Location currentStartPosition = currentTrip.get(0).getStartPosition();
		Location currentEndPosition = currentTrip.get(0).getEndPosition();
		String currentProgress = currentTrip.get(0).getProgress();
		System.out.println("\n currentTripStatus:");
		System.out.println("currentTripType: " + currentTripType);
		System.out.println("currentVaTime: " + currentVaTime);
		System.out.println("currentStartPosition: " + currentStartPosition);
		System.out.println("currentEndPosition: " + currentEndPosition);
		System.out.println("currentProgress: " + currentProgress);
	}

	boolean customerMiss() {
		//##########################################
		//todo: access time information and determine if the customer have alraedy leaved
		//##########################################
		boolean customerMiss = true;

		return customerMiss;
	}


}








