package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TestBeliefChangeTrigger;


import io.github.agentsoz.ees.jadexextension.jadexagent.TrikeMain2;
import jadex.bdiv3.BDIAgentFactory;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.micro.annotation.*;

import java.util.*;


@Agent(type=BDIAgentFactory.TYPE)
@Description("<h1>Trike Robot Agent</h1>")





/*This is the most actual one that is using for Testing the whole Run1 process*/
public class SimplesupportAgent {

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
	private List<String> ActiveAgentList = new ArrayList<>();



//	private final Logger logger = LoggerFactory.getLogger(TrikeAgentMVP.class);


	/**
	 * The agent body.
	 */
	@OnStart
	public void body() {

		// wait until all the Jadex Agents have send their name to the JadexAgentNameList, then start create a mapping
		//	while (true) {
		//		if (JadexAgentNameList.size() == 2) {
		//			CreateBDIAgentMap(JadexAgentNameList);
		//			break;
		//		}
		//	}

		//bdiFeature.dispatchTopLevelGoal(new CheckActiveAgent());
		//bdiFeature.dispatchTopLevelGoal(new AchieveCleanupWaste());

			bdiFeature.dispatchTopLevelGoal(new AddtoActiveList());


	}

	public void addActiveAgent(String activeAgentList) {
		this.ActiveAgentList.add(activeAgentList);
	}

	public List<String> getActiveAgentList() {
		return this.ActiveAgentList;
	}


	@Goal(recur=true, recurdelay=3000)
	public class CheckActiveAgent {
		@GoalCreationCondition(factadded = "ActiveAgentList")
			public CheckActiveAgent() {

				System.out.println("Goal react if the changes from TrikeAgent is taken into here ");
			}
		//public static boolean CheckActiveAgent() {
		//	return ActiveAgentList.size() == 3;
		//}

		@GoalTargetCondition
		boolean ActiveAgentListupdated() {
			return (ActiveAgentList.size() == 3);
		}

		@GoalContextCondition
		boolean isActiveAgentlistEmpty() {
			return ActiveAgentList.size() == 3;
		}
	}

	@Plan(trigger = @Trigger(goalfinisheds = CheckActiveAgent.class))
	public void PrintActiveAgent() {
	//	TrikeMain2.CansendtoAdc = true;
		System.out.println("the Agent active List now have " + ActiveAgentList);
		System.out.println( "Size:" + ActiveAgentList.size());
	}

	// This goal is used to add new element to the ActiveAgentList
	@Goal(recur=true, recurdelay=5000)
	class AddtoActiveList {}


	@Plan(trigger = @Trigger(goals = AddtoActiveList.class))
	public void AddtoActiveListplan() {
		addActiveAgent("Hello" );


	}



}

