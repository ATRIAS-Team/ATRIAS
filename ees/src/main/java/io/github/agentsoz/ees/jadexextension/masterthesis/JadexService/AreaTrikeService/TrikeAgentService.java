package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaTrikeService;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.*;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.trikeagent.Utils;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
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

	public HashMap AgentMap;

	public void sendMessage(String messageStr){
		final TrikeAgent trikeAgent = (TrikeAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		Message messageObj = Message.deserialize(messageStr);

		if(trikeAgent.receivedMessageIds.containsKey(messageObj.getId())) return;
		trikeAgent.receivedMessageIds.put(messageObj.getId(), Instant.now().toEpochMilli());

		switch (messageObj.getComAct()){
			case CALL_FOR_PROPOSAL:
			case PROPOSE:
			case ACCEPT_PROPOSAL:
			case REJECT_PROPOSAL:
				trikeAgent.cnpBuffer.write(messageObj);
				break;
			case INFORM:{
				trikeAgent.messagesBuffer.write(messageObj);
				break;
			}
			case REQUEST:
				trikeAgent.jobsBuffer.write(messageObj);
				break;
			case ACK:
				switch (messageObj.getContent().getAction()){
					case "confirmAccept": {
						trikeAgent.cnpBuffer.write(messageObj);
						break;
					}
				}
				break;
		}
	}
}