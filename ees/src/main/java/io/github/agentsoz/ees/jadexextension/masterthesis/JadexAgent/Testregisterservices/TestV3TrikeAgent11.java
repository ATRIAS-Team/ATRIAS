package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Testregisterservices;



import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.annotation.OnInit;
import jadex.bridge.service.annotation.OnStart;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.*;

import java.util.List;


@Agent(type = "bdi")
@ProvidedServices({
		@ProvidedService(type = INotifyService1.class, implementation = @Implementation(ControlService1.class)),
		@ProvidedService(type = INotifyService2.class, implementation = @Implementation(ControlService2.class)),
})
@RequiredServices({
		@RequiredService(name="notifyservices1", type= INotifyService1.class, scope= ServiceScope.PLATFORM),
		@RequiredService(name="notifyservices2", type= INotifyService2.class, scope= ServiceScope.PLATFORM),
		// multiple=true,
})


/*This is the most actual one that is using for Testing the whole Run1 process*/

public class TestV3TrikeAgent11 {

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
	protected String username ="agent2";

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
	@OnInit
	public IFuture<Void> agentInit()
	{
		IServiceIdentifier sid = ((IService) agent.getProvidedService(INotifyService1.class)).getServiceId();
		agent.setTags(sid, "user:" + username);
		return null;
	}

	/**
	 * The agent body.
	 */
	@OnStart
	public void body() {

		/**31.01.2022: Adding mapping to Jadex Agent because of shitty name that could not be changed**/
		// Alternative 1 mit seperate Service
	//	agent.getFeature(IProvidedServicesFeature.class).addService("notifyservices1", INotifyService1.class, new ControlService1());

		// ALternative 2 : Service is added directly in the agent
	/*
		agent.addService("notifyservices1", INotifyService1.class, new INotifyService1() {
			@Override
			public void NotifyotherAgent(String sender, String text)
				{
					System.out.println(agent.getId().getLocalName()+" received from: "+sender+" message: "+text);
		//			changeservice = text;
					IServiceIdentifier sid10 = ((IService) agent.getProvidedService("notifyservices1")).getServiceId();
					System.out.println( "the Service Identifier is now :"+sid10);
					agent.removeService((agent.getProvidedService("notifyservices1")).getServiceId());
			}
		});

	//	IServiceIdentifier sid4 = ((IService) agent.getProvidedService(INotifyService1.class)).getServiceId();
	//	agent.removeService(sid4);
*/
}
	/**Goals and Plans for Sending data to AgentDataContainer, for the Aktorik */


	/**
	 * Goals and plans that will be triggered if the new data from MATSIM is coming then trigger the action in plan to react
	 */
	@Goal
	class Registernewservice {
		@GoalCreationCondition(beliefs ="changeservice" )
		public Registernewservice() {
			System.out.println(" about to change the service");
		}

		@GoalTargetCondition
		boolean resultupdated() {
			return !(changeservice == null);
		}
	}

	@Plan(trigger = @Trigger(goalfinisheds = Registernewservice.class))
	private void ReacttoMessagefromEnvActAgent() {
			if (changeservice != null ) {
				IServiceIdentifier sid10 = agent.getProvidedService("notifyservices11").getServiceId();
				//agent.setTags()

				System.out.println(sid10);
				//IServiceIdentifier sid3 = ((IService) agent.getProvidedService(INotifyService1.class)).getServiceId();
				agent.removeService(sid10);
				System.out.println( "the Service Identifier is now :"+sid10);



			//	agent.addService("notifyservices2", ControlService2.class, new ControlService2());
			//	IServiceIdentifier sid2 = ((IService) agent.getProvidedService(INotifyService1.class)).getServiceId();
			//	int i = 1;

			}
			//if (this.changeservice == "notifyservices2" ) {
			//	IServiceIdentifier sid = ((IService) agent.getProvidedService(INotifyService2.class)).getServiceId();
			//	agent.removeService(sid);
			//	agent.addService("notifyservices1", ControlService2.class, new ControlService1());
			//}
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







	}










