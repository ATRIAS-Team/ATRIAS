package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaAgentService;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.AreaAgent;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.LocatedAgent;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Message;
import io.github.agentsoz.util.Location;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 *  Chat service implementation.
 */
@Service
public class SendAreaAgentService implements IAreaAgentService
{
	//-------- attributes --------

	/** The agent. */
	@ServiceComponent
	protected IInternalAccess agent;

	//protected SendtripGui gui;

	/** The required services feature **/
	@ServiceComponent
	private IRequiredServicesFeature requiredServicesFeature;

	/** The clock service. */
	protected IClockService clock;

	/** The time format. */
	protected DateFormat format;

	//-------- attributes --------

	/**
	 *  Init the service.
	 */
	//@ServiceStart
	@OnStart
	public IFuture<Void> startService()
	{
		final Future<Void> ret = new Future<Void>();
		this.format = new SimpleDateFormat("hh:mm:ss");
		IFuture<IClockService>	fut	= requiredServicesFeature.getService("clockservice");
		fut.addResultListener(new ExceptionDelegationResultListener<IClockService, Void>(ret)
		{
			public void customResultAvailable(IClockService result)
			{
				clock = result;
				//gui = createGui(agent.getExternalAccess());
				ret.setResult(null);
			}
		});
		return ret;
	}


	//public void sendTrip(String text)
	public void sendAreaAgentUpdate(String messageStr)
	{
		final AreaAgent areaAgent	= (AreaAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		Message messageObj = Message.deserialize(messageStr);
		ArrayList<String> locationParts = messageObj.getContent().getValues();
		Location location = new Location(locationParts.get(0), Double.parseDouble(locationParts.get(1)), Double.parseDouble(locationParts.get(2)));
		LocatedAgent locatedAgent = new LocatedAgent(messageObj.getSenderId(), location);
		areaAgent.locatedAgentList.updateLocatedAgentList(locatedAgent, messageObj.getSimTime(), messageObj.getContent().getAction());
		//System.out.println(areaAgent.locatedAgentList.LocatedAgentList.get(0).getLastPosition().getX());
	}

	public void sendJob(String messageStr){
	}
}