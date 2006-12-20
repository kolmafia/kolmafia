/**
 * Copyright (c) 2002-2005, Simone Bordet
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
 * Specialized ConditionalEventPump for Sun's JDK 1.4.1.
 * @deprecated This class implements a workaround for bug #4531693 that has been fixed
 * in JDK 1.4.2 and backported to 1.4.1. Therefore it is recommended to upgrade to those
 * fixed JDK versions, as the bug not only affects Foxtrot but also the usage of dialogs.
 * @version $Revision: 1.3 $
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
      try
      {
         Object first = getFirstMethod.invoke(event, null);
         if (first == event) return true;
      }
      catch (Exception ignored)
      {
      }
      return false;
   }
}
