package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * Represents switch operation.
 * Switches working tree of <code>target</code> to <code>switchTarget</code>\
 * <code>switchTarget</code>'s <code>pegRevision</code> at <code>revision</code>.
 * 
 * <p/>
 * Summary of purpose: this is normally used to switch a working directory
 * over to another line of development, such as a branch or a tag. Switching
 * an existing working directory is more efficient than checking out
 * <code>switchTarget</code> from scratch.
 * 
 * <p/>
 * If <code>depth</code> is {@link SVNDepth#INFINITY}, switches fully
 * recursively. Else if it is {@link SVNDepth#IMMEDIATES}, switches
 * <code>target</code> and its file children (if any), and switches
 * subdirectories but does not update them. Else if {@link SVNDepth#FILES},
 * switches just file children, ignoring subdirectories completely. Else if
 * {@link SVNDepth#EMPTY}, switches just <code>target</code> and touches
 * nothing underneath it.
 * 
 * <p/>
 * If externals are ignored (<code>ignoreExternals</code> is <code>true</code>), doesn't process
 * externals definitions as part of this operation.
 * 
 * <p/>
 * If <code>allowUnversionedObstructions</code> is <code>true</code> then the switch tolerates existing
 * unversioned items that obstruct added paths. Only obstructions of the
 * same type (file or directory) as the added item are tolerated. The text of
 * obstructing files is left as-is, effectively treating it as a user
 * modification after the switch. Working properties of obstructing items
 * are set equal to the base properties. If
 * <code>allowUnversionedObstructions</code> is <code>false</code> then the switch will abort if there are
 * any unversioned obstructing items.
 * 
 * <p/>
 * If the caller's {@link ISVNEventHandler} is non-<code>null</code>, it is invoked for paths affected by the
 * switch, and also for files restored from text-base. Also
 * {@link ISVNEventHandler#checkCancelled()} will be used at various places
 * during the switch to check whether the caller wants to stop the switch.
 * 
 * <p/>
 * This operation requires repository access (in case the repository is not
 * on the same machine, network connection is established).
 * 
 * <p/>
 * {@link #run()} method returns value of the revision value to which the working copy was actually
 * switched.
 * 
 * <p/>
 * {@link #run()} method returns value of the revision to which the working copy was actually switched.
 *             	<ul>
 *             <li/>exception with {@link SVNErrorCode#UNSUPPORTED_FEATURE}
 *             	error code - if <code>target</code> is not in the repository yet
 *             <li/>exception with {@link SVNErrorCode#ENTRY_MISSING_URL} error code 
 *        		- if <code>switchTarget</code> directory has no URL
 *        		<li/>exception with {@link SVNErrorCode#WC_INVALID_SWITCH} error code 
 *        		- if <code>switchTarget</code> is not the same repository as <code>target</code>'s repository
 *       		<li/>exception with {@link SVNErrorCode#CLIENT_UNRELATED_RESOURCES} error code 
 *        		- if <code>ignoreAncestry</code> is <code>false</code> and  
 *        		<code>switchTarget</code> shares no common ancestry with <code>target</code>
 *        		</ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnSwitch extends AbstractSvnUpdate<Long> {

    private boolean depthIsSticky;
    private boolean ignoreAncestry;
    
    private SvnTarget switchTarget;

    protected SvnSwitch(SvnOperationFactory factory) {
        super(factory);
    }

    /**
     * Returns whether depth is sticky.
     * If <code>depthIsSticky</code> is set the operation will use <code>depth</code> as status scope, otherwise 
     * {@link SVNDepth#UNKNOWN} will be used.
	 * 
     * @return <code>true</code> if the depth is sticky, otherwise <code>false</code>
     */
    public boolean isDepthIsSticky() {
        return depthIsSticky;
    }

    /**
     * Returns whether to ignore ancestry when calculating merges.
     * 
     * @return <code>true</code> if ancestry should be ignored, otherwise <code>false</code>
     * @since 1.7, Subversion 1.7
     */
    public boolean isIgnoreAncestry() {
        return ignoreAncestry;
    }

    /**
     * Returns the repository location as a target against which the item will be switched.
     * 
     * @return switch target
     */
    public SvnTarget getSwitchTarget() {
        return switchTarget;
    }

    /**
     * Sets whether depth is sticky.
     * If <code>depthIsSticky</code> is set the operation will use <code>depth</code> as status scope, otherwise 
     * {@link SVNDepth#UNKNOWN} will be used.
	 * 
     * @param depthIsSticky <code>true</code> if the depth is sticky, otherwise <code>false</code>
     */
    public void setDepthIsSticky(boolean depthIsSticky) {
        this.depthIsSticky = depthIsSticky;
    }

    /**
     * Sets whether to ignore ancestry when calculating merges.
     * 
     * @param ignoreAncestry <code>true</code> if ancestry should be ignored, otherwise <code>false</code>
     * @since 1.7, Subversion 1.7
     */
    public void setIgnoreAncestry(boolean ignoreAncestry) {
        this.ignoreAncestry = ignoreAncestry;
    }

    /**
     * Sets the repository location as a target against which the item will be switched.
     * 
     * @param switchTarget switch target
     */
    public void setSwitchTarget(SvnTarget switchTarget) {
        this.switchTarget = switchTarget;
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        super.ensureArgumentsAreValid();
        if (getDepth() == SVNDepth.EXCLUDE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Cannot both exclude and switch a path");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
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
