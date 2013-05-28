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

package org.tmatesoft.svn.core.io.diff;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.zip.DeflaterOutputStream;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * The <b>SVNDiffWindow</b> class represents a diff window that
 * contains instructions and new data of a delta to apply to a file.
 * 
 * <p>
 * Instructions are not immediately contained in a window. A diff window 
 * provides an iterator that reads and constructs one <b>SVNDiffInstruction</b> 
 * from provided raw bytes per one iteration. There is even an ability to 
 * use a single <b>SVNDiffInstruction</b> object for read and decoded instructions: 
 * for subsequent iterations an iterator simply uses the same instruction object 
 * to return as a newly read and decoded instruction.      
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see     SVNDiffInstruction
 */
public class SVNDiffWindow {
    
    /**
     * Bytes of the delta header of an uncompressed diff window. 
     */
    public static final byte[] SVN_HEADER = new byte[] {'S', 'V', 'N', '\0'};

    /**
     * Bytes of the delta header of a compressed diff window.
     * @since 1.1, new in Subversion 1.4 
     */
    public static final byte[] SVN1_HEADER = new byte[] {'S', 'V', 'N', '\1'};
    
    /**
     * An empty window (in particular, its instructions length = 0). Corresponds 
     * to the case of an empty delta, so, it's passed to a delta consumer to 
     * create an empty file. 
     */
    public static final SVNDiffWindow EMPTY = new SVNDiffWindow(0,0,0,0,0);
    
    private final long mySourceViewOffset;
    private final int mySourceViewLength;
    private final int myTargetViewLength;
    private final int myNewDataLength;
    private int myInstructionsLength;
    
    private SVNDiffInstruction myTemplateInstruction = new SVNDiffInstruction(0,0,0);
    private SVNDiffInstruction myTemplateNextInstruction = new SVNDiffInstruction(0,0,0);
    
    private byte[] myData;
    private int myDataOffset;
    private int myInstructionsCount;
    
    /**
     * Constructs an <b>SVNDiffWindow</b> object. This constructor is
     * used when bytes of instructions are not decoded and converted to
     * <b>SVNDiffInstruction</b> objects yet, but are kept elsewhere 
     * along with new data.
     * 
     * @param sourceViewOffset    an offset in the source view
     * @param sourceViewLength    a number of bytes to read from the
     *                            source view
     * @param targetViewLength    a length in bytes of the target view 
     *                            it must have after copying bytes
     * @param instructionsLength  a number of instructions bytes  
     * @param newDataLength       a number of bytes of new data
     * @see                       SVNDiffInstruction
     */
    public SVNDiffWindow(long sourceViewOffset, int sourceViewLength, int targetViewLength, int instructionsLength, int newDataLength) {
        mySourceViewOffset = sourceViewOffset;
        mySourceViewLength = sourceViewLength;
        myTargetViewLength = targetViewLength;
        myInstructionsLength = instructionsLength;
        myNewDataLength = newDataLength;
    }
    
    /**
     * Returns the length of instructions in bytes. 
     * 
     * @return a number of instructions bytes
     */
    public int getInstructionsLength() {
        return myInstructionsLength;
    }
    
    /**
     * Returns the source view offset.
     * 
     * @return an offset in the source from where the source bytes
     *         must be copied
     */
    public long getSourceViewOffset() {
        return mySourceViewOffset;
    }
    
    /**
     * Returns the number of bytes to copy from the source view to the target one.
     * 
     * @return a number of source bytes to copy
     */
    public int getSourceViewLength() {
        return mySourceViewLength;
    }
    
    /**
     * Returns the length in bytes of the target view. The length of the target
     * view is actually the number of bytes that should be totally copied by all the 
     * instructions of this window.
     * 
     * @return a length in bytes of the target view
     */
    public int getTargetViewLength() {
        return myTargetViewLength;
    }
    
    /**
     * Returns the number of new data bytes to copy to the target view.
     * 
     * @return a number of new data bytes
     */
    public int getNewDataLength() {
        return myNewDataLength;
    }
    
