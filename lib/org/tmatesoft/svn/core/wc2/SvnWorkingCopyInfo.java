package org.tmatesoft.svn.core.wc2;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNConflictDescription;

/**
 * Provides information about working copy info, used by {@link SvnInfo}.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 * @see SvnInfo
 * @see SvnGetInfo
 */
public class SvnWorkingCopyInfo {
    
    private File path;
    
    private SvnSchedule schedule;
    private SVNURL copyFromUrl;
    private long copyFromRevision;
    private SvnChecksum checksum;
    private String changelist;    
    private SVNDepth depth;
    
    private long recordedSize;
    private long recordedTime;
    
    private Collection<SVNConflictDescription> conflicts;
    
    private File wcRoot;
    
    /**
     * Returns working copy path.
     * 
     * @return path
     */
    public File getPath() {
        return path;
    }

    /**
     * Returns {@link SvnSchedule} attribute, <code>null</null> if not scheduled. 
     * 
     * @return schedule of the item
     */
    public SvnSchedule getSchedule() {
        return schedule;
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
     * Returns checksum of the file, <code>null</null> for directory. 
     * 
     * @return checksum of the file
     */
    public SvnChecksum getChecksum() {
        return checksum;
    }

    /**
     * Returns changelist name assigned to item, <code>null</null> if item is not in changelist.
     *  
     * @return changelist name
     */
    public String getChangelist() {
        return changelist;
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
     * Returns last changed file size.
     * 
     * @return file size
     */
    public long getRecordedSize() {
        return recordedSize;
    }

    /**
     * Return last changed item's time.
     * 
     * @return changed time of the item
     */
    public long getRecordedTime() {
        return recordedTime;
    }

    /**
     * Returns all item's conflicts.
     * 
     * @return conflicts of the item
     */
    public Collection<SVNConflictDescription> getConflicts() {
        if (conflicts == null) {
            return Collections.emptyList();
        }
        return conflicts;
    }

    /**
     * Return item's working copy root.
     * 
     * @return working copy root of the item
     */
    public File getWcRoot() {
        return wcRoot;
    }

    /**
     * Sets working copy path.
     * 
     * @param path path
     */
    public void setPath(File path) {
        this.path = path;
    }

    /**
     * Sets {@link SvnSchedule} attribute. 
     * 
     * @param schedule schedule of the item
     */
    public void setSchedule(SvnSchedule schedule) {
        this.schedule = schedule;
    }

    /**
     * Sets the URL (repository location) of the ancestor from which the item
     * was copied. That is when the item is added with history.
     *
     * @param copyFromURL the item ancestor's URL
     */
    public void setCopyFromUrl(SVNURL copyFromURL) {
        this.copyFromUrl = copyFromURL;
    }

    /**
     * Sets checksum of the file. 
     * 
     * @param checksum checksum of the file
     */
    public void setChecksum(SvnChecksum checksum) {
        this.checksum = checksum;
    }

    /**
     * Sets changelist name assigned to item.
     *  
     * @param changelist changelist name
     */
    public void setChangelist(String changelist) {
        this.changelist = changelist;
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
     * Sets last changed file size.
     * 
     * @param recordedSize file size
     */
    public void setRecordedSize(long recordedSize) {
        this.recordedSize = recordedSize;
    }

    /**
     * Sets last changed item's time.
     * 
     * @param recordedTime changed time of the item
     */
    public void setRecordedTime(long recordedTime) {
        this.recordedTime = recordedTime;
    }

    /**
     * Sets the item's conflicts.
     * 
     * @param conflicts conflicts of the item
     */
    public void setConflicts(Collection<SVNConflictDescription> conflicts) {
        this.conflicts = conflicts;
    }

    /**
     * Sets item's working copy root.
     * 
     * @param wcRoot working copy root of the item
     */
    public void setWcRoot(File wcRoot) {
        this.wcRoot = wcRoot;
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
     * Sets the revision of the item's ancestor from which the item was copied
     * (the item is added with history).
     *
     * @param copyFromRevision the ancestor's revision
     */
    public void setCopyFromRevision(long copyFromRevision) {
        this.copyFromRevision = copyFromRevision;
    }
}
