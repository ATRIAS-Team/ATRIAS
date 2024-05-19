package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;

import java.util.List;

/**
 *  The chat service interface.
 */
public interface INotifyService2
{

	public void NotifyotherAgent(String AgentID);

	public void removeTrikeAgentfromActiveList(String AgentID);



}

