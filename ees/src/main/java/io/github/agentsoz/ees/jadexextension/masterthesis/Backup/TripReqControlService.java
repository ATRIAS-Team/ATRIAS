package io.github.agentsoz.ees.jadexextension.masterthesis.Backup;

import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;

/**
 *  Chat service implementation.
 */
@Service
public class TripReqControlService implements ICheckIDCompleteService
{
	//-------- attributes --------
	
	/** The agent. */
	@ServiceComponent
	protected IInternalAccess agent;
			
	//-------- attributes --------	
	
	/**
	 *  Receives a chat message.

	 *  @param text The message text.
	 */
	public void WriteinIDAssignedList(final String text)
	{
		// Write into the Belief "ActiveAgentList" of the SupportAgent
		final TripRequestControlAgent TripRequestControlAgent	= (io.github.agentsoz.ees.jadexextension.masterthesis.Backup.TripRequestControlAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		if(text != null)
		{
						TripRequestControlAgent.addassignedIDAgent(text);
  						System.out.println(TripRequestControlAgent.getNumberAgentAssignedID());
						}
	}
}
