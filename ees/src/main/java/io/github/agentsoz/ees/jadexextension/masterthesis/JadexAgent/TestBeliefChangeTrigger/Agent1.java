package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TestBeliefChangeTrigger;


import jadex.bdiv3.BDIAgentFactory;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.micro.annotation.*;

import java.util.ArrayList;
import java.util.List;


@Agent(type=BDIAgentFactory.TYPE)
@Description("<h1>Trike Robot Agent</h1>")
@ProvidedServices(@ProvidedService(type= ITestBeliefChangeService.class, implementation=@Implementation(TrikeAgentService.class)))
@RequiredServices(@RequiredService(name="testbeliefchangeservices", type= ITestBeliefChangeService.class))





/*This is the most actual one that is using for Testing the whole Run1 process*/
public class Agent1 {

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
	private static List<String> ActiveAgentList = new ArrayList<>();



	/**
	 * The agent body.
	 */
	@OnStart
	public void body() {


		bdiFeature.dispatchTopLevelGoal(new CheckActiveAgent());


	}

	public static void addActiveAgent(String activeAgentList) {
		ActiveAgentList.add(activeAgentList);
	}

	public static List<String> getActiveAgentList() {
		return ActiveAgentList;
	}



	@Goal(excludemode=ExcludeMode.Never)
	public static class CheckActiveAgent {
		@GoalCreationCondition(factadded = "ActiveAgentList")
		//	public CheckActiveAgent() {

		//		System.out.println("Goal react if the changes from TrikeAgent is taken into here ");
		//	}
		public static boolean CheckActiveAgent() {
			return ActiveAgentList.size() == 3;
		}

		@GoalTargetCondition
		boolean ActiveAgentListupdated() {
			return (ActiveAgentList.size() == 3);
		}

//		@GoalContextCondition
//		boolean isActiveAgentlistEmpty() {
//			return ActiveAgentList.size() == 3;
//		}
	}

	@Plan(trigger = @Trigger(goals = CheckActiveAgent.class))
	public void PrintActiveAgent() {

				System.out.println("the Agent active List now have " + ActiveAgentList);
			}
	}


