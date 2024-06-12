package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;


import io.github.agentsoz.bdiabm.Agent;
import io.github.agentsoz.bdiabm.EnvironmentActionInterface;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import io.github.agentsoz.bdiabm.v3.QueryPerceptInterface;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;

/* temporary using this one to send the data to MATSIM, in the reality it doesnt have to be a JADEX Agent */

public class SimActuator implements EnvironmentActionInterface, Agent {

	private AgentDataContainer outAdc;
	private EnvironmentActionInterface envActionInterface;
	private QueryPerceptInterface queryInterface;

	public SimActuator() {
		outAdc = new AgentDataContainer();
		setEnvironmentActionInterface(this);
	}


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

		if (JadexModel.inBDIcycle == true) {

			JadexModel.outAdcincycle.putAction(agentId, actionID, ac);
		}
		else
		{
			JadexModel.outAdcoutCycle.putAction(agentId, actionID, ac);
		}

		System.out.println( "agent "+ agentId +" has sent an action "+ actionID +" to Data Container");

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

	@Override
	public void updateAction(String actionID, ActionContent content) {

	}


	//@Override
	public void kill() {

	}

	//@Override
	public void setQueryPerceptInterface(QueryPerceptInterface queryInterface) {
		this.queryInterface = queryInterface;

	}

	//@Override
	public QueryPerceptInterface getQueryPerceptInterface() {

		return queryInterface;
	}

	//@Override
	public void setEnvironmentActionInterface(EnvironmentActionInterface envActInterface) {
		this.envActionInterface = envActInterface;
	}

	//@Override
	public EnvironmentActionInterface getEnvironmentActionInterface() {
		return envActionInterface;
	}









}
