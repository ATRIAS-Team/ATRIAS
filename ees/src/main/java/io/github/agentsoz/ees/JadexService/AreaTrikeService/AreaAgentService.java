package io.github.agentsoz.ees.JadexService.AreaTrikeService;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import io.github.agentsoz.ees.areaagent.AreaAgent;
import io.github.agentsoz.ees.shared.Message;
import io.github.agentsoz.ees.shared.SharedUtils;
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



	public IFuture<Void> sendMessage(String messageStr){
		final AreaAgent areaAgent	= (AreaAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		Message messageObj = Message.deserialize(messageStr);

		if(areaAgent.receivedMessageIds.containsKey(messageObj.getId())) return IFuture.DONE;
		areaAgent.receivedMessageIds.put(messageObj.getId(), SharedUtils.getSimTime());

		switch (messageObj.getComAct()){
			case INFORM:
			case ACK:
			case NACK:
				areaAgent.messagesBuffer.write(messageObj);
				areaAgent.plans.checkTrikeMessagesBuffer();
				break;
			case REQUEST:
				switch (messageObj.getContent().getAction()) {
					case "trikesInArea":
						areaAgent.messagesBuffer.write(messageObj);
						//areaAgent.plans.checkTrikeMessagesBuffer();
						break;
					}
				break;
			case CALL_FOR_PROPOSAL:
			case REJECT_PROPOSAL:
				areaAgent.areaMessagesBuffer.write(messageObj);
				//areaAgent.plans.checkAreaMessagesBuffer();
				break;
			case PROPOSE:
			case REFUSE:
				areaAgent.proposalBuffer.write(messageObj);
				//areaAgent.plans.checkProposalBuffer();
				break;
			case ACCEPT_PROPOSAL:
				areaAgent.jobRingBuffer.write(messageObj);
				//areaAgent.plans.checkAssignedJobs();
				break;
		}
		return IFuture.DONE;
	}
}
