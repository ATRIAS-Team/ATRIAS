package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.MappingService;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.SimSensoryInputBroker;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;

import java.util.HashMap;

/**
 *  Mapping service implementation.
 */
@Service
public class WritingBrokerIDService implements IMappingInputBrokerService {
	//-------- attributes --------

	/**
	 * The agent.
	 */
	@ServiceComponent
	protected IInternalAccess agent;

	public HashMap AgentMap;

	//-------- attributes --------	

	/**
	 * 01.02.2023: Writting the agent ID to each Jadex Agent `s AgentID argument
	 */


	public void MapSensoryInput(final HashMap MapSensoryInputMap)
	{
		final SimSensoryInputBroker SensoryInput	= (SimSensoryInputBroker) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();

		// Getting the AgentID for the Agent from the Agent Map
		String agentID = (String) MapSensoryInputMap.get(agent.getId().getName());
		SensoryInput.setSensoryInputID(agentID);

	}
}

