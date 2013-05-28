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
package org.tmatesoft.svn.core.internal.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.tmatesoft.svn.util.ISVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.0
 * @author  TMate Software Ltd.
 */
public class SVNLogStream extends OutputStream {
    
    private ISVNDebugLog myLog;
    private ByteArrayOutputStream myBuffer;
    private String myPrefix;

    public SVNLogStream(ISVNDebugLog log, boolean write) {
        myLog = log;
        myBuffer = new ByteArrayOutputStream(2048);
        myPrefix = write ? "SENT" : "READ";
    }

    public void write(int b) throws IOException {
        myBuffer.write(b);
        flushBuffer(false);
    }    
    
    public void close() throws IOException {
        flushBuffer(true);        
    }

    public void flush() throws IOException {
        flushBuffer(true);        
    }

    public void write(byte[] b, int off, int len) throws IOException {
        myBuffer.write(b, off, len);
        flushBuffer(false);
    }

    public void flushBuffer(boolean force) {
        if (!force && myBuffer.size() < 1024) {
            return;
        }
        if (myLog != null && myBuffer.size() > 0) {
            myLog.log(SVNLogType.NETWORK, myPrefix, myBuffer.toByteArray());
        }
        myBuffer.reset();
    }
}
