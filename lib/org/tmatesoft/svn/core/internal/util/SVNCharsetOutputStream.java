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

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.IOExceptionWrapper;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNCharsetOutputStream extends FilterOutputStream {

    private static final byte[] EMPTY_ARRAY = new byte[0];

    private SVNCharsetConvertor myCharsetConvertor;
    private ByteBuffer myOutputBuffer;
    private boolean myFlushed;

    public SVNCharsetOutputStream(OutputStream out, Charset inputCharset, Charset outputCharset,
                                  CodingErrorAction malformedInputAction, CodingErrorAction unmappableCharAction) {
        super(out);
        CharsetDecoder decoder = inputCharset.newDecoder();
        decoder.onMalformedInput(malformedInputAction);
        decoder.onUnmappableCharacter(unmappableCharAction);

        CharsetEncoder encoder = outputCharset.newEncoder();
        encoder.onMalformedInput(malformedInputAction);
        encoder.onUnmappableCharacter(unmappableCharAction);
        
        myCharsetConvertor = new SVNCharsetConvertor(decoder, encoder);
        myFlushed = false;
    }

    public void write(int b) throws IOException {
        write(new byte[]{(byte) (b & 0xFF)});
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        try {
            myOutputBuffer = myCharsetConvertor.convertChunk(b, off, len, myOutputBuffer, false);
            myOutputBuffer.flip();
            out.write(myOutputBuffer.array(), myOutputBuffer.arrayOffset(), myOutputBuffer.limit());
        } catch (SVNException e) {
            throw new IOExceptionWrapper(e);
        }
    }

    public void flush() throws IOException {
        if (!myFlushed) {
            try {
                myOutputBuffer = myCharsetConvertor.convertChunk(EMPTY_ARRAY, 0, 0, myOutputBuffer, true);
                myOutputBuffer = myCharsetConvertor.flush(myOutputBuffer);
                myOutputBuffer.flip();
                out.write(myOutputBuffer.array(), myOutputBuffer.arrayOffset(), myOutputBuffer.limit());
            } catch (SVNException e) {
                throw new IOExceptionWrapper(e);
            } finally {
                myFlushed = true;                
            }
        }
        super.flush();
    }

    public void close() throws IOException {
        flush();
        out.close();
    }

    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append("SVNCharsetOutputStream");
        buffer.append("[").append(myCharsetConvertor);
        buffer.append(']');
        return buffer.toString();
    }
}