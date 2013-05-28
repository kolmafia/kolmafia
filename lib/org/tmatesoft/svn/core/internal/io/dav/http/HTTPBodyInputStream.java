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
package org.tmatesoft.svn.core.internal.io.dav.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class HTTPBodyInputStream extends InputStream {
    
    private File myFile;
    private InputStream myDelegate;

    public HTTPBodyInputStream(File file) {
        myFile = file;
    }


    public int read(byte[] b, int off, int len) throws IOException {
        return getDelegate().read(b, off, len);
    }

    public int read(byte[] b) throws IOException {
        return getDelegate().read(b);
    }

    public int read() throws IOException {
        return getDelegate().read();
    }

    public void close() throws IOException {
        if (myDelegate != null) {
            try {
                getDelegate().close();
            } finally {
                myDelegate = null;
            }
        }
    }

    public synchronized void reset() throws IOException {
        close();
    }
    
    private InputStream getDelegate() throws IOException {
        if (myDelegate == null) {
            try {
                myDelegate = SVNFileUtil.openFileForReading(myFile, SVNLogType.NETWORK);
            } catch (SVNException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                }
                throw new IOException(e.getMessage());
            }
        }
        return myDelegate;
    }

}
