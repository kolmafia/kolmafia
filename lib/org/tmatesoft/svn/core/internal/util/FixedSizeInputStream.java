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
import java.io.InputStream;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FixedSizeInputStream extends InputStream {
    
    private long myLength;
    private InputStream mySource;

    public FixedSizeInputStream(InputStream source, long length) {
    	mySource = source;
        myLength = length;
    }

    public int read() throws IOException {
        int read = -1;
        if (myLength > 0) {
            read = mySource.read();
            if (read != -1) {
                myLength--;
            }
        }
        return read;
    }
    
    public int read(byte[] b, int off, int len) throws IOException {
        if (myLength <= 0) {
            return -1;
        }
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
               ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        long toRead = Math.min(myLength, len);
        toRead = mySource.read(b, off, (int) toRead);
        if (toRead >= 0) {
            myLength -= toRead;
        }
        return (int) toRead;
    }
    
    public void close() {
        // just read remaining data.
        if (myLength > 0) {
            try {
                consumeRemaining(this);
            } catch (IOException e) {
            }
        }
    }
    
    static void consumeRemaining(InputStream is) throws IOException {
        byte[] buffer = new byte[1024];
        while(is.read(buffer) >= 0);
    }

}
