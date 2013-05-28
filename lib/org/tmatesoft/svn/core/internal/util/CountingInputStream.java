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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * @version 1.1.2
 * @author  TMate Software Ltd.
 */
public class CountingInputStream extends FilterInputStream {
    private long myBytesRead;

    public CountingInputStream(InputStream in) {
        super(in);
        myBytesRead = 0;
    }

    public long getBytesRead() {
        return myBytesRead;
    }

    public int read() throws IOException {
        int r = super.read();
        if (r > 0) {
            myBytesRead++;
        }
        return r;
    }
    
    public int read(byte[] b) throws IOException {
        int count = super.read(b);
        if (count > 0) {
            myBytesRead += count;
        }
        return count;
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
        int count = super.read(b, off, len);
        if (count > 0) {
            myBytesRead += count;
        }
        return count;
    }
    
}
