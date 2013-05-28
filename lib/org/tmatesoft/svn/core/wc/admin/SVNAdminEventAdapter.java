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

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNEvent;


/**
 * <b>SVNAdminEventAdapter</b> is an adapter class for {@link ISVNAdminEventHandler}.
 * Users's event handler implementations should extend this adapter class rather than implementing 
 * {@link ISVNAdminEventHandler} directly. This way, if the {@link ISVNAdminEventHandler} interface is changed  
 * in future, users' event handler implementations won't get broken since the changes will be reflected in 
 * this adapter class. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNAdminEventAdapter implements ISVNAdminEventHandler {

    /**
     * Does nothing. To be overridden by a user's implementation.
     * 
     * @param event 
     * @param progress 
     * @throws SVNException 
     */
    public void handleAdminEvent(SVNAdminEvent event, double progress) throws SVNException {
    }

    /**
     * Does nothing. To be overridden by a user's implementation.
     * 
     * @throws SVNCancelException 
     */
    public void checkCancelled() throws SVNCancelException {
    }

    /**
     * Does nothing. To be overridden by a user's implementation.
     * 
     * @param event 
     * @param progress 
     * @throws SVNException 
     */
    public void handleEvent(SVNEvent event, double progress) throws SVNException {
    }

}
