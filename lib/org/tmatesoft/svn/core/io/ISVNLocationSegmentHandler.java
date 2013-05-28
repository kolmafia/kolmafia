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


/**
 * The <b>ISVNLocationSegmentHandler</b> is an interface for location segment handlers which is used in the
 * {@link SVNRepository#getLocationSegments(String, long, long, long, ISVNLocationSegmentHandler)} method. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public interface ISVNLocationSegmentHandler {

    /**
     * Handles a next location segment.
     * 
     * @param  locationSegment    location segment      
     * @throws SVNException 
     */
    public void handleLocationSegment(SVNLocationSegment locationSegment) throws SVNException;

}
