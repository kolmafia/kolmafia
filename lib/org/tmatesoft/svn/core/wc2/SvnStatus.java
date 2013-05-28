package org.tmatesoft.svn.core.wc2;

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.wc.SVNStatusType;

/**
 * Provides detailed status information for
 * a working copy item as a result of a status operation invoked by a {@link SvnGetStatus} operation. 
 *
 * @author TMate Software Ltd.
 * @version 1.7
 * @see SvnGetStatus
 */
public class SvnStatus extends SvnObject {

    private SVNNodeKind kind;
    private File path;
    private long fileSize;
    private boolean versioned;
    private boolean conflicted;
    
    private SVNStatusType nodeStatus;
    private SVNStatusType textStatus;
    private SVNStatusType propertiesStatus;
    
    private boolean wcLocked;
    private boolean copied;
    private SVNURL copyFromUrl;
    private long copyFromRevision;
    
    private SVNURL repositoryRootUrl;
    private String repositoryUuid;
    
    private String repositoryRelativePath;
    
    private long revision;
    private long changedRevision;
    private SVNDate changedDate;
    private String changedAuthor;
    
    private boolean switched;
    private boolean fileExternal;
    
    private SVNLock lock;
    private String changelist;
    private SVNDepth depth;
    
    private SVNNodeKind repositoryKind;
    private SVNStatusType repositoryNodeStatus;
    private SVNStatusType repositoryTextStatus;
    private SVNStatusType repositoryPropertiesStatus;    
    private SVNLock repositoryLock;
    
    private long repositoryChangedRevision;
    private SVNDate repositoryChangedDate;
    private String repositoryChangedAuthor;

    private int workingCopyFormat = ISVNWCDb.WC_FORMAT_17;
    
    /**
     * Gets the item's node kind characterizing it as an entry.
     *
     * @return the item's node kind (whether it's a file, directory, etc.)
     */
    public SVNNodeKind getKind() {
        return kind;
    }
    
    /**
     * Gets the item's path in the filesystem.
     *
     * @return a File representation of the item's path
     */
    public File getPath() {
        return path;
    }
    
    /**
     * Returns the item's file size.
     * 
     * @return the file size of the item
     */
    public long getFileSize() {
        return fileSize;
    }
    
    /**
     * Returns whether the item is versioned.
     * 
     * @return <code>true</code> if the item is versioned, otherwise <code>false</code>
     */
    public boolean isVersioned() {
        return versioned;
    }
    
    /**
     * Returns whether the item is in conflict state.
     * 
     * @return <code>true</code> if the item is in conflict state, otherwise <code>false</code>
     */
    public boolean isConflicted() {
        return conflicted;
    }
    
    /**
     * Returns the item's node status.
     * 
     * @return the node status of the item
     */
    public SVNStatusType getNodeStatus() {
        return nodeStatus;
    }
        
    /**
     * Gets the working copy local item's contents status type.
     *
     * @return the local contents status type
     */
    public SVNStatusType getTextStatus() {
        return textStatus;
    }
    
    /**
     * Gets the working copy local item's properties status type.
     *
     * @return the local properties status type
     */
    public SVNStatusType getPropertiesStatus() {
        return propertiesStatus;
    }
    
    /**
     * Finds out if the item is locked (not a user lock but a driver's one when
     * during an operation a working copy is locked in <i>.svn</i>
     * administrative areas to prevent from other operations interrupting until
     * the running one finishes).
     * <p>
     * To clean up a working copy use {@link SvnCleanup}.
     *
     * @return <code>true</code> if locked, otherwise <code>false</code>
     * @see SvnCleanup
     */
    public boolean isWcLocked() {
        return wcLocked;
    }
        
    /**
     * Finds out if the item is added with history.
     *
     * @return <code>true</code> if the item is added with history, otherwise <code>false</code>
     */
    public boolean isCopied() {
        return copied;
    }
    
    /**
     * Returns the item's repository root URL.
     * 
     * @return the repository root URL of the item
     */
    public SVNURL getRepositoryRootUrl() {
        return repositoryRootUrl;
    }
    
    /**
     * Gets the repository Universal Unique IDentifier (UUID). 
     * 
     * @return the repository UUID
     */
    public String getRepositoryUuid() {
        return repositoryUuid;
    }
    
