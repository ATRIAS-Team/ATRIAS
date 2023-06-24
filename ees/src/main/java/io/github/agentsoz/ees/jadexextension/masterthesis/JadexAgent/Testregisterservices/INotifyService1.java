package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Testregisterservices;

/**
 *  The chat service interface.
 */
public interface INotifyService1
{
	/**
	 *  Receives a chat message.
	 *  @param sender The sender's name.
	 *  @param text The message text.
	 */
	public void NotifyotherAgent(String sender, String text);


}

