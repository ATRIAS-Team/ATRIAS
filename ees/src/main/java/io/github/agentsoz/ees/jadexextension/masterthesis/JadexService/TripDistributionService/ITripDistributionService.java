package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.TripDistributionService;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;

import java.util.List;

/**
 *  The chat service interface.
 */
public interface ITripDistributionService
{

	public void NotifyotherAgent(List<ActionContent> ActionContentList, List<PerceptContent> PerceptContent, boolean activestatus);

}

