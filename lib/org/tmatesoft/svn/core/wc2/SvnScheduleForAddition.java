package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;


/**
 * Represents add operation.
 * Schedules working copy <code>targets</code> for addition to the repository.
 * 
 * <p/>
 * If <code>depth</code> is {@link SVNDepth#EMPTY}, adds just
 * <code>targets</code> and nothing below it. If {@link SVNDepth#FILES}, adds
 * <code>targets</code> and any file children of <code>targets</code>. If
 * {@link SVNDepth#IMMEDIATES}, adds <code>targets</code>, any file children,
 * and any immediate subdirectories (but nothing underneath those
 * subdirectories). If {@link SVNDepth#INFINITY}, adds <code>targets</code>
 * and everything under it fully recursively.
 * 
 * <p/>
 * <code>targets</code>' parent must be under revision control already (unless
 * <code>makeParents</code> is <code>true</code>), but
 * <code>targets</code> are not.
 * 
 * <p/>
 * If <code>force</code> is set, <code>target</code> is a directory, <code>depth</code> is
 * {@link SVNDepth#INFINITY}, then schedules for addition unversioned files
 * and directories scattered deep within a versioned tree.
 * 
 * <p/>
 * If <code>includeIgnored</code> is <code>false</code>,
 * doesn't add files or directories that match ignore patterns.
 * 
 * <p/>
 * If <code>makeParents</code> is <code>true</code>,
 * recurse up path's directory and look for a versioned directory. If found,
 * add all intermediate paths between it and the path.
 * 
 * <p/>
 * Important: this is a *scheduling* operation. No changes will happen to
 * the repository until a commit occurs. This scheduling can be removed with
 * {@link SvnRevert} operation.
 * 
 * <p/>
 * {@link #run()} method throws {@link SVNException} in the following cases: 
 *             <ul>
 *             <li/>exception with {@link SVNErrorCode#ENTRY_EXISTS} error
 *             code - if <code>force</code> is not set and a path is already
 *             under version 
 *             <li/>exception with
 *             {@link SVNErrorCode#CLIENT_NO_VERSIONED_PARENT} error code -
 *             if <code>makeParents</code> is <code>true</code> but no unversioned paths
 *             stepping upper from a path are found
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnScheduleForAddition extends SvnOperation<Void> {
    
    private boolean force;
    private boolean includeIgnored;
    private boolean applyAutoProperties;
    private boolean addParents;
    private boolean mkDir;
    private ISvnAddParameters addParameters;

    protected SvnScheduleForAddition(SvnOperationFactory factory) {
        super(factory);
    }

    @Override
    protected void initDefaults() {
        setApplyAutoProperties(true);
        super.initDefaults();
    }

    /**
     * Returns whether to throw exceptions on already-versioned items
     * 
     * @return <code>true</code> if operation does not throw exceptions on already-versioned items, 
     *  <code>false</code> if exception should be thrown
     */
    public boolean isForce() {
        return force;
    }

    /**
     * Return whether to add files or directories that match ignore patterns.
     * 
     * @return <code>true</code> if ignore patterns should not be applied to paths being added, otherwise <code>false</code>
     */
    public boolean isIncludeIgnored() {
        return includeIgnored;
    }

    public boolean isApplyAutoProperties() {
        return applyAutoProperties;
    }

    /**
     * Returns whether to recurse up path's directory and look for a versioned directory. If found,
     * add all intermediate paths between it and the path.
     * 
     * @return <code>true</code> if operation should climb upper and schedule also all unversioned paths in the way
     */
    public boolean isAddParents() {
        return addParents;
    }

    /**
     * Sets whether to throw exceptions on already-versioned items
     * 
     * @param force <code>true</code> if operation does not throw exceptions on already-versioned items, 
     *  <code>false</code> if exception should be thrown
     */
    public void setForce(boolean force) {
        this.force = force;
    }

    /**
     * Sets whether to add files or directories that match ignore patterns.
     * 
     * @param includeIgnored <code>true</code> if ignore patterns should not be applied to paths being added, otherwise <code>false</code>
     */
    public void setIncludeIgnored(boolean includeIgnored) {
        this.includeIgnored = includeIgnored;
    }

    public void setApplyAutoProperties(boolean applyAutoProperties) {
        this.applyAutoProperties = applyAutoProperties;
    }

    /**
     * Sets whether to recurse up path's directory and look for a versioned directory. If found,
     * add all intermediate paths between it and the path.
     * 
     * @param addParents <code>true</code> if operation should climb upper and schedule also all unversioned paths in the way
     */
    public void setAddParents(boolean addParents) {
        this.addParents = addParents;
    }

    /**
     * Returns whether a directory at <code>target</code>'s path also should be created
     * 
     * @return <code>true</code>, if a directory at <code>target</code>'s path also should be created
     */
    public boolean isMkDir() {
        return mkDir;
    }

    /**
     * Sets whether a directory at <code>target</code>'s path also should be created
     * 
     * @param mkDir <code>true</code>, if a directory at <code>target</code>'s path also should be created
     */
    public void setMkDir(boolean mkDir) {
        this.mkDir = mkDir;
    }

    @Override
    protected int getMaximumTargetsCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isUseParentWcFormat() {
        return true;
    }

    /**
     * Returns operation's add parameters, whose controls inconsistent EOL's.
     * 
     * @return add parameters of the operation
     * @see ISvnAddParameters
     */
    public ISvnAddParameters getAddParameters() {
        return addParameters;
    }

    /**
     * Sets operation's add parameters, whose controls inconsistent EOL's.
     * 
     * @param addParameters add parameters of the operation
     * @see ISvnAddParameters
     */
    public void setAddParameters(ISvnAddParameters addParameters) {
        this.addParameters = addParameters;
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
