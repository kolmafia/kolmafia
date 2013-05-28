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
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.delta.SVNDeltaAlgorithm;
import org.tmatesoft.svn.core.internal.delta.SVNXDeltaAlgorithm;
import org.tmatesoft.svn.core.internal.wc.IOExceptionWrapper;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.ISVNDeltaConsumer;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * The <b>SVNDeltaGenerator</b> is intended for generating diff windows of 
 * fixed size having a target version of a file against a source one. 
 * File contents are provided as two streams - source and target ones, or just 
 * target if delta is generated against empty contents. 
 * 
 * <p>
 * The generator uses X-Delta algorithm for generating all kinds of deltas.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNDeltaGenerator {
    
    private SVNDeltaAlgorithm myXDelta = new SVNXDeltaAlgorithm();
    
    private byte[] mySourceBuffer;
    private byte[] myTargetBuffer;
    private int myMaximumBufferSize;
    
    /**
     * Creates a generator that will produce diff windows of 
     * 100Kbytes contents length. That is, after applying of 
     * such a window you get 100 Kbytes of file contents.
     * 
     * @see #SVNDeltaGenerator(int)
     */
    public SVNDeltaGenerator() {
        this(1024*100);
    }
    
    /**
     * Creates a generator that will produce diff windows of 
     * a specified contents length.  
     * 
     * @param maximumDiffWindowSize a maximum size of a file contents
     *                              chunk that a single applied diff 
     *                              window would produce
     */
    public SVNDeltaGenerator(int maximumDiffWindowSize) {
        myMaximumBufferSize = maximumDiffWindowSize;
        int initialSize = Math.min(8192, myMaximumBufferSize);
        mySourceBuffer = new byte[initialSize];
        myTargetBuffer = new byte[initialSize];
    }
    
    /**
     * Generates a series of diff windows of fixed size comparing 
     * target bytes (from <code>target</code> stream) against an 
     * empty file and sends produced windows to the provided 
     * consumer. <code>consumer</code>'s {@link org.tmatesoft.svn.core.io.ISVNDeltaConsumer#textDeltaChunk(String, SVNDiffWindow) textDeltaChunk()} 
     * method is called to receive and process generated windows. 
     * Now new data comes within a window, so the output stream is either 
     * ignored (if it's <span class="javakeyword">null</span>) or immediately closed 
     * (if it's not <span class="javakeyword">null</span>).  
     * 
     * <p>
     * If <code>computeChecksum</code> is <span class="javakeyword">true</span>, 
     * the return value will be a strig containing a hex representation 
     * of the MD5 digest computed for the target contents. 
     * 
     * @param  path             a file repository path
     * @param  target           an input stream to read target bytes
     *                          from
     * @param  consumer         a diff windows consumer
     * @param  computeChecksum  <span class="javakeyword">true</span> to 
     *                          compute a checksum 
     * @return                  if <code>computeChecksum</code> is <span class="javakeyword">true</span>,  
     *                          a string representing a hex form of the 
     *                          MD5 checksum computed for the target contents; otherwise  <span class="javakeyword">null</span>
     * @throws SVNException
     */
    public String sendDelta(String path, InputStream target, ISVNDeltaConsumer consumer, boolean computeChecksum) throws SVNException {
        return sendDelta(path, SVNFileUtil.DUMMY_IN, 0, target, consumer, computeChecksum);
    }

    /**
     * Generates a series of diff windows of fixed size comparing 
     * target bytes (read from <code>target</code> stream) against source
     * bytes (read from <code>source</code> stream), and sends produced windows to the provided 
     * consumer. <code>consumer</code>'s {@link org.tmatesoft.svn.core.io.ISVNDeltaConsumer#textDeltaChunk(String, SVNDiffWindow) textDeltaChunk()} 
     * method is called to receive and process generated windows. 
     * Now new data comes within a window, so the output stream is either 
     * ignored (if it's <span class="javakeyword">null</span>) or immediately closed 
     * (if it's not <span class="javakeyword">null</span>). 
     * 
     * <p>
     * If <code>computeChecksum</code> is <span class="javakeyword">true</span>, 
     * the return value will be a strig containing a hex representation 
     * of the MD5 digest computed for the target contents. 
     * 
     * @param  path             a file repository path
     * @param  source           an input stream to read source bytes
     *                          from
     * @param  sourceOffset     an offset of the source view in the given <code>source</code> stream 
     * @param  target           an input stream to read target bytes
     *                          from
     * @param  consumer         a diff windows consumer
     * @param  computeChecksum  <span class="javakeyword">true</span> to 
     *                          compute a checksum 
     * @return                  if <code>computeChecksum</code> is <span class="javakeyword">true</span>,  
     *                          a string representing a hex form of the 
     *                          MD5 checksum computed for the target contents; otherwise  <span class="javakeyword">null</span>
     * @throws SVNException
     */
    public String sendDelta(String path, InputStream source, long sourceOffset, InputStream target, ISVNDeltaConsumer consumer, boolean computeChecksum) throws SVNException {
        MessageDigest digest = null;
        if (computeChecksum) {
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "MD5 implementation not found: {0}", e.getLocalizedMessage());
                SVNErrorManager.error(err, e, SVNLogType.DEFAULT);
                return null;
            }
        }
        boolean windowSent = false;
        while(true) {
            int targetLength;
            int sourceLength;
            try {
                targetLength = readToBuffer(target, myTargetBuffer);
            } catch (IOExceptionWrapper ioew) {
                throw ioew.getOriginalException();
            } catch (IOException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getLocalizedMessage());
                SVNErrorManager.error(err, e, SVNLogType.DEFAULT);
                return null;
            }
            if (targetLength <= 0) {
                // send empty window, needed to create empty file. 
                // only when no windows was sent at all.
                if (!windowSent && consumer != null) {
                    consumer.textDeltaChunk(path, SVNDiffWindow.EMPTY);
                }
                break;
            } 
            try {
                sourceLength = readToBuffer(source, mySourceBuffer);
            } catch (IOExceptionWrapper ioew) {
                throw ioew.getOriginalException();
            } catch (IOException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e.getLocalizedMessage());
                SVNErrorManager.error(err, e, SVNLogType.DEFAULT);
                return null;
            }
            if (sourceLength < 0) {
                sourceLength = 0;
            }
            // update digest,
            if (digest != null) {
                digest.update(myTargetBuffer, 0, targetLength);
            }
            // generate and send window
            sendDelta(path, sourceOffset, mySourceBuffer, sourceLength, myTargetBuffer, targetLength, consumer);
            windowSent = true;
            sourceOffset += sourceLength;
        }
        if (consumer != null) {
            consumer.textDeltaEnd(path);
        }
        return SVNFileUtil.toHexDigest(digest);
    }

    /**
     * Generates a series of diff windows of fixed size comparing 
     * target bytes (read from <code>target</code> stream) against an empty file, and sends produced windows to 
     * the provided consumer.  
     * 
     * <p/>
     * This is identical to <code>sendDelta(path, null, 0, 0, target, targetLength, consumer)</code>.
     * 
     * @param  path             a file repository path
     * @param  target           an input byte array to read target bytes from
     * @param  targetLength      
     * @param  consumer         a diff windows consumer
     * @throws SVNException
     */
    public void sendDelta(String path, byte[] target, int targetLength, ISVNDeltaConsumer consumer) throws SVNException {
        sendDelta(path, null, 0, 0, target, targetLength, consumer);
    }

    /**
     * Generates a series of diff windows of fixed size comparing 
     * <code>targetLength</code> of target bytes (read from <code>target</code> stream) against 
     * <code>sourceLength</code> of source bytes (read from <code>source</code> stream at offset 
     * <code>sourceOffset</code>), and sends produced windows to the provided <code>consumer</code>. 
     * 
     * <p/>
     * Size of the produced windows is set in a constructor of this delta generator.
     *  
     * <p/>
     * <code>consumer</code>'s {@link org.tmatesoft.svn.core.io.ISVNDeltaConsumer#textDeltaChunk(String, SVNDiffWindow) textDeltaChunk()} 
     * method is called to receive and process generated windows. 
     * Now new data comes within a window, so the output stream is either 
     * ignored (if it's <span class="javakeyword">null</span>) or immediately closed 
     * (if it's not <span class="javakeyword">null</span>). 
     * 
     * @param  path             a file repository path
     * @param  source           an input stream to read source bytes from
     * @param  sourceLength     the size of the source view
     * @param  sourceOffset     an offset of the source view in the given <code>source</code> stream 
     * @param  target           an input stream to read target bytes from
     * @param  targetLength     the size of the target view
     * @param  consumer         a diff windows consumer
     * @throws SVNException
     */
    public void sendDelta(String path, byte[] source, int sourceLength, long sourceOffset, byte[] target, 
            int targetLength, ISVNDeltaConsumer consumer) throws SVNException {
        if (targetLength == 0 || target == null) {
            // send empty window, needed to create empty file. 
            // only when no windows was sent at all.
            if (consumer != null) {
                consumer.textDeltaChunk(path, SVNDiffWindow.EMPTY);
            }
            return;
        } 
        if (source == null) {
            source = new byte[0];
            sourceLength = 0;
        } else if (sourceLength < 0) {
            sourceLength = 0;
        }
        // generate and send window
        sendDelta(path, sourceOffset, source == null ? new byte[0] : source, sourceLength, target, targetLength, consumer);
    }

    private void sendDelta(String path, long sourceOffset, byte[] source, int sourceLength, byte[] target, int targetLength, ISVNDeltaConsumer consumer) throws SVNException {
        // always use x algorithm, v is deprecated now.
        SVNDeltaAlgorithm algorithm = myXDelta;
        algorithm.computeDelta(source, sourceLength, target, targetLength);
        // send single diff window to the editor.
        if (consumer == null) {
            algorithm.reset();
            return;
        }
        int instructionsLength = algorithm.getInstructionsLength();
        int newDataLength = algorithm.getNewDataLength();
        SVNDiffWindow window = new SVNDiffWindow(sourceOffset, sourceLength, targetLength, instructionsLength, newDataLength);
        window.setData(algorithm.getData());
        OutputStream os = consumer.textDeltaChunk(path, window);
        SVNFileUtil.closeFile(os);
        algorithm.reset();
    }
    
    private int readToBuffer(InputStream is, byte[] buffer) throws IOException {
        int read = SVNFileUtil.readIntoBuffer(is, buffer, 0, buffer.length);
        if (read <= 0) {
            return read;
        }
        if (read == buffer.length && read < myMaximumBufferSize) {
            byte[] expanded = new byte[myMaximumBufferSize];
            System.arraycopy(buffer, 0, expanded, 0, read);
            if (buffer == myTargetBuffer) {
                myTargetBuffer = expanded;
            } else {
                mySourceBuffer = expanded;
            }
            buffer = expanded;
            
            int anotherRead = SVNFileUtil.readIntoBuffer(is, buffer, read, buffer.length - read);
            if (anotherRead <= 0) {
                return read;
            }
            read += anotherRead;
        }
        return read;
    }
}
