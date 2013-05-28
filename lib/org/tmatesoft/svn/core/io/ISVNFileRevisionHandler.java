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
 * The <b>ISVNFileRevisionHandler</b> interface should be implemented for handling
 * information about file revisions  - that is file path, properties, revision properties
 * against a particular revision.
 * 
 * <p>
 * This interface is provided to a   
 * {@link SVNRepository#getFileRevisions(String, long, long, ISVNFileRevisionHandler) getFileRevisions()}
 * method of <b>SVNRepository</b> when getting file revisions (in particular, when annotating).
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see 	SVNRepository
 * @see     org.tmatesoft.svn.core.SVNAnnotationGenerator
 */
public interface ISVNFileRevisionHandler extends ISVNDeltaConsumer {
    
    /**
     * Handles a file revision info.
     *  
     * @param  fileRevision 	a <b>SVNFileRevision</b> object representing file
     * 							revision information
     * @throws SVNException
     * @see 					SVNFileRevision
     */
	public void openRevision(SVNFileRevision fileRevision) throws SVNException;
	
    /**
     * Performs final handling for the processed file revision (when all 
     * deltas are applied and fulltext is got). 
     * 
     * @param  token         a file token (name or path)
     * @throws SVNException
     */
    public void closeRevision(String token) throws SVNException;

}

