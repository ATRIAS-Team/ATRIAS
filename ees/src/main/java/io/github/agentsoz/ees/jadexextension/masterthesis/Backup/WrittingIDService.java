package io.github.agentsoz.ees.jadexextension.masterthesis.Backup;

import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;

import java.util.HashMap;

/**
 *  Mapping service implementation.
 */
@Service
public class WrittingIDService implements IMappingAgentsService {
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
	public void MapAgents(final HashMap BDIAgentMap)
	{// Accesing the Trike Agents
		 AgentMap = BDIAgentMap;
	//	final TestV2TrikeAgent1 TrikeAgent	= (TestV2TrikeAgent1) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
	final TestV3TrikeAgent1 TrikeAgent	= (TestV3TrikeAgent1) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();

		// Getting the AgentID for the Agent from the Agent Map
		String agentID = (String) BDIAgentMap.get(agent.getId().getName());
		if (!BDIAgentMap.isEmpty()) {
						{
							TrikeAgent.setAgentID(agentID);

						}
		}
	}
}

