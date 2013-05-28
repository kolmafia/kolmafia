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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;

/**
 * The <b>SVNDiffWindowApplyBaton</b> class is used to provide the source 
 * and target streams during applying diff windows. Also an instance of 
 * <b>SVNDiffWindowApplyBaton</b> may be supplied with an MD5 digest object
 * for on-the-fly updating it with the bytes of the target view. So that when
 * a diff window's instructions are applied, the digest will be the checksum
 * for the full expanded text written to the target stream during delta application. 
 *  
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNDiffWindowApplyBaton {

    InputStream mySourceStream;
    OutputStream myTargetStream;

    long mySourceViewOffset;
    int mySourceViewLength;
    int myTargetViewSize;

    byte[] mySourceBuffer;
    byte[] myTargetBuffer;
    MessageDigest myDigest;
    
    /**
     * Creates a diff window apply baton whith source and target streams 
     * represented by files. 
     * 
     * @param  source           a source file (from where the source views would
     *                          be taken) 
     * @param  target           a target file where the full text is written
     * @param  digest           an MD5 checksum for the full text that would be
     *                          updated after each instruction applying 
     * @return                  a new <b>SVNDiffWindowApplyBaton</b> object
     * @throws SVNException
     */
    public static SVNDiffWindowApplyBaton create(File source, File target, MessageDigest digest) throws SVNException {
        SVNDiffWindowApplyBaton baton = new SVNDiffWindowApplyBaton();
        baton.mySourceStream = source.exists() ? SVNFileUtil.openFileForReading(source) : SVNFileUtil.DUMMY_IN;
        baton.myTargetStream = SVNFileUtil.openFileForWriting(target, true);
        baton.mySourceBuffer = new byte[0];
        baton.mySourceViewLength = 0;
        baton.mySourceViewOffset = 0;
        baton.myDigest = digest;
        return baton;
    }

    /**
     * Creates a diff window apply baton whith initial source and target streams. 
     * 
     * @param  source           a source input stream (from where the source 
     *                          views would be taken) 
     * @param  target           a target output stream where the full text is written
     * @param  digest           an MD5 checksum for the full text that would be
     *                          updated after each instruction applying 
     * @return                  a new <b>SVNDiffWindowApplyBaton</b> object
     */
    public static SVNDiffWindowApplyBaton create(InputStream source, OutputStream target, MessageDigest digest) {
        SVNDiffWindowApplyBaton baton = new SVNDiffWindowApplyBaton();
        baton.mySourceStream = source;
        baton.myTargetStream = target;
        baton.mySourceBuffer = new byte[0];
        baton.mySourceViewLength = 0;
        baton.mySourceViewOffset = 0;
        baton.myDigest = digest;
        return baton;
    }

    private SVNDiffWindowApplyBaton() {
    }
    
    /**
     * Closes the source and target streams, finalizes 
     * the checksum computation and returns it in a hex representation.
     * 
     * @return an MD5 checksum in a hex representation.
     */
    public String close() {
        SVNFileUtil.closeFile(mySourceStream);
        mySourceStream = null;
        SVNFileUtil.closeFile(myTargetStream);
        myTargetStream = null;
        if (myDigest != null) {
            MessageDigest d = myDigest;
            myDigest = null;
            return SVNFileUtil.toHexDigest(d);
        }
        return null;
    }

}