    /**
     * Returns the item's repository relative path.
     * 
     * @return the repository relative path of the item
     */
    public String getRepositoryRelativePath() {
        return repositoryRelativePath;
    }
    
    /**
     * Gets the item's current working revision.
     *
     * @return the item's working revision
     */
    public long getRevision() {
        return revision;
    }
    
    /**
     * Gets the revision when the item was last changed (committed).
     *
     * @return the last committed revision
     */
    public long getChangedRevision() {
        return changedRevision;
    }
    
    /**
     * Gets the timestamp when the item was last changed (committed).
     *
     * @return the last committed date
     */
    public SVNDate getChangedDate() {
        return changedDate;
    }
    
    /**
     * Gets the author who last changed the item.
     *
     * @return the item's last commit author
     */
    public String getChangedAuthor() {
        return changedAuthor;
    }
        
    /**
     * Returns whether the item is switched to a different repository location.
     * 
     * @return <code>true</code> if item is switched to a different repository location, otherwise <code>false</code>
     */
    public boolean isSwitched() {
        return switched;
    }
    
    /**
     * Returns whether the item is the external file.
     * 
     * @return <code>true</code> if is the external file, otherwise <code>false</code>
     */
    public boolean isFileExternal() {
        return fileExternal;
    }
    
    /**
     * Gets the file item's local lock.
     *
     * @return file item's local lock
     */
    public SVNLock getLock() {
        return lock;
    }
    
    /**
     * Returns the local item's depth.
     * 
     * @return the local depth of the item
     */
    public SVNDepth getDepth() {
        return depth;
    }
    
    /**
     * Returns the kind of the item got from the repository. Relevant for a
     * remote status invocation.
     *
     * @return a repository item kind
     */
    public SVNNodeKind getRepositoryKind() {
        return repositoryKind;
    }
    
    /**
     * Returns the node status of the item got from the repository. Relevant for a
     * remote status invocation.
     *
     * @return a repository node status
     */
    public SVNStatusType getRepositoryNodeStatus() {
        return repositoryNodeStatus;
    }
    
    /**
     * Gets the working copy item's contents status type against the repository
     * - that is comparing the item's BASE revision and the latest one in the
     * repository when the item was changed. Applicable for a remote status
     * invocation.
     *
     * <p>
     * If the repository contents status type != {@link SVNStatusType#STATUS_NONE}
     * the local file may be out of date.
     *
     * @return the repository contents status type
     */
    public SVNStatusType getRepositoryTextStatus() {
        return repositoryTextStatus;
    }
    
    /**
     * Gets the working copy item's properties status type against the
     * repository - that is comparing the item's BASE revision and the latest
     * one in the repository when the item was changed. Applicable for a remote
     * status invocation.
     *
     * <p>
     * If the repository properties status type != {@link SVNStatusType#STATUS_NONE}
     * the local file may be out of date.
     *
     * @return the repository properties status type
     */
    public SVNStatusType getRepositoryPropertiesStatus() {
        return repositoryPropertiesStatus;
    }
    
    /**
     * Gets the file item's repository lock.
     *
     * @return file item's repository lock
     */
    public SVNLock getRepositoryLock() {
        return repositoryLock;
    }
    
    /**
     * Gets the item's last revision. 
     *
     * @return a repository last revision
     */
    public long getRepositoryChangedRevision() {
        return repositoryChangedRevision;
    }
    
    /**
     * Gets the item's last changed date. 
     *
     * @return a repository last changed date
     */
    public SVNDate getRepositoryChangedDate() {
        return repositoryChangedDate;
    }
    
    /**
     * Gets the item's last changed author. 
     *
     * @return a last commit author
     */
    public String getRepositoryChangedAuthor() {
        return repositoryChangedAuthor;
    }
    
    /**
     * Sets item's node kind.
     * 
     * @param kind node kind of the item
     */
    public void setKind(SVNNodeKind kind) {
        this.kind = kind;
    }
    
    /**
     * Sets item's working copy path.
     * 
     * @param path working copy path of the item
     */
    public void setPath(File path) {
        this.path = path;
    }
    
