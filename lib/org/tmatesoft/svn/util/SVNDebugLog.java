/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.util;


import org.tmatesoft.svn.core.internal.util.DefaultSVNDebugLogger;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNDebugLog {
    
    private static ISVNDebugLog ourDefaultLog;

    public static void setDefaultLog(ISVNDebugLog log) {
        ourDefaultLog = log;
    }
    
    public static ISVNDebugLog getDefaultLog() {
        if (ourDefaultLog == null) {
            ourDefaultLog = new DefaultSVNDebugLogger();
        }
        return ourDefaultLog;
    }

}