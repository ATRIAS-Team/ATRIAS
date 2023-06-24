package io.github.agentsoz.ees.jadexextension.masterthesis.Backup;

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;

import java.util.List;

/**
 *  The chat service interface.
 */
public interface INotifyService
{

	public void NotifyotherAgent(List<ActionContent> ActionContentList, List<PerceptContent> PerceptContent, boolean activestatus);

}

