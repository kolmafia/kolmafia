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


/**
 * The <b>ISVNChangelistHandler</b> is an interface for handlers used in 
 * changelist retrieval methods of {@link SVNChangelistClient}. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public interface ISVNChangelistHandler {

    /**
     * Handles the next path belonging to <code>changelistName</code>.
     * <p/>
     * On each invocation, <code>path</code> is a newly discovered member of the changelist named 
     * <code>changelistName</code>.
     * 
     * @param path                working copy path 
     * @param changelistName      changelist name
     */
    public void handle(File path, String changelistName);

}
