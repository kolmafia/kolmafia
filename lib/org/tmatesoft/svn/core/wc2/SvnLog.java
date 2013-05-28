package org.tmatesoft.svn.core.wc2;

import java.util.ArrayList;
import java.util.Collection;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNRevisionRange;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * Represents log operation.
 * Gets commit log messages with other revision specific information for <code>target</code>' paths(<code>targetPaths</code>) 
 * from a repository and returns them as list of {@link SVNLogEntry} items. 
 * Useful for observing the history of affected paths, author, date and log comments information
 * per revision.
 * <code>Target</code> can represent one repository URL with list of relative paths - <code>targetPaths</code>
 * or many targets representing working copy paths can be provided, and <code>targetPaths</code> should not be set.
 * Operation finds root URL of the working copy paths, and calculates relative<code>targetPaths</code> for them.
 * 
 * Operation creates {@link SVNLogEntry} item on each log message from
 * <code>startRevision</code> to <code>endRevision</code> in turn, inclusive
 * (but never creates item on a given log message more than once).
 * 
 * <p/>
 * Log entries are created only on messages whose revisions involved
 * a change to some path in <code>targetPaths</code>. <code>Targets</code>' <code>pegRevisions</code>
 * indicates in which revision <code>targetPaths</code>' paths are valid. If <code>target</code>'s
 * <code>pegRevision</code> is {@link SVNRevision#isValid() invalid}, it
 * defaults to {@link SVNRevision#WORKING} if <code>target</code> is working copy path, or 
 * {@link SVNRevision#HEAD} if <code>target</code> is URL.
 * 
 * <p/>
 * If <code>limit</code> is non-zero, only creates first <code>limit</code> log entries.
 * 
 * <p/>
 * If <code>discoverChangedPaths</code> is set, then the changed paths
 * <code>Map</code> argument will be passed to a constructor of
 * {@link SVNLogEntry} on each invocation of <code>handler</code>.
 * 
 * <p/>
 * If <code>stopOnCopy</code> is set, copy history (if any exists) will not
 * be traversed while harvesting revision logs for each <code>targetPath</code>.
 * 
 * <p/>
 * If <code>useMergedHistory</code> is set, log information for
 * revisions which have been merged to <code>targetPaths</code> will also be
 * returned.
 * 
 * <p/>
 * Refer to {@link org.tmatesoft.svn.core.SVNLogEntry#hasChildren()} for
 * additional information on how to handle mergeinfo information during a
 * log operation.
 *  
 * <p/>
 * If
 * <code>revisionProperties</code> is <code>null</code>, retrieves all revision properties; 
 * else, retrieves only the revision properties named in the array (i.e. retrieves none if the array is empty).
 *       
 * <p/>
 * For every {@link SvnRevisionRange} in <code>revisionRanges</code>: <b/>
 * If <code>startRevision</code> is {@link SVNRevision#isValid() valid} but
 * <code>endRevision</code> is not, then <code>endRevision</code> defaults
 * to <code>startRevision</code>. If both <code>startRevision</code> and
 * <code>endRevision</code> are invalid, then <code>endRevision</code>
 * defaults to revision <code>0</code>, and <code>startRevision</code>
 * defaults either to <code>target</code>'s <code>pegRevision</code> in case the latter one is
 * valid, or to {@link SVNRevision#BASE}, if it is not.
 * 
 * <p/>
 * Important: to avoid an exception with the
 * {@link SVNErrorCode#FS_NO_SUCH_REVISION} error code when invoked against
 * an empty repository (i.e. one not containing a revision 1), callers
 * should specify the range {@link SVNRevision#HEAD}:<code>0</code>.
 * 
 * <p/>
 * If the caller has provided a non-<code>null</code>
 * {@link ISVNEventHandler}, it will be called with the
 * {@link SVNEventAction#SKIP} event action on any unversioned paths.
 * 
 * <p/>
 * Note: this routine requires repository access.
 * 
 * {@link #run()} method throws {@link SVNException} in if one of the following is true:
 *             <ul>
 *             <li>a path is not under version control 
 *             <li>can not obtain a URL of a working copy path
 *             <li><code>paths</code> contain entries that belong to different repositories
 *             </ul>
 * @see SVNLogEntry
 */
public class SvnLog extends SvnReceivingOperation<SVNLogEntry> {
    
    private long limit;
    private boolean useMergeHistory;
    private boolean stopOnCopy;
    private boolean discoverChangedPaths;
    private String[] targetPaths;
    private String[] revisionProperties;
    
    
    private Collection<SvnRevisionRange> revisionRanges;
    
    protected SvnLog(SvnOperationFactory factory) {
        super(factory);
    }

    /**
     * Returns a maximum number of log entries to be processed
     * 
     * @return maximum number of entries
     */
    public long getLimit() {
        return limit;
    }

    /**
     * Sets a maximum number of log entries to be processed
     * 
     * @param limit maximum number of entries
     */
    public void setLimit(long limit) {
        this.limit = limit;
    }

    /**
     * Returns whether the log information for revisions which have 
     * been merged to <code>targetPaths</code> will also be returned.
     * 
     * @return <code>true</code> if merged revisions should be also reported, otherwise <code>false</code>
     */
    public boolean isUseMergeHistory() {
        return useMergeHistory;
    }

    /**
     * Sets whether the log information for revisions which have 
     * been merged to <code>targetPaths</code> will also be returned.
     * 
     * @param useMergeHistory <code>true</code> if merged revisions should be also reported, otherwise <code>false</code>
     */
    public void setUseMergeHistory(boolean useMergeHistory) {
        this.useMergeHistory = useMergeHistory;
    }
    
    /**
     * Returns whether to report of all changed paths for every revision being processed
     * If <code>true</code> then the changed paths <code>Map</code> argument will be passed to a constructor of
     * {@link SVNLogEntry}.
     * 
     * @return <code>true</code> if all changed paths for every revision being processed should be reported, otherwise <code>false</code>
     */
    public boolean isDiscoverChangedPaths() {
        return discoverChangedPaths;
    }

    /**
     * Sets whether to report of all changed paths for every revision being processed
     * If <code>true</code> then the changed paths <code>Map</code> argument will be passed to a constructor of
     * {@link SVNLogEntry}.
     * 
     * @param discoverChangedPaths <code>true</code> if all changed paths for every revision being processed should be reported, otherwise <code>false</code>
     */
    public void setDiscoverChangedPaths(boolean discoverChangedPaths) {
        this.discoverChangedPaths = discoverChangedPaths;
    }

    /**
     * Returns whether to copy history (if any exists) should be traversed while harvesting revision logs for each <code>targetPath</code>.
     * 
     * @return <code>true</code> if not to cross copies while traversing history, otherwise copies history will be also included into processing
     */
    public boolean isStopOnCopy() {
        return stopOnCopy;
    }

    /**
     * Sets whether to copy history (if any exists) should be traversed while harvesting revision logs for each <code>targetPath</code>.
     * 
     * @param stopOnCopy <code>true</code> if not to cross copies while traversing history, otherwise copies history will be also included into processing
     */
    public void setStopOnCopy(boolean stopOnCopy) {
        this.stopOnCopy = stopOnCopy;
    }
    
    /**
     * Returns all revision ranges for those log should be reported.
     * 
     * @return collection of {@link SVNRevisionRange} objects
     */
    public Collection<SvnRevisionRange> getRevisionRanges()
    {
    	return revisionRanges;
    }
    
    /**
     * Sets all revision ranges for those log should be reported.
     * 
     * @param revisionRanges collection of {@link SVNRevisionRange} objects
     */
    public void setRevisionRanges(Collection<SvnRevisionRange> revisionRanges)
    {
    	this.revisionRanges = revisionRanges;
    }
    
    /**
     * Returns all relative paths what should be reported for each <code>target</code>.
     * 
     * @return relative paths of the <code>target</code>
     */
    public String[] getTargetPaths() {
		return targetPaths;
	}

    /**
     * Sets all relative paths what should be reported for each <code>target</code>.
     * 
     * @param targetPaths relative paths of the <code>target</code>
     */
	public void setTargetPaths(String[] targetPaths) {
		this.targetPaths = targetPaths;
	}
	
	/**
	 * Returns what properties should be retrieved. 
	 * If <code>revisionProperties</code> is <code>null</code>, retrieves all revision properties; 
     * else retrieves only the revision properties named in the array (i.e. retrieves none if the array is empty).
     * 
	 * @return array of names of the properties
	 */
	public String[] getRevisionProperties() {
		return revisionProperties;
	}

	/**
	 * Sets what properties should be retrieved. 
	 * If <code>revisionProperties</code> is <code>null</code>, retrieves all revision properties; 
     * else retrieves only the revision properties named in the array (i.e. retrieves none if the array is empty).
     * 
	 * @param revisionProperties array of names of the properties
	 */
	public void setRevisionProperties(String[] revisionProperties) {
		this.revisionProperties = revisionProperties;
	}
	
    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        super.ensureArgumentsAreValid();
        
        if (getLimit() > Long.MAX_VALUE) {
            setLimit(Long.MAX_VALUE);
        }
        
        if (getRevisionRanges() == null || getRevisionRanges().size() == 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_BAD_REVISION, "Missing required revision specification");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        if (hasRemoteTargets() && getTargets().size() > 1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "When specifying URL, only one target may be given.");
            SVNErrorManager.error(err, SVNLogType.CLIENT);
        }
    }

    /**
     * Adds the revision range to the operation's revision ranges.
     * 
     * @param range revision range
     */
    public void addRange(SvnRevisionRange range) {
        if (range != null) {
            if (getRevisionRanges() == null) {
                this.revisionRanges = new ArrayList<SvnRevisionRange>();
            }
            this.revisionRanges.add(range);
        }
    }
    
    @Override
    protected int getMaximumTargetsCount() {
        return Integer.MAX_VALUE;
    }

    /**
     * Gets whether the operation changes working copy
     * @return <code>true</code> if the operation changes the working copy, otherwise <code>false</code>
     */
    @Override
    public boolean isChangesWorkingCopy() {
        return false;
    }
}
