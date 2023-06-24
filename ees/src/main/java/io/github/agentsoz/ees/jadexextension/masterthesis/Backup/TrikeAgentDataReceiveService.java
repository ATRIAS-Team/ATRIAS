package io.github.agentsoz.ees.jadexextension.masterthesis.Backup;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;

import java.util.List;

/**
 *  Chat service implementation.
 */
@Service
public class TrikeAgentDataReceiveService implements INotifyService {
	//-------- attributes --------

	/**
	 * The agent.
	 */
	@ServiceComponent
	protected IInternalAccess agent;

	//-------- attributes --------	


	public void NotifyotherAgent(List<ActionContent> ActionContentList, List<PerceptContent> PerceptContentList, boolean activestatus) {
		// Reply if the message contains the keyword.
		final TestV3TrikeAgent1 TrikeAgent = (TestV3TrikeAgent1) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();

		TrikeAgent.setActionContentList(ActionContentList);
		TrikeAgent.setPerceptContentList(PerceptContentList);
		TrikeAgent.resultfromMATSIM = "true";
		if (activestatus)
		{
			TrikeAgent.activestatus = activestatus;
		}

	}


}
