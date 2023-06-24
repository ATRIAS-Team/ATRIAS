package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Testregisterservices;


import jadex.bdiv3.BDIAgentFactory;
import jadex.bdiv3.annotation.*;
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
@ProvidedServices(
		@ProvidedService(type= INotifyService1.class, implementation=@Implementation(BroadcastService1.class)))
@RequiredServices({
		@RequiredService(name="clockservice", type= IClockService.class),
		@RequiredService(name="notifyservices1", type= INotifyService1.class, scope= ServiceScope.PLATFORM)}
)
		// multiple=true,


/*This is the most actual one that is using for Testing the whole Run1 process*/
public class TestV3EnvironmentActionAgent11 {

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


		bdiFeature.dispatchTopLevelGoal(new TestregisterserviceGoal());
		//	NewDatafromMATSIM = false;
		//	System.out.println("This Agent is used to send Information to MATSIM and notify JADEX agents when new data from MATSIM is coming");
		//TrikeMain2.NotifromEnvActAgent = true;
		//	while (true) {
		//		if (NewDatafromMATSIM == true) {
		//			System.out.println("Sensor Service Agent Receives Data from MATSIM ");
		//			bdiFeature.dispatchTopLevelGoal(new NotifyJadexAgentGoal());

		//	bdiFeature.adoptPlan(new AssignAgentIDtoTrikeAgent());
		// bdiFeature.adoptPlan(new NotifyJadexAgentPlan());
		//			NewDatafromMATSIM = false;

		//			break;
		//		}

		//	}
	}

	/**
	 * 31.01.2023 : from a list of name of agents, create a map with ID number to distribute them to agents later
	 **/
	public Map CreateBDIAgentMap(List<String> BDIAgentnameList) {
		int i = 1;
		for (String agentname : BDIAgentnameList) {
			BDIMATSIMAgentMap.put(agentname, Integer.toString(i));
			i++;
		}
		return BDIMATSIMAgentMap;
	}


	//	@Plan(trigger = @Trigger(goals = NotifyJadexAgent.class))
	// Plan to Notify Jadex Agent through the Notify Service that the data from MATSIM is available
// and through the service, the percept update coming from MATSIM will be written into the belief of each TrikeAgent.

	/**
	 * 14.2.2023 add goal to the notify and assign id plan because somehow these two plans can not run at the same time
	 **/
	@Goal (recur = true, recurdelay = 3000)
	class  TestregisterserviceGoal {
		public  TestregisterserviceGoal() {
			System.out.println("my goal is to notify jadex agent ");
		}
	}

	@Plan(trigger = @Trigger(goals =  TestregisterserviceGoal.class))
	public class TestregisterservicePlan {

		public TestregisterservicePlan() {
		}

		@PlanBody
		public void Testregisterservice() {
		//	IFuture<Collection<INotifyService1>> chatservices = requiredServicesFeature.getServices("notifyservices1");
		//	chatservices.addResultListener(new DefaultResultListener<Collection<INotifyService1>>() {
		//		public void resultAvailable(Collection<INotifyService1> result) {
		//			for (Iterator<INotifyService1> it = result.iterator(); it.hasNext(); ) {
		//				INotifyService1 cs = it.next();
		//				cs.NotifyotherAgent(agent.getId().getName(), "notifyservices1");


			ServiceQuery<INotifyService1> query = new ServiceQuery<>(INotifyService1.class);
			query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
			query.setServiceTags("user:"+"2");
			Collection<INotifyService1> service = agent.getLocalServices(query);
			for (Iterator<INotifyService1> it = service.iterator(); it.hasNext(); ) {
				INotifyService1 cs = it.next();
				cs.NotifyotherAgent(agent.getId().getName(), "Im sending something different only for agent 2");
			}


					}
				}
		//	});

		//}
	}



