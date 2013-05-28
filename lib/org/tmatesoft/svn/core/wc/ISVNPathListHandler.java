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
package org.tmatesoft.svn.core.wc;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;


/**
 * The <b>ISVNPathListHandler</b> is used by <b>SVN*Client</b> classes as a callback in operations performed 
 * on multiple paths.  
 * 
 * <p/>
 * Implementations of this interface can be provided to an <b>SVN*Client</b> object via a call to 
 * {@link SVNBasicClient#setPathListHandler(ISVNPathListHandler)}.
 * 
 * <p/>
 * For example, this handler is used in 
 * {@link SVNUpdateClient#doUpdate(File[], SVNRevision, org.tmatesoft.svn.core.SVNDepth, boolean, boolean)}
 * where the handler is called before updating a next working copy path from the <code>paths</code> array.     
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public interface ISVNPathListHandler {
   
    /**
     * Hadles a working copy path. 
     * 
     * @param  path           working copy path 
     * @throws SVNException 
     */
    public void handlePathListItem(File path) throws SVNException;

}
