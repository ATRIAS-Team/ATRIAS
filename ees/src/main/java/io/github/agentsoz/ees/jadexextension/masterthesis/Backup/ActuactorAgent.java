package io.github.agentsoz.ees.jadexextension.masterthesis.Backup;


import io.github.agentsoz.bdiabm.Agent;
import io.github.agentsoz.bdiabm.EnvironmentActionInterface;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.bdiabm.v3.QueryPerceptInterface;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexModel.JadexModel;
import jadex.bdiv3.BDIAgentFactory;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.*;
import jadex.micro.tutorial.ChatServiceD1;
import jadex.micro.tutorial.IChatService;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


@jadex.micro.annotation.Agent(type=BDIAgentFactory.TYPE)
@Description("<h1>Trike Robot Agent</h1>")
@ProvidedServices(@ProvidedService(type= IChatService.class,
		implementation=@Implementation(ChatServiceD1.class)))
@RequiredServices({
		@RequiredService(name="clockservice", type= IClockService.class),
		@RequiredService(name="chatservices", type= IChatService.class, scope= ServiceScope.PLATFORM) // multiple=true,
})
@Plans(@Plan(body=@Body(ActuactorAgent.NotifyJadexAgentPlan.class)))

/* temporary using this one to send the data to MATSIM, in the reality it doesnt have to be a JADEX Agent */

public class ActuactorAgent implements EnvironmentActionInterface, Agent {

	/**
	 * The bdi agent. Automatically injected
	 */
	private IInternalAccess agent;
	@AgentFeature
	protected IRequiredServicesFeature requiredServicesFeature;
	@AgentFeature
	protected IBDIAgentFeature bdiFeature;

	@Belief
	public static boolean NewDatafromMATSIM; // if the Data from MATSIM is coming, JadexModel should flag it as true

//	private final Logger logger = LoggerFactory.getLogger(TrikeAgentMVP.class);

	private AgentDataContainer outAdc;
	private EnvironmentActionInterface envActionInterface;


	public ActuactorAgent() {
		outAdc = new AgentDataContainer();
		setEnvironmentActionInterface(this);
		NewDatafromMATSIM = false;
	}

	/**
	 * The agent body.
	 */



	@Override
	public void packageAction(String agentId, String actionID, Object[] parameters, String actionState) {
		ActionContent.State state = ActionContent.State.INITIATED;
		if (actionState != null) {
			try {
				state = ActionContent.State.valueOf(actionState);
			} catch (Exception e) {
				//logger.warn("agent {} ignoring unknown action state {}", agentId, actionState);
			}
		}
		ActionContent ac = new ActionContent(parameters, state, actionID);
		// outAdc.putAction(agentId, actionID, ac);

		JadexModel.outAdcincycle.putAction(agentId, actionID, ac);
		//	copy(outAdc, JadexModel.outAdc);
	//	outAdc = null;
		System.out.println("packageAction is succesful");

	}

	private void copy(AgentDataContainer from, AgentDataContainer to) {
		if (from != null) {
			Iterator<String> it = from.getAgentIdIterator();
			while (it.hasNext()) {
				String agentId = it.next();
				// Copy percepts
				Map<String, PerceptContent> percepts = from.getAllPerceptsCopy(agentId);
				for (String perceptId : percepts.keySet()) {
					PerceptContent content = percepts.get(perceptId);
					to.putPercept(agentId, perceptId, content);
				}
				// Copy actions
				Map<String, ActionContent> actions = from.getAllActionsCopy(agentId);
				for (String actionId : actions.keySet()) {
					ActionContent content = actions.get(actionId);
					to.putAction(agentId, actionId, content);
				}
			}
		}
	}

	//@Override
	public void init(String[] args) {

	}

	//@Override
	public void start() {

	}

	//@Override
	public void handlePercept(String perceptID, Object parameters) {

	}


	//@Override
	public void updateAction(String actionID, ActionContent content) {
		//logger.debug("{} received action update: {}", logPrefix(), content);
		ActionContent.State actionState = content.getState();
		if (actionState == ActionContent.State.PASSED ||
				actionState == ActionContent.State.FAILED ||
				actionState == ActionContent.State.DROPPED) {


			//memorise(BushfireAgent.MemoryEventType.BELIEVED.name(),
			//        BushfireAgent.MemoryEventValue.LAST_ENV_ACTION_STATE.name() + "=" + actionState.name());


			// remove the action and records it as the last action performed
			//	EnvironmentAction lastAction = removeActiveEnvironmentAction(content.getAction_type());
			//setLastBdiActionAndState(lastAction, actionState);
			if (content.getAction_type().equals(Constants.DRIVETO)) {
				// Wake up the agent that was waiting for drive action to finish
				//	suspend(false);
			}
		}
	}

	//@Override
	public void kill() {

	}

	//@Override
	public void setQueryPerceptInterface(QueryPerceptInterface queryInterface) {

	}

	//@Override
	public QueryPerceptInterface getQueryPerceptInterface() {
		return null;
	}

	//@Override
	public void setEnvironmentActionInterface(EnvironmentActionInterface envActInterface) {
		this.envActionInterface = envActInterface;
	}

	//@Override
	public EnvironmentActionInterface getEnvironmentActionInterface() {
		return envActionInterface;
	}



	@Goal
	class NotifyJadexAgent
	{
		@GoalCreationCondition(beliefs = "NewDatafromMATSIM")
		public NotifyJadexAgent() {
		}
		@GoalTargetCondition
		boolean	DatafromMatSIMreceived()
		{
			// Test if the waste is not believed to be in the environment
			return NewDatafromMATSIM;
		}
	}


	@Plan
	public class NotifyJadexAgentPlan {

		public NotifyJadexAgentPlan() {}

		@PlanBody
		public void NotifyJadexAgents()
		{
			IFuture<Collection<IChatService>> chatservices = requiredServicesFeature.getServices("chatservices");
			chatservices.addResultListener(new DefaultResultListener<Collection<IChatService>>() {
				public void resultAvailable(Collection<IChatService> result) {
					for (Iterator<IChatService> it = result.iterator(); it.hasNext(); ) {
						IChatService cs = it.next();
						cs.message(agent.getId().getName(), "new Data from MATSIM is coming");

					}
				}
			});
			NewDatafromMATSIM = false;
		}
	}





}
