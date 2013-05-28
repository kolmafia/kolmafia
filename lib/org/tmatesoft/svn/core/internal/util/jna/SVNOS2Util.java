/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
  * ====================================================================
 */
package org.tmatesoft.svn.core.internal.util.jna;

import java.io.File;
import java.lang.reflect.Method;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNOS2Util {
    private static boolean ourIsJNAEnabled;
    private static boolean ourIsOS2IO4JPresent;

    private static Class<?> os2io4jClazz;
    private static Method setReadOnlyMethod;
    private static Method setHiddenMethod;
    private static Method moveFileMethod;

    static {
        try {
            os2io4jClazz = Class.forName("de.rbri.os2io4j.OS2IO4J");
            setReadOnlyMethod = os2io4jClazz.getMethod("setReadOnly",  new Class[] {File.class, Boolean.TYPE});
            setHiddenMethod = os2io4jClazz.getMethod("setHidden",  new Class[] {File.class, Boolean.TYPE});
            moveFileMethod = os2io4jClazz.getMethod("moveFile",  new Class[] {File.class, File.class});
            ourIsOS2IO4JPresent = true;
        } catch (Throwable e) {
            // not found
            ourIsOS2IO4JPresent = false;
        }
        String jnaEnabledProperty = System.getProperty("svnkit.useJNA", "true");
        ourIsJNAEnabled = Boolean.valueOf(jnaEnabledProperty).booleanValue();
    }


    public static void setJNAEnabled(boolean enabled) {
        synchronized (SVNOS2Util.class) {
            ourIsJNAEnabled = enabled;
        }
    }
    
    public static boolean isOS2IO4JPresent() {
        synchronized (SVNOS2Util.class) {
            return ourIsOS2IO4JPresent && ourIsJNAEnabled;
        }
    }

    /**
     * makes file writable and
     * returns true in case operations succeeded, false in case of any error
     */
    public static boolean setWritable(File file) {
        if (file == null) {
            return false;
        }
        
        if (isOS2IO4JPresent()) {
            try {
                Object[] arguments = new Object[2];
                arguments[0] = file;
                arguments[1] = Boolean.FALSE;

                setReadOnlyMethod.invoke(null, arguments);
                return true;
            } catch (Throwable th) {
            }
        }
        return false;
    }

    /**
     * makes file hidden and
     * returns true in case operations succeeded, false in case of any error
     */
    public static boolean setHidden(File file, boolean hidden) {
        if (file == null) {
            return false;
        }
        
        if (isOS2IO4JPresent()) {
            try {
                Object[] arguments = new Object[2];
                arguments[0] = file;
                arguments[1] = Boolean.valueOf(hidden);

                setHiddenMethod.invoke(null, arguments);
                return true;
            } catch (Throwable th) {
            }
        }
        return false;
    }


    /**
     * atomically renames src to dst and
     * returns false if there is any error on rename.
     */
    public static boolean moveFile(File src, File dst) {
        if (src == null || dst == null) {
            return false;
        }
        
        if (isOS2IO4JPresent()) {
            try {
                Object[] arguments = new Object[2];
                arguments[0] = src;
                arguments[1] = dst;

                moveFileMethod.invoke(null, arguments);
                return true;
            } catch (Throwable th) {
            }
        }
        return false;
    }
}
