/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot.pumps;

import java.awt.AWTEvent;

import java.lang.reflect.Field;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import java.util.LinkedList;

/**
 * Specialized ConditionalEventPump for Sun's JDK 1.4.0.
 * @deprecated This class implements a workaround for bug #4531693 that has been fixed
 * in JDK 1.4.2 and backported to 1.4.1. Therefore it is recommended to upgrade to those
 * fixed JDK versions, as the bug not only affects Foxtrot but also the usage of dialogs.
 * @version $Revision: 1.4 $
 */
public class SunJDK140ConditionalEventPump extends SunJDK14ConditionalEventPump
{
   private static Class sequencedEventClass;
   private static Field listField;

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
               listField = sequencedEventClass.getDeclaredField("list");
               listField.setAccessible(true);
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
         LinkedList list = (LinkedList)listField.get(event);
         synchronized (sequencedEventClass)
         {
            if (list.getFirst() == event) return true;
         }
      }
      catch (Exception x)
      {
      }
      return false;
   }
}
