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

import org.tmatesoft.svn.core.SVNException;

/**
 * This public interface should be implemented for using within 
 * {@link SVNRepository#getLocations(String, long, long[], ISVNLocationEntryHandler) 
 * SVNRepository.getLocations(String, long, long[], ISVNLocationEntryHandler)}. The
 * mentioned  method retrieves file locations for interested revisions and uses an
 * implementation of <code>ISVNLocationEntryHandler</code> to handle them. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see 	SVNLocationEntry 
 */
public interface ISVNLocationEntryHandler {
    /**
     * To be implemented for location entries handling.
     * 
     * @param  locationEntry 	a location entry
     * @see 					SVNLocationEntry
     * @throws SVNException
     */
    public void handleLocationEntry(SVNLocationEntry locationEntry) throws SVNException;

}
