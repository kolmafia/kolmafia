/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Helper class that returns which is the current JRE version
 * @version $Revision: 1.4 $
 */
class JREVersion
{
   private static Boolean jre141;
   private static Boolean jre140;
   private static Boolean jre14;
   private static Boolean jre13;
   private static Boolean jre12;

   /**
    * @deprecated
    */
   static boolean isJRE141()
   {
      if (jre141 == null)
      {
         Class cls = loadClass("java.awt.SequencedEvent");
         if (cls == null) jre141 = Boolean.FALSE;
         else jre141 = hasGetFirst(cls);
      }
      return jre141.booleanValue();
   }

   /**
    * @deprecated
    */
   static boolean isJRE140()
   {
      if (jre140 == null)
      {
         Class cls = loadClass("java.awt.SequencedEvent");
         if (cls == null) jre140 = Boolean.FALSE;
         else jre140 = hasGetFirst(cls).booleanValue() ? Boolean.FALSE : Boolean.TRUE;
      }
      return jre140.booleanValue();
   }

   static boolean isJRE14()
   {
      if (jre14 == null) jre14 = loadClass("java.util.logging.Logger") == null ? Boolean.FALSE : Boolean.TRUE;
      return jre14.booleanValue();
   }

   static boolean isJRE13()
   {
      if (jre13 == null) jre13 = loadClass("java.lang.reflect.Proxy") == null ? Boolean.FALSE : Boolean.TRUE;
      return jre13.booleanValue();
   }

   static boolean isJRE12()
   {
      if (jre12 == null) jre12 = loadClass("java.util.Collection") == null ? Boolean.FALSE : Boolean.TRUE;
      return jre12.booleanValue();
   }

   private static Class loadClass(String className)
   {
      // Avoid some smart guy puts the classes in the classpath or in lib/ext.
      // We ask directly to the boot classloader
      try
      {
         return Class.forName(className, false, null);
      }
      catch (ClassNotFoundException ignored)
      {
      }
      return null;
   }

   private static Boolean hasGetFirst(final Class cls)
   {
      return (Boolean)AccessController.doPrivileged(new PrivilegedAction()
      {
         public Object run()
         {
            try
            {
               cls.getDeclaredMethod("getFirst", null);
               return Boolean.TRUE;
            }
            catch (Exception ignored)
            {
            }
            return Boolean.FALSE;
         }
      });
   }
}
