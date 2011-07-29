/**
 * Copyright (c) 2002-2008, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot.utils;

import javax.swing.SwingUtilities;

import net.sourceforge.foxtrot.Job;

/**
 * This job flushes all pending AWT events that are waiting in the EventQueue.
 * <br />
 * For example, imagine a complex GUI that displays different components on the same container
 * (a single document user interface).
 * The GUI has mechanisms to close one component and then open another one (for example clicking
 * on a menu item of a menubar or a popup menu, or clicking on a toolbar button).
 * If opening a component is a lenghty operation that must run in the EventDispatchThread, then
 * the GUI freezes: the menu remains open (or the toolbar button pressed) and repaints do not
 * happen until the lengthy operation of opening the new component is finished.
 * <br />
 * FlushJob can be used to avoid this freezing:
 * <pre>
 * public void actionPerformed(ActionEvent e)
 * {
 *     // First step: close the current component (quick operation);
 *     // this removes it from its container, so that the GUI is now empty.
 *     closeCurrentComponent();
 * <p/>
 *     // Second step: flush events. At this point in the EventQueue there
 *     // may be repaint events for the menu (or the button), and repaint events
 *     // for the container (which should repaint an empty GUI, since the component
 *     // has been removed in the previous step)
 *     Worker.post(new FlushJob());
 * <p/>
 *     // After flushing the events, the user sees that the previous component
 *     // has been removed (sees that the GUI is empty).
 *     // Menus have been closed (or buttons repainted in non-pressed state),
 *     // time to open the new component (lengthy operation).
 *     loadComponent();
 * }
 * </pre>
 *
 * @version $Revision: 255 $
 */
public class FlushJob extends Job
{
    private final Object lock = new Object();
    private boolean flushed;

    public Object run()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                synchronized (lock)
                {
                    flushed = true;
                    lock.notify();
                }
            }
        });

        synchronized (lock)
        {
            while (!flushed)
            {
                try
                {
                    lock.wait();
                }
                catch (InterruptedException x)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return null;
    }
}
