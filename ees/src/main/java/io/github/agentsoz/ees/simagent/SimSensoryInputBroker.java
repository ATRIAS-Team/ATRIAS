package io.github.agentsoz.ees.simagent;

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


import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.ees.JadexService.AreaTrikeService.IAreaTrikeService;
import io.github.agentsoz.ees.JadexService.MappingService.IMappingInputBrokerService;
import io.github.agentsoz.ees.JadexService.MappingService.WritingBrokerIDService;
import io.github.agentsoz.ees.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.ees.JadexService.NotifyService2.SimInputBrokerReceiveService;
import io.github.agentsoz.ees.JadexService.NotifyService.INotifyService;
import io.github.agentsoz.ees.JadexService.NotifyService.SimInputBrokerService;
import io.github.agentsoz.ees.Run.JadexModel;
import io.github.agentsoz.ees.shared.SharedUtils;
import io.github.agentsoz.ees.trikeagent.TrikeAgent;
import jadex.bdiv3.BDIAgentFactory;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.ServiceQuery;
import jadex.bridge.service.types.clock.IClockService;
import jadex.micro.annotation.*;


import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Agent(type=BDIAgentFactory.TYPE)
@Description("<h1>Trike Robot Agent</h1>")
@ProvidedServices({
		@ProvidedService(type= INotifyService.class, implementation=@Implementation(SimInputBrokerService.class)),
		@ProvidedService(type= INotifyService2.class, implementation=@Implementation(SimInputBrokerReceiveService.class)),
		@ProvidedService(type= IMappingInputBrokerService.class, implementation = @Implementation(WritingBrokerIDService.class)),
})
@RequiredServices({
		@RequiredService(name="mapbrokerservices", type= IMappingInputBrokerService.class),
		@RequiredService(name="clockservice", type= IClockService.class),
		@RequiredService(name="notifyservices", type= INotifyService.class, scope= ServiceScope.PLATFORM),
		@RequiredService(name="notifywhenexecutiondoneservice", type= INotifyService2.class, scope= ServiceScope.PLATFORM),


})

public class SimSensoryInputBroker {

	/**
	 * The bdi agent. Automatically injected
	 */
	@Agent
	private IInternalAccess agent;
	@AgentFeature
	protected IRequiredServicesFeature requiredServicesFeature;
	@AgentFeature
	protected IBDIAgentFeature bdiFeature;


	public static boolean NewDatafromMATSIM;// if the Data from MATSIM is coming, JadexModel should flag it as true

	public static AgentDataContainer inAdcMATSIM = new AgentDataContainer();

	@Belief
	private String SensoryInputID;

	public boolean executed = false;



	@Belief
	public List<String> Registeredagents = new ArrayList<>();

	public List<String> ActiveAgentList = new ArrayList<>();

	ExecutorService executorService = Executors.newFixedThreadPool(4);

	@Belief
	public boolean WriteinTrikeAgent = false; // contain only agent id and action content of relevant agents

//	private final Logger logger = LoggerFactory.getLogger(TrikeAgentMVP.class);

	/**
	 * The agent body.
	 */
	@OnStart
	public void body() {
		System.out.println("SimSensoryInputBroker is sucessfully started");
		AddSimSensoryNametoList();
		bdiFeature.dispatchTopLevelGoal(new PerformCheckDatafromMATSIM());
	}


	//#######################################################################
	//Goals and Plans : When data from MATSIM is coming, process and assign data to the belief
	// of Trike Agent. Mark this action as done when this process is done
	//#######################################################################

	@Goal(recur = true, orsuccess = false, recurdelay = 100)
	class PerformCheckDatafromMATSIM {
		public void PerformCheckDatafromMATSIM() {
		}

	}

	@Plan(trigger = @Trigger(goals = PerformCheckDatafromMATSIM.class))
	private void performcheckDatafromMATSIM() throws IOException {

		if (NewDatafromMATSIM == true) {
			if (!JadexModel.answeredInputBroker.contains(SensoryInputID)) { // to check if its already answer JADEXModel after finishing in this iteration. if yes, not execute again in the iteration

				if (this.WriteinTrikeAgent == false) // to make sure to write once in the trike agent in an iteration
				//get the list of newly active agent (agent with status success/ drop who are ready to take next trip)
				//Filter only result that is relevant to the agents that register in this area
				{
					Iterator<String> it = inAdcMATSIM.getAgentIdIterator();
					while (it.hasNext()) {
						List<ActionContent> ActionContentList = new ArrayList<>();
						List<PerceptContent> PerceptContentList = new ArrayList<>();
						boolean Activestatus = false;

						String agentId = it.next();
						if (this.Registeredagents.contains(agentId)) // only process data from agents that register here
						{
							Map<String, ActionContent> actions = inAdcMATSIM.getAllActionsCopy(agentId);// key: ActionID, value :Content . only content is important

							for (String actionId : actions.keySet()) { //likely there will only one action ID at a time but to be sure

								ActionContent content = actions.get(actionId);
								if (content != null) {
									ActionContentList.add(content); // add to the actioncontentlist to send to trike agent
									if (actionId == "drive_to") {
										ActionContent.State actionState = content.getState();
										if (actionState == ActionContent.State.PASSED ||
												actionState == ActionContent.State.FAILED)
										{
											ActiveAgentList.add(agentId); // create a list of potential active agents
											Activestatus = true; // local active status to assign to each trike agent in this iteration via service later
										}
									}
								}
							}

							Map<String, PerceptContent> percepts = inAdcMATSIM.getAllPerceptsCopy(agentId);
							for (String perceptId : percepts.keySet()) {
								PerceptContent content = percepts.get(perceptId);
								PerceptContentList.add(content); // add to the percept content list to send to trike agent
							}


						}

						// sending data to specific TrikeAgent by calling its serviceTag
						if ((!ActionContentList.isEmpty()) || (!PerceptContentList.isEmpty())) {
							System.out.println(SensoryInputID + " start delivering data from MATSIM to "+agentId);
							TrikeAgent trikeAgent = SharedUtils.trikeAgentMap.get(agentId);
							trikeAgent.NotifyotherAgent(executorService, ActionContentList, PerceptContentList, Activestatus);

							//ServiceQuery<INotifyService> query = new ServiceQuery<>(INotifyService.class);
							//query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
							//query.setServiceTags("" + agentId); // calling the tag of a trike agent
							//Collection<INotifyService> service = agent.getLocalServices(query);
							//for (Iterator<INotifyService> iteration = service.iterator(); iteration.hasNext(); ) {
							//	INotifyService cs = iteration.next();
							//	cs.NotifyotherAgent(ActionContentList, PerceptContentList, Activestatus); // assign data to vehicle agents via service
								/*
								MyLogger logger = new MyLogger(SensoryInputID + "-inputbroker.txt", MyLogger.Status.INFO);
								logger.info("Trike id: " + agentId);
								logger.newLine();
								if(ActionContentList.isEmpty()){
									logger.info("ActionContentList: []");
								}else{
									logger.info("ActionContentList: ");
									ActionContentList.forEach((simAction) ->
											logger.info("Action type: " + simAction.getAction_type() + "; "
													+ "Instance Id: " + simAction.getInstance_id()
													+ "; " + "State: "  + simAction.getState().name() + ";"
													+ "Params: "  + Arrays.toString(simAction.getParameters()))
									);
								}
								logger.newLine();
								if(PerceptContentList.isEmpty()){
									logger.info("PerceptContentList: []");
								}else{
									logger.info("PerceptContentList:");
									PerceptContentList.forEach((simPercept) -> logger.info("Percept type: " + simPercept.getPercept_type() + "; "
											+ "Value: " + simPercept.getValue()
											+ "; " + "Params: "  + Arrays.toString(simPercept.getParameters())));
								}
								logger.info("#########################################################");
								logger.close();

								 */
							//}

						}
					}
					this.WriteinTrikeAgent = true; // marked as done once
				}


				if (this.WriteinTrikeAgent == true) {// indicate that the processing of writing MATSIM result is finished
					if (ActiveAgentList.isEmpty()) { // if all newly active agents are done

						// reset for next round and notify JadexModel
						this.WriteinTrikeAgent = false;
						JadexModel.answeredInputBroker.add(SensoryInputID); // indicate that this SimSensoryInputBroker is done with this iteration so that even the variable ResultfromMATSIM is true, the rest of the plan will not be executed
						JadexModel.flagMessage();
						//System.out.println(SensoryInputID + "finished with this round");
					}
				}
			}
		}
	}


	//#######################################################################
	//Goals and Plans :As soon as the SensoryInputID is assign, SimSensoryInputBroker
	//prepare the fuction set up so the synchronization could take place
	//#######################################################################


	@Goal(recur = true, recurdelay = 3000)
	class ReactoSensoryInputIDAdded {
		@GoalCreationCondition(beliefs = "SensoryInputID")
		public ReactoSensoryInputIDAdded() {
		}

		@GoalTargetCondition
		boolean IDupdated() {
			return !(SensoryInputID == null);
		}
	}

	@Plan(trigger = @Trigger(goalfinisheds = ReactoSensoryInputIDAdded.class))
	private void ReacttoIDAdded() {
		if (SensoryInputID != null) {

			if (executed == false) {
				executed = true;
				System.out.println("The ID assigned to this SimSensoryInputBroker is "+ SensoryInputID );
				IServiceIdentifier sid = ((IService) agent.getProvidedService(INotifyService2.class)).getServiceId();
				agent.setTags(sid, "" + SensoryInputID); // setTag for itself so vehicle agents could communciate using INotifyService2 service
				SimIDMapper.NumberSimInputAssignedID.add(SensoryInputID); // to store the total number of finished SimInputBroker

			}
		}
	}

	public void setSensoryInputID(String sensoryInputID) {
		SensoryInputID = sensoryInputID;
	}

	public void AddSimSensoryNametoList()
	{
		SimIDMapper.SimsensoryNameList.add(agent.getId().getName());
	}



}

