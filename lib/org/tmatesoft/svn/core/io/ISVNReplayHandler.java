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

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;


/**
 * The <b>ISVNReplayHandler</b> is used in {@link SVNRepository#replayRange(long, long, long, boolean, ISVNReplayHandler)}
 * to provide and editor for replaying a revision. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public interface ISVNReplayHandler {

    /**
     * Handles the start of replaying a next revision and returns an editor through which the revision 
     * will be replayed. 
     * 
     * @param  revision               target revision number of the received replay report
     * @param  revisionProperties     contains key/value pairs for each revision properties for this 
     *                                <code>revision</code>
     * @return                        editor for replicating <code>revision</code>
     * @throws SVNException 
     */
    public ISVNEditor handleStartRevision(long revision, SVNProperties revisionProperties) throws SVNException;

    /**
     * Handles the end of replaying a next revision. In this method the implementor should close the 
     * <code>editor</code>.
     * 
     * @param  revision               target revision number of the received replay report
     * @param  revisionProperties     contains key/value pairs for each revision properties for this 
     *                                <code>revision</code>
     * @param  editor                 replication editor   
     * @throws SVNException 
     */
    public void handleEndRevision(long revision, SVNProperties revisionProperties, ISVNEditor editor) throws SVNException;
}