    /**
     * Sets the item's file size.
     * 
     * @param fileSize the file size of the item
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    /**
     * Sets whether the item is versioned.
     * 
     * @param versioned <code>true</code> if the item is versioned, otherwise <code>false</code>
     */
    public void setVersioned(boolean versioned) {
        this.versioned = versioned;
    }
    
    /**
     * Sets whether the item is in conflict state.
     * 
     * @param conflicted <code>true</code> if the item is in conflict state, otherwise <code>false</code>
     */
    public void setConflicted(boolean conflicted) {
        this.conflicted = conflicted;
    }
    
    /**
     * Sets the item's   status.
     * 
     * @param nodeStatus the node status of the item
     */
    public void setNodeStatus(SVNStatusType nodeStatus) {
        this.nodeStatus = nodeStatus;
    }
    
    /**
     * Sets the item's contents status type. 
     *
     * @param textStatus
     *            status type of the item's contents
     */
    public void setTextStatus(SVNStatusType textStatus) {
        this.textStatus = textStatus;
    }
    
    /**
     * Sets the item's properties status type. 
     *
     * @param propertiesStatus
     *            status type of the item's properties
     */
    public void setPropertiesStatus(SVNStatusType propertiesStatus) {
        this.propertiesStatus = propertiesStatus;
    }
    
    /**
     * Sets if the item is locked (not a user lock but a driver's one when
     * during an operation a working copy is locked in <i>.svn</i>
     * administrative areas to prevent from other operations interrupting until
     * the running one finishes).
     *
     * @param wcLocked <code>true</code> if locked, otherwise <code>false</code>
     */
    public void setWcLocked(boolean wcLocked) {
        this.wcLocked = wcLocked;
    }
    
    /**
     * Sets whether the item is in conflict state.
     * 
     * @param copied <code>true</code> if the item is in conflict state, otherwise <code>false</code>
     */
    public void setCopied(boolean copied) {
        this.copied = copied;
    }
    
    /**
     * Sets the item's repository root URL.
     * 
     * @param repositoryRootUrl the repository root URL of the item
     */
    public void setRepositoryRootUrl(SVNURL repositoryRootUrl) {
        this.repositoryRootUrl = repositoryRootUrl;
    }
    
    /**
     * Sets the repository Universal Unique IDentifier (UUID). 
     * 
     * @param repositoryUuid the repository UUID
     */
    public void setRepositoryUuid(String repositoryUuid) {
        this.repositoryUuid = repositoryUuid;
    }
    
    /**
     * Sets the item's repository relative path.
     * 
     * @param repositoryRelativePath the relative path of the item
     */
    public void setRepositoryRelativePath(String repositoryRelativePath) {
        this.repositoryRelativePath = repositoryRelativePath;
    }
    
    /**
     * Sets the item's current working revision.
     *
     * @param revision the item's working revision
     */
    public void setRevision(long revision) {
        this.revision = revision;
    }
    
    /**
     * Sets the revision when the item was last changed (committed).
     *
     * @param changedRevision the last committed revision
     */
    public void setChangedRevision(long changedRevision) {
        this.changedRevision = changedRevision;
    }
    
    /**
     * Sets the timestamp when the item was last changed (committed).
     *
     * @param changedDate the last committed date
     */
    public void setChangedDate(SVNDate changedDate) {
        this.changedDate = changedDate;
    }
    
    /**
     * Sets the author who last changed the item.
     *
     * @param changedAuthor the item's last commit author
     */
    public void setChangedAuthor(String changedAuthor) {
        this.changedAuthor = changedAuthor;
    }
    
    /**
     * Sets whether the item is switched to a different repository location.
     * 
     * @param switched <code>true</code> if item is switched to a different repository location, otherwise <code>false</code>
     */
    public void setSwitched(boolean switched) {
        this.switched = switched;
    }
    
    /**
     * Sets whether the item is the external file.
     * 
     * @param fileExternal <code>true</code> if is the external file, otherwise <code>false</code>
     */
    public void setFileExternal(boolean fileExternal) {
        this.fileExternal = fileExternal;
    }
    
    /**
     * Sets the file item's local lock.
     *
     * @param lock file item's local lock
     */
    public void setLock(SVNLock lock) {
        this.lock = lock;
    }
    
