package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.wc2.hooks.ISvnCommitHandler;

/**
 * Base class for operations that change repository. 
 * The <b>AbstractSvnCommit</b> class provides methods to perform operations that
 * relate to committing changes to an SVN repository. These operations are
 * similar to respective commands of the native SVN command line client and
 * include ones which operate on working copy items as well as ones that operate
 * only on a repository: commit, import, remote copy, remote delete, remote make directory, remote set property.
 * 
 * <p/>
 * {@link #run()} method returns {@link SVNCommitInfo} information on a new revision as the result of the commit.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 * @see SvnCommit
 * @see SvnImport
 * @see SvnRemoteCopy
 * @see SvnRemoteDelete
 * @see SvnRemoteMkDir
 * @see SvnRemoteSetProperty
 */
public abstract class AbstractSvnCommit extends SvnReceivingOperation<SVNCommitInfo> {

    private String commitMessage;
    private SVNProperties revisionProperties;
    private ISvnCommitHandler commitHandler;

    protected AbstractSvnCommit(SvnOperationFactory factory) {
        super(factory);
        setRevisionProperties(new SVNProperties());
    }
    
    /**
     * Gets custom revision properties for the operation.
     * If non-<code>null</code>, <code>revisionProperties</code> 
     * holds additional, custom revision properties (<code>String</code> names
     * mapped to {@link SVNPropertyValue} values) to be set on the new revision.
     * This table cannot contain any standard Subversion properties.
     * 
     * @return custom revision properties
     */
    public SVNProperties getRevisionProperties() {
        return revisionProperties;
    }

    /**
     * Sets custom revision properties for the operation.
     * If non-<code>null</code>, <code>revisionProperties</code> 
     * holds additional, custom revision properties (<code>String</code> names
     * mapped to {@link SVNPropertyValue} values) to be set on the new revision.
     * This table cannot contain any standard Subversion properties.
     * 
     * @param revisionProperties custom revision properties
     */
    public void setRevisionProperties(SVNProperties revisionProperties) {
        this.revisionProperties = revisionProperties;
    }

    /**
     * Gets commit log message.
     * 
     * @return commit log message
     */
    public String getCommitMessage() {
        return commitMessage;
    }

    /**
     * Sets commit log message.
     * 
     * @param commitMessage commit log message
     */
    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    /**
     * Adds custom revision properties for the operation.
     * See {@link #setRevisionProperties(SVNProperties)}
     * 
     * @param name name of custom revision property
     * @param value value of custom revision property
     */
    public void setRevisionProperty(String name, SVNPropertyValue value) {
        if (value != null) {
            getRevisionProperties().put(name, value);
        } else {
            getRevisionProperties().remove(name);
        }
    }

    /**
     * Gets the commit handler for the operation.
     * 
     * @return commit handler 
     */
    public ISvnCommitHandler getCommitHandler() {
        if (commitHandler == null) {
            commitHandler = new ISvnCommitHandler() {                
                public SVNProperties getRevisionProperties(String message, SvnCommitItem[] commitables, SVNProperties revisionProperties) throws SVNException {
                    return revisionProperties == null ? new SVNProperties() : revisionProperties;
                }                
                public String getCommitMessage(String message, SvnCommitItem[] commitables) throws SVNException {
                    return message == null ? "" : message;
                }
            };
        }
        return commitHandler;
    }

    /**
     * Sets the commit handler for the operation.
     * 
     * @param commitHandler commit handler
     */
    public void setCommitHandler(ISvnCommitHandler commitHandler) {
        this.commitHandler = commitHandler;
    }

}
