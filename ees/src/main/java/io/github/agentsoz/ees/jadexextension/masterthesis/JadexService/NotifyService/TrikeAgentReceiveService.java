package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TrikeAgent;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;

import java.util.List;

/**
 *  Chat service implementation.
 */
@Service
public class TrikeAgentReceiveService implements INotifyService {
	//-------- attributes --------

	/**
	 * The agent.
	 */
	@ServiceComponent
	protected IInternalAccess agent;
	int number;

	//-------- attributes --------	


	public void NotifyotherAgent(List<ActionContent> ActionContentList, List<PerceptContent> PerceptContentList, boolean activestatus) {
		// Reply if the message contains the keyword.
		final TrikeAgent trikeAgent = (TrikeAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();

		for (ActionContent actionContent : ActionContentList){
			boolean isSuccess = trikeAgent.actionContentRingBuffer.write(actionContent);
			if(!isSuccess){
				throw new RuntimeException("BUFFER OVERRIDE!!!");
			}
		}

		for (PerceptContent perceptContent : PerceptContentList){
			trikeAgent.perceptContentRingBuffer.write(perceptContent);
		}
		trikeAgent.print("receives information from MATSIM");

		if (activestatus)
		{
			trikeAgent.isMatsimFree = true;
		}
	}
}
