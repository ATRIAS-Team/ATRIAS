package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.SimSensoryInputBroker;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;

/**
 *  Chat service implementation.
 */
@Service
public class TrikeAgentSendService implements INotifyService2 {
	//-------- attributes --------

	/**
	 * The agent.
	 */
	@ServiceComponent
	protected IInternalAccess agent;

	//-------- attributes --------	

	public void NotifyotherAgent(String AgentID)
	{

	}


	public void removeTrikeAgentfromActiveList(String AgentID)
	{

	}
}
