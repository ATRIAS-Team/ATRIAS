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
public class ReceiveTripService implements IsendTripService {
	//-------- attributes --------

	/**
	 * The agent.
	 */
	@ServiceComponent
	protected IInternalAccess agent;

	public HashMap AgentMap;

	//-------- attributes --------	

	public void sendTrip(String text)
	{
		final TestV3TrikeAgent1 TrikeAgent	= (TestV3TrikeAgent1) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		if (text != null) {
							TrikeAgent.AddTriptoTripList(text);}
		}
	}


