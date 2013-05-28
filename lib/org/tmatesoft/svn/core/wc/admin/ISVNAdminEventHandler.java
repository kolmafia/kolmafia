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
import org.tmatesoft.svn.core.wc.ISVNEventHandler;


/**
 * The <b>ISVNAdminEventHandler</b> is used in several methods of <b>SVNAdminClient</b> 
 * to notify callers of operation progress.
 * 
 * To register your <b>ISVNAdminEventHandler</b> in {@link SVNAdminClient} 
 * pass it to {@link SVNAdminClient#setEventHandler(ISVNEventHandler) SVNAdminClient.setEventHandler()}.
 * Or if you are using {@link org.tmatesoft.svn.core.wc.SVNClientManager} you may register your handler 
 * by passing it to 
 * {@link org.tmatesoft.svn.core.wc.SVNClientManager#setEventHandler(ISVNEventHandler) SVNClientManager.setEventHandler()}.
 * <b>ISVNAdminEventHandler</b> extends {@link org.tmatesoft.svn.core.wc.ISVNEventHandler}, so at the same time you 
 * may use it with other <b>SVN</b>*<b>Client</b> objects and as a cancellation editor.   
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public interface ISVNAdminEventHandler extends ISVNEventHandler {
    /**
     * Handles the current admin event. 
     * 
     * @param  event         an event to handle
     * @param  progress      progress state (reserved for future purposes)
     * @throws SVNException
     */
    public void handleAdminEvent(SVNAdminEvent event, double progress) throws SVNException;
}
