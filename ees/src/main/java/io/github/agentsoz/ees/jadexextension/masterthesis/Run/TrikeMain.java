package io.github.agentsoz.ees.jadexextension.masterthesis.Run;


import io.github.agentsoz.bdiabm.v2.AgentDataContainer;
import jadex.base.IPlatformConfiguration;
import jadex.base.PlatformConfigurationHandler;
import jadex.base.Starter;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.types.cms.CreationInfo;

/**
 *  Main for starting the example programmatically.
 *  
 *  To start the example via this Main.java Jadex platform 
 *  as well as examples must be in classpath.
 */
public class TrikeMain


 //TODO: LOGGER_NAME = "TrikeAgent"    für den Logger erstellen
{
	private static final Object lock = new Object();
	public static AgentDataContainer outAdc;
	public static int TrikeAgentNumber;

	public TrikeMain() {

	}

	/**
	 * Start a platform and the example.
	 */
	public static void main(String[] args) {
		start();
	}


	public static void start() {
		IExternalAccess platform = Starter.createPlatform(PlatformConfigurationHandler.getDefaultNoGui()).get();

		CreationInfo ci = new CreationInfo().setFilename(Config.pathname);
		platform.createComponent(ci).get();
		IPlatformConfiguration config = PlatformConfigurationHandler.getMinimal();
		Starter.createPlatform(config).get();



	}
}

