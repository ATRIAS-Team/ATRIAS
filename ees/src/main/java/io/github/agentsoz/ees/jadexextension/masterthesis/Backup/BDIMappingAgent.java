package io.github.agentsoz.ees.jadexextension.masterthesis.Backup;


import jadex.bdiv3.BDIAgentFactory;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.*;

import java.util.*;


@Agent(type=BDIAgentFactory.TYPE)
@Description("<h1>Trike Robot Agent</h1>")
@ProvidedServices(@ProvidedService(type= IMappingAgentsService.class, implementation=@Implementation(AssignIDService.class)))
@RequiredServices(
	{	@RequiredService(name="chatservices", type= IMappingAgentsService.class),
		@RequiredService(name="clockservice", type= IClockService.class),
})
		// multiple=true,

/*This is the most actual one that is using for Testing the whole Run1 process*/
public class BDIMappingAgent {

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
	public static List<String> ActiveAgentList = new ArrayList<>();

	/**
	 * 01.02.2023
	 **/
	public HashMap<String, String> BDIMATSIMAgentMap = new HashMap<>();
	public HashMap<String, String> MATSIMBDIAgentMap = new HashMap<>(); // reversedMap of BDIMATSIMAgentMap

//	private final Logger logger = LoggerFactory.getLogger(TrikeAgentMVP.class);


	/**
	 * The agent body.
	 */
	@OnStart
	public void body() {
		// wait until all the Jadex Agents have send their name to the JadexAgentNameList, then start create a mapping
		// This only works with breakpoints. in real time it doesnt work??
		//	while (true) {
		//		if (JadexAgentNameList.size() == 3) {
		//			CreateBDIAgentMap(JadexAgentNameList);
		//			break;
		//		}
		//	}
		//	bdiFeature.dispatchTopLevelGoal(new AssignAgentIDtoTrikeAgentGoal());
		bdiFeature.dispatchTopLevelGoal(new PerformCheckTrikeList());

	}

	/**
	 * 31.01.2023 : from a list of name of agents, create a map with ID number to distribute them to agents later
	 * Also created a reversed map for later to search for the ID coming from in MATSIM
	 **/
	public Map CreateBDIAgentMap(List<String> BDIAgentnameList) {
		int i = 1;
		for (String agentname : BDIAgentnameList) {
			BDIMATSIMAgentMap.put(agentname, Integer.toString(i));
			i++;
		}

	//	 for (String j : BDIMATSIMAgentMap.keySet()) {
	//		MATSIMBDIAgentMap.put(BDIMATSIMAgentMap.get(j), j);
	//	} NOt necessary because Jadex Agent now have the same ID number with MATSIM agent.
		return BDIMATSIMAgentMap;
	}

	@Goal
	class AssignAgentIDtoTrikeAgentGoal {
		public AssignAgentIDtoTrikeAgentGoal() {
			System.out.println("my goal is to assign agent ID to Trike Agent ");
		}
	}

	@Plan(trigger = @Trigger(goals = AssignAgentIDtoTrikeAgentGoal.class))
	public class AssignAgentIDtoTrikeAgent {

		public AssignAgentIDtoTrikeAgent() {
		}

		@PlanBody
		public void AssignAgentID() {
			IFuture<Collection<IMappingAgentsService>> chatservices = requiredServicesFeature.getServices("chatservices");
			chatservices.addResultListener(new DefaultResultListener<Collection<IMappingAgentsService>>() {
				public void resultAvailable(Collection<IMappingAgentsService> result) {
					for (Iterator<IMappingAgentsService> it = result.iterator(); it.hasNext(); ) {
						IMappingAgentsService cs = it.next();

						cs.MapAgents(BDIMATSIMAgentMap);
					}
				}
			});
		}
	}


	@Goal(recur = true, orsuccess = false, recurdelay = 3000)
	class PerformCheckTrikeList {
		public PerformCheckTrikeList() {
		}

	}

	@Plan(trigger = @Trigger(goals = PerformCheckTrikeList.class))
	private void performcheckTrikeList() {
		if (JadexAgentNameList.size() ==3) {
			CreateBDIAgentMap(JadexAgentNameList);
			bdiFeature.dispatchTopLevelGoal(new AssignAgentIDtoTrikeAgentGoal());
			JadexAgentNameList = new ArrayList<>();
		}

		agent.getFeature(IBDIAgentFeature.class).dropGoal(new PerformCheckTrikeList()); // this needs to be tested



	}










}
