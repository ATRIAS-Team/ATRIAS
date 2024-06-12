package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.AreaAgentService;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.*;
import io.github.agentsoz.util.Location;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;
import org.apache.xpath.operations.Bool;

import java.nio.DoubleBuffer;
import java.time.LocalDateTime;
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
	public void sendAreaAgentUpdate(String messageStr)
	{
	}
	public void sendJob(String messageStr){
		final TrikeAgent trikeAgent = (TrikeAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		Message messageObj = Message.deserialize(messageStr);
		Job job = new Job(messageObj.getContent().getValues());
		//trikeAgent.AddJobToJobList(job); //todo: remove
		DecisionTask decisionTask = new DecisionTask(job, messageObj.getSenderId(), "new");
		trikeAgent.AddDecisionTask(decisionTask);
	}
}