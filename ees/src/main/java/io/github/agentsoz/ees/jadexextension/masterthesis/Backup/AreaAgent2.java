package io.github.agentsoz.ees.jadexextension.masterthesis.Backup;


import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexModel.JadexModel;
import jadex.bdiv3.BDIAgentFactory;
import jadex.bdiv3.annotation.Belief;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.Trigger;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.ServiceQuery;
import jadex.bridge.service.types.clock.IClockService;
import jadex.micro.annotation.*;

import java.util.*;


@Agent(type=BDIAgentFactory.TYPE)
@Description("<h1>Trike Robot Agent</h1>")
@ProvidedServices({
		@ProvidedService(type= INotifyService.class, implementation=@Implementation(SensorAgentNotifyService.class))
		})
@RequiredServices({
		@RequiredService(name="clockservice", type= IClockService.class),
		@RequiredService(name="notifyservices", type= INotifyService.class, scope= ServiceScope.PLATFORM)

})
		// multiple=true,



/*This is the most actual one that is using for Testing the whole Run1 process*/
public class AreaAgent2 {

	/**
	 * The bdi agent. Automatically injected
	 */
	@Agent
	private IInternalAccess agent;
	@AgentFeature
	protected IRequiredServicesFeature requiredServicesFeature;
	@AgentFeature
	protected IBDIAgentFeature bdiFeature;

	@Belief
	public static boolean NewDatafromMATSIM;// if the Data from MATSIM is coming, JadexModel should flag it as true
	public static AgentDataContainer inAdcMATSIM = new AgentDataContainer();



	@Belief
	private static List<String> Registeredagents = new ArrayList<>();

	public static List<String> ActiveAgentList = new ArrayList<>();

	public static boolean WriteinTrikeAgent = false; // contain only agent id and action content of relevant agents


//	private final Logger logger = LoggerFactory.getLogger(TrikeAgentMVP.class);


	/**
	 * The agent body.
	 */
	@OnStart
	public void body() {

		bdiFeature.dispatchTopLevelGoal(new PerformCheckDatafromMATSIM());
		bdiFeature.dispatchTopLevelGoal(new PerformCheckinfoFromTrikeAgent());

	}


	//#######################################################################
	//Goals and Plans : When data from MATSIM is coming process and assign data to the belief
	// of Trike Agent. Mark this action as done when this process is done
	//#######################################################################

	@Goal(recur = true, orsuccess = false, recurdelay = 3000)
	class  PerformCheckDatafromMATSIM {
		public void PerformCheckDatafromMATSIM() {
			System.out.println("query goal data from MATSIM ");
		}

	}

	@Plan(trigger = @Trigger(goals = PerformCheckDatafromMATSIM.class))
	private void performcheckDatafromMATSIM() {

		if (NewDatafromMATSIM == true) {
			if (WriteinTrikeAgent == false)
			//get the list of potential active agent (agent with status success/ drop who are ready to take next trip)
			//Filter only result that is relevant to the agents that register in this area
			{
				Iterator<String> it = inAdcMATSIM.getAgentIdIterator();
				while (it.hasNext()) {
					List<ActionContent> ActionContentList = new ArrayList<>();
					List<PerceptContent> PerceptContentList = new ArrayList<>();
					boolean Activestatus = false;

					String agentId = it.next();
					if (Registeredagents.contains(agentId)) // only process data from agents that register here
					{
						Map<String, ActionContent> actions = inAdcMATSIM.getAllActionsCopy(agentId);// key: ActionID, value :Content . only content is important

						for (String actionId : actions.keySet()) { //likely there will only one action ID at a time but to be sure

							ActionContent content = actions.get(actionId);
							if (content != null) {
								ActionContentList.add(content);
								if (actionId == "DRIVE_TO") {

									ActionContent.State actionState = content.getState();
									if (actionState == ActionContent.State.PASSED ||
											actionState == ActionContent.State.FAILED) // create a list of potential active agents
									{
										ActiveAgentList.add(agentId);
										Activestatus = true;
									}
								}
							}
						}

						Map<String, PerceptContent> percepts = inAdcMATSIM.getAllPerceptsCopy(agentId);
						for (String perceptId : percepts.keySet()) {
							PerceptContent content = percepts.get(perceptId);
							PerceptContentList.add(content);
						}


					}

					if ((!ActionContentList.isEmpty()) || (!PerceptContentList.isEmpty())) {
						ServiceQuery<INotifyService> query = new ServiceQuery<>(INotifyService.class);
						query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
						query.setServiceTags("user:" + agentId);
						Collection<INotifyService> service = agent.getLocalServices(query);
						for (Iterator<INotifyService> iteration = service.iterator(); iteration.hasNext(); ) {
							INotifyService cs = iteration.next();
							cs.NotifyotherAgent(ActionContentList, PerceptContentList, Activestatus);
						}

					}
				}
			WriteinTrikeAgent = true;
		}

		}


	}

	//#######################################################################
	//Goals and Plans : Checking if all the SIMpercepts and and SIMactions already written to beliefs
	// of Trike Agents. Checking if all Trike agents are done with the execution in this cycle
	// If yes, notify JadexModel so it could carry on with other task and reset the variable for the
	//next cycle
	//#######################################################################

	@Goal(recur = true, orsuccess = false, recurdelay = 3000)
	class PerformCheckinfoFromTrikeAgent {
		public PerformCheckinfoFromTrikeAgent() {
			System.out.println("Trigger to send Noti to JADEX Model if all Trike Agents are done in this cycle");
		}

	}

	@Plan(trigger = @Trigger(goals = PerformCheckinfoFromTrikeAgent.class))
	private void performcheckinfoFromTrikeAgent() {
			if (NewDatafromMATSIM == true) // indicate that the BDI side is takeControl at the moment
			{
				if(WriteinTrikeAgent ==true)// indicate that the processing of MATSIM result is finished
				if (ActiveAgentList.isEmpty()) {
					JadexModel.finishedAreaAgent = ++JadexModel.finishedAreaAgent;
					NewDatafromMATSIM = false; // reset for the next cycle
					WriteinTrikeAgent = false; // reset for the next cycle
				}
			}
		}

	public static void addTrikeAgent(String AgentID)
	{
		Registeredagents.add(AgentID);
		System.out.println(Registeredagents);

	}

}

