package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;


import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.AssignIDBrokerService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.AssignIDService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.IMappingAgentsService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.IMappingInputBrokerService;
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
@ProvidedServices(
		{@ProvidedService(type = IMappingAgentsService.class, implementation = @Implementation(AssignIDService.class)),
		@ProvidedService(type = IMappingInputBrokerService.class, implementation = @Implementation(AssignIDBrokerService.class)),
		})


@RequiredServices(
	{	@RequiredService(name="mapservices", type= IMappingAgentsService.class),
		@RequiredService(name="clockservice", type= IClockService.class),
		@RequiredService(name="mapbrokerservices", type= IMappingInputBrokerService.class),

})
		// multiple=true,

/*This is the most actual one that is using for Testing the whole Run1 process*/
public class SimIDMapper {

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
	public static List<String> TrikeAgentNameList = new ArrayList<>();

	public static List<String> SimsensoryNameList = new ArrayList<>();


	@Belief
	public static List<String> ActiveAgentList = new ArrayList<>();

	@Belief
	public static List<String> NumberSimInputAssignedID = new ArrayList<>();

	public HashMap<String, String> BDIMATSIMAgentMap = new HashMap<>();
	public HashMap<String, String> SimSensoryMap = new HashMap<>(); // reversedMap of BDIMATSIMAgentMap

//	private final Logger logger = LoggerFactory.getLogger(TrikeAgentMVP.class);


	/**
	 * The agent body.
	 */
	@OnStart
	public void body() {
		System.out.println("SimIDMapper is successfully started");
		bdiFeature.dispatchTopLevelGoal(new PerformCheckTrikeList());
		bdiFeature.dispatchTopLevelGoal(new PerformCheckSensoryInputList());

	}

	/**
	 * 31.01.2023 : from a list of name of agents, create a map with ID number to distribute them to agents later
	 *
	 **/
	public Map CreateBDIAgentMap(List<String> BDIAgentnameList) {
		int i = 0;
		for (String agentname : BDIAgentnameList) {
			BDIMATSIMAgentMap.put(agentname, Integer.toString(i));
			i++;
		}
		return BDIMATSIMAgentMap;
	}

	public Map CreateSimSensorytMap(List<String> SensoryInputnameList) {
		int i = 1;
		for (String agentname : SensoryInputnameList) {
			SimSensoryMap.put(agentname, "SimSensory"+i);
			i++;
		}
		return SimSensoryMap;
	}

	//#######################################################################
	//Goals and Plans : to check if SimIDMapper receives the name of all available Trike Agent yet
	// to start the Map creation and assign the ID
	//#######################################################################

	@Goal(recur = true, orsuccess = false, recurdelay = 3000, deliberation=@Deliberation(inhibits={PerformCheckSensoryInputList.class}))
	class PerformCheckTrikeList {
		public PerformCheckTrikeList() {
		}

	}

	@Plan(trigger = @Trigger(goals = PerformCheckTrikeList.class))
	private void performcheckTrikeList() {
		if (TrikeAgentNameList.size() ==JadexModel.TrikeAgentnumber) {
			CreateBDIAgentMap(TrikeAgentNameList);
			AssignAgentID();
			TrikeAgentNameList = new ArrayList<>(); // have to set back so the code will not be executed several time
		}

	}

	//#######################################################################
	//Goals and Plans : to check if SimIDMapper receives the name of all available SimSensoryInput yet
	// to start the Map creation and assign the ID
	//#######################################################################



	@Goal(recur = true, orsuccess = false, recurdelay = 3000, deliberation = @Deliberation(inhibits = {PerformCheckTrikeList.class}))
	class PerformCheckSensoryInputList {
		public PerformCheckSensoryInputList() {
		}

	}

	@Plan(trigger = @Trigger(goals = PerformCheckSensoryInputList.class))
	private void performcheckSensoryInputList() {
		if (SimsensoryNameList.size() == JadexModel.SimSensoryInputBrokernumber) {
		CreateSimSensorytMap(SimsensoryNameList);
			AssignSimSensoryInputBrokerID();
			SimsensoryNameList = new ArrayList<>(); // have to set back so the code will not be executed several time
		}

	}


	public void AssignSimSensoryInputBrokerID() {
		IFuture<Collection<IMappingInputBrokerService>> mapservices = requiredServicesFeature.getServices("mapbrokerservices");
		mapservices.addResultListener(new DefaultResultListener<Collection<IMappingInputBrokerService>>() {
			public void resultAvailable(Collection<IMappingInputBrokerService> result) {
				for (Iterator<IMappingInputBrokerService> it = result.iterator(); it.hasNext(); ) {
					IMappingInputBrokerService cs = it.next();

					cs.MapSensoryInput(SimSensoryMap);
				}
			}
		});
		System.out.println("SimIDMapper finished assigning the ID to all SimSensoryInputBroker ");
	}


	public void AssignAgentID() {
		IFuture<Collection<IMappingAgentsService>> mapservices = requiredServicesFeature.getServices("mapservices");
		mapservices.addResultListener(new DefaultResultListener<Collection<IMappingAgentsService>>() {
			public void resultAvailable(Collection<IMappingAgentsService> result) {
				for (Iterator<IMappingAgentsService> it = result.iterator(); it.hasNext(); ) {
					IMappingAgentsService cs = it.next();

					cs.MapAgents(BDIMATSIMAgentMap);
				}
			}
		});
		System.out.println("SimIDMapper finished assigning the ID to all trike agents ");
	}












}
