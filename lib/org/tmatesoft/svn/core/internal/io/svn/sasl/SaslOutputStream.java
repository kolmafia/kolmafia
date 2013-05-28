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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import javax.security.sasl.SaslClient;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SaslOutputStream extends OutputStream {
    
    private OutputStream myOut;
    private SaslClient myClient;
    private byte[] myLengthBuffer = new byte[4];
    private ByteBuffer myBuffer;

    public SaslOutputStream(SaslClient client, int bufferSize, OutputStream out) {
        myOut = out;
        myClient = client;
        myBuffer = ByteBuffer.allocate(bufferSize);
    }

    public void write(int b) throws IOException {
        write(new byte[] {(byte) (b & 0xFF)});
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        // write to buffer, then flush if necessary.
        while(len > 0) {
            int toPut = Math.min(myBuffer.remaining(), len);
            myBuffer.put(b, off, toPut);
            off += toPut;
            len -= toPut;
            if (myBuffer.remaining() == 0) {
                flush();
            }
        }
    }

    public void close() throws IOException {
        flush();
        myOut.close();
    }

    public void flush() throws IOException {
        byte[] bytes = myBuffer.array();
        int off = myBuffer.arrayOffset();
        int len = myBuffer.position();
        if (len == 0) {
            return;
        }
        byte[] encoded = myClient.wrap(bytes, off, len);
        myLengthBuffer[0] = (byte) ((encoded.length & 0xFF000000) >> 24);
        myLengthBuffer[1] = (byte) ((encoded.length & 0x00FF0000) >> 16);
        myLengthBuffer[2] = (byte) ((encoded.length & 0x0000FF00) >> 8);
        myLengthBuffer[3] = (byte) ((encoded.length & 0x000000FF));
        myOut.write(myLengthBuffer, 0, myLengthBuffer.length);
        myOut.write(encoded, 0, encoded.length);
        myOut.flush();
        myBuffer.clear();
    }
}
