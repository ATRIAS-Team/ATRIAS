package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.*;
import io.github.agentsoz.ees.jadexextension.masterthesis.Run.JadexModel;
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
public class AreaAgentService implements IAreaTrikeService
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
		Location location = new Location("", Double.parseDouble(locationParts.get(0)), Double.parseDouble(locationParts.get(1)));
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
			// Send a list of Neighbours back
			if(messageObj.getContent().getAction().equals("callForNeighbours")){
				messageObj.getContent().getValues().get(0);
				//messageObj.getSenderId();


				ArrayList<String> locatedAgentIds = new ArrayList<>();
				locatedAgentIds.add(messageObj.getContent().getValues().get(0));
				locatedAgentIds.add("#");

				//todo: when everywhre just the ID and not user: is used remove this
				String requestID = messageObj.getSenderId();
				requestID = requestID.replace("", "");


				for (LocatedAgent locatedAgent: areaAgent.locatedAgentList.LocatedAgentList) {
					if ((!locatedAgent.getAgentID().equals(requestID))) {
						//System.out.println("equal? " + !(locatedAgent.getAgentID().equals(messageObj.getSenderId())));
						locatedAgentIds.add(locatedAgent.getAgentID());
					}
				}
				//todo: send answere here
				//todo: define where to store the list inside the trike

				// neu
				//hier decisiontaskID hinzufügen

				MessageContent messageContent = new MessageContent("sendNeighbourList", locatedAgentIds);
				//todo: crate a unique message id
				Message message = new Message("1", messageObj.getReceiverId(), messageObj.getSenderId(), "inform", JadexModel.simulationtime, messageContent);
				IAreaTrikeService service = IAreaTrikeService.messageToService(agent, message);

				//	sends back agent ids
				//todo: replace it by something generic
				service.trikeReceiveAgentsInArea(message.serialize());
				//
			}
		}
	}
	///////////////////////////////////////////////////////
	//trike agent part

	//DON'T PUT ANY CODE INSIDE METHODS
	public void trikeReceiveJob(String message) {}

	public void trikeReceiveTrikeMessage(String message) {}

	public void trikeReceiveAgentsInArea(String messageStr) {}
	///////////////////////////////////////////////////////
}