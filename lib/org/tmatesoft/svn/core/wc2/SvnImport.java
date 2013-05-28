package org.tmatesoft.svn.core.wc2;

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;

/**
 * Imports file or directory <code>source</code> into repository directory
 * defined in operation's <code>target</code> at HEAD revision. If some components of
 * operations' <code>target</code> does not exist, then creates parent directories as
 * necessary. The <code>target</code> of the operation should represent URL.
 * 
 * <p/>
 * If the caller's {@link ISVNEventHandler event handler} is not <span
 * class="javakeyword">null</span> it will be called as the import
 * progresses with {@link SVNEventAction#COMMIT_ADDED} action. If the commit
 * succeeds, the handler will be called with
 * {@link SVNEventAction#COMMIT_COMPLETED} event action.
 * 
 * <p/>
 * If non-<code>null</code>,
 * <code>revisionProperties</code> holds additional, custom revision
 * properties (<code>String</code> names mapped to {@link SVNPropertyValue}
 * values) to be set on the new revision. This table cannot contain any
 * standard Subversion properties.
 * 
 * <p/>
 * <code>commitHandler</code> will be asked for a commit log message.
 * 
 * <p/>
 * If <code>depth</code> is {@link SVNDepth#EMPTY}, imports just
 * <code>source</code> and nothing below it. If {@link SVNDepth#FILES},
 * imports <code>source</code> and any file children of <code>source</code>. If
 * {@link SVNDepth#IMMEDIATES}, imports <code>source</code>, any file
 * children, and any immediate subdirectories (but nothing underneath those
 * subdirectories). If {@link SVNDepth#INFINITY}, imports <code>source</code>
 * and everything under it fully recursively.
 *             
 * <p/>
 * {@link #run()} method returns {@link org.tmatesoft.svn.core.SVNCommitInfo} information about the new committed revision.
 * This method throws SVNException in the following cases:
 *             <ul>
 *             <li>exception with {@link SVNErrorCode#ENTRY_NOT_FOUND}
 *             error code - if <code>source</code> does not exist</li> 
 *             <li>exception with {@link SVNErrorCode#ENTRY_EXISTS} error code -
 *             if operation's <code>target</code> already exists and  <code>source</code> is a file</li> 
 *             <li>exception with {@link SVNErrorCode#CL_ADM_DIR_RESERVED} error code - if
 *             trying to import an item with a reserved SVN name (like
 *             <code>'.svn'</code> or <code>'_svn'</code>)</li>
 *             </ul>           
 *            
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnImport extends AbstractSvnCommit {

    private boolean applyAutoProperties;
    private boolean useGlobalIgnores;
    private boolean force;
    
    private File source;
    
    /**
     * Gets whether to enable automatic properties
     * 
     * @return <code>true</code> if automatic properties should be enabled, otherwise <code>false</code>
     */
    
    public boolean isApplyAutoProperties() {
        return applyAutoProperties;
    }

    
    /**
     * Sets whether to enable automatic properties
     * 
     * @param applyAutoProperties <code>true</code> if automatic properties should be enabled, otherwise <code>false</code>
     */
    public void setApplyAutoProperties(boolean applyAutoProperties) {
        this.applyAutoProperties = applyAutoProperties;
    }

   
    /**
     * Returns import operation's source. 
     * 
     * @return return source of the import operation
     * @see #getSource()
     */
    public File getSource() {
        return source;
    }

    /**
     * Sets source of the import. If <code>source</code> is a directory, the contents of that directory are
     * imported directly into the directory identified by <code>target</code>.
     * Note that the directory itself is not imported -- that
     * is, the base name of directory is not part of the import.
     * 
     * <p/>
     * If <code>source</code> is a file, then the parent of operation's <code>target</code> is
     * the directory receiving the import. The base name of <code>source</code>
     * is the filename in the repository. In this case if this filename already exists, throws {@link SVNException}.
     * 
     * @param source source of the import operation
     */
    public void setSource(File source) {
        this.source = source;
    }

    /**
     * Gets whether to ignore files of unknown node types or not.
     * 
     * @return <code>true</code> if files of unknown node types should be ignored, otherwise <code>false</code>
     * @see #setForce(boolean)
     */
    public boolean isForce() {
        return force;
    }

    /**
     * Sets whether to ignore files of unknown node types or not.
     * Unversionable items such as device files and pipes are ignored if <code>force</code> is <code>true</code>.
     * 
     * @param force <code>true</code> if files of unknown node types should be ignored, otherwise <code>false</code>
     */
    public void setForce(boolean force) {
        this.force = force;
    }

    protected SvnImport(SvnOperationFactory factory) {
        super(factory);
    }

    /**
     * Gets whether to adds files or directories that match ignore patterns.
     * @return <code>true</code> adds files or directories that match ignore patterns, otherwise <code>false</code>
     */
    public boolean isUseGlobalIgnores() {
        return useGlobalIgnores;
    }

    /**
    * Sets whether to adds files or directories that match ignore patterns.
    * @param useGlobalIgnores <code>true</code> adds files or directories that match ignore patterns, otherwise <code>false</code>
    */
    public void setUseGlobalIgnores(boolean useGlobalIgnores) {
        this.useGlobalIgnores = useGlobalIgnores;
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        if (getDepth() == null || getDepth() == SVNDepth.UNKNOWN) {
            setDepth(SVNDepth.INFINITY);
        }
        super.ensureArgumentsAreValid();
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
