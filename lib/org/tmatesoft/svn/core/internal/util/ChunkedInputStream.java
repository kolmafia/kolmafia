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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.IOExceptionWrapper;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author TMate Software Ltd.
 */
public class ChunkedInputStream extends InputStream {

    private String myCharset;
    private InputStream myInputStream;
    private int myChunkSize;
    private int myPosition;
    private boolean myIsBOF = true;
    private boolean myIsEOF = false;
    private boolean myIsClosed = false;

    public ChunkedInputStream(final InputStream in, String charset) {
        myInputStream = in;
        myPosition = 0;
        myCharset = charset;
    }
    public int read() throws IOException {
        if (myIsClosed) {
            throw new IOException("Attempted read from closed stream.");
        }
        if (myIsEOF) {
            return -1;
        } 
        if (myPosition >= myChunkSize) {
            nextChunk();
            if (myIsEOF) { 
                return -1;
            }
        }
        myPosition++;
        return myInputStream.read();
    }

    public int read (byte[] b, int off, int len) throws IOException {
        if (myIsClosed) {
            throw new IOException("Attempted read from closed stream.");
        }
        if (myIsEOF) { 
            return -1;
        }
        if (myPosition >= myChunkSize) {
            nextChunk();
            if (myIsEOF) { 
                return -1;
            }
        }
        len = Math.min(len, myChunkSize - myPosition);
        int count = myInputStream.read(b, off, len);
        myPosition += count;
        return count;
    }

    public int read (byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    private void readCRLF() throws IOException {
        int cr = myInputStream.read();
        int lf = myInputStream.read();
        if ((cr != '\r') || (lf != '\n')) { 
            throw new IOException(
                "CRLF expected at end of chunk: " + cr + "/" + lf);
        }
    }

    private void nextChunk() throws IOException {
        if (!myIsBOF) {
            readCRLF();
        }
        myChunkSize = getChunkSizeFromInputStream(myInputStream, myCharset);
        myIsBOF = false;
        myPosition = 0;
        if (myChunkSize == 0) {
            myIsEOF = true;
        }
    }

    private static int getChunkSizeFromInputStream(final InputStream in, String charset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // States: 0=normal, 1=\r was scanned, 2=inside quoted string,
        // -1=end
        int state = 0; 
        while (state != -1) {
        int b = in.read();
            if (b == -1) { 
                throw new IOException("chunked stream ended unexpectedly");
            }
            switch (state) {
                case 0: 
                    switch (b) {
                        case '\r':
                            state = 1;
                            break;
                        case '\"':
                            state = 2;
                            /* fall through */
                        default:
                            baos.write(b);
                    }
                    break;

                case 1:
                    if (b == '\n') {
                        state = -1;
                    } else {
                        // this was not CRLF
                        throw new IOException("Protocol violation: Unexpected"
                            + " single newline character in chunk size");
                    }
                    break;

                case 2:
                    switch (b) {
                        case '\\':
                            b = in.read();
                            baos.write(b);
                            break;
                        case '\"':
                            state = 0;
                            /* fall through */
                        default:
                            baos.write(b);
                    }
                    break;
                default: 
                    try {
                        SVNErrorManager.assertionFailure(false, null, SVNLogType.NETWORK);
                    } catch (SVNException svne) {
                        throw new IOExceptionWrapper(svne);
                    }
            }
        }

        // parse data
        String dataString = new String(baos.toByteArray(), charset);
        int separator = dataString.indexOf(';');
        dataString = (separator > 0) ? dataString.substring(0, separator).trim() : dataString.trim();

        int result;
        try {
            result = Integer.parseInt(dataString.trim(), 16);
        } catch (NumberFormatException e) {
            throw new IOException ("Bad chunk size: " + dataString);
        }
        return result;
    }

    public void close() throws IOException {
        if (!myIsClosed) {
            try {
                if (!myIsEOF) {
                    FixedSizeInputStream.consumeRemaining(this);
                }
            } finally {
                myIsEOF = true;
                myIsClosed = true;
            }
        }
    }
}

