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

/**
 * The <b>SVNRevisionRange</b> class represents a revision range between a start revision and an end revision.
 * Revision range objects are passed to the pegged versions of the {@link SVNDiffClient}'s <code>doMerge()</code> 
 * method to specify ranges of the source which must be merged into the target. Read more, for example, 
 * in the description for 
 * {@link SVNDiffClient#doMerge(java.io.File, SVNRevision, java.util.Collection, java.io.File, org.tmatesoft.svn.core.SVNDepth, boolean, boolean, boolean, boolean)}.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNRevisionRange {
	private SVNRevision myStartRevision;
	private SVNRevision myEndRevision;
	
	/**
	 * Constructs a new revision range object given start and end revisions.
	 * 
	 * @param startRevision start of the range 
	 * @param endRevision   end of the range
	 */
	public SVNRevisionRange(SVNRevision startRevision, SVNRevision endRevision) {
		myStartRevision = startRevision;
		myEndRevision = endRevision;
	}

	/**
	 * Returns the start revision of this range.
	 * @return start revision 
	 */
	public SVNRevision getStartRevision() {
		return myStartRevision;
	}

	/**
	 * Returns the end revision of this range.
	 * @return end revision
	 */
	public SVNRevision getEndRevision() {
		return myEndRevision;
	}
	
}
