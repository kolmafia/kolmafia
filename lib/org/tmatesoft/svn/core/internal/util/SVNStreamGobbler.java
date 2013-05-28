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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNStreamGobbler extends Thread {

    private InputStreamReader is;
    private final StringBuffer result = new StringBuffer();
    private IOException error;
    private boolean myIsEOF;
    private boolean myIsClosed;

    public SVNStreamGobbler(InputStream is) {
        try {
            this.is = new InputStreamReader(is, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            this.is = new InputStreamReader(is);
        }
    }

    public void run() {
        char[] buffer = new char[1024];
        synchronized (result) {
            while(true) {
                try {
                    int r = is.read(buffer);
                    if (r < 0) {
                        break;
                    }
                    if (r > 0) {
                        result.append(buffer, 0, r);
                    }
                } catch (IOException e) {
                    if (!myIsClosed) {
                        error = e;
                    }
                    break;
                }
            }
            myIsEOF = true;
            result.notifyAll();
        }
    }

    public void waitFor() {
        synchronized (result) {
            while (!myIsEOF) {
                try {
                    result.wait();
                } catch (InterruptedException e) {                    
                }
            }
        }
    }

    public void close() {
        synchronized (result) {
            myIsEOF = true;
            result.notifyAll();
            myIsClosed = true;
            SVNFileUtil.closeFile(is);
        }
    }

    public String getResult() {
        synchronized(result) {
            return result.toString();
        }
    }

    public IOException getError() {
        return error;
    }
}
