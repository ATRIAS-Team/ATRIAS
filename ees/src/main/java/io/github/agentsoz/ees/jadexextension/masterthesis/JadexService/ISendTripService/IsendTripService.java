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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Iterator;

/**
 *  The chat service interface.
 */
public interface IsendTripService
{
	/**
	 *  Receives a chat message.
     *  //@Marcel
	 *  //@param sender The sender's name.
	 *  //@param text The message text.
	 */

    //@Marcel
    //public void sendTrip(String text);
    //public void sendTrip(Trip trip);
    public void sendTrip(String trip);

    /**
     *  Basic chat user interface.
     */
    class SendtripGui extends JFrame //@marcel gui entfernt
    {
        //-------- attributes --------

        /** The textfield with received messages. */
        //@marcel gui entfernt
        //protected JTextArea received;

        //-------- constructors --------

        /**
         *  Create the user interface
         */
        public SendtripGui(final IExternalAccess agent)
        {
            //@marcel gui entfernt
            super(agent.getId().getName());
            //this.setLayout(new BorderLayout());

           //received = new JTextArea(10, 20);
            //final JTextField message = new JTextField();
            //JButton send = new JButton("send");

            //JPanel panel = new JPanel(new BorderLayout());
            //panel.add(message, BorderLayout.CENTER);
            //panel.add(send, BorderLayout.EAST);

            //getContentPane().add(new JScrollPane(received), BorderLayout.CENTER);
            //getContentPane().add(panel, BorderLayout.SOUTH);

            //send.addActionListener(new ActionListener()
            //{
                //@marcel gui entfernt
                //public void actionPerformed(ActionEvent e)
                //{
                    //@marcel gui entfernt
                    //final String text = message.getText();
                    final String text = "1-1";

                    //@Marcel
                    String segments[] = text.split("-");
                    String agentID = segments[0];
                    //String trip = segments [1];

                    //@Marcel fixen Trip senden
                    ///*
                    Location Location1 = new Location("", 268674.543999, 5901195.908183);
                    Trip Trip1 = new Trip("1", "CustomerTrip", Location1, "NotStarted");
                    //*/

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




                                //@Marcel
                                //cs.sendTrip(trip);



                                String trip = Trip1.tripForTransfer();


                                cs.sendTrip(trip);




                                //@Marcel
                                //System.out.println( "TripReqControlAgents send trip "+trip+ "to vehicle agent "+agentID);
                                System.out.println( "TripReqControlAgents send trip " + "to vehicle agent "+agentID);
                                }




                            return IFuture.DONE;
                        }
                    });
                }
            //});
    //@marcel gui entfernt
            /*
            addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    agent.killComponent();
                }

            });


            pack();
            setVisible(true);

             */
        }

        /**
         *  Method to add a new text message.
         *  @param text The text.
         */
        //@marcel gui entfernt
        /*
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
        */
    //}
}