    /**
     * Returns an iterator to read instructions in series. 
     * Objects returned by an iterator's <code>next()</code> method 
     * are separate <b>SVNDiffInstruction</b> objects.
     * 
     * <p>
     * Instructions as well as new data are read from a byte 
     * buffer that is passed to this window object via the 
     * {@link #setData(ByteBuffer) setData()} method.   
     * 
     * <p>
     * A call to this routine is equivalent to a call 
     * <code>instructions(false)</code>.
     * 
     * @return an instructions iterator
     * @see    #instructions(boolean)
     * @see    SVNDiffInstruction 
     */
    public Iterator instructions() {
        return instructions(false);
    }

    /**
     * Returns an iterator to read instructions in series. 
     * 
     * <p>
     * If <code>template</code> is <span class="javakeyword">true</span> 
     * then each instruction returned by the iterator is actually the 
     * same <b>SVNDiffInstruction</b> object, but with proper options. 
     * This prevents from allocating new memory.  
     * 
     * <p>
     * On the other hand, if <code>template</code> is <span class="javakeyword">false</span> 
     * then the iterator returns a new allocated <b>SVNDiffInstruction</b> object per 
     * each instruction read and decoded.
     * 
     * <p>
     * Instructions as well as new data are read from a byte buffer that is 
     * passed to this window object via the 
     * {@link #setData(ByteBuffer) setData()} method.   
     * 
     * @param  template  to use a single/multiple instruction objects
     * @return           an instructions iterator
     * @see              #instructions()
     * @see              SVNDiffInstruction 
     */
    public Iterator instructions(boolean template) {
        return new InstructionsIterator(template);
    }
    
    /**
     * Applies this window's instructions. The source and target streams
     * are provided by <code>applyBaton</code>. 
     * 
     * <p>
     * If this window has got any {@link SVNDiffInstruction#COPY_FROM_SOURCE} instructions, then: 
     * <ol>
     * <li>At first copies a source view from the source stream 
     *     of <code>applyBaton</code> to the baton's inner source buffer.  
     *    {@link SVNDiffInstruction#COPY_FROM_SOURCE} instructions of this window are 
     *    relative to the bounds of that source buffer (source view, in other words).
     * <li>Second, according to instructions, copies source bytes from the source buffer
     *     to the baton's target buffer (or target view, in other words). 
     * <li>Then, if <code>applyBaton</code> is supplied with an MD5 digest, updates it with those bytes
     *     in the target buffer. So, after instructions applying completes, it will be the checksum for
     *     the full text expanded.
     * <li>The last step - appends the target buffer bytes to the baton's 
     *     target stream.        
     * </ol> 
     * 
     * <p>
     * {@link SVNDiffInstruction#COPY_FROM_NEW_DATA} instructions rule to copy bytes from 
     * the instructions & new data buffer provided to this window object via a call to the 
     * {@link #setData(ByteBuffer) setData()} method.
     * 
     * <p>
     * {@link SVNDiffInstruction#COPY_FROM_TARGET} instructions are relative to the bounds of
     * the target buffer. 
     * 
     * @param  applyBaton    a baton that provides the source and target 
     *                       views as well as holds the source and targed 
     *                       streams 
     * @throws SVNException
     * @see                  #apply(byte[], byte[])
     */
    public void apply(SVNDiffWindowApplyBaton applyBaton) throws SVNException {
        // here we have streams and buffer from the previous calls (or nulls).
        
        // 1. buffer for target.
        if (applyBaton.myTargetBuffer == null || applyBaton.myTargetViewSize < getTargetViewLength()) {
            applyBaton.myTargetBuffer = new byte[getTargetViewLength()];
        }
        applyBaton.myTargetViewSize = getTargetViewLength();
        
        // 2. buffer for source.
        int length = 0;
        if (getSourceViewOffset() != applyBaton.mySourceViewOffset || getSourceViewLength() > applyBaton.mySourceViewLength) {
            byte[] oldSourceBuffer = applyBaton.mySourceBuffer;
            // create a new buffer
            applyBaton.mySourceBuffer = new byte[getSourceViewLength()];
            // copy from the old buffer.
            if (applyBaton.mySourceViewOffset + applyBaton.mySourceViewLength > getSourceViewOffset()) {
                // copy overlapping part to the new buffer
                int start = (int) (getSourceViewOffset() - applyBaton.mySourceViewOffset);
                System.arraycopy(oldSourceBuffer, start, applyBaton.mySourceBuffer, 0, (applyBaton.mySourceViewLength - start));
                length = (applyBaton.mySourceViewLength - start);
            }            
        }
        if (length < getSourceViewLength()) {
            // fill what remains.
            try {
                int toSkip = (int) (getSourceViewOffset() - (applyBaton.mySourceViewOffset + applyBaton.mySourceViewLength));
                if (toSkip > 0) {
                    applyBaton.mySourceStream.skip(toSkip);
                }
                SVNFileUtil.readIntoBuffer(applyBaton.mySourceStream, applyBaton.mySourceBuffer, length, applyBaton.mySourceBuffer.length - length);
            } catch (IOException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getLocalizedMessage());
                SVNErrorManager.error(err, e, SVNLogType.DEFAULT);
            }
        }
        // update offsets in baton.
        applyBaton.mySourceViewLength = getSourceViewLength();
        applyBaton.mySourceViewOffset = getSourceViewOffset();
        
