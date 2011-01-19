/**
 * Copyright (c) 2003-2006, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EventListener;

/**
 * This class wraps an EventListener subclass (and thus any AWT/Swing event listener such as
 * <tt>ActionListener</tt>s, <tt>MouseListener</tt>s and so on) making sure that if
 * a wrapped listener is executing, another wrapped listener (even of different type) it is
 * not executed.
 * <br />
 * For example, if a user clicks very quickly on a button, it may trigger the execution of
 * the associated listener more than once. When the listener contains Foxtrot code, the
 * second click event is dequeued by Foxtrot and processed again, invoking the listener
 * again. Using this class to wrap the listener avoids this problem: the second event will
 * be dequeued and processed by Foxtrot as before, but the wrapped listener will not be
 * called.
 * <br />
 * Example Usage:
 * <pre>
 * final JButton apply = new JButton("Apply");
 * apply.addActionListener((ActionListener)EventListenerProxy.create(ActionListener.class, new ActionListener()
 * {
 *    public void actionPerformed(ActionEvent e)
 *    {
 *       apply.setEnabled(false);
 *       Worker.post(new Job()
 *       {
 *          public Object run()
 *          {
 *             // Lenghty apply code
 *          }
 *       });
 *    }
 * }));
 * <p/>
 * JButton cancel = new JButton("Cancel");
 * cancel.addActionListener((ActionListener)EventListenerProxy.create(ActionListener.class, new ActionListener()
 * {
 *    public void actionPerformed(ActionEvent e)
 *    {
 *       // For example, dispose a dialog
 *    }
 * }));
 * </pre>
 * Without using EventListenerProxy, when a user clicks on the apply button and immediately
 * after on the cancel button, it happens that apply's button listener is executed; in there,
 * usage of Foxtrot's Worker will dequeue and execute the event associated to the cancel
 * button click, that will - for example - dispose the dialog <strong>before</strong> the
 * apply operation is finished. <br />
 * When using EventListenerProxy instead, the second event - the cancel button click - will
 * not be executed: the event will be processed without invoking the wrapped listener. <br />
 * The overhead in the code is to change plain listeners:
 * <pre>
 * button.addActionListener(new ActionListener() {...});
 * </pre>
 * to this:
 * <pre>
 * button.addActionListener((ActionListener)EventListenerProxy.create(ActionListener.class, new ActionListener() {...}));
 * </pre>
 *
 * @version $Revision: 259 $
 */
public class EventListenerProxy implements InvocationHandler
{
    /**
     * This flag signals the fact that the wrapped listener is in execution.
     * It is static since event listeners are never processed in parallel,
     * but one after the other in the Event Dispatch Thread.
     * Only after one listener finishes another one can execute.
     */
    private static boolean working;

    private EventListener listener;

    /**
     * Creates an instance that wraps the given listener
     */
    protected EventListenerProxy(EventListener listener)
    {
        if (listener == null) throw new NullPointerException("EventListener cannot be null");
        this.listener = listener;
    }

    /**
     * Creates a proxy for the given listener. <br />
     * The listener must implement the given listener interface
     *
     * @param listenerInterface The interface used to create the proxy
     * @param listener          The listener to proxy
     * @return A proxy for the given listener
     * @throws NullPointerException     When the interface or the listener is null
     * @throws IllegalArgumentException When the listener does not implement the interface
     */
    public static EventListener create(Class listenerInterface, EventListener listener)
    {
        if (!listenerInterface.isInstance(listener)) throw new IllegalArgumentException("EventListener " + listener + " must implement " + listenerInterface.getName());
        return (EventListener)Proxy.newProxyInstance(listenerInterface.getClassLoader(), new Class[]{listenerInterface}, new EventListenerProxy(listener));
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (working) return null;

        try
        {
            working = true;
            return method.invoke(listener, args);
        }
        finally
        {
            working = false;
        }
    }
}
