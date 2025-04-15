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

import io.github.agentsoz.ees.shared.Message;
import io.github.agentsoz.ees.trikeagent.TrikeAgent;
import io.github.agentsoz.ees.shared.SharedUtils;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;
import jadex.commons.future.IFuture;

import java.util.HashMap;

/**
 *  Mapping service implementation.
 */
@Service
public class TrikeAgentService implements IAreaTrikeService {
	//-------- attributes --------

	/**
	 * The agent.
	 */
	@ServiceComponent
	protected IInternalAccess agent;

	@ServiceComponent
	public IExternalAccess externalAccess;

	public HashMap AgentMap;

	public IFuture<Void> sendMessage(String messageStr){
		final TrikeAgent trikeAgent = (TrikeAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		Message messageObj = Message.deserialize(messageStr);

		if(trikeAgent.receivedMessageIds.containsKey(messageObj.getId())) return IFuture.DONE;
		trikeAgent.receivedMessageIds.put(messageObj.getId(), SharedUtils.getSimTime());

		switch (messageObj.getComAct()){
			case CALL_FOR_PROPOSAL:
			case PROPOSE:
			case ACCEPT_PROPOSAL:
			case REJECT_PROPOSAL:
			case REFUSE:
				trikeAgent.cnpBuffer.write(messageObj);
				//trikeAgent.plans.checkCNPBuffer();
				break;
			case INFORM:{
				trikeAgent.messagesBuffer.write(messageObj);
				//trikeAgent.plans.checkMessagesBuffer();
				break;
			}
			case REQUEST:
				trikeAgent.jobsBuffer.write(messageObj);
				//trikeAgent.plans.checkJobBuffer();
				break;
			case ACK:
				switch (messageObj.getContent().getAction()){
					case "confirmAccept": {
						trikeAgent.cnpBuffer.write(messageObj);
						//trikeAgent.plans.checkCNPBuffer();
						break;
					}
				}
				break;
		}
		return IFuture.DONE;
	}
}
