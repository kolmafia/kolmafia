package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNDate;

/**
 * Represents information details for versioned item's (located either
 * in a working copy or a repository). When running an 
 * {@link SvnInfo} operation all collected item information data is 
 * packed inside an <b>SvnInfo</b> object.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 * @see SvnGetInfo
 */
public class SvnInfo extends SvnObject {
    
    private SVNURL url;
    private long revision;
    private SVNURL repositoryRootURL;
    private String repositoryUuid;
    
    private SVNNodeKind kind;
    private long size;
    
    private long lastChangedRevision;
    private SVNDate lastChangedDate;
    private String lastChangedAuthor;
    
    private SVNLock lock;
    
    private SvnWorkingCopyInfo wcInfo;

    /**
     * Gets the item's URL - its repository location.
     * 
     * @return the item's URL
     */
    public SVNURL getUrl() {
        return url;
    }

    /**
     * Gets the item's revision.
     * 
     * @return the item's revision
     */
    public long getRevision() {
        return revision;
    }

    /**
     * Gets the repository root url (where the repository itself
     * is installed). Applicable only for remote info operation invocations 
     * (for items in a repository).
     * 
     * @return the repository's root URL
     */
    public SVNURL getRepositoryRootUrl() {
        return repositoryRootURL;
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
     * Gets the item's node kind. Used to find out whether the item is
     * a file, directory, etc. 
     * 
     * @return the item's node kind
     */
    public SVNNodeKind getKind() {
        return kind;
    }

    /**
     * Gets the file size.
     * 
     * @return size of file
     */
    public long getSize() {
        return size;
    }

    /**
     * Gets the item's last changed revision.  
     * 
     * @return the item's last changed revision.
     */
    public long getLastChangedRevision() {
        return lastChangedRevision;
    }

    /**
     * Gets the item's last changed date. 
     *
     * @return a repository last changed date
     */
    public SVNDate getLastChangedDate() {
        return lastChangedDate;
    }

    /**
     * Gets the item's last changed author. 
     *
     * @return a repository last changed author
     */
    public String getLastChangedAuthor() {
        return lastChangedAuthor;
    }

    /**
     * Gets the file item's lock. Used to get lock information - lock 
     * token, comment, etc. 
     * 
     * @return the file item's lock.
     */
    public SVNLock getLock() {
        return lock;
    }

    /**
     * Gets working copy info.
     * 
     * @return working copy info
     * @see SvnWorkingCopyInfo
     */
    public SvnWorkingCopyInfo getWcInfo() {
        return wcInfo;
    }

    /**
     * Sets the item's URL - its repository location.
     * 
     * @param url the item's URL
     */
    public void setUrl(SVNURL url) {
        this.url = url;
    }

    /**
     * Sets the item's revision.
     * 
     * @param revision the item's revision
     */
    public void setRevision(long revision) {
        this.revision = revision;
    }

    /**
     * Sets the repository root url (where the repository itself
     * is installed). Applicable only for remote info operation invocations 
     * (for items in a repository).
     * 
     * @param repositoryRootURL the repository's root URL
     */
    public void setRepositoryRootURL(SVNURL repositoryRootURL) {
        this.repositoryRootURL = repositoryRootURL;
    }

    /**
     * Sets the repository Universal Unique IDentifier (UUID). 
     * 
     * @param repositoryUUID the repository UUID
     */
    public void setRepositoryUuid(String repositoryUUID) {
        this.repositoryUuid = repositoryUUID;
    }

    /**
     * Sets the item's node kind. Used to find out whether the item is
     * a file, directory, etc. 
     * 
     * @param kind the item's node kind
     */
    public void setKind(SVNNodeKind kind) {
        this.kind = kind;
    }

    /**
     * Sets the file size.
     * 
     * @param size size of file
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Sets the item's last changed revision.  
     * 
     * @param lastChangedRevision the item's last changed revision.
     */
    public void setLastChangedRevision(long lastChangedRevision) {
        this.lastChangedRevision = lastChangedRevision;
    }

    /**
     * Sets the item's last changed date. 
     *
     * @param lastChangedDate a repository last changed date
     */
    public void setLastChangedDate(SVNDate lastChangedDate) {
        this.lastChangedDate = lastChangedDate;
    }

    /**
     * Sets the item's last changed author. 
     *
     * @param lastChangedAuthor a repository last changed author
     */
    public void setLastChangedAuthor(String lastChangedAuthor) {
        this.lastChangedAuthor = lastChangedAuthor;
    }

    /**
     * Sets the file item's lock. Used to get lock information - lock 
     * token, comment, etc. 
     * 
     * @param lock the file item's lock.
     */
    public void setLock(SVNLock lock) {
        this.lock = lock;
    }

    /**
     * Sets working copy info.
     * 
     * @param wcInfo working copy info
     * @see SvnWorkingCopyInfo
     */
    public void setWcInfo(SvnWorkingCopyInfo wcInfo) {
        this.wcInfo = wcInfo;
    }
}