    /**
     * Sets the local item's depth.
     * 
     * @param depth the local depth of the item
     */
    public void setDepth(SVNDepth depth) {
        this.depth = depth;
    }
    
    /**
     * Sets the kind of the item got from the repository. 
     *
     * @param repositoryKind a repository item kind
     */
    public void setRepositoryKind(SVNNodeKind repositoryKind) {
        this.repositoryKind = repositoryKind;
    }
    
    /**
     * Sets the node status of the item got from the repository. 
     *
     * @param repositoryNodeStatus a repository node status
     */
    public void setRepositoryNodeStatus(SVNStatusType repositoryNodeStatus) {
        this.repositoryNodeStatus = repositoryNodeStatus;
    }
    
    /**
     * Sets the working copy item's contents status type against the repository.
     *
     * @param repositoryTextStatus the repository contents status type
     */
    public void setRepositoryTextStatus(SVNStatusType repositoryTextStatus) {
        this.repositoryTextStatus = repositoryTextStatus;
    }
    
    /**
     * Sets the working copy item's properties status type against the
     * repository.
     *
     * @param repositoryPropertiesStatus the repository properties status type
     */
    public void setRepositoryPropertiesStatus(SVNStatusType repositoryPropertiesStatus) {
        this.repositoryPropertiesStatus = repositoryPropertiesStatus;
    }
    
    /**
     * Sets the file item's repository lock.
     *
     * @param repositoryLock file item's repository lock
     */
    public void setRepositoryLock(SVNLock repositoryLock) {
        this.repositoryLock = repositoryLock;
    }
    
    /**
     * Sets the item's last revision. 
     *
     * @param repositoryChangedRevision a repository last revision
     */
    public void setRepositoryChangedRevision(long repositoryChangedRevision) {
        this.repositoryChangedRevision = repositoryChangedRevision;
    }
    
    /**
     * Sets the item's last changed date. 
     *
     * @param repositoryChangedDate a repository last changed date
     */
    public void setRepositoryChangedDate(SVNDate repositoryChangedDate) {
        this.repositoryChangedDate = repositoryChangedDate;
    }
    
    /**
     * Sets the item's last changed author. 
     *
     * @param repositoryChangedAuthor a last commit author
     */
    public void setRepositoryChangedAuthor(String repositoryChangedAuthor) {
        this.repositoryChangedAuthor = repositoryChangedAuthor;
    }
    
    /**
     * Returns the name of the changelist which the working copy item, denoted
     * by this object, belongs to.
     *
     * @return changelist name
     */
    public String getChangelist() {
        return changelist;
    }
    
    /**
     * Sets the name of the changelist which the working copy item, denoted
     * by this object, belongs to.
     *
     * @param changelist name of changelist
     */
    public void setChangelist(String changelist) {
        this.changelist = changelist;
    }
    
    /**
     * Sets the URL (repository location) of the ancestor from which the item
     * was copied. That is when the item is added with history.
     *
     * @param copyFromUrl the item ancestor's URL
     */
    public void setCopyFromUrl(SVNURL copyFromUrl) {
        this.copyFromUrl = copyFromUrl;
    }
    
    /**
     * Gets the URL (repository location) of the ancestor from which the item
     * was copied. That is when the item is added with history.
     *
     * @return the item ancestor's URL
     */
    public SVNURL getCopyFromUrl() {
        return copyFromUrl;
    }
    
    /**
     * Sets the revision of the item's ancestor from which the item was copied
     * (the item is added with history).
     *
     * @param copyFromRevision the ancestor's revision
     */
    public void setCopyFromRevision(long copyFromRevision) {
        this.copyFromRevision = copyFromRevision;
    }
    
    /**
     * Gets the revision of the item's ancestor from which the item was copied
     * (the item is added with history).
     *
     * @return the ancestor's revision
     */
    public long getCopyFromRevision() {
        return copyFromRevision;
    }

    /**
     * Get the working copy format. This method is used for internal purposes only
     * @return working copy format
     */
    public int getWorkingCopyFormat() {
        return workingCopyFormat;
    }

    /**
     * Set the working copy format. This method is used for internal purposes only
     * @param workingCopyFormat working copy format
     */
    public void setWorkingCopyFormat(int workingCopyFormat) {
        this.workingCopyFormat = workingCopyFormat;
    }
}
