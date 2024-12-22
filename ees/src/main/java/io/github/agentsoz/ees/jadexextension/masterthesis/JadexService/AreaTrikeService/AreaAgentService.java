package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.*;
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

@Service
public class AreaAgentService implements IAreaTrikeService
{
	//-------- attributes --------

	/** The agent. */
	@ServiceComponent
	protected IInternalAccess agent;

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


	///////////////////////////////////////////////////////
	//	custom functions


	//area agent part

	//	updates located agent list
	public void areaReceiveUpdate(String messageStr)
	{
		final AreaAgent areaAgent = (AreaAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		Message messageObj = Message.deserialize(messageStr);
		ArrayList<String> locationParts = messageObj.getContent().getValues();
		//Location location = new Location(locationParts.get(0), Double.parseDouble(locationParts.get(1)), Double.parseDouble(locationParts.get(2)));
		Location location = null;
		if(locationParts != null){
			location = new Location("", Double.parseDouble(locationParts.get(0)), Double.parseDouble(locationParts.get(1)));
		}
		LocatedAgent locatedAgent = new LocatedAgent(messageObj.getSenderId(), location);
		areaAgent.locatedAgentList.updateLocatedAgentList(locatedAgent, messageObj.getSimTime(), messageObj.getContent().getAction());
	}


	/** todo: use receiveMessage for everything
	 *  receives messages
	 *
	 * @param messageStr
	 */

	public void receiveMessage(String messageStr){

		final AreaAgent areaAgent	= (AreaAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		Message messageObj = Message.deserialize(messageStr);

		if(messageObj.getComAct().equals("inform")){
			if(messageObj.getContent().getAction().equals("")){
				//todo: handle updates from trike for example their location here
			}
		}
		if(messageObj.getComAct().equals("request")){
			switch (messageObj.getContent().getAction()){
				case "trikesInArea":
					areaAgent.trikeMessagesBuffer.write(messageObj);
					break;
				case "BROADCAST":
					areaAgent.areaMessagesBuffer.write(messageObj);
					break;
				case "PROPOSE":
					areaAgent.proposalBuffer.write(messageObj);
					break;
				case "ASSIGN":
					areaAgent.jobRingBuffer.write(new Job(messageObj.getContent().getValues()));
					break;
			}
		}
	}
}