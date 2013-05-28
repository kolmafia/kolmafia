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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public interface ISVNDebugLog {
    
    public void logError(SVNLogType logType, String message);

    public void logError(SVNLogType logType, Throwable th);

    public void logSevere(SVNLogType logType, String message);

    public void logSevere(SVNLogType logType, Throwable th);

    public void logFine(SVNLogType logType, Throwable th);

    public void logFine(SVNLogType logType, String message);

    public void logFiner(SVNLogType logType, Throwable th);

    public void logFiner(SVNLogType logType, String message);

    public void logFinest(SVNLogType logType, Throwable th);

    public void logFinest(SVNLogType logType, String message);
    
    public void log(SVNLogType logType, Throwable th, Level logLevel);
    
    public void log(SVNLogType logType, String message, Level logLevel);
    
    public void log(SVNLogType logType, String message, byte[] data);

    public InputStream createLogStream(SVNLogType logType, InputStream is);
    
    public OutputStream createLogStream(SVNLogType logType, OutputStream os);

    public OutputStream createOutputLogStream();

    public OutputStream createInputLogStream();

    public void flushStream(Object stream);

}
