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
package org.tmatesoft.svn.core.internal.io.svn.sasl;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.security.sasl.SaslClient;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SaslInputStream extends InputStream {

    private InputStream mySource;
    private SaslClient myClient;
    private byte[] myReadBuffer;
    
    private ByteBuffer myByteBuffer;

    public SaslInputStream(SaslClient client, int bufferSize, InputStream in) {
        mySource = in;
        myReadBuffer = new byte[bufferSize*2];
        myClient = client;
        
    }

    public void close() throws IOException {
        mySource.close();
    }

    public int read() throws IOException {
        byte[] b = new byte[1];
        int r = read(b, 0, 1);
        if (r != 1) {
            return -1;
        }
        return b[0];
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int read = 0;
        while(true) {
            if (myByteBuffer == null) {
                fetchDecodedBuffer();
            }
            int toCopy = Math.min(len, myByteBuffer.remaining());
            myByteBuffer.get(b, off, toCopy);
            len -= toCopy;
            off += toCopy;
            read += toCopy;
            if (len == 0 || myByteBuffer.remaining() == 0) {
                if (myByteBuffer.remaining() == 0) {
                    myByteBuffer = null;
                }
                return read;
            }
        }
    }

    public long skip(long n) throws IOException {
        return 0;
    }

    private void fetchDecodedBuffer() throws IOException {
        DataInputStream dis = new DataInputStream(mySource);
        int encodedLength = dis.readInt();
        if (myReadBuffer.length < encodedLength) {
            myReadBuffer = new byte[(encodedLength * 3) / 2];
        }
        dis.readFully(myReadBuffer, 0, encodedLength);
        myByteBuffer = ByteBuffer.wrap(myClient.unwrap(myReadBuffer, 0, encodedLength));
    }
}
