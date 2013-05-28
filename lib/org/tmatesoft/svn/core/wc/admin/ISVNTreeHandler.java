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
 * The <b>ISVNTreeHandler</b> is used in traversing tree 
 * operations of <b>SVNLookClient</b>.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public interface ISVNTreeHandler {
    /**
     * Handles path information.
     * 
     * @param  path          repository path information
     * @throws SVNException
     */
    public void handlePath(SVNAdminPath path) throws SVNException;
}
