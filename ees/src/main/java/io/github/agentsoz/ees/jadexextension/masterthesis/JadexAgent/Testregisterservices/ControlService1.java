package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Testregisterservices;

import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IPojoComponentFeature;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;

/**
 *  Chat service implementation.
 */
@Service
public class ControlService1 implements INotifyService1 {
	//-------- attributes --------

	/**
	 * The agent.
	 */
	@ServiceComponent
	protected IInternalAccess agent;

	//-------- attributes --------	

	/**
	 * Receives a chat message.
	 *
	 * @param sender The sender's name.
	 * @param text   The message text.
	 */
	public void NotifyotherAgent(final String sender, final String text) {
		// Reply if the message contains the keyword.
		final TestV3TrikeAgent1 TrikeAgent	= (TestV3TrikeAgent1) agent.getFeature(IPojoComponentFeature.class).getPojoAgent();
		if(text != null)
		{
	//		IFuture<Collection<INotifyService1>> fut = agent.getFeature(IRequiredServicesFeature.class).getServices("notifyservices1");
	//		fut.addResultListener(new DefaultResultListener<Collection<INotifyService1>>()
	//		{
	//			public void resultAvailable(Collection<INotifyService1> result) {
	//				for (Iterator<INotifyService1> it = result.iterator(); it.hasNext(); ) {
	//					INotifyService1 cs = it.next();
						/** 31.01.2023** test to send seperate message to different Agents */
	//					if (it.hasNext() ==false) {
							TrikeAgent.setChangeservice(text);

							TrikeAgent.getChangeservice();


	//						IServiceIdentifier sid3 = ((IService) agent.getProvidedService(INotifyService1.class)).getServiceId();
	//						System.out.println( "Service Identifier is now :"+sid3);
	//					}
	//			}
				}

	//		});
		}
	}

