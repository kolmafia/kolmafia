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
package org.tmatesoft.svn.core.internal.delta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.InflaterInputStream;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.ISVNDeltaConsumer;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * Reads diff windows from stream and feeds them to the ISVNDeltaConsumer instance.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNDeltaReader {
    
    private ByteBuffer myBuffer;
    
    private int myHeaderBytes;
    private long myLastSourceOffset;
    private int myLastSourceLength;
    private boolean myIsWindowSent;
    private byte myVersion;
    
    public SVNDeltaReader() {
        myBuffer = ByteBuffer.allocate(4096);
        myBuffer.clear();
        myBuffer.limit(0);
    }
    
    public void reset(String path, ISVNDeltaConsumer consumer) throws SVNException {
        // if header was read, but data was not -> fire empty window.
        if (myHeaderBytes == 4 && !myIsWindowSent) {
            OutputStream os = consumer.textDeltaChunk(path, SVNDiffWindow.EMPTY);
            SVNFileUtil.closeFile(os);
        }
        myLastSourceLength = 0;
        myLastSourceOffset = 0;
        myHeaderBytes = 0;
        myIsWindowSent = false;
        myVersion = 0;

        myBuffer.clear();
        myBuffer.limit(0);
    }
    
    public void nextWindow(byte[] data, int offset, int length, String path, ISVNDeltaConsumer consumer) throws SVNException {
        appendToBuffer(data, offset, length);
        if (myHeaderBytes < 4) {
            if (myBuffer.remaining() < 4) {
                return;
            }
            if (myBuffer.get(0) != 'S' || myBuffer.get(1) != 'V' || myBuffer.get(2) != 'N' ||
                    (myBuffer.get(3) != '\0' && myBuffer.get(3) != '\1')) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.SVNDIFF_CORRUPT_WINDOW, "Svndiff has invalid header");
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }
            myVersion = myBuffer.get(3);
            myBuffer.position(4);
            int remainging = myBuffer.remaining();
            myBuffer.compact();
            myBuffer.position(0);
            myBuffer.limit(remainging);
            myHeaderBytes = 4;
        }
        while(true) {
            long sourceOffset = readLongOffset();
            if (sourceOffset < 0) {
                return;
            }
            int sourceLength = readOffset();
            if (sourceLength < 0) {
                return;
            }
            int targetLength = readOffset();
            if (targetLength < 0) {
                return;
            }
            int instructionsLength = readOffset();
            if (instructionsLength < 0) {
                return;
            }
            int newDataLength = readOffset();
            if (newDataLength < 0) {
                return;
            }
            if (sourceLength > 0 && 
                    (sourceOffset < myLastSourceOffset || 
                     sourceOffset + sourceLength < myLastSourceOffset + myLastSourceLength)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.SVNDIFF_CORRUPT_WINDOW, "Svndiff has backwards-sliding source views");
                SVNErrorManager.error(err, SVNLogType.DEFAULT);
            }
            if (myBuffer.remaining() < instructionsLength + newDataLength) {
                return;
            }
            myLastSourceOffset = sourceOffset;
            myLastSourceLength = sourceLength;
            SVNDiffWindow window = null;
            int allDataLength = newDataLength + instructionsLength;
            if (myVersion == 1) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int bufferPosition = myBuffer.position();
                try {
                    instructionsLength = deflate(instructionsLength, out);
                    newDataLength = deflate(newDataLength, out);
                } catch (IOException e) {
                    SVNDebugLog.getDefaultLog().logSevere(SVNLogType.DEFAULT, e);
                }
                byte[] bytes = out.toByteArray();
                ByteBuffer decompressed = ByteBuffer.wrap(bytes);
                decompressed.position(0);
                window = new SVNDiffWindow(sourceOffset, sourceLength, targetLength, instructionsLength, newDataLength);
                window.setData(decompressed);
                myBuffer.position(bufferPosition);
            } else {
                window = new SVNDiffWindow(sourceOffset, sourceLength, targetLength, instructionsLength, newDataLength);
                window.setData(myBuffer);
            }
            int position = myBuffer.position();
            OutputStream os = consumer.textDeltaChunk(path, window);
            SVNFileUtil.closeFile(os);
            myBuffer.position(position + allDataLength);
            int remains = myBuffer.remaining();
            myIsWindowSent = true;
            
            // then clear the buffer, shift remaining to the beginning.
            myBuffer.compact();
            myBuffer.position(0);
            myBuffer.limit(remains);
        }
    }
    
    private int deflate(int compressedLength, OutputStream out) throws IOException {
        int originalPosition = myBuffer.position();
        int uncompressedLength = readOffset();
        // substract offset length from the total length.
        if (uncompressedLength == (compressedLength - (myBuffer.position() - originalPosition))) {
            int offset = myBuffer.arrayOffset() + myBuffer.position();
            out.write(myBuffer.array(), offset, uncompressedLength);
        } else {
            byte[] uncompressedData = new byte[uncompressedLength];
            byte[] compressed = myBuffer.array();
            int offset = myBuffer.arrayOffset() + myBuffer.position();
            InputStream in = new InflaterInputStream(new ByteArrayInputStream(compressed, offset, compressedLength));
            int read = 0;
            while(read < uncompressedLength) {
                int r = in.read(uncompressedData, read, uncompressedLength - read);
                if (r < 0) {
                    break;
                }
                read += r;
            }
            out.write(uncompressedData);
        }
        myBuffer.position(originalPosition + compressedLength);
        return uncompressedLength;
    }

    private void appendToBuffer(byte[] data, int offset, int length) {
        int limit = myBuffer.limit(); // amount of pending data?
        if (myBuffer.capacity() < limit + length) {
            ByteBuffer newBuffer = ByteBuffer.allocate((limit + length)*3/2);
            myBuffer.position(0);
            newBuffer.put(myBuffer);
            myBuffer = newBuffer;
        } else {
            myBuffer.limit(limit + length);
            myBuffer.position(limit);
        }
        myBuffer.put(data, offset, length);
        myBuffer.position(0);
        myBuffer.limit(limit + length);
    }
    
    private int readOffset() {
        myBuffer.mark();
        int offset = 0;
        byte b;
        while(myBuffer.hasRemaining()) {
            b = myBuffer.get();
            offset = (offset << 7) | (b & 0x7F);
            if ((b & 0x80) != 0) {
                continue;
            }
            return offset;
        }
        myBuffer.reset();
        return -1;
    }

    private long readLongOffset() {
        myBuffer.mark();
        long offset = 0;
        byte b;
        while(myBuffer.hasRemaining()) {
            b = myBuffer.get();
            offset = (offset << 7) | (b & 0x7F);
            if ((b & 0x80) != 0) {
                continue;
            }
            return offset;
        }
        myBuffer.reset();
        return -1;
    }
    
    
}
