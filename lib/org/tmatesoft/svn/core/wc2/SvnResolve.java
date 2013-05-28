package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.internal.wc.admin.ISVNEntryHandler;
import org.tmatesoft.svn.core.wc.SVNConflictChoice;
import org.tmatesoft.svn.core.wc.SVNEventAction;

/**
 * Represents resolve operation.
 * Performs automatic conflict resolution on a working copy
 * <code>target</code>.
 * 
 * <p/>
 * If <code>depth</code> is {@link SVNDepth#EMPTY}, acts only on
 * <code>target</code>; if{@link SVNDepth#FILES}, resolves <code>target</code>
 * and its conflicted file children (if any); if {@link SVNDepth#IMMEDIATES}
 * , resolves <code>target</code> and all its immediate conflicted children
 * (both files and directories, if any); if {@link SVNDepth#INFINITY},
 * resolves <code>target</code> and every conflicted file or directory
 * anywhere beneath it.
 * 
 * <p/>
 * If <code>target</code> is not in a state of conflict to begin with, does
 * nothing. If <code>target</code>'s conflict state is removed and caller's
 * {@link ISVNEntryHandler} is not <span class="javakeyword">null</span>,
 * then an {@link SVNEventAction#RESOLVED} event is dispatched to the
 * handler.
 *  
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnResolve extends SvnOperation<Void> {

    private SVNConflictChoice conflictChoice;
    private boolean resolveContents = true;
    private boolean resolveProperties = true;
    private boolean resolveTree = true;
        
    protected SvnResolve(SvnOperationFactory factory) {
        super(factory);
    }

    /**
     * Gets kind of choice object for making decision while resolving.
     * 
     * @return choice object for the operation
     * @see #setConflictChoice(SVNConflictChoice)
     */
    public SVNConflictChoice getConflictChoice() {
        return conflictChoice;
    }

    /**
     * Sets kind of choice object for making decision while resolving.
     * 
     * <p/>
     * If <code>conflictChoice</code> is {@link SVNConflictChoice#BASE},
     * resolves the conflict with the old file contents; if
     * {@link SVNConflictChoice#MINE_FULL}, uses the original working contents;
     * if {@link SVNConflictChoice#THEIRS_FULL}, the new contents; and if
     * {@link SVNConflictChoice#MERGED}, doesn't change the contents at all,
     * just removes the conflict status, which is the pre-1.2 (pre-SVN 1.5)
     * behavior.
     * <p/>
     * {@link SVNConflictChoice#THEIRS_CONFLICT} and
     * {@link SVNConflictChoice#MINE_CONFLICT} are not legal for binary files or
     * properties.
     * @param conflictChoice choice object for the operation
     */
    public void setConflictChoice(SVNConflictChoice conflictChoice) {
        this.conflictChoice = conflictChoice;
    }
    
    /**
     * Returns whether to resolve target's content conflict
     * @return <code>true</code> if content conflict of the target should be resolved, otherwise <code>false</code>
     */
    public boolean isResolveContents() {
        return resolveContents;
    }
    
    /**
     * Sets whether to resolve target's content conflict
     * @param resolveContents <code>true</code> if content conflict of the target should be resolved, otherwise <code>false</code>
     */
    public void setResolveContents(boolean resolveContents) {
        this.resolveContents = resolveContents;
    }
    
    /**
     * Returns whether to resolve target's properties conflict
     * @return <code>true</code> if properties conflict of the target should be resolved, otherwise <code>false</code>
     */
    public boolean isResolveProperties() {
        return resolveProperties;
    }

    /**
     * Sets whether to resolve target's properties conflict
     * @param resolveProperties <code>true</code> if properties conflict of the target should be resolved, otherwise <code>false</code>
     */
    public void setResolveProperties(boolean resolveProperties) {
        this.resolveProperties = resolveProperties;
    }
    
    /**
     * Returns whether to resolve any target's tree conflict
     * @return <code>true</code> if any tree conflict of the target should be resolved, otherwise <code>false</code>
     */
    public boolean isResolveTree() {
        return resolveTree;
    }

    /**
     * Sets whether to resolve any target's tree conflict
     * @param resolveTree <code>true</code> if any tree conflict of the target should be resolved, otherwise <code>false</code>
     */
    public void setResolveTree(boolean resolveTree) {
        this.resolveTree = resolveTree;
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