        // apply instructions.
        int tpos = 0;
        int npos = myInstructionsLength;
        try {
            for (Iterator instructions = instructions(true); instructions.hasNext();) {
                SVNDiffInstruction instruction = (SVNDiffInstruction) instructions.next();
                int iLength = instruction.length < getTargetViewLength() - tpos ? (int) instruction.length : getTargetViewLength() - tpos; 
                switch (instruction.type) {
                    case SVNDiffInstruction.COPY_FROM_NEW_DATA:
                        System.arraycopy(myData, myDataOffset + npos, applyBaton.myTargetBuffer, tpos, iLength);
                        npos += iLength;
                        break;
                    case SVNDiffInstruction.COPY_FROM_TARGET:
                        int start = instruction.offset;
                        int end = instruction.offset + iLength;
                        int tIndex = tpos;
                        for(int j = start; j < end; j++) {
                            applyBaton.myTargetBuffer[tIndex] = applyBaton.myTargetBuffer[j];
                            tIndex++;
                        }
                        break;
                    case SVNDiffInstruction.COPY_FROM_SOURCE:
                        System.arraycopy(applyBaton.mySourceBuffer, instruction.offset, applyBaton.myTargetBuffer, tpos, iLength);
                        break;
                    default:
                }
                tpos += instruction.length;
                if (tpos >= getTargetViewLength()) {
                    break;
                }
            }
            // save tbuffer.
            if (applyBaton.myDigest != null) {
                applyBaton.myDigest.update(applyBaton.myTargetBuffer, 0, getTargetViewLength());
            }
            applyBaton.myTargetStream.write(applyBaton.myTargetBuffer, 0, getTargetViewLength());
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getLocalizedMessage());
            SVNErrorManager.error(err, e, SVNLogType.DEFAULT);
        }
    }

    /**
     * Applies this window's instructions provided source and target view buffers. 
     * 
     * <p>
     * If this window has got any {@link SVNDiffInstruction#COPY_FROM_SOURCE} instructions, then 
     * appropriate bytes described by such an instruction are copied from the <code>sourceBuffer</code> 
     * to the <code>targetBuffer</code>.
     *   
     * <p>
     * {@link SVNDiffInstruction#COPY_FROM_NEW_DATA} instructions rule to copy bytes from 
     * the instructions & new data buffer provided to this window object via a call to the 
     * {@link #setData(ByteBuffer) setData()} method.
     * 
     * <p>
     * {@link SVNDiffInstruction#COPY_FROM_TARGET} instructions are relative to the bounds of
     * the <code>targetBuffer</code> itself. 
     * 
     * @param sourceBuffer  a buffer containing a source view
     * @param targetBuffer  a buffer to get a target view
     * @return              the size of the resultant target view
     * @see                 #apply(SVNDiffWindowApplyBaton)
     */
    public int apply(byte[] sourceBuffer, byte[] targetBuffer) {
        int dataOffset = myInstructionsLength;
        int tpos = 0;
        for (Iterator instructions = instructions(true); instructions.hasNext();) {
            SVNDiffInstruction instruction = (SVNDiffInstruction) instructions.next();
            int iLength = instruction.length < getTargetViewLength() - tpos ? (int) instruction.length : getTargetViewLength() - tpos;
            switch (instruction.type) {
                case SVNDiffInstruction.COPY_FROM_NEW_DATA:
                    System.arraycopy(myData, myDataOffset + dataOffset, targetBuffer, tpos, iLength);
                    dataOffset += iLength;
                    break;
                case SVNDiffInstruction.COPY_FROM_TARGET:
                    int start = instruction.offset;
                    int end = instruction.offset + iLength;
                    int tIndex = tpos;
                    for(int j = start; j < end; j++) {
                        targetBuffer[tIndex] = targetBuffer[j];
                        tIndex++;
                    }
                    break;
                case SVNDiffInstruction.COPY_FROM_SOURCE:
                    System.arraycopy(sourceBuffer, instruction.offset, targetBuffer, tpos, iLength);
                    break;
                default:
            }
            tpos += instruction.length;
            if (tpos >= getTargetViewLength()) {
                break;
            }
        }
        return getTargetViewLength();
    }
    
    /**
     * Sets a byte buffer containing instruction and new data bytes 
     * of this window. 
     * 
     * <p>
     * Instructions will go before new data within the buffer and should start 
     * at <code>buffer.position() + buffer.arrayOffset()</code>.
     * 
     * <p>
     * Applying a diff window prior to setting instruction and new data bytes 
     * may cause a NPE.  
     * 
     * @param buffer an input data buffer
     */
    public void setData(ByteBuffer buffer) {
        myData = buffer.array();
        myDataOffset = buffer.position() + buffer.arrayOffset();
    }
    
    /**
     * Gives a string representation of this object.
     * 
     * @return a string representation of this object
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getSourceViewOffset());
        sb.append(":");
        sb.append(getSourceViewLength());
        sb.append(":");
        sb.append(getTargetViewLength());
        sb.append(":");
        sb.append(getInstructionsLength());
        sb.append(":");
        sb.append(getNewDataLength());
        sb.append(":");
        sb.append(getDataLength());
        sb.append(":");
        sb.append(myDataOffset);
        return sb.toString();
    }
    
    /**
     * Tells if this window is not empty, i.e. has got any instructions.
     * 
     * @return <span class="javakeyword">true</span> if has instructions, 
     *         <span class="javakeyword">false</span> if has not 
     */
    public boolean hasInstructions() {
        return myInstructionsLength > 0;
    }
    
    /**
     * Writes this window object to the provided stream.
     * 
     * <p>
     * If <code>writeHeader</code> is <span class="javakeyword">true</span> 
     * then writes {@link #SVN_HEADER} bytes also.
     * 
     * @param os             an output stream to write to 
     * @param writeHeader    controls whether the header should be written 
     *                       or not
     * @throws IOException   if an I/O error occurs
     */
    public void writeTo(OutputStream os, boolean writeHeader) throws IOException {
        writeTo(os, writeHeader, false);
    }
    
    /**
     * Formats and writes this window bytes to the specified output stream.
     * 
     * @param os              an output stream to write the window to
     * @param writeHeader     if <span class="javakeyword">true</span> a window
     *                        header will be also written
     * @param compress        if <span class="javakeyword">true</span> writes  
     *                        compressed window bytes using {@link #SVN1_HEADER} 
     *                        to indicate that (if <code>writeHeader</code> is 
     *                        <span class="javakeyword">true</span>), otherwise 
     *                        non-compressed window is written with {@link #SVN_HEADER} 
     *                        (again if <code>writeHeader</code> is <span class="javakeyword">true</span>) 
     * @throws IOException
     * @since                 1.1
     */
    public void writeTo(OutputStream os, boolean writeHeader, boolean compress) throws IOException {
        if (writeHeader) {
            os.write(compress ? SVN1_HEADER : SVN_HEADER);
        }
        if (!hasInstructions()) {
            return;
        }
        ByteBuffer offsets = ByteBuffer.allocate(100);
        SVNDiffInstruction.writeLong(offsets, mySourceViewOffset);
        SVNDiffInstruction.writeInt(offsets, mySourceViewLength);
        SVNDiffInstruction.writeInt(offsets, myTargetViewLength);

        ByteBuffer instructions = null;
        ByteBuffer newData = null;
        int instLength = 0;
        int dataLength = 0;
        if (compress) {
            instructions = inflate(myData, myDataOffset, myInstructionsLength);
            instLength = instructions.remaining();
            newData = inflate(myData, myDataOffset + myInstructionsLength, myNewDataLength);
            dataLength = newData.remaining();
            SVNDiffInstruction.writeInt(offsets, instLength);
            SVNDiffInstruction.writeInt(offsets, dataLength);
        } else {
            SVNDiffInstruction.writeInt(offsets, myInstructionsLength);
            SVNDiffInstruction.writeInt(offsets, myNewDataLength);
        }
        os.write(offsets.array(), offsets.arrayOffset(), offsets.position());
        if (compress) {
            os.write(instructions.array(), instructions.arrayOffset(), instructions.remaining());
            os.write(newData.array(), newData.arrayOffset(), newData.remaining());
        } else {
            os.write(myData, myDataOffset, myInstructionsLength);
            if (myNewDataLength > 0) {
                os.write(myData, myDataOffset + myInstructionsLength, myNewDataLength);
            }
        }
    }
    
    /**
     * Returns the total amount of new data and instruction bytes.
     * 
     * @return new data length + instructions length
     */
    public int getDataLength() {
        return myNewDataLength + myInstructionsLength;
    }

    /**
     * Tells whether this window contains any copy-from-source 
     * instructions. 
     * 
     * @return <span class="javakeyword">true</span> if this window 
     *         has got at least one {@link SVNDiffInstruction#COPY_FROM_SOURCE} 
     *         instruction
     */
    public boolean hasCopyFromSourceInstructions() {
        for(Iterator instrs = instructions(true); instrs.hasNext();) {
            SVNDiffInstruction instruction = (SVNDiffInstruction) instrs.next();
            if (instruction.type == SVNDiffInstruction.COPY_FROM_SOURCE) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Creates an exact copy of this window object. 
     * 
     * <p> 
     * <code>targetData</code> is written instruction & new data bytes and 
     * then is set to a new window object via a call to its {@link #setData(ByteBuffer) setData()} 
     * method.
     * 
     * @param  targetData a byte buffer to receive a copy of this wondow data
     * @return            a new window object that is an exact copy of this one
     */
    public SVNDiffWindow clone(ByteBuffer targetData) {
        int targetOffset = targetData.position() + targetData.arrayOffset();
        int position = targetData.position();
        targetData.put(myData, myDataOffset, myInstructionsLength + myNewDataLength);
        targetData.position(position);
        SVNDiffWindow clone = new SVNDiffWindow(getSourceViewOffset(), getSourceViewLength(), getTargetViewLength(), 
                getInstructionsLength(), getNewDataLength());
        clone.setData(targetData);
        clone.myDataOffset = targetOffset;
        return clone;
    }
    
    private static ByteBuffer inflate(byte[] src, int offset, int length) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(length*2 + 2);
        SVNDiffInstruction.writeInt(buffer, length);
        if (length < 512) {
            buffer.put(src, offset, length);
        } else {
            DeflaterOutputStream out = new DeflaterOutputStream(new OutputStream() {
                public void write(int b) throws IOException {
                    buffer.put((byte) (b & 0xFF));
                }
                public void write(byte[] b, int off, int len) throws IOException {
                    buffer.put(b, off, len);
                }
                public void write(byte[] b) throws IOException {
                    write(b, 0, b.length);
                }
            });
            out.write(src, offset, length);
            out.finish();
            if (buffer.position() >= length) {
                buffer.clear();
                SVNDiffInstruction.writeInt(buffer, length);
                buffer.put(src, offset, length);
            }
        }
        buffer.flip();
        return buffer;
    }
    
    private class InstructionsIterator implements Iterator {
        
        private SVNDiffInstruction myNextInsruction;
        private int myOffset;
        private int myNewDataOffset;
        private boolean myIsTemplate;
        
        public InstructionsIterator(boolean useTemplate) {
            myIsTemplate = useTemplate;
            myNextInsruction = readNextInstruction();
        }

        public boolean hasNext() {
            return myNextInsruction != null;
        }

        public Object next() {
            if (myNextInsruction == null) {
                return null;
            }
        
            if (myIsTemplate) {
                myTemplateNextInstruction.type = myNextInsruction.type;
                myTemplateNextInstruction.length = myNextInsruction.length;
                myTemplateNextInstruction.offset = myNextInsruction.offset;
                myNextInsruction = readNextInstruction();
                return myTemplateNextInstruction;
            } 
            Object next = myNextInsruction;
            myNextInsruction = readNextInstruction();
            return next;
        }

        public void remove() {
        }
        
        private SVNDiffInstruction readNextInstruction() {
            if (myData == null || myOffset >= myInstructionsLength) {
                return null;
            }
            SVNDiffInstruction instruction = myIsTemplate ? myTemplateInstruction : new SVNDiffInstruction();
            instruction.type = (myData[myDataOffset + myOffset] & 0xC0) >> 6;
            instruction.length = myData[myDataOffset + myOffset] & 0x3f;
            myOffset++;
            if (instruction.length == 0) {
                // read length from next byte                
                instruction.length = readInt();
            } 
            if (instruction.type == 0 || instruction.type == 1) {
                // read offset from next byte (no offset without length).
                instruction.offset = readInt();
            } else { 
                // set offset to offset in newdata.
                instruction.offset = myNewDataOffset;
                myNewDataOffset += instruction.length;
            }
            return instruction;
        }
        
        private int readInt() {
            int result = 0;
            while(true) {
                byte b = myData[myDataOffset + myOffset];
                result = result << 7;
                result = result | (b & 0x7f);
                if ((b & 0x80) != 0) {
                    myOffset++;
                    if (myOffset >= myInstructionsLength) {
                        return -1;
                    }
                    continue;
                }
                myOffset++;
                return result;
            }
        }
    }
    
    /**
     * Returns an array of instructions of this window.
     * 
     * <p>
     * If <code>target</code> is large enough to receive all instruction 
     * objects, then it's simply filled up to the end of instructions.
     * However if it's not, it will be expanded to receive all instructions. 
     * 
     * @param  target  an instructions receiver 
     * @return         an array  containing all instructions
     */
    public SVNDiffInstruction[] loadDiffInstructions(SVNDiffInstruction[] target) {
        int index = 0;
        for (Iterator instructions = instructions(); instructions.hasNext();) {
            if (index >= target.length) {
                SVNDiffInstruction[] newTarget = new SVNDiffInstruction[index*3/2];
                System.arraycopy(target, 0, newTarget, 0, index);
                target = newTarget;
            }
            target[index] = (SVNDiffInstruction) instructions.next();
            index++;
        }
        myInstructionsCount = index;
        return target;
    }
    
    /**
     * Returns the amount of instructions of this window object.
     * 
     * @return a total number of instructions
     */
    public int getInstructionsCount() {
        return myInstructionsCount;
    }

    /**
     * Fills a target buffer with the specified number of new data bytes 
     * of this window object taken at the specified offset.  
     * 
     * @param target a buffer to copy to
     * @param offset an offset relative to the position of the first 
     *               new data byte of this window object 
     * @param length a number of new data bytes to copy
     */
    public void writeNewData(ByteBuffer target, int offset, int length) {
        offset += myDataOffset + myInstructionsLength;
        target.put(myData, offset, length);
    }

}
