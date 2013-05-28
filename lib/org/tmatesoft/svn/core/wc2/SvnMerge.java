package org.tmatesoft.svn.core.wc2;

import java.util.ArrayList;
import java.util.Collection;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNDiffOptions;
import org.tmatesoft.svn.core.wc.SVNRevision;


/**
 * Represents merge operation.
 * There are three possible cases of merge operation.
 * If revision ranges (<code>ranges</code>) are provided, merges the changes 
 * between <code>source</code> in its <code>pegRevision</code>, 
 * as it changed between the <code>ranges</code> in to 
 * the working copy path defined in operation's <code>target</code>.
 * If revision ranges are not provided, merges changes from <code>firstSource</code>/its <code>pegRevision</code> to
 * <code>secondSource</code>/its <code>pegRevision</code> into the working copy path 
 * defined in operation's <code>target</code>.
 * The third case is if <code>reintegrate</code> is <code>true</code> performs 
 * a reintegration merge of <code>source</code> at its <code>pegRevision</code> 
 * into working copy <code>target</code>.
 * 
 * <ul>
 * <li>
 * <b>Merge between sources/revision ranges, no reintegration.</b>
 * 
 * <p/>
 * If <code>depth</code> is {@link SVNDepth#INFINITY}, merges fully
 * recursively. Else if {@link SVNDepth#IMMEDIATES}, merges changes at most
 * to files that are immediate children of <code>target</code> and to
 * directory properties of <code>target</code> and its immediate
 * subdirectory children. Else if {@link SVNDepth#FILES}, merges at most to
 * immediate file children of <code>target</code> and to
 * <code>target</code> itself. Else if {@link SVNDepth#EMPTY}, applies
 * changes only to <code>target</code> (i.e., directory property changes
 * only).
 * 
 * <p/>
 * If <code>depth</code> is {@link SVNDepth#UNKNOWN}, uses the depth of
 * <code>target</code>.
 * 
 * <p/>
 * Uses <code>ignoreAncestry</code> to control whether or not items being
 * diffed will be checked for relatedness first. Unrelated items are
 * typically transmitted to the editor as a deletion of one thing and the
 * addition of another, but if this flag is <code>false</code>, unrelated items will be diffed as if
 * they were related.
 * 
 * <p/>
 * If <code>force</code> is not set and the merge involves deleting locally
 * modified or unversioned items the operation will fail. If
 * <code>force</code> is set such items will be deleted.
 * 
 * <p/>
 * Merge options <code>mergeOptions</code> is a collection of {@link SVNDiffOptions},
 * they are used to pass arguments to the merge processes (internal or external).
 * 
 * <p/>
 * If the caller's {@link ISVNEventHandler} is not <code>null</code>, 
 * then it will be called once for each merged target.
 * 
 * <p/>
 * If <code>recordOnly</code> is <code>true</code>, the
 * merge isn't actually performed, but the mergeinfo for the revisions which
 * would've been merged is recorded in the working copy (and must be
 * subsequently committed back to the repository).
 * 
 * <p/>
 * If <code>dryRun</code> is <code>true</code>, the
 * merge is carried out, and full notification feedback is provided, but the
 * working copy is not modified.
 * 
 * <ul>
 * <li>
 * <b>Merge between revision ranges.</b>
 * 
 * <p/>
 * <code>Ranges</code> is a collection of {@link SvnRevisionRange}
 * ranges. These ranges may describe additive and/or subtractive merge
 * ranges, they may overlap fully or partially, and/or they may partially or
 * fully negate each other. This range list is not required to be sorted.
 * 
 * </li>
 * <li>
 * <b>Merge between two sources.</b>
 * 
 * <p/>
 * <code>FirstSource</code> and <code>secondSource</code> must both represent the same node
 * kind - that is, if <code>firstSource</code> is a directory, <code>secondSource</code>
 * must also be, and if <code>firstSource</code> is a file, <code>secondSource</code> must
 * also be.
 * 
 * </li>
 * </ul>
 * 
 * </li>
 * <li>
 * <b>Reintegration merge.</b>
 * 
 * <p/>
 * This kind of merge should be used for back merging (for example, merging
 * branches back to trunk, in which case merge is carried out by comparing
 * the latest trunk tree with the latest branch tree; i.e. the resulting
 * difference is exactly the branch changes which will go back to trunk).
 * 
 * <p/>
 * Destination <code>target</code> must be a single-revision, {@link SVNDepth#INFINITY}, 
 * pristine, unswitched working copy - in other words, it must reflect a
 * single revision tree, the "target". The mergeinfo on <code>source</code>
 * must reflect that all of the target has been merged into it.
 *  
 * <p/>
 * The depth of the merge is always {@link SVNDepth#INFINITY}.
 * 
 * <p/>
 * If <code>source</code>'s <code>pegRevision</code> is 
 * <code>null</code> or {@link SVNRevision#isValid() invalid}, then it defaults to
 * {@link SVNRevision#HEAD}.
 * 
 * </li>
 * </ul>
 * 
 * <p/>
 * Note: this operation requires repository access.
 * 
 * <p/>
 * {@link #run()} method throws org.tmatesoft.svn.core.SVNException in
 *             the following cases:
 *             <ul>
 *             <li/>exception with {@link SVNErrorCode#CLIENT_BAD_REVISION}-
 *             If any revision in the list of provided ranges is
 *             {@link SVNRevision#isValid() invalid}
 *             <li/>exception with {@link SVNErrorCode#CLIENT_BAD_REVISION}
 *             error code - if either <code>firstSource</code>'s <code>pegRevision</code> or <code>
 *             firstSource</code>'s <code>pegRevision</code> is {@link SVNRevision#isValid() invalid}
 *             </ul>
 *             </ul>
 *             
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnMerge extends SvnOperation<Void> {
    
    private SvnTarget firstSource;
    private SvnTarget secondSource;
    
    private boolean ignoreAncestry;
    private boolean force;
    private boolean recordOnly;
    private boolean dryRun;
    private boolean allowMixedRevisions;
    
    private SvnTarget source;
    private boolean reintegrate;

    private Collection<SvnRevisionRange> ranges;
    private SVNDiffOptions mergeOptions;
    
    protected SvnMerge(SvnOperationFactory factory) {
        super(factory);
    }

    /**
     * Adds the revision range to collection of revision ranges for merging.
     * @param range revision range for merge
     * @see #getRevisionRanges()
     */
    public void addRevisionRange(SvnRevisionRange range) {
        if (ranges == null) {
            ranges = new ArrayList<SvnRevisionRange>();
        }
        SVNRevision start = range.getStart();
        SVNRevision end = range.getEnd();        
        if (start == SVNRevision.UNDEFINED && end == SVNRevision.UNDEFINED) {
            start = SVNRevision.create(0);
            end = getSource().getResolvedPegRevision();
            range  = SvnRevisionRange.create(start, end);
        }
        ranges.add(range);
    }
    
    /**
    * Returns the  collection of {@link SvnRevisionRange}
    * ranges. These ranges may describe additive and/or subtractive merge
    * ranges, they may overlap fully or partially, and/or they may partially or
    * fully negate each other. This range list is not required to be sorted.
    * 
    * @return revision ranges of the merge
    */
    public Collection<SvnRevisionRange> getRevisionRanges() {
        return ranges;
    }
    
    /**
     * Sets source of the merge with reintegrate flag.
     * This <code>source</code> is used in merge between revisions
     * and reintegrate merge.
     *   
     * @param source of merge
     * @param reintegrate <code>true</code> if it is reintegrate merge, otherwise <code>false</code>
     * @see #isReintegrate()
     */
    public void setSource(SvnTarget source, boolean reintegrate) {
        this.source = source;
        this.reintegrate = reintegrate;
        if (source != null) {
            setSources(null, null);
        }
    }
    
    /**
     * Sets first and seconds sources of the merge.
     * Those sources are used in merge between two sources.
     * 
     * @param source1 first source
     * @param source2 second source
     */
    public void setSources(SvnTarget source1, SvnTarget source2) {
        this.firstSource = source1;
        this.secondSource = source2;
        if (firstSource != null) {
            setSource(null, false);
        }
    }
    
    /**
     * Returns source for merge between revisions and reintegrate merge.
     *
     * @return merge source 
     */
    public SvnTarget getSource() {
        return this.source;
    }
    
    /**
     * Returns first source for merge between two sources.
     * 
     * @return first source of merge
     */
    public SvnTarget getFirstSource() {
        return this.firstSource;
    }
    
    /**
     * Returns second source for merge between two sources.
     * 
     * @return first source of merge
     */
    public SvnTarget getSecondSource() {
        return this.secondSource;
    }
    
    /**
     * Returns whether it is reintegrate merge.
     * This kind of merge should be used for back merging (for example, merging
     * branches back to trunk, in which case merge is carried out by comparing
     * the latest trunk tree with the latest branch tree; i.e. the resulting
     * difference is exactly the branch changes which will go back to trunk).
     * 
     * @return <code>true</code> if it is reintegrate merge, otherwise <code>false</code>
     */
    public boolean isReintegrate() {
        return this.reintegrate;
    }

    /**
     * Returns whether or not items being diffed will be checked for relatedness first. Unrelated items are
	 * typically transmitted to the editor as a deletion of one thing and the
	 * addition of another, but if this flag is <code>false</code>, unrelated items will be diffed as if
	 * they were related.
	 * 
     * @return <code>true</code> if ancestry should be ignored, otherwise <code>false</code>
     */
    public boolean isIgnoreAncestry() {
        return ignoreAncestry;
    }

    /**
     * Sets whether or not items being diffed will be checked for relatedness first. Unrelated items are
	 * typically transmitted to the editor as a deletion of one thing and the
	 * addition of another, but if this flag is <code>false</code>, unrelated items will be diffed as if
	 * they were related.
	 * 
     * @param ignoreAncestry <code>true</code> if ancestry should be ignored, otherwise <code>false</code>
     */
    public void setIgnoreAncestry(boolean ignoreAncestry) {
        this.ignoreAncestry = ignoreAncestry;
    }

    /**
     * Returns whether to fail if merge involves deleting locally
	 * modified or unversioned items. If <code>force</code> is <code>true</code> such items will be deleted,
	 * otherwise operation will fail.
	 * 
     * @return <code>true</code> if operation should be forced to run, otherwise <code>false</code>
     */
    public boolean isForce() {
        return force;
    }

    /**
     * Sets whether to fail if merge involves deleting locally
	 * modified or unversioned items. If <code>force</code> is <code>true</code> such items will be deleted,
	 * otherwise operation will fail.
	 * 
     * @param force <code>true</code> if operation should be forced to run, otherwise <code>false</code>
     */
    public void setForce(boolean force) {
        this.force = force;
    }

    /**
     * Returns whether the merge isn't actually performed, but the mergeinfo for the revisions which
     * would've been merged is recorded in the working copy (and must be
     * subsequently committed back to the repository).
     * 
     * 
     * @return <code>true</code> if operation should record only the result of merge - mergeinfo data, otherwise <code>false</code>
     */
    public boolean isRecordOnly() {
        return recordOnly;
    }

    /**
     * Sets whether the merge isn't actually performed, but the mergeinfo for the revisions which
     * would've been merged is recorded in the working copy (and must be
     * subsequently committed back to the repository).
     * 
     * 
     * @param recordOnly <code>true</code> if operation should record only the result of merge - mergeinfo data, otherwise <code>false</code>
     */
    public void setRecordOnly(boolean recordOnly) {
        this.recordOnly = recordOnly;
    }

    /**
     * Returns whether the merge is carried out, and full notification feedback is provided, but the
     * working copy is not modified.
     * 
     * @return <code>true</code> if the operation should only find out if a file can be merged successfully, otherwise <code>false</code>
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * Sets whether the merge is carried out, and full notification feedback is provided, but the
     * working copy is not modified.
     * 
     * @param dryRun <code>true</code> if the operation should only find out if a file can be merged successfully, otherwise <code>false</code>
     */
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * Returns whether to allow merge target to have mixed revisions.
     * If set to <code>false</code> and target has mixed revisions, {@link SVNException} is 
     * thrown with error codes {@link SVNErrorCode#CLIENT_NOT_READY_TO_MERGE} 
     * or {@link SVNErrorCode#CLIENT_MERGE_UPDATE_REQUIRED}.
     * 
     * @return <code>true</code> if operation allows merging to mixed-revision working copy, otherwise <code>false</code>
     * @since 1.7, SVN 1.7
     */
    public boolean isAllowMixedRevisions() {
        return allowMixedRevisions;
    }

    /**
     * Sets whether to allow merge target to have mixed revisions.
     * If set to <code>false</code> and target has mixed revisions, {@link SVNException} is 
     * thrown with error codes {@link SVNErrorCode#CLIENT_NOT_READY_TO_MERGE} 
     * or {@link SVNErrorCode#CLIENT_MERGE_UPDATE_REQUIRED}.
     * 
     * @param allowMixedRevisions <code>true</code> if operation allows merging to mixed-revision working copy, otherwise <code>false</code>
     * @since 1.7, SVN 1.7
     */
    public void setAllowMixedRevisions(boolean allowMixedRevisions) {
        this.allowMixedRevisions = allowMixedRevisions;
    }

    /**
     * Returns the operation's merge options controlling white-spaces and eol-styles.
     * 
     * @return merge options of the operation
     */
    public SVNDiffOptions getMergeOptions() {
        return mergeOptions;
    }

    /**
     * Sets the operation's merge options controlling white-spaces and eol-styles.
     * 
     * @param mergeOptions merge options of the operation
     */
    public void setMergeOptions(SVNDiffOptions mergeOptions) {
        this.mergeOptions = mergeOptions;
    }
    
    /**
     * Gets whether the operation changes working copy
     * @return <code>true</code> if the operation changes the working copy, otherwise <code>false</code>
     */
    @Override
    public boolean isChangesWorkingCopy() {
        return true;
    }
}
