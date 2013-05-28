package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * Represents update operation. Updates working copy to <code>revision</code>.
 * If no revision is given, it brings working copy up-to-date with {@link SVNRevision#HEAD} revision.
   
 * Unversioned paths that are direct children of a versioned path will cause
 * an update that attempts to add that path, other unversioned paths are
 * skipped.
 *  
 * <p/>
 * The <code>targets</code> can be from multiple working copies from
 * multiple repositories, but even if they all come from the same repository
 * there is no guarantee that revision represented by
 * {@link SVNRevision#HEAD} will remain the same as each path is updated.
 * 
 * <p/>
 * If externals are ignored (<code>ignoreExternals</code> is <code>true</code>), doesn't process
 * externals definitions as part of this operation.
 * 
 * <p/>
 * If <code>depth</code> is {@link SVNDepth#INFINITY}, updates fully
 * recursively. Else if it is {@link SVNDepth#IMMEDIATES} or
 * {@link SVNDepth#FILES}, updates each <code>target</code> and its file entries, but not
 * its subdirectories. Else if {@link SVNDepth#EMPTY}, updates exactly each
 * target, nonrecursively (essentially, updates the target's properties).
 * 
 * <p/>
 * If <code>depth</code> is {@link SVNDepth#UNKNOWN}, takes the working
 * depth from <code>paths</code> and then behaves as described above.
 * 
 * <p/>
 * If <code>depthIsSticky</code> is set and <code>depth</code> is not
 * {@link SVNDepth#UNKNOWN}, then in addition to updating <code>paths</code>
 * , also sets their sticky ambient depth value to <code>depth</code>.
 * 
 * <p/>
 * If <code>allowUnversionedObstructions</code> is <code>
 * true</code> then the update tolerates existing
 * unversioned items that obstruct added paths. Only obstructions of the
 * same type (file or directory) as the added item are tolerated. The text of
 * obstructing files is left as-is, effectively treating it as a user
 * modification after the update. Working properties of obstructing items
 * are set equal to the base properties. If
 * <code>allowUnversionedObstructions</code> is <code>false
 * </code> then the update will abort if there are
 * any unversioned obstructing items.
 * 
 * <p/>
 * If the operation's {@link ISVNEventHandler} is non-<code>null</code>, 
 * it is invoked for each item handled by
 * the update, and also for files restored from text-base. Also
 * {@link ISVNEventHandler#checkCancelled()} will be used at various places
 * during the update to check whether the caller wants to stop the update.
 * 
 * <p/>
 * This operation requires repository access (in case the repository is not
 * on the same machine, network connection is established).
 * 
 * <p/>
 * {@link #run()} method returns an array of <code>long</code> revisions with each element set to
 * the revision to which <code>revision</code> was resolved.
 * 
 * {@link #run()} throws {@link org.tmatesoft.svn.core.SVNException} in the following cases:
 *             	<ul>
 *             	<li/>exception with {@link SVNErrorCode#ILLEGAL_TARGET} error code 
 *             	- if <code>target</code> is not a local path
 *             	<li/>exception with {@link SVNErrorCode#RA_ILLEGAL_URL} error code 
 *             	- if external item at the <code>revision</code> doesn't exist, 
 *             	or if external item at the <code>revision</code> is not file or a directory
 *            	<li/>exception with {@link SVNErrorCode#ENTRY_MISSING_URL} error code 
 *        		- if working copy item that has no URL
 *        		</ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnUpdate extends AbstractSvnUpdate<long[]> {
    
    private boolean depthIsSticky;
    private boolean makeParents;
    private boolean treatAddsAsModifications;

    protected SvnUpdate(SvnOperationFactory factory) {
        super(factory);
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        for (SvnTarget target : getTargets()) {
            if (target.isURL()) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "''{0}'' is not a local path", target.getURL());
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        if (getDepth() == null) {
            setDepth(SVNDepth.UNKNOWN);
        }
        if (getDepth() == SVNDepth.UNKNOWN) {
            setDepthIsSticky(false);
        }
        super.ensureArgumentsAreValid();
    }

    /**
     * Gets whether or not the requested depth should be written to the working copy.
     * 
     * @return <code>true</code> if the requested depth should be written to the working copy, otherwise <code>false</code>
     */
     public boolean isDepthIsSticky() {
         return depthIsSticky;
     }

    /**
    * Sets whether or not the requested depth should be written to the working copy.
    *
    * @param depthIsSticky <code>true</code> if the requested depth should be written to the working copy, otherwise <code>false</code>
    */
    public void setDepthIsSticky(boolean depthIsSticky) {
        this.depthIsSticky = depthIsSticky;
    }
    
    /**
     * Gets whether or not intermediate directories should be made.
     * 
     * @return <code>true</code> if intermediate directories should be made, otherwise <code>false</code>
     */
    public boolean isMakeParents() {
        return makeParents;
    }

    /**
     * Sets whether or not intermediate directories should be made.
     * 
     * @param makeParents <code>true</code> if intermediate directories should be made, otherwise <code>false</code>
     */
    public void setMakeParents(boolean makeParents) {
        this.makeParents = makeParents;
    }
    
    /**
     * Gets whether or not adds should be treated as modifications.
     * 
     * @return <code>true</code> if adds should be treated as modifications, otherwise <code>false</code>
     */
    public boolean isTreatAddsAsModifications() {
        return treatAddsAsModifications;
    }
    
    /**
     * Sets whether or not adds should be treated as modifications.
     * 
     * @param treatAddsAsModifications <code>true</code> if adds should be treated as modifications, otherwise <code>false</code>
     */
    public void setTreatAddsAsModifications(boolean treatAddsAsModifications) {
        this.treatAddsAsModifications = treatAddsAsModifications;
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
        return true;
    }
}
