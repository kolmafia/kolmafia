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


/**
 * The <b>SVNLocationSegment</b> is a representation of a segment of an object's version history with an 
 * emphasis on the object's location in the repository as of various revisions.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNLocationSegment {
    private long myStartRevision;
    private long myEndRevision;
    private String myPath;

    /**
     * Creates a new <code>SVNLocationSegment</code> object.
     * 
     * <p/>
     * <code>path</code> may be <span class="javakeyword">null</span> to indicate 
     * gaps in an object's history.
     * 
     * @param startRevision   revision start of the location segment 
     * @param endRevision     revision end of the location segment
     * @param path            absolute (with leading slash) path for this segment
     */
    public SVNLocationSegment(long startRevision, long endRevision, String path) {
        myStartRevision = startRevision;
        myEndRevision = endRevision;
        myPath = path;
    }

    /**
     * Returns the absolute repository path.
     * 
     * <p/>
     * This may be <span class="javakeyword">null</span> to indicate 
     * gaps in an object's history.
     * 
     * @return  absolute (with leading slash) path for this segment
     */
    public String getPath() {
        return myPath;
    }

    /**
     * Returns the beginning (oldest) revision of this segment.
     * 
     * @return  beginning revision of the segment
     */
    public long getStartRevision() {
        return myStartRevision;
    }

    /**
     * Returns the ending (youngest) revision of this segment.
     * 
     * @return  ending revision of the segment
     */
    public long getEndRevision() {
        return myEndRevision;
    }

    /**
     * Sets the start revision of the segment.
     * 
     * <p/>
     * Note: this method is not intended for API users.
     * 
     * @param startRevision start segment revision
     */
    public void setStartRevision(long startRevision) {
        myStartRevision = startRevision;
    }

    /**
     * Sets the end revision of the segment.
     * 
     * <p/>
     * Note: this method is not intended for API users.
     * 
     * @param endRevision end segment revision
     */
    public void setEndRevision(long endRevision) {
        myEndRevision = endRevision;
    }
}
