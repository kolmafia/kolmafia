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

import java.nio.ByteBuffer;

import org.tmatesoft.svn.core.io.diff.SVNDiffInstruction;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public abstract class SVNDeltaAlgorithm {

    private ByteBuffer myNewData;
    private ByteBuffer myData;
    private int myNewDataLength;
    private int myInstructionsLength;
    private SVNDiffInstruction myTemplateInstruction;
    
    public SVNDeltaAlgorithm() {
        myNewData = ByteBuffer.allocate(1024);
        myData = ByteBuffer.allocate(2048);
        myTemplateInstruction = new SVNDiffInstruction(0,0,0);
    }
    
    public void reset() {
        myNewData.clear();
        myData.clear();
        myInstructionsLength = 0;
        myNewDataLength = 0;
    }

    public abstract void computeDelta(byte[] a, int aLength, byte[] b, int bLength);
    
    public ByteBuffer getData() {
        if (myNewData.position() > 0) {
            myData = ensureBufferSize(myData, myNewData.position());
            myData.put(myNewData.array(), 0, myNewData.position());
            myNewData.clear();
        }
        myData.flip();
        return myData;
    }

    public int getInstructionsLength() {
        return myInstructionsLength;
    }

    public int getNewDataLength() {
        return myNewDataLength;
    }

    protected void copyFromSource(int position, int length) {
        myTemplateInstruction.type = SVNDiffInstruction.COPY_FROM_SOURCE;
        myTemplateInstruction.offset = position;
        myTemplateInstruction.length = length;
        myData = ensureBufferSize(myData, 10);
        myTemplateInstruction.writeTo(myData);
        myInstructionsLength = myData.position();
    }

    protected void copyFromTarget(int position, int length) {
        myTemplateInstruction.type = SVNDiffInstruction.COPY_FROM_TARGET;
        myTemplateInstruction.offset = position;
        myTemplateInstruction.length = length;
        myData = ensureBufferSize(myData, 10);
        myTemplateInstruction.writeTo(myData);
        myInstructionsLength = myData.position();
    }

    protected void copyFromNewData(byte[] data, int offset, int length) {
        myTemplateInstruction.type = SVNDiffInstruction.COPY_FROM_NEW_DATA;
        myTemplateInstruction.offset = 0;
        myTemplateInstruction.length = length;
        myData = ensureBufferSize(myData, 10);
        myTemplateInstruction.writeTo(myData);
        myInstructionsLength = myData.position();
        myNewData = ensureBufferSize(myNewData, length);
        myNewData.put(data, offset, length);
        myNewDataLength += length;
    }
    
    private static ByteBuffer ensureBufferSize(ByteBuffer buffer, int size) {
        if (buffer.remaining() < size) {
            ByteBuffer newBuffer = ByteBuffer.allocate((buffer.position() + size)*3/2);
            newBuffer.put(buffer.array(), 0, buffer.position());
            buffer = newBuffer;
        }
        return buffer;
    }
}
