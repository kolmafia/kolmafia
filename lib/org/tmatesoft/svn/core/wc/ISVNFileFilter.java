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
 * <code>ISVNFileFilter</code> is used by {@link SVNCommitClient} during an import operation 
 * to filter out undesired paths, so that those paths do not get to a repository. 
 *
 * @version 1.3
 * @since   1.3
 * @author  TMate Software Ltd.
 */
public interface ISVNFileFilter {

    /**
     * This method is called to add extra filtering of files.
     * 
     * @param  file           file to accept or not
     * @return                <span class="javakeyword">true</span> if the file should be accepted
     * @throws SVNException
     */
    public boolean accept(File file) throws SVNException;
}
