/**
 * Copyright (c) 2002-2008, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot.pumps;

import java.awt.AWTEvent;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * Specialized ConditionalEventPump for Sun's JDK 1.4.1, 1.4.2, 5.0 and 6.0.
 * This class used to implement a workaround for bug #4531693 that has been
 * fixed in JDK 1.4.2 and backported to 1.4.1.
 * However, the handling of sequenced events is very difficult in the JDK
 * itself (especially in conjunction with usage of modal dialogs, which use
 * the same mechanism used by Foxtrot) and various reincarnations of the
 * bug reappeared in JDK 5 and 6.
 * Basically it is <em>dangerous</em> to pump events when the pump starts
 * as a consequence of events delivered with <tt>SequencedEvent</tt>s
 * (such as focus events and window focus events).
 * SequencedEvents must be delivered in sequence, so it is not good to pump
 * the second event in the sequence from the listener of the first event,
 * and this class avoids just that.
 *
 * @version $Revision: 259 $
 */
public class SunJDK141ConditionalEventPump extends SunJDK14ConditionalEventPump
{
    private static Class sequencedEventClass;
    private static Method getFirstMethod;

    static
    {
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws Exception
                {
                    ClassLoader loader = ClassLoader.getSystemClassLoader();
                    sequencedEventClass = loader.loadClass("java.awt.SequencedEvent");
                    getFirstMethod = sequencedEventClass.getDeclaredMethod("getFirst", new Class[0]);
                    getFirstMethod.setAccessible(true);
                    return null;
                }
            });
        }
        catch (Throwable x)
        {
            if (debug) x.printStackTrace();
            throw new Error(x.toString());
        }
    }

    protected boolean canPumpEvent(AWTEvent event)
    {
        boolean result = false;
        if (sequencedEventClass.isInstance(event))
        {
            try
            {
                Object first = getFirstMethod.invoke(event, null);
                if (first == event) result = true;
            }
            catch (Throwable x)
            {
                if (debug) x.printStackTrace();
                result = false;
            }
            if (debug) System.out.println("[SunJDK141ConditionalEventPump] SequencedEvent can" + (result ? "" : "not") + " be pumped");
        }
        else
        {
            result = true;
        }

        return result;
    }
}
