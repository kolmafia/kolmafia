package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * Represents a revision range between the start revision and the end revision.
 *  
 * @author TMate Software Ltd.
 * @version 1.7
 * @see SvnMerge
 * @see SvnLog
 */
public class SvnRevisionRange extends SvnObject {
    
    private SVNRevision start;
    private SVNRevision end;
    
    /**
     * Creates revision range and initializes its fields
     * 
     * @param start start revision
     * @param end end revision
     * 
     * @return newly created range
     */
    public static SvnRevisionRange create(SVNRevision start, SVNRevision end) {
        return new SvnRevisionRange(start, end);
    }
    
    private SvnRevisionRange(SVNRevision start, SVNRevision end) {
        this.start = start == null ? SVNRevision.UNDEFINED : start;
        this.end = end == null ? SVNRevision.UNDEFINED : end;
    }
    
    /**
     * Returns <code>String</code> representation of start and end revisions, separated by colon.
     * 
     * @return <code>String</code> representation of the range
     */
    @Override
    public String toString() {
        return getStart() + ":" + getEnd();
    }

    /**
     * Returns range's start revision
     * 
     * @return start revision of the range
     */
    public SVNRevision getStart() {
        return start;
    }

    /**
     * Returns range's end revision
     * 
     * @return end revision of the range
     */
    public SVNRevision getEnd() {
        return end;
    }
}
