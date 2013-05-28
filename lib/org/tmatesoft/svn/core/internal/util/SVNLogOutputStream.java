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

import java.io.IOException;
import java.io.OutputStream;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNLogOutputStream extends OutputStream {

    private OutputStream myOut;
    private OutputStream myLog;

    public SVNLogOutputStream(OutputStream out, OutputStream log) {
        myOut = out;
        myLog = log;
    }

    public void close() throws IOException {
        try {
            myOut.close();
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (myLog != null) {
                    myLog.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public void flush() throws IOException {
        try {
            myOut.flush();
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (myLog != null) {
                    myLog.flush();
                }
            } catch (IOException e) {
            }
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        try {
            myOut.write(b, off, len);
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (myLog != null) {
                    myLog.write(b, off, len);
                }
            } catch (IOException e) {
            }
        }
    }

    public void write(int b) throws IOException {
        try {
            myOut.write(b);
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (myLog != null) {
                    myLog.write(b);
                }
            } catch (IOException e) {
            }
        }
    }
    
    public void flushBuffer() {
        try {
            if (myLog != null) {
                myLog.flush();
            }
        } catch (IOException e) {
        }
    }
}
