package io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.ISendTripService;

import io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent.Trip;
import io.github.agentsoz.ees.jadexextension.masterthesis.JadexService.NotifyService2.INotifyService2;
import io.github.agentsoz.util.Location;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.ServiceQuery;
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
 *  The chat service interface.
 */
public interface IsendTripService
{
	/**
	 *  Receives a chat message.
	 *  @param sender The sender's name.
	 *  @param text The message text.
	 */
	public void sendTrip(String text);


    /**
     *  Basic chat user interface.
     */
    class SendtripGui extends JFrame
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
                    String segments[] = text.split("-");

                    String agentID = segments[0];
                    String trip = segments [1];

                    agent.scheduleStep(new IComponentStep<Void>()
                    {
                        public IFuture<Void> execute(IInternalAccess ia)
                        {
                            ServiceQuery<IsendTripService> query = new ServiceQuery<>(IsendTripService.class);
                            query.setScope(ServiceScope.PLATFORM); // local platform, for remote use GLOBAL
                            query.setServiceTags("user:" + agentID);
                            Collection<IsendTripService> service = ia.getLocalServices(query);
                            for (Iterator<IsendTripService> it = service.iterator(); it.hasNext(); )
                            {
                                IsendTripService cs = it.next();

                                cs.sendTrip(trip);

                                System.out.println( "TripReqControlAgents send trip "+trip+ "to vehicle agent "+agentID);
                                }




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
}

