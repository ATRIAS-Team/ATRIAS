package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.TestBeliefChangeTrigger;

import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;

/**
 *  Chat service implementation.
 */
@Service
public class TrikeAgentService implements ITestBeliefChangeService
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
	public void WriteinSAgent(final String text)
	{
		// Write into the Belief "ActiveAgentList" of the SupportAgent
		final SupportingAgent SupportingAgent	= (SupportingAgent) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		if(text != null)
		{
			//IFuture<Collection<ITestBeliefChangeService>> fut = agent.getFeature(IRequiredServicesFeature.class).getServices("testbeliefchangeservices");
			//fut.addResultListener(new DefaultResultListener<Collection<ITestBeliefChangeService>>()
			//{
			//	public void resultAvailable(Collection<ITestBeliefChangeService> result)
			//	{
			//		for(Iterator<ITestBeliefChangeService> it = result.iterator(); it.hasNext(); )
			//		{
			//			ITestBeliefChangeService cs = it.next();
			//			if (it.hasNext() ==false)

			//			{
						SupportingAgent.addActiveAgent(text);
						SupportingAgent.setTestbeliefs("true");

//						System.out.println(SupportingAgent.getActiveAgentList());
			//			}


			//		}
			//	}
			//});
		}
	}
}
