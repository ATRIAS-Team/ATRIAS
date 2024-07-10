package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.ees.jadexextension.masterthesis.DisruptionComponent.DisruptionInjector;
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

	//Robustheit
	//call the setter of the trike agents, to set the information of to disrupt agents in all trike agents.
	public void toDisruptAgents(List<DisruptionInjector> toDisruptAgents){
		final TrikeAgent TrikeAgent = (io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TrikeAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		TrikeAgent.setToDisruptAgents(toDisruptAgents);
	}
	public void NotifyotherAgent(List<ActionContent> ActionContentList, List<PerceptContent> PerceptContentList, boolean activestatus) {
		// Reply if the message contains the keyword.
		final TrikeAgent TrikeAgent = (io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TrikeAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		TrikeAgent.setActionContentList(ActionContentList);
		TrikeAgent.setPerceptContentList(PerceptContentList);
		TrikeAgent.setResultfromMASIM("true");
		TrikeAgent.informSimInput = false;
		if (activestatus)
		{
			TrikeAgent.activestatus = activestatus;
			//todo: !!!
			//TrikeAgent.currentTrip.get(0).setProgress("Finished"); //was ist das? angleichen mit meinem plan!
			//TrikeAgent.tripIDList.add("0");
		}

	}


}
