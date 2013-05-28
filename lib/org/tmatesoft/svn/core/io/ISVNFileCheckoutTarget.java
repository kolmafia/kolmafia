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
package org.tmatesoft.svn.core.io;

import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;


/**
 * The <b>ISVNFileCheckoutTarget</b> interface is used in the {@link SVNRepository#checkoutFiles(long, String[], ISVNFileCheckoutTarget)}
 * method to receive versioned file data from the repository.
 * 
 * @version 1.3
 * @author  TMate Software Ltd. 
 * @since   1.2
 */
public interface ISVNFileCheckoutTarget {
    /**
     * Returns an output stream that will receive file contents of <code>path</code>.
     * 
     * @param  path             file path relative to the location of an {@link SVNRepository} object
     * @return                  output stream to receive file contents
     * @throws SVNException 
     */
    public OutputStream getOutputStream(String path) throws SVNException;

    /**
     * Receives and handles a next file property. Since this handler is used in a checkout-kind operations only,
     * the <code>value</code> can never be <span class="javakeyword">null</span>.  
     *  
     * @param  path             file path relative to the location of an {@link SVNRepository} object
     * @param  name             property name 
     * @param  value            property value
     * @throws SVNException 
     */
    public void filePropertyChanged(String path, String name, SVNPropertyValue value) throws SVNException;

}
