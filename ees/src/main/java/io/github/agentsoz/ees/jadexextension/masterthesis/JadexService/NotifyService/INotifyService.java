package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;
import io.github.agentsoz.ees.jadexextension.masterthesis.DisruptionComponent.DisruptionInjector;

import java.util.List;

/**
 *  The chat service interface.
 */
public interface INotifyService
{
	//Robustheit
	public void toDisruptAgents(List<DisruptionInjector> agentID);
	public void NotifyotherAgent(List<ActionContent> ActionContentList, List<PerceptContent> PerceptContent, boolean activestatus);

}

