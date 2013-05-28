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
package org.tmatesoft.svn.core.wc.admin;

import org.tmatesoft.svn.core.SVNException;


/**
 * The <b>ISVNChangeEntryHandler</b> is the interface for handling changed paths 
 * in <b>SVNLookClient</b>'s <code>doGetChanged()</code> methods.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public interface ISVNChangeEntryHandler {
     /**
      * Handles information on a changed path.
      * 
      * @param  entry            an object containing details of a path change
      * @throws SVNException
      */
    public void handleEntry(SVNChangeEntry entry) throws SVNException;
    
}
