package org.tmatesoft.svn.core.wc2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.DefaultSVNCommitParameters;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNExternalsHandler;
import org.tmatesoft.svn.core.wc.ISVNRepositoryPool;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.hooks.ISvnExternalsHandler;

 /**
 * Represents copy to repository operation. 
 * Copies each source in <code>sources</code> to operation's <code>target</code> destination.
 * <code>Target</code> should represent repository URL.
 * 
 * <p/>
 * If multiple <code>sources</code> are given, <code>target</code> must be a
 * directory, and <code>sources</code> will be copied as children of
 * <code>target</code>.
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
 * <code>target</code>. If <code>failWhenDstExists</code> is <code>false</code> and <code>target</code> already exists,
 * attempts to copy the item as a child of <code>target</code> If
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
 * {@link ISVNAuthenticationManager Authentication manager} (whether
 * provided directly through the appropriate constructor or in an
 * {@link ISVNRepositoryPool} instance) and {@link #getCommitHandler()
 * commit handler} are used to immediately attempt to commit the copy action
 * in the repository.
 * 
 * <p/>
 * If the caller's {@link ISVNEventHandler} is non-<code>null</code>, invokes it for each item added at the
 * new location.
 * 
 * <p/>
 * When performing a wc-to-url copy (tagging|branching from a working copy)
 * it's possible to fix revisions of external working copies (if any) which
 * are located within the working copy being copied. For example, imagine
 * you have a working copy and on one of its subdirectories you set an 
 * <code>"svn:externals"</code> property which does not contain
 * a revision number. Suppose you have made a tag from your working copy and
 * in some period of time a user checks out that tag. It could have happened
 * that the external project has evolved since the tag creation moment and
 * the tag version is no more compatible with it. So, the user has a broken
 * project since it will not compile because of the API incompatibility
 * between the two versions of the external project: the HEAD one and the
 * one existed in the moment of the tag creation. That is why it appears
 * useful to fix externals revisions during a wc-to-url copy. To enable
 * externals revision fixing a user should implement
 * {@link ISVNExternalsHandler}. The user's implementation
 * {@link ISVNExternalsHandler#handleExternal(File,SVNURL,SVNRevision,SVNRevision,String,SVNRevision)}
 * method will be called on every external that will be met in the working
 * copy. If the user's implementation returns non-<code>null</code> 
 * external revision, it's compared with the
 * revisions fetched from the external definition. If they are different,
 * the user's revision will be written in the external definition of the
 * tag. Otherwise if the returned revision is equal to the revision from the
 * external definition or if the user's implementation returns <code>null</code> 
 * for that external, it will be skipped (i.e. left as is, unprocessed).
 * 
 * <p/>
 * Note: this routine requires repository access.
 * 
 * {@link #run()} returns {@link SVNCommitInfo} commit information information about the new committed revision.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnRemoteCopy extends AbstractSvnCommit {
    
    private boolean move;
    private boolean makeParents;
    private boolean failWhenDstExists;
    private ISvnExternalsHandler externalsHandler;
    private ISvnCommitParameters commitParameters;
    private boolean disableLocalModifications;

    private Collection<SvnCopySource> sources;
    
    protected SvnRemoteCopy(SvnOperationFactory factory) {
        super(factory);
        sources = new ArrayList<SvnCopySource>();
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
     * @param move <code>true</code> if move operation should be done, otherwise <code>false</code>
     */
    public void setMove(boolean move) {
        this.move = move;
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
    * @param makeParents <code>true</code> if non-existent parent directories should be created, otherwise <code>false</code>
    */
    public void setMakeParents(boolean makeParents) {
        this.makeParents = makeParents;
    }

    /**
     * Sets whether to disable local modifications.
     * 
     * @return <code>true</code> if local modifications are disabled, otherwise <code>false</code>
     * @see #setDisableLocalModifications(boolean)
     */
    public boolean isDisableLocalModifications() {
        return disableLocalModifications;
    }

    /**
     * Sets whether to disable local modifications.
     * If <code>true</code> and any local modification is found, 
     * {@link #run()} method throws {@link SVNException} exception with {@link SVNErrorCode#ILLEGAL_TARGET} code.
     * 
     * @param disableLocalModifications <code>true</code> if local modifications are disabled, otherwise <code>false</code>
     */
    public void setDisableLocalModifications(boolean disableLocalModifications) {
        this.disableLocalModifications = disableLocalModifications;
    }

    /**
     * Returns all operation's sources.
     * 
     * @return sources of the operation
     * @see SvnRemoteCopy
     */
    public Collection<SvnCopySource> getSources() {
        return sources;
    }

    /**
     * And one source to the operation's sources.
     * 
     * @param source source of the operation
     * @see SvnRemoteCopy
     */
    public void addCopySource(SvnCopySource source) {
        if (source != null) {
            this.sources.add(source);
        }
    }

    /**
     * Gets whether to fail if <code>target</code> already exists.
     * 
     * @return <code>true</code> if fail when <code>target</code> already exists, otherwise <code>false</code>
     * @see SvnRemoteCopy
     */
    public boolean isFailWhenDstExists() {
        return failWhenDstExists;
    }

    /**
     * Sets whether to fail if <code>target</code> already exists.
     * 
     * @param failWhenDstExists <code>true</code> if fail when <code>target</code> already exists, otherwise <code>false</code>
     * @see SvnRemoteCopy
     */
    public void setFailWhenDstExists(boolean failWhenDstExists) {
        this.failWhenDstExists = failWhenDstExists;
    }

    /**
     * Runs copy operation.
     * 
     * @return {@link SVNCommitInfo} commit information information about the new committed revision. 
     */
    @Override
    public SVNCommitInfo run() throws SVNException {
        return super.run();
    }

    /**
     * Gets operation's externals handler.
     * 
     * @return externals handler of the operation
     * @see SvnRemoteCopy
     */
    public ISvnExternalsHandler getExternalsHandler() {
        return externalsHandler;
    }

    /**
     * Sets operation's externals handler.
     * 
     * @param externalsHandler externals handler of the operation
     * @see SvnRemoteCopy
     */
    public void setExternalsHandler(ISvnExternalsHandler externalsHandler) {
        this.externalsHandler = externalsHandler;
    }

    /**
     * Returns operation's parameters of the commit.
     * If no user parameters were previously specified, once creates and returns
     * {@link DefaultSVNCommitParameters default} ones.
     * 
     * @return commit parameters of the operation
     * @see ISvnCommitParameters
     */
    public ISvnCommitParameters getCommitParameters() {
        return commitParameters;
    }

    /**
     * Sets operation's parameters of the commit.
     * When no parameters are set {@link DefaultSVNCommitParameters default}ones
     * are used.
     * 
     * @param commitParameters commit parameters of the operation
     * @see ISvnCommitParameters
     */
    public void setCommitParameters(ISvnCommitParameters commitParameters) {
        this.commitParameters = commitParameters;
    }

     /**
      * Gets whether the operation changes working copy
      * @return <code>true</code> if the operation changes the working copy, otherwise <code>false</code>
      */
     @Override
     public boolean isChangesWorkingCopy() {
         return false;
     }

     @Override
     protected File getOperationalWorkingCopy() {
         SvnCopySource firstSource = getSources().iterator().next();
         if (firstSource.getSource().isLocal()) {
             return firstSource.getSource().getFile();
         }
         return null;
     }
}