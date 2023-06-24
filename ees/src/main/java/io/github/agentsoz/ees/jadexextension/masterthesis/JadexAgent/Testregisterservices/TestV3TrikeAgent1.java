package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Testregisterservices;



//import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.testAgent.*;
//import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.IMappingAgentsService;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService.WrittingIDService;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.annotation.OnStart;
import jadex.micro.annotation.*;

import java.util.List;


@Agent(type = "bdi")
@ProvidedServices({
		@ProvidedService(type= IMappingAgentsService.class, implementation=@Implementation(WrittingIDService.class)),
		@ProvidedService(type = INotifyService1.class, implementation = @Implementation(ControlService1.class)),
		@ProvidedService(type = INotifyService2.class, implementation = @Implementation(ControlService2.class)),
})
@RequiredServices({
		@RequiredService(name="chatservices", type= IMappingAgentsService.class),
		@RequiredService(name="notifyservices1", type= INotifyService1.class, scope= ServiceScope.PLATFORM),
		@RequiredService(name="notifyservices2", type= INotifyService2.class, scope= ServiceScope.PLATFORM),
		// multiple=true,
})


/*This is the most actual one that is using for Testing the whole Run1 process*/

public class TestV3TrikeAgent1 {

	/**
	 * The bdi agent. Automatically injected
	 */
	@Agent
	private IInternalAccess agent;
	@AgentFeature
	protected IBDIAgentFeature bdiFeature;
	@AgentFeature
	protected IExecutionFeature execFeature;
	@AgentArgument
	protected String username ="agent1";

	@Belief
	protected String resultfromMASIM;

	// to indicate if the agent is available to take the new ride
	@Belief
	public boolean activestatus;

	@Belief
	public static List<String> Triplist;

	@Belief
	private String changeservice = null;


	@Belief
	private String agentID = null; // store agent ID from the map
	public String actionID;
	public Object[] actionparameters;
	public String agentName;
	//public TestV2EnvironmentActionAgent storageagent;

	/**
	 * only for the test to make sure the dispatch goal only works when JadexModel is done
	 */
//	@OnInit
//	public IFuture<Void> agentInit()
//	{
		IServiceIdentifier sid = ((IService) agent.getProvidedService(INotifyService1.class)).getServiceId();
	//	agent.setTags(sid, "user:" + username);
//		return null;
//	}

	/**
	 * The agent body.
	 */
	@OnStart
	public void body() {
		AddAgentNametoAgentList();


}
	/**Goals and Plans for Sending data to AgentDataContainer, for the Aktorik */


	/**
	 * Goals and plans that will be triggered if the new data from MATSIM is coming then trigger the action in plan to react
	 */

	@Goal(recur = true, recurdelay = 3000)
	class ReactoAgentIDAdded
	{
		@GoalCreationCondition(beliefs ="agentID")
		public ReactoAgentIDAdded()
		{ System.out.println("Goal react if agentID is added ");
		}
		@GoalTargetCondition
		boolean	IDupdated()
		{
			return !(agentID== null);
		}
	}
	@Plan(trigger=@Trigger(goalfinisheds = ReactoAgentIDAdded.class))
	private void	ReacttoAgentIDAdded()
	{
		IServiceIdentifier sid = ((IService) agent.getProvidedService(INotifyService1.class)).getServiceId();
	    agent.setTags(sid, "user:" + agentID);}

	public void AddAgentNametoAgentList()
	{
		TestV3EnvironmentActionAgent1.JadexAgentNameList.add(agent.getId().getName());
	}


	public void setResultfromMASIM(String Result) {
		this.resultfromMASIM = Result;
	}

	public void setChangeservice(String Result) {
		this.changeservice = changeservice+Result;
	}

	public String getChangeservice() {
		System.out.println(changeservice);
		return changeservice;
	}

	public void setAgentID(String agentid) {
		agentID = agentid;
	}

	public String getAgentID() {
		System.out.println(agentID);

		return agentID;
	}







	}










