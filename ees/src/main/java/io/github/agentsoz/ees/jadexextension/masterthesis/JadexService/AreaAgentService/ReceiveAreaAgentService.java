package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaAgentService;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Job;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TrikeAgent;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.util.Location;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;

import java.util.HashMap;

/**
 *  Mapping service implementation.
 */
@Service
public class ReceiveAreaAgentService implements IAreaAgentService {
	//-------- attributes --------

	/**
	 * The agent.
	 */
	@ServiceComponent
	protected IInternalAccess agent;

	public HashMap AgentMap;



	//-------- attributes --------

	//public void sendTrip(String text)
	public void sendAreaAgentUpdate(String text)
	{
		final TrikeAgent TrikeAgent	= (TrikeAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();

		//TODO: anpassen an AreaAgent
		Job Job = new Job(text);
		TrikeAgent.AddJobToJobList(Job);



		//TrikeAgent.AddTripIDTripList(text);



	}
}