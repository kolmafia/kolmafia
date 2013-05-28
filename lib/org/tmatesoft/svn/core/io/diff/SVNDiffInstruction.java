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

import java.nio.ByteBuffer;


/**
 * The <b>SVNDiffInstruction</b> class represents instructions used as delta 
 * applying rules. 
 * 
 * <p>
 * For now there are three types of copy instructions:
 * <ul>
 * <li>
 * {@link SVNDiffInstruction#COPY_FROM_SOURCE}: that is when bytes are copied from
 * a source view (for example, existing revision of a file) to the target 
 * one.    
 * </li>
 * <li>
 * {@link SVNDiffInstruction#COPY_FROM_NEW_DATA}: new data bytes (e.g. new 
 * text) are copied to the target view.
 * </li>
 * <li>
 * {@link SVNDiffInstruction#COPY_FROM_TARGET}: that is, when a sequence of bytes in the 
 * target must be repeated.
 * </li>
 * </ul>
 * 
 * These are three different ways how full text representation bytes are 
 * obtained. 
 *  
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNDiffInstruction {
    /**
     * A type of an instruction that says that data must be copied
     * from the source view to the target one.
     */
    public static final int COPY_FROM_SOURCE = 0x00;
    
    /**
     * A type of an instruction that says that data must be copied
     * from the target view to the target itself.  
     */
    public static final int COPY_FROM_TARGET = 0x01;
    
    /**
     * A type of an instruction that says that data must be copied
     * from the new data to the target view.  
     */
    public static final int COPY_FROM_NEW_DATA = 0x02;
    
    /**
     * Creates a particular type of a diff instruction.
     * Instruction offsets are relative to the bounds of views, i.e.
     * a source/target view is a window of bytes (specified in a concrete 
     * diff window) in the source/target stream (this can be a file, a buffer).
     * 
     * @param t  a type of an instruction
     * @param l  a number of bytes to copy
     * @param o  an offset in the source (which may be a source or a target
     *           view, or a new data stream) from where
     *           the bytes are to be copied
     * @see      SVNDiffWindow
     */
    public SVNDiffInstruction(int t, int l, int o) {
        type = t;
        length = l;
        offset = o;        
    }
    
    /**
     * Creates a new instruction object.
     * It's the instruction for the empty contents file.
     *
     */
    public SVNDiffInstruction() {
        this(0,0,0);
        
    }
    
    /**
     * A type of this instruction.
     */
    public int type; 
    
    /**
     * A length bytes to copy.    
     */
    public int length;
    
    /**
     * An offset in the source from where the bytes
     * should be copied. Instruction offsets are relative to the bounds of 
     * views, i.e. a source/target view is a window of bytes (specified in a concrete 
     * diff window) in the source/target stream (this can be a file, a buffer).
     *  
     */
    public int offset;
    
    /**
     * Gives a string representation of this object.
     * 
     * @return a string representation of this object
     */
    public String toString() {
        StringBuffer b = new StringBuffer();
        switch(type) {
            case COPY_FROM_SOURCE:
                b.append("S->");
                break;
            case COPY_FROM_TARGET:
                b.append("T->");
                break;
            case COPY_FROM_NEW_DATA:
                b.append("D->");
                break;
        }
        if (type == 0 || type == 1) {
            b.append(offset);
        } else {
            b.append(offset);
        }
        b.append(":");
        b.append(length);
        return b.toString();
    }
    
    /**
     * Wirtes this instruction to a byte buffer.
     * 
     * @param target a byte buffer to write to
     */
    public void writeTo(ByteBuffer target) {
        byte first = (byte) (type << 6);
        if (length <= 0x3f && length > 0) {
            // single-byte lenght;
            first |= (length & 0x3f);
            target.put((byte) (first & 0xff));
        } else {
            target.put((byte) (first & 0xff));
            writeInt(target, length);
        }
        if (type == 0 || type == 1) {
            writeInt(target, offset);
        }
    }
    
    /**
     * Writes an integer to a byte buffer.
     * 
     * @param os a byte buffer to write to 
     * @param i  an integer to write 
     */
    public static void writeInt(ByteBuffer os, int i) {
        if (i == 0) {
            os.put((byte) 0);
            return;
        }
        int count = 1;
        long v = i >> 7;
        while(v > 0) {
            v = v >> 7;
            count++;
        }
        byte b;
        int r;
        while(--count >= 0) {
            b = (byte) ((count > 0 ? 0x1 : 0x0) << 7);
            r = ((byte) ((i >> (7 * count)) & 0x7f)) | b;
            os.put((byte) r);
        }
    }

    /**
     * Writes a long to a byte buffer. 
     * 
     * @param os a byte buffer to write to 
     * @param i  a long number to write 
     */
    public static void writeLong(ByteBuffer os, long i) {
        if (i == 0) {
            os.put((byte) 0);
            return;
        }
        // how many bytes there are:
        int count = 1;
        long v = i >> 7;
        while(v > 0) {
            v = v >> 7;
            count++;
        }
        byte b;
        int r;
        while(--count >= 0) {
            b = (byte) ((count > 0 ? 0x1 : 0x0) << 7);
            r = ((byte) ((i >> (7 * count)) & 0x7f)) | b;
            os.put((byte) r);
        }
    }
}
