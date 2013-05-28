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
package org.tmatesoft.svn.core.internal.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLogAdapter;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class DefaultSVNDebugLogger extends SVNDebugLogAdapter {

    private Map myLoggers;
    private Handler myTestHandler;

    public DefaultSVNDebugLogger() {
        myLoggers = new SVNHashMap();
    }

    public void log(SVNLogType logType, Throwable th, Level logLevel) {
        Logger logger = getLogger(logType);
        if (logger.isLoggable(logLevel) && th != null) {
            logger.log(logLevel, getMessage(logType, th.getMessage()), th);
        }
    }

    public void log(SVNLogType logType, String message, Level logLevel) {
        Logger logger = getLogger(logType); 
        if (logger.isLoggable(logLevel) && message != null) {
            logger.log(logLevel, getMessage(logType, message));
        }
    }
    
    public void log(SVNLogType logType, String message, byte[] data) {
        Logger logger = getLogger(logType);
        if (logger.isLoggable(Level.FINEST)) {
            try {
                logger.log(Level.FINEST, message + "\n" + new String(data, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                logger.log(Level.FINEST, message + "\n" + new String(data));
            }
        }
    }

    public InputStream createLogStream(SVNLogType logType, InputStream is) {
        if (getLogger(logType).isLoggable(Level.FINEST)) {
            return super.createLogStream(logType, is);
        }
        return is;
    }

    public OutputStream createLogStream(SVNLogType logType, OutputStream os) {
        if (getLogger(logType).isLoggable(Level.FINEST)) {
            return super.createLogStream(logType, os);
        }
        return os;
    }
    
    private Logger getLogger(SVNLogType logType) {
        Logger logger = (Logger) myLoggers.get(logType);
        if (logger == null) {
            logger = Logger.getLogger(logType.getName());
            String testName = SVNFileUtil.getEnvironmentVariable("SVN_CURRENT_TEST");
            if (testName != null) {
                if (myTestHandler == null) {
                    try {
                        myTestHandler = createTestLogger(testName);
                    } catch (IOException e) {}
                }
                if (myTestHandler != null) {
                    Handler[] existingHandlers = logger.getHandlers();
                    for (int i = 0; i < existingHandlers.length; i++) {
                        logger.removeHandler(existingHandlers[i]);
                    }
                    logger.addHandler(myTestHandler);
                }
            }
            myLoggers.put(logType, logger);
        }
        return logger;
    }

    private String getMessage(SVNLogType logType, String originalMessage) {
        return logType.getShortName() + ": " + originalMessage;
    }

    private static Handler createTestLogger(String testName) throws IOException {
        URL mySource = DefaultSVNDebugLogger.class.getProtectionDomain().getCodeSource().getLocation();
        File programDir = new File(mySource.getPath()).getParentFile();
        
        File logFile = new File(programDir, "../logs/" + testName.trim() + ".log");
        FileHandler fileHandler = new FileHandler(logFile.getAbsolutePath(), 0, 1, true);
        fileHandler.setLevel(Level.FINEST);
        fileHandler.setFormatter(new DefaultSVNDebugFormatter());
        return fileHandler;
    }
}