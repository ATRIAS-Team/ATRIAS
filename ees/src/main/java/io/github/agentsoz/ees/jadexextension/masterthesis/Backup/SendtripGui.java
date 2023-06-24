package io.github.agentsoz.ees.jadexextension.masterthesis.Backup;

import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.commons.future.DefaultResultListener;
import jadex.commons.future.IFuture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Iterator;

/**
 *  Basic chat user interface.
 */
public class SendtripGui extends JFrame
{
	//-------- attributes --------

	/** The textfield with received messages. */
	protected JTextArea received;

	//-------- constructors --------

	/**
	 *  Create the user interface
	 */
	public SendtripGui(final IExternalAccess agent)
	{
		super(agent.getId().getName());
		this.setLayout(new BorderLayout());
		
		received = new JTextArea(10, 20);
		final JTextField message = new JTextField();
		JButton send = new JButton("send");
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(message, BorderLayout.CENTER);
		panel.add(send, BorderLayout.EAST);
		
		getContentPane().add(new JScrollPane(received), BorderLayout.CENTER);
		getContentPane().add(panel, BorderLayout.SOUTH);
		
		send.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				final String text = message.getText(); 
				agent.scheduleStep(new IComponentStep<Void>()
				{
					public IFuture<Void> execute(IInternalAccess ia)
					{
						IFuture<Collection<IsendTripService>>	sendtripservices	= ia.getFeature(IRequiredServicesFeature.class).getServices("sendtripservices");
						sendtripservices.addResultListener(new DefaultResultListener<Collection<IsendTripService>>()
						{
							public void resultAvailable(Collection<IsendTripService> result)
							{
								for(Iterator<IsendTripService> it = result.iterator(); it.hasNext(); )
								{
									IsendTripService cs = it.next();
									cs.sendTrip(text);
								}
							}
						});
						return IFuture.DONE;
					}
				});
			}
		});
		
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				agent.killComponent();
			}
		});
		
		pack();
		setVisible(true);
	}
	
	/**
	 *  Method to add a new text message.
	 *  @param text The text.
	 */
	public void addMessage(final String text)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				received.append(text+"\n");
			}
		})
	;}
}
