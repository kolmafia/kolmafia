package org.tmatesoft.svn.core.wc2;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;

/**
 * Represents copy operation.
 * Copies each source in <code>sources</code> to operation's <code>target</code>
 * representing working copy path, 
 * or converts a disjoint working copy to a copied one,
 * or does virtual copy (see below).
 * 
 * <ul>
 * <li>
 * <b> If <code>disjoint</code> and <code>virtual</code> are <code>false</code>:</b>
 * 
 * <p/>
 * If multiple <code>sources</code> are given, <code>target</code> must be a
 * directory, and <code>sources</code> will be copied as children of
 * directory.
 * 
 * <p/>
 * Each <code>src</code> in <code>sources</code> must be files or
 * directories under version control, or URLs of a versioned item in the
 * repository. If <code>sources</code> has multiple items, they must be all
 * repository URLs or all working copy paths.
 * 
 * <p/>
 * The parent of <code>target</code> must already exist.
 * 
 * <p/>
 * If <code>sources</code> has only one item, attempts to copy it to
 * <code>target</code>. If <code>failWhenDstExists</code> is <code>false</code> 
 * and <code>target</code> already exists,
 * attempts to copy the item as a child of <code>target</code>. If
 * <code>failWhenDstExists</code> is <code>true</code>
 * and <code>target</code> already exists, throws an {@link SVNException} with
 * the {@link SVNErrorCode#FS_ALREADY_EXISTS} error code.
 * 
 * <p/>
 * If <code>sources</code> has multiple items, and
 * <code>failWhenDstExists</code> is <code>false</code>,
 * all <code>sources</code> are copied as children of <code>target</code>. If
 * any child of <code>target</code> already exists with the same name any item
 * in <code>sources</code>, throws an {@link SVNException} with the
 * {@link SVNErrorCode#FS_ALREADY_EXISTS} error code.
 * 
 * <p/>
 * If <code>sources</code> has multiple items, and
 * <code>failWhenDstExists</code> is <code>true</code>,
 * throws an {@link SVNException} with the
 * {@link SVNErrorCode#CLIENT_MULTIPLE_SOURCES_DISALLOWED}.
 * 
 * <p/>
 * If the caller's {@link org.tmatesoft.svn.core.wc.ISVNEventHandler} is non-<code>null</code>, invokes 
 * it for each item added at the new location.
 * 
 * <p/>
 * This method is just a variant of a local add operation, where
 * <code>sources</code> are scheduled for addition as copies. No changes
 * will happen to the repository until a commit occurs. This scheduling can
 * be removed with {@link SvnRevert}.
 *
 * <p/>
 * Note: this routine requires repository access only when sources are URLs.
 * 
 * </li>
 * 
 * <li>
 * 
 * <b> If <code>disjoint</code> is <code>true</code>:</b>
 * 
 * <p/>
 * <code>Targets</code> represent the roots of the working copies located in another working copies.
 * 
 * <p/> 
 * This copy operation uses only <code>sources</code> as operation's parameters.
 * 
 * <p/>
 * Note: this routine does not require repository access. However if it's
 * performed on an old format working copy where repository root urls were
 * not written, the routine will connect to the repository to fetch the
 * repository root urls.
 * 
 * {@link #run()} throws {@link SVNException} in the following cases:
 *             <ul>
 *             <li/>exception with {@link SVNErrorCode#UNSUPPORTED_FEATURE}
 *             error code - if <code>target</code> is either not a
 *             directory, or has no parent at all; if the current local
 *             filesystem parent of <code>target</code> is actually a
 *             child of it in the repository 
 *             <li/>exception with
 *             {@link SVNErrorCode#ENTRY_EXISTS} error code - if <code>
 *             target</code> is not a disjoint working copy, i.e. there is
 *             already a versioned item under the parent path of <code>
 *             target</code>; if <code>target</code> is not in the
 *             repository yet (has got a schedule for addition flag) 
 *             <li/>
 *             exception with {@link SVNErrorCode#WC_INVALID_SCHEDULE} error
 *             code - if <code>target</code> is not from the same
 *             repository as the parent directory; if the parent of <code>
 *             target</code> is scheduled for deletion; if <code>target
 *             </code> is scheduled for deletion 
 *             </ul>
 * 
 * </li>
 * <li>
 * 
 * <b> If <code>virtual</code> is <code>true</code>:</b>
 * 
 * <p/>
 * Copies/moves administrative version control information of a source files
 * to administrative information of a destination file. For example, if you
 * have manually copied/moved a source files to a <code>target</code> one (manually means
 * just in the filesystem, not using version control operations) and then
 * would like to turn this copying/moving into a complete version control
 * copy or move operation, use this method that will finish all the work for
 * you - it will copy/move all the necessary administrative information
 * (kept in the source <i>.svn</i> directory) to the <code>target</code> <i>.svn</i>
 * directory.
 * <p>
 * In that case when you have your files copied/moved in the filesystem, you
 * can not perform standard (version control) copying/moving - since the
 * <code>target</code> already exists and the source may be already deleted. Use this
 * method to overcome that restriction.
 * 
 * <p/>
 * This operation uses <code>sources</code> and <code>move</code> parameters.
 * If <code>move</code> is <code>true</code> then completes moving
 * <code>src</code> to <code>target</code>, otherwise completes copying <code>src</code> to <code>dst</code>
 * {@link #run()} throws {@link SVNException} if one of the following is true:
 *             <ul>
 *             <li><code>move = true</code>
 *             and <code>src</code> still exists <li><code>target</code> does
 *             not exist <li><code>target</code> is a directory <li><code>src
 *             </code> is a directory <li><code>src</code> is not under
 *             version control <li><code>target</code> is already under version
 *             control <li>if <code>src</code> is copied but not scheduled
 *             for addition, and SVNKit is not able to locate the copied
 *             directory root for <code>src</code>
 *             </ul>
 * </li>
 * </ul>
 *             
 * @author TMate Software Ltd.
 * @version 1.7
 * @see SvnCopySource
 */

