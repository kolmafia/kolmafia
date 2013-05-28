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

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
public class SVNCharsetConvertor {

    private static final int DEFAULT_BUFFER_CAPACITY = 1024;

    private CharsetDecoder myDecoder;
    private CharsetEncoder myEncoder;

    private CharBuffer myCharBuffer;
    private ByteBuffer myInputByteBuffer;

    public SVNCharsetConvertor(CharsetDecoder decoder, CharsetEncoder encoder) {
        myDecoder = decoder;
        myEncoder = encoder;
        reset();
    }

    public SVNCharsetConvertor reset() {
        myEncoder = myEncoder.reset();
        myDecoder = myDecoder.reset();
        myCharBuffer = null;
        myInputByteBuffer = null;
        return this;
    }

    public ByteBuffer convertChunk(byte[] b, int offset, int length, ByteBuffer dst, boolean endOfInput) throws SVNException {
        myInputByteBuffer = allocate(myInputByteBuffer, length);
        myInputByteBuffer.put(b, offset, length);
        myInputByteBuffer.flip();
        myCharBuffer = allocate(myCharBuffer, (int) (myDecoder.maxCharsPerByte() * myInputByteBuffer.remaining()));

        CoderResult result = myDecoder.decode(myInputByteBuffer, myCharBuffer, endOfInput);
        if (result.isError()) {
            throwException(result);
        } else if (result.isUnderflow()) {
            myInputByteBuffer.compact();
        } else {
            myInputByteBuffer.clear();
        }

        myCharBuffer.flip();
        dst = allocate(dst, (int) (myEncoder.maxBytesPerChar() * myCharBuffer.remaining()));

        result = myEncoder.encode(myCharBuffer, dst, false);
        if (result.isError()) {
            throwException(result);
        } else if (result.isUnderflow()) {
            myCharBuffer.compact();
        } else {
            myCharBuffer.clear();
        }

        return dst;
    }

    public ByteBuffer flush(ByteBuffer dst) throws SVNException {
        if (myCharBuffer != null) {
            CoderResult result;
            while (true) {
                result = myDecoder.flush(myCharBuffer);
                if (result.isError()) {
                    throwException(result);
                }
                if (result.isUnderflow()) {
                    break;
                }
            }
            myCharBuffer.flip();
            dst = allocate(dst, (int) (myEncoder.maxBytesPerChar() * myCharBuffer.remaining()));
            result = myEncoder.encode(myCharBuffer, dst, true);
            if (result.isError()) {
                throwException(result);
            }
            while (true) {
                result = myEncoder.flush(dst);
                if (result.isError()) {
                    throwException(result);
                }
                if (result.isUnderflow()) {
                    break;
                }
            }
        }
        reset();
        return dst;
    }

    private static ByteBuffer allocate(ByteBuffer buffer, int length) {
        if (buffer == null) {
            length = Math.max(length * 3 / 2, DEFAULT_BUFFER_CAPACITY);
            return ByteBuffer.allocate(length);
        }
        if (buffer.remaining() < length) {
            ByteBuffer expandedBuffer = ByteBuffer.allocate((buffer.position() + length) * 3 / 2);
            buffer.flip();
            expandedBuffer.put(buffer);
            return expandedBuffer;
        }
        return buffer;
    }

    private static CharBuffer allocate(CharBuffer buffer, int length) {
        if (buffer == null) {
            length = Math.max(length * 3 / 2, DEFAULT_BUFFER_CAPACITY);
            return CharBuffer.allocate(length);
        }
        if (buffer.remaining() < length) {
            CharBuffer expandedBuffer = CharBuffer.allocate((buffer.position() + length) * 3 / 2);
            buffer.flip();
            expandedBuffer.put(buffer);
            return expandedBuffer;
        }
        return buffer;
    }

    private static void throwException(CoderResult result) throws SVNException {
        try {
            result.throwException();
        } catch (CharacterCodingException e) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e), e, SVNLogType.DEFAULT);
        }
    }

    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append("SVNCharsetConvertor");
        buffer.append("[from=").append(myDecoder.charset().displayName());
        buffer.append(", to=").append(myEncoder.charset().displayName());
        buffer.append(']');
        return buffer.toString();
    }
}
