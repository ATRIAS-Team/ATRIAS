package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TestBeliefChangeTrigger;


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
@ProvidedServices(@ProvidedService(type= ITestBeliefChangeService.class, implementation=@Implementation(TrikeAgentService.class)))
@RequiredServices(@RequiredService(name="testbeliefchangeservices", type= ITestBeliefChangeService.class))





/*This is the most actual one that is using for Testing the whole Run1 process*/
public class SupportingAgent {

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
	/**
	 * 31.01.2023 : create a list to store all the JadexAgentName for mapping
	 **/
	public static List<String> JadexAgentNameList = new ArrayList<>();

	@Belief
	private static List<String> ActiveAgentList = new ArrayList<>();

	@Belief
	private  List<String> ActiveAgentList2 = new ArrayList<>();

	@Belief
	public String testbeliefs;


	/**
	 * 01.02.2023
	 **/
	public HashMap<String, String> BDIMATSIMAgentMap = new HashMap<>();

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

		//	bdiFeature.dispatchTopLevelGoal(new CheckActiveAgent());
		//bdiFeature.dispatchTopLevelGoal(new AchieveCleanupWaste());

		//	bdiFeature.dispatchTopLevelGoal(new BecomeRich());


	}

	public static void addActiveAgent(String activeAgentList) {
		ActiveAgentList.add(activeAgentList);
		System.out.println(ActiveAgentList);
	}

	public void setTestbeliefs (String beliefs) {this.testbeliefs = beliefs;}

	public List<String> getActiveAgentList() {
		return ActiveAgentList;
	}


	@Goal(recur = true, recurdelay = 3000)
	public class CheckActiveAgent {

		@GoalCreationCondition(factadded = "ActiveAgentList" , beliefs = "testbeliefs")
		public CheckActiveAgent() {
		}


			@GoalTargetCondition
			boolean ActiveAgentListupdated() {
				return (ActiveAgentList.size() == 0 || testbeliefs == "true");
			}


	}

	@Plan(trigger = @Trigger(goalfinisheds = CheckActiveAgent.class))
	public void PrintActiveAgent() {
//		TrikeMain2.CansendtoAdc = true;
		System.out.println("the Agent active List now have " + ActiveAgentList);
		System.out.println("Size:" + ActiveAgentList.size());
	}
}




	/*
	@Goal(recur = true, recurdelay = 3000)    // Pause patrol goal while loading battery
	class CheckActiveAgent {
		@GoalMaintainCondition
			// The cleaner aims to maintain the following expression, i.e. act to restore the condition, whenever it changes to false.
		boolean isallActiveAgent() {
			return ActiveAgentList.size() != 3; // everything is fine if ActiveAgentList didnt reach 3. trigger when its = 3
		}

		@GoalTargetCondition
			// Only stop charging, when this condition is true
		boolean isBatteryFullyLoaded() {
			return ActiveAgentList.size() == 3; // Charge until 90%
		}
	}

	@Plan(trigger = @Trigger(goals = CheckActiveAgent.class))
	public void PrintActiveAgent() {
//		TrikeMain2.CansendtoAdc = true;
		System.out.println("the Agent active List now have " + ActiveAgentList);
		System.out.println("Size:" + ActiveAgentList.size());

	}
}

	 */

