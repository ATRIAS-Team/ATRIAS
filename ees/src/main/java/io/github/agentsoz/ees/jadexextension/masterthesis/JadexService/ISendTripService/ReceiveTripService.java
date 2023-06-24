package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.ISendTripService;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TrikeAgent;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.ISendTripService.IsendTripService;
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
public class ReceiveTripService implements IsendTripService {
	//-------- attributes --------

	/**
	 * The agent.
	 */
	@ServiceComponent
	protected IInternalAccess agent;

	public HashMap AgentMap;

	Location Location2= new Location("", 288654.693529, 5286721.094209);

	public Trip Trip2 = new Trip("Trip2", "CustomerTrip", Location2, "NotStarted");

	Location Location3= new Location("", 238654.693529, 5886721.094209);

	public Trip Trip3 = new Trip("Trip3", "CustomerTrip", Location3, "NotStarted");

	Location Location4 = new Location("", 238674.543999, 5901195.908183);

	public  Trip Trip4 = new Trip("Trip4", "CustomerTrip", Location4, "NotStarted");

	//-------- attributes --------	

	public void sendTrip(String text)
	{
		final TrikeAgent TrikeAgent	= (TrikeAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		if (text.equals("3")) {
			TrikeAgent.AddTriptoTripList(Trip3);

		}
		if (text.equals("4")) {
			TrikeAgent.AddTriptoTripList(Trip4);
		}

		if (text.equals("2")) {
			TrikeAgent.AddTriptoTripList(Trip2);

		}

			TrikeAgent.AddTripIDTripList(text);

		}
	}


