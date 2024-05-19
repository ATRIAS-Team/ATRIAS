package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;

import java.util.List;
import java.util.Map;

/**
 *  The chat service interface.
 */
public interface INotifyService
{

	public void NotifyotherAgent(List<ActionContent> ActionContentList, List<PerceptContent> PerceptContent, boolean activestatus);

}

