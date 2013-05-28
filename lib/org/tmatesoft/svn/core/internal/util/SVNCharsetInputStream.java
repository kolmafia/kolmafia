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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.IOExceptionWrapper;


/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNCharsetInputStream extends FilterInputStream {

    private static final int DEFAULT_BUFFER_CAPACITY = 1024;

    private SVNCharsetConvertor myCharsetConvertor;
    private byte[] mySourceBuffer;
    private ByteBuffer myConvertedBuffer;
    private boolean myEndOfStream;
    
    public SVNCharsetInputStream(InputStream in, Charset inputCharset, Charset outputCharset,
            CodingErrorAction malformedInputAction, CodingErrorAction unmappableCharAction) {
        super(in);
        CharsetDecoder decoder = inputCharset.newDecoder();
        decoder.onMalformedInput(malformedInputAction);
        decoder.onUnmappableCharacter(unmappableCharAction);

        CharsetEncoder encoder = outputCharset.newEncoder();
        encoder.onMalformedInput(malformedInputAction);
        encoder.onUnmappableCharacter(unmappableCharAction);

        myCharsetConvertor = new SVNCharsetConvertor(decoder, encoder);
        mySourceBuffer = new byte[DEFAULT_BUFFER_CAPACITY];
        myConvertedBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_CAPACITY);
        myEndOfStream = false;
    }

    public int read() throws IOException {
        byte[] b = new byte[1];
        int r = read(b);
        if (r < 0) {
            return -1;
        }
        return b[0];
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte b[], int off, int len) throws IOException {
        int available = myConvertedBuffer.position();
        if (myEndOfStream && available == 0){
            return -1;           
        }
        while (available < len) {
            int readed = fillBuffer();                        
            try {
                myConvertedBuffer = myCharsetConvertor.convertChunk(mySourceBuffer, 0, readed, myConvertedBuffer, myEndOfStream);
                if (myEndOfStream) {
                    myConvertedBuffer = myCharsetConvertor.flush(myConvertedBuffer);
                    break;
                }
            } catch (SVNException e) {
                throw new IOExceptionWrapper(e);
            } finally {
                available = myConvertedBuffer.position();
            }
        }
        myConvertedBuffer.flip();
        len = Math.min(myConvertedBuffer.remaining(), len);
        myConvertedBuffer = myConvertedBuffer.get(b, off, len);
        myConvertedBuffer = myConvertedBuffer.compact();
        return len;
    }

    private int fillBuffer() throws IOException {
        int readed = 0;
        while (readed < mySourceBuffer.length) {
            int r = in.read(mySourceBuffer, readed, mySourceBuffer.length - readed);
            if (r < 0) {
                myEndOfStream = true;
                break;
            }
            readed += r;
        }
        return readed;
    }

    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append("SVNCharsetInputStream");
        buffer.append("[").append(myCharsetConvertor);
        buffer.append(']');
        return buffer.toString();
    }
}