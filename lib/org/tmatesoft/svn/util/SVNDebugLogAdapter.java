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

import org.tmatesoft.svn.core.internal.util.SVNLogInputStream;
import org.tmatesoft.svn.core.internal.util.SVNLogOutputStream;
import org.tmatesoft.svn.core.internal.util.SVNLogStream;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public abstract class SVNDebugLogAdapter implements ISVNDebugLog {

    public void logError(SVNLogType logType, String message) {
        log(logType, message, Level.INFO);
    }

    public void logError(SVNLogType logType, Throwable th) {
        log(logType, th, Level.INFO);
    }

    public void logSevere(SVNLogType logType, String message) {
        log(logType, message, Level.SEVERE);
    }

    public void logSevere(SVNLogType logType, Throwable th) {
        log(logType, th, Level.SEVERE);
    }

    public void logFine(SVNLogType logType, Throwable th) {
        log(logType, th, Level.FINE);
    }

    public void logFine(SVNLogType logType, String message) {
        log(logType, message, Level.FINE);
    }

    public void logFiner(SVNLogType logType, Throwable th) {
        log(logType, th, Level.FINER);
    }

    public void logFiner(SVNLogType logType, String message) {
        log(logType, message, Level.FINER);
    }

    public void logFinest(SVNLogType logType, Throwable th) {
        log(logType, th, Level.FINEST);
    }

    public void logFinest(SVNLogType logType, String message) {
        log(logType, message, Level.FINEST);
    }

    public void flushStream(Object stream) {
        if (stream instanceof SVNLogInputStream) {
            SVNLogInputStream logStream = (SVNLogInputStream) stream;
            logStream.flushBuffer();
        } else if (stream instanceof SVNLogOutputStream) {
            SVNLogOutputStream logStream = (SVNLogOutputStream) stream;
            logStream.flushBuffer();
        }
    }

    public InputStream createLogStream(SVNLogType logType, InputStream is) {
        return new SVNLogInputStream(is, createInputLogStream());
    }

    public OutputStream createLogStream(SVNLogType logType, OutputStream os) {
        return new SVNLogOutputStream(os, createOutputLogStream());
    }

    public OutputStream createInputLogStream() {
        return new SVNLogStream(this, false);
    }

    public OutputStream createOutputLogStream() {
        return new SVNLogStream(this, true);
    }

}
