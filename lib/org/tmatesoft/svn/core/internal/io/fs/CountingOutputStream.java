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
package org.tmatesoft.svn.core.internal.io.fs;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class CountingOutputStream extends FilterOutputStream {

    private long myPosition;

    public CountingOutputStream(OutputStream stream, long offset) {
        super(stream);
        myPosition = offset >= 0 ? offset : 0;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        myPosition += len;
    }

    public void write(int b) throws IOException {
        out.write(b);
        myPosition++;
    }

    public void write(byte[] b) throws IOException {
        out.write(b);
        myPosition += b.length;
    }

    public long getPosition() {
        return myPosition;
    }
}
