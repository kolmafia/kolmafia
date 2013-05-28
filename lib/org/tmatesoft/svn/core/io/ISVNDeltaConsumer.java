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
package org.tmatesoft.svn.core.io;

import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;


/**
 * The <b>ISVNDeltaConsumer</b> interface is implemented by receivers 
 * of diff windows. For example, such consumers are passed to a 
 * {@link org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator delta
 * generator} when generating a series of diff windows from sources (text/binary streams). 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public interface ISVNDeltaConsumer {
    /**
     * Starts applying text delta(s) to an opened file. 
     *  
     * @param  path             a file path relative to the edit root       
     *                          directory                   
     * @param  baseChecksum     an MD5 checksum for the base file contents (before the
     *                          file is changed) 
     * @throws SVNException     if the calculated base file checksum didn't match the expected 
     *                          <code>baseChecksum</code> 
     */
    public void applyTextDelta(String path, String baseChecksum) throws SVNException;
    
    /**
     * Collects a next delta chunk. 
     * The return type is nomore relevant and is left only for backward compatibility. 
     * So, the return value may be just <span class="javakeyword">null</span>. Otherwise 
     * if it's not <span class="javakeyword">null</span>, the stream 
     * will be immediately closed. 
     * 
     * <p>
     * If there are more than one windows for the file,
     * this method is called several times.
     * 
     * @param  path           a file path relative to the edit root       
     *                        directory
     * @param  diffWindow     a next diff window
     * @return                an output stream
     * @throws SVNException
     */
    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException;
    
    /**
     * Finalizes collecting text delta(s).  
     * 
     * @param  path           a file path relative to the edit root       
     *                        directory
     * @throws SVNException
     */
    public void textDeltaEnd(String path) throws SVNException;

}
