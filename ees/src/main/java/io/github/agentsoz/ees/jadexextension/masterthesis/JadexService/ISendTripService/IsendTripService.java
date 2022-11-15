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
    //public void sendTrip(String text);
    public void sendJob(String text);


    /**
     *  Basic chat user interface.
     */




}