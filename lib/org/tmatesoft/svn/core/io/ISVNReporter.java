/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */

package org.tmatesoft.svn.core.io;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

/**
 * The <b>ISVNReporter</b> interface provides methods to describe
 * the state of local paths in order to get the differences in revisions
 * between those local paths and what is actually in the repository.
 * 
 * <p>
 * <b>ISVNReporter</b> objects are used by <b>ISVNReporterBaton</b>
 * implementations, provided by callers of the <b>SVNRepository</b>'s update, 
 * switch, status, diff operations.
 * 
 * <p>
 * Paths for report calls are relative to the target of the operation (that is the 
 * directory where the command was run). Report calls must be made in depth-first 
 * order: parents before children, all children of a parent before any
 * siblings of the parent.  The first report call must be a 
 * {@link #setPath(String, String, long, boolean) setPath()} with a path argument of
 * <span class="javastring">""</span> and a valid revision. If the target of the operation 
 * is locally deleted or missing, use the root path's revision. If the target of the operation is 
 * deleted or switched relative to the root path, follow up the initial 
 * {@link #setPath(String, String, long, boolean) setPath()} call with a
 * {@link #linkPath(SVNURL, String, String, long, boolean) linkPath()}
 * or {@link #deletePath(String) deletePath()} call with a path argument of 
 * <span class="javastring">""</span> to
 * indicate that. In no other case may there be two report
 * descriptions for the same path.  If the target of the operation is
 * a locally added file or directory (which previously did not exist),
 * it may be reported as having revision 0 or as having the parent
 * directory's revision.
 *
 * For more information on using reporters, please, read these on-line article: 
 * <a href="http://svnkit.com/kb/dev-guide-update-operation.html">Using ISVNReporter/ISVNEditor in update-related operations</a>
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see 	ISVNReporterBaton
 * @see 	SVNRepository
 * @see     <a href="http://svnkit.com/kb/examples/">Examples</a>
 */
public interface ISVNReporter {

	/**
	 * <p>
	 * Describes a local path as being at a particular revision.  
	 * 
     * <p>
	 * If <code>startEmpty</code> is <span class="javakeyword">true</span> and the 
     * <code>path</code> is a directory, an implementor should assume the 
     * directory has no entries or properties (used in checkouts and aborted updates).
	 * 
     * <p>
	 * A next call to this method will "override" any previous <code>setPath()</code> calls made on parent
	 * paths. The <code>path</code> is relative to the repository location specified for an 
	 * <b>SVNRepository</b> driver.
	 * 
     * @param  path				a local item's path 
     * @param  lockToken		if not <span class="javakeyword">null</span>, it is a lock token 
     *                          for the <code>path</code>
     * @param  revision 		the local item's revision number
     * @param  startEmpty 		if <span class="javakeyword">true</span> and if the <code>path</code> is a 
     * 							directory, then means there're no entries yet
     * @throws SVNException     in case the repository could not be connected
     * @deprecated              use {@link #setPath(String, String, long, SVNDepth, boolean)} instead
     */
	public void setPath(String path, String lockToken, long revision, boolean startEmpty) throws SVNException;

	/** 
	 * Describes a working copy <code>path</code> as being at a particular
	 * <code>revision</code> and having depth <code>depth</code>.
	 * 
	 * <p/>
	 * <code>revision</code> may be invalid (<code>&lt;0</code>) if (for example) <code>path</code>
	 * represents a locally-added path with no revision number, or <code>depth</code> is {@link SVNDepth#EXCLUDE}.
	 * 
	 * <p/>
	 * <code>path</code> may not be underneath a path on which <code>setPath()</code> was
	 * previously called with {@link SVNDepth#EXCLUDE} in this report.
	 * 
	 * <p/>
	 * If <code>startEmpty</code> is set and <code>path</code> is a directory, this will mean that
	 * the directory has no entries or properties.
	 * 
	 * <p/>
	 * This will *override* any previous <code>setPath()</code> calls made on parent
	 * paths. 
	 * 
	 * <p/>
	 * <code>path</code> is relative to the {@link SVNRepository#getLocation() location} of the repository access 
	 * object.
	 *
	 * <p/>
	 * If <code>lockToken</code> is non-<span class="javakeyword">null</span>, it is the lock token for 
	 * <code>path</code> in the local tree.
     * 
     * @param  path                         a local item's path 
     * @param  lockToken                    if not <span class="javakeyword">null</span>, it is a lock token 
     *                                      for the <code>path</code>
     * @param  revision                     the local item's revision number
     * @param  depth                        depth of <code>path</code>
     * @param  startEmpty                   if <span class="javakeyword">true</span> and if the <code>path</code> is a 
     *                                      directory, then means there're no entries yet
     * @throws SVNException                 in case the repository could not be connected
     * @since                               1.2.0, New in Subversion 1.5.0
	 */
	public void setPath(String path, String lockToken, long revision, SVNDepth depth, boolean startEmpty) throws SVNException;

	/**
     * 
     * Describes a working copy <code>path</code> as deleted or missing.
     * 
     * @param  path 			a path relative to the root of the report
     * @throws SVNException     in case the repository could not be connected
     */
    public void deletePath(String path) throws SVNException;

    /**
     * Describes a local path as being at a particular revision
     * to switch the path to a different repository location.  
     * 
     * <p/>
     * Like {@link #setPath(String, String, long, boolean) setPath()}, but differs in 
     * that the local item's <code>path</code> (relative to the root
     * of the report driver) isn't a reflection of the path in the repository, 
     * but is instead a reflection of a different repository path at a 
     * <code>revision</code>.
     * 
     * <p/>
     * If <code>startEmpty</code> is set and the <code>path</code> is a directory,
     * the implementor should assume the directory has no entries or properties.
     * 
     * @param  url 		 	a new repository location to switch to
     * @param  path 		the local item's path 
     * @param  lockToken    if not <span class="javakeyword">null</span>, it is a lock token 
     *                      for the <code>path</code>
     * @param  revision 	the local item's revision number 
     * @param  startEmpty   if <span class="javakeyword">true</span> and if the <code>path</code> is a 
     *                      directory, then means there're no entries yet
     * @throws SVNException in case the repository could not be connected
     * @deprecated          use {@link #linkPath(SVNURL, String, String, long, SVNDepth, boolean)} instead
     */
    public void linkPath(SVNURL url, String path, String lockToken, long revision, boolean startEmpty) throws SVNException;

    /** 
     * Describes a local path as being at a particular revision
     * to switch the path to a different repository location.  
     * 
     * <p/> 
     * Like {@link #setPath(String, String, long, SVNDepth, boolean)}, but differs in  
     * that the local item's <code>path</code> (relative to the root
     * of the report driver) isn't a reflection of the path in the repository, 
     * but is instead a reflection of a different repository <code>url</code> at 
     * <code>revision</code>, and has depth <code>depth</code>. 
     * 
     * <p/>
     * <code>path</code> may not be underneath a path on which {@link #setPath(String, String, long, SVNDepth, boolean)} 
     * was previously called with {@link SVNDepth#EXCLUDE} in this report.
     *
     * If <code>startEmpty</code> is set and <code>path</code> is a directory, that will mean that 
     * the directory has no entries or props.
     * 
     * <p/>
     * If <code>lockToken</code> is non-<span class="javakeyword">null</span>, it is the lock token for 
     * <code>path</code> in the local tree.
     *
     * @param  url          a new repository location to switch to
     * @param  path         the local item's path 
     * @param  lockToken    if not <span class="javakeyword">null</span>, it is a lock token 
     *                      for the <code>path</code>
     * @param  revision     the local item's revision number 
     * @param  depth        depth of <code>path</code>
     * @param  startEmpty   if <span class="javakeyword">true</span> and if the <code>path</code> is a 
     *                      directory, then means there're no entries yet
     * @throws SVNException in case the repository could not be connected
     * @since               1.2.0, New in Subversion 1.5.0
     */
    public void linkPath(SVNURL url, String path, String lockToken, long revision, SVNDepth depth, boolean startEmpty) throws SVNException;
    
    /**
     * Finalizes the report. Must be called when having traversed a local 
     * tree of paths.
     * 
     * <p>
     * Any directories or files not explicitly set (described) 
     * are assumed to be at the baseline revision. 
     * 
     * @throws SVNException                 in case the repository could not be connected
     */
    public void finishReport() throws SVNException;
    
    /**
     * Aborts the current running report due to errors occured.
     * 
     * <p>
     * If an error occurs during a report, call this method
     * to abort the reporter correctly. 
     * 
     * @throws SVNException     in case the repository could not be connected
     */
    public void abortReport() throws SVNException;
}