public class SvnCopy extends SvnOperation<Void> {
    
    private Collection<SvnCopySource> sources = new HashSet<SvnCopySource>();
    private boolean move;
    private boolean makeParents;
    private boolean failWhenDstExist;
    private boolean ignoreExternals;
    private boolean virtual;
    private boolean disjoint;

    protected SvnCopy(SvnOperationFactory factory) {
        super(factory);
        this.sources = new HashSet<SvnCopySource>();
    }

    /**
     * Returns operation's all copy sources, object containing information about what to copy.
     * 
     * @return the copy sources of the operation, unmodifiable
     */
    public Collection<SvnCopySource> getSources() {
        return Collections.unmodifiableCollection(sources);
    }
    
    /**
     * Adds copy source information to the operation
     * 
     * @param source copy source information 
     */
    public void addCopySource(SvnCopySource source) {
        if (source != null) {
            this.sources.add(source);
        }
    }
    
    /**
     * Gets whether to do copy as move operation (delete, then add with history).
     * 
     * @return <code>true</code> if move operation should be done, otherwise <code>false</code>
     */
    public boolean isMove() {
        return move;
    }

    /**
     * Sets whether to do copy as move operation (delete, then add with history).
     * 
     * @param isMove <code>true</code> if move operation should be done, otherwise <code>false</code>
     */
    public void setMove(boolean isMove) {
        this.move = isMove;
    }
    
    /**
     * Gets whether to make parent folders if don't exist.
     * 
     * @return <code>true</code> if non-existent parent directories should be created, otherwise <code>false</code>
     */
    public boolean isMakeParents() {
        return makeParents;
    }

    /**
     * Sets whether to make parent folders if don't exist.
     * 
     * @param isMakeParents <code>true</code> if non-existent parent directories should be created, otherwise <code>false</code>
     */
    public void setMakeParents(boolean isMakeParents) {
        this.makeParents = isMakeParents;
    }

    /**
     * Gets whether to fail if <code>target</code> already exists.
     * 
     * @return <code>true</code> if fail when <code>target</code> already exists, otherwise <code>false</code>
     * @see SvnRemoteCopy
     */
    public boolean isFailWhenDstExists() {
        return failWhenDstExist;
    }

    /**
     * Sets whether to fail if <code>target</code> already exists.
     * 
     * @param isFailWhenDstExist <code>true</code> if fail when <code>target</code> already exists, otherwise <code>false</code>
     * @see SvnRemoteCopy
     */
    public void setFailWhenDstExists(boolean isFailWhenDstExist) {
        this.failWhenDstExist = isFailWhenDstExist;
    }

    /**
     * Returns whether to ignore externals definitions.
     * 
     * @return <code>true</code> if externals definitions should be ignored, otherwise <code>false</code>
     */
    public boolean isIgnoreExternals() {
        return ignoreExternals;
    }

    /**
     * Sets whether to ignore externals definitions.
     * 
     * @param ignoreExternals <code>true</code> if externals definitions should be ignored, otherwise <code>false</code>
     */
    public void setIgnoreExternals(boolean ignoreExternals) {
        this.ignoreExternals = ignoreExternals;
    }

    /**
     * Returns whether copy is virtual copy.
     * 
     * @return <code>true</code> if it is virtual copy, otherwise <code>false</code>
     */
    public boolean isVirtual() {
        return virtual;
    }

    /**
     * Sets whether copy is virtual copy.
     * 
     * @param virtual <code>true</code> if it is virtual copy, otherwise <code>false</code>
     */
    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    /**
     * Returns whether copy is disjoint working copy.
     * 
     * @return <code>true</code> if it is disjoint working copy, otherwise <code>false</code>
     */
    public boolean isDisjoint() {
        return disjoint;
    }

    /**
     * Sets whether copy is disjoint working copy.
     * 
     * @param disjoint <code>true</code> if it is disjoint working copy, otherwise <code>false</code>
     */
    public void setDisjoint(boolean disjoint) {
        this.disjoint = disjoint;
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
