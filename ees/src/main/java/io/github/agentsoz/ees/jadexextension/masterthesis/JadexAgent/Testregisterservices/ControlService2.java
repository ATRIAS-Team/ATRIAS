package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Testregisterservices;

import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.IFuture;

import java.util.Collection;
import java.util.Iterator;

/**
 *  Chat service implementation.
 */
@Service
public class ControlService2 implements INotifyService2
{
	//-------- attributes --------
	
	/** The agent. */
	@ServiceComponent
	protected IInternalAccess agent;
			
	//-------- attributes --------	
	
	/**
	 *  Receives a chat message.
	 *  @param sender The sender's name.
	 *  @param text The message text.
	 */
	public void NotifyotherAgent(final String sender, final String text)
	{
		// Reply if the message contains the keyword.
		final TestV3TrikeAgent1 TrikeAgent	= (TestV3TrikeAgent1) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		if(text == "notifyservices2")
		{
			IFuture<Collection<INotifyService2>> fut = agent.getFeature(IRequiredServicesFeature.class).getServices("notifyservices2");
			fut.addResultListener(new DefaultResultListener<Collection<INotifyService2>>()
			{
				public void resultAvailable(Collection<INotifyService2> result)
				{
					for(Iterator<INotifyService2> it = result.iterator(); it.hasNext(); )
					{
						INotifyService2 cs = it.next();
						if (it.hasNext() ==false)
						/** 31.01.2023** test to send seperate message to different Agents */ {
							TrikeAgent.setChangeservice(text);

							TrikeAgent.getChangeservice();
						}

					}
				}
			});
		}
	}
}
