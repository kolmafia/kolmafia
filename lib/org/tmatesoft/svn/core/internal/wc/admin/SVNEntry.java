/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc.admin;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNTreeConflictUtil;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNTreeConflictDescription;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public abstract class SVNEntry {

    private String myName;
    private String author;
    private String[] cachableProperties;
    private String changelistName;
    private String checksum;
    private SVNDate committedDate;
    private long committedRevision;
    private String conflictNew;
    private String conflictOld;
    private String conflictWorking;
    private long copyFromRevision;
    private String copyFromURL;
    private SVNDepth depth;
    private String externalFilePath;
    private SVNRevision externalFilePegRevision;
    private SVNRevision externalFileRevision;
    private SVNNodeKind kind;
    private String lockComment;
    private SVNDate lockCreationDate;
    private String lockOwner;
    private String lockToken;
    private String[] presentProperties;
    private String propRejectFile;
    private SVNDate propTime;
    private String repositoryRoot;
    private long revision;
    private String schedule;
    private SVNDate textTime;
    private String treeConflictData;
    private String url;
    private String uuid;
    private long workingSize;
    private boolean absent;
    private boolean copied;
    private boolean deleted;
    private boolean incomplete;
    private boolean keepLocal;
    private boolean hasProperties;
    private boolean hasPropertiesModifications;
    private String parentURL;

    public abstract boolean isThisDir(); 
    
    public abstract SVNAdminArea getAdminArea();

    public String getURL() {
        if (url == null && parentURL != null) {
            return SVNPathUtil.append(parentURL, SVNEncodingUtil.uriEncode(myName));
        }
        return url;
    }

    public SVNURL getSVNURL() throws SVNException {
        String url = getURL();
        if (url != null) {
            return SVNURL.parseURIEncoded(url);
        }
        return null;
    }

    public String getName() {
        return myName;
    }

    public boolean isDirectory() {
        return kind == SVNNodeKind.DIR;
    }

    public long getRevision() {
        return revision;
    }

    public boolean isScheduledForAddition() {
        return SVNProperty.SCHEDULE_ADD.equals(schedule);
    }

    public boolean isScheduledForDeletion() {
        return SVNProperty.SCHEDULE_DELETE.equals(schedule);
    }

    public boolean isScheduledForReplacement() {
        return SVNProperty.SCHEDULE_REPLACE.equals(schedule);
    }

    public boolean isHidden() {
        return (isDeleted() && !isScheduledForAddition() && !isScheduledForReplacement()) ||
            isAbsent() || getDepth() == SVNDepth.EXCLUDE;
    }

    public boolean isFile() {
        return kind == SVNNodeKind.FILE;
    }

    public String getLockToken() {
        return lockToken;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isAbsent() {
        return absent;
    }

    public String toString() {
        return myName;
    }

    public boolean setRevision(long revision) {
        boolean changed = revision != this.revision;
        this.revision = revision;
        return changed;
    }

    public boolean setCommittedRevision(long cmtRevision) {
        boolean changed = cmtRevision != this.committedRevision;
        committedRevision = cmtRevision;
        return changed;
    }

    public boolean setAuthor(String cmtAuthor) {
        boolean changed = cmtAuthor != null ? !cmtAuthor.equals(author) : cmtAuthor != author;
        author = cmtAuthor;
        return changed;
    }

    public boolean setChangelistName(String changelistName) {
        boolean changed = changelistName != null ? !changelistName.equals(this.changelistName) : changelistName != this.changelistName;
        this.changelistName = changelistName;
        return changed;
    }

    public String getChangelistName() {
        return this.changelistName;
    }

    public boolean setWorkingSize(long size) {
        boolean changed = workingSize != size;
        workingSize = size;
        return changed;
    }

    public long getWorkingSize() {
        return workingSize;
    }

    public SVNDepth getDepth() {
        return depth;
    }

    public void setDepth(SVNDepth depth) {
        if (depth == null) {
            depth = SVNDepth.INFINITY;
        }
        this.depth = depth;
    }

    public boolean setURL(String url) {
        boolean changed = url != null ? !url.equals(this.url) : url != this.url;
        this.url = url;
        return changed;
    }

    public void setIncomplete(boolean incomplete) {
        this.incomplete = incomplete;
    }

    public boolean isIncomplete() {
        return incomplete;
    }

    public String getConflictOld() {
        return conflictOld;
    }

    public void setConflictOld(String name) {
        this.conflictOld = name;
    }

    public String getConflictNew() {        
        return conflictNew;
    }

    public void setConflictNew(String name) {
        this.conflictNew = name;
    }

    public String getConflictWorking() {
        return conflictWorking;
    }

    public void setConflictWorking(String name) {
        conflictWorking = name;
    }

    public String getPropRejectFile() {
        return propRejectFile;
    }

    public void setPropRejectFile(String name) {
        propRejectFile = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setCommittedDate(String date) {
        committedDate = date != null ? SVNDate.parseDate(date) : null;
    }

    public String getCommittedDate() {
        return committedDate != null ? committedDate.format() : null;
    }

    public long getCommittedRevision() {
        return committedRevision;
    }

    public void setTextTime(String time) {
        textTime = time != null ? SVNDate.parseDate(time) : null;
    }

    public void setKind(SVNNodeKind kind) {
        this.kind = kind;
    }

    public void setAbsent(boolean absent) {
        this.absent = absent;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public SVNNodeKind getKind() {
        return kind;
    }

    public String getTextTime() {
        return textTime != null ? textTime.format() : null;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setLockComment(String comment) {
        this.lockComment = comment;
    }

    public void setLockOwner(String owner) {
        this.lockOwner = owner;
    }

    public void setLockCreationDate(String date) {
        this.lockCreationDate = date != null ? SVNDate.parseDate(date) : null;
    }

    public void setLockToken(String token) {
        this.lockToken = token;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public void unschedule() {
        this.schedule = null;
    }

    public void scheduleForAddition() {
        schedule = SVNProperty.SCHEDULE_ADD;
    }

    public void scheduleForDeletion() {
        schedule = SVNProperty.SCHEDULE_DELETE;
    }

    public void scheduleForReplacement() {
        schedule = SVNProperty.SCHEDULE_REPLACE;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;;
    }

    public void setCopyFromRevision(long revision) {
        this.copyFromRevision = revision;
    }

    public boolean setCopyFromURL(String url) {
        boolean changed = url != null ? !url.equals(copyFromURL) : url != copyFromURL;
        copyFromURL = url;
        return changed;
    }

    public void setCopied(boolean copied) {
        this.copied = copied; 
    }

    public String getCopyFromURL() {
        return copyFromURL;
    }

    public SVNURL getCopyFromSVNURL() throws SVNException {
        String url = getCopyFromURL();
        if (url != null) {
            return SVNURL.parseURIEncoded(url);
        }
        return null;
    }

    public long getCopyFromRevision() {
        return copyFromRevision;
    }

    public String getPropTime() {
        return propTime != null ? propTime.format() : null;
    }

    public void setPropTime(String time) {
        this.propTime = time != null ? SVNDate.parseDate(time) : null;
    }

    public boolean isCopied() {
        return copied;
    }

    public String getUUID() {
        return this.uuid;
    }

    public String getRepositoryRoot() {
        return this.repositoryRoot;
    }

    public SVNURL getRepositoryRootURL() throws SVNException {
        String url = getRepositoryRoot();
        if (url != null) {
            return SVNURL.parseURIEncoded(url);
        }
        return null;
    }

    public boolean setRepositoryRoot(String url) {
        boolean changed = url != null ? !url.equals(repositoryRoot) : url != repositoryRoot;
        this.repositoryRoot = url;
        return changed;
    }

    public boolean setRepositoryRootURL(SVNURL url) {
        return setRepositoryRoot(url == null ? null : url.toString());
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public String getLockComment() {
        return lockComment;
    }

    public String getLockCreationDate() {
        return lockCreationDate != null ? lockCreationDate.format() : null;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setCachableProperties(String[] cachableProps) {
        this.cachableProperties = cachableProps;
    }

    public void setPresentProperties(String[] properties) {
        presentProperties = properties;
    }

    public void setKeepLocal(boolean keepLocal) {
        this.keepLocal = keepLocal;
    }

    public boolean isKeepLocal() {
        return keepLocal;
    }

    public String[] getCachableProperties() {
        return cachableProperties;
    }

    public String[] getPresentProperties() {
        return presentProperties;
    }

    public String getExternalFilePath() {
        return externalFilePath;
    }

    public SVNRevision getExternalFileRevision() {
        return externalFileRevision;
    }

    public SVNRevision getExternalFilePegRevision() {
        return externalFilePegRevision;
    }

    public void setExternalFilePath(String path) {
        externalFilePath = path;
    }

    public void setExternalFileRevision(SVNRevision rev) {
        externalFileRevision = rev;
    }

    public void setExternalFilePegRevision(SVNRevision pegRev) {
        externalFilePegRevision = pegRev;
    }

    public String getTreeConflictData() {
        return treeConflictData;
    }

    public abstract Map<File, SVNTreeConflictDescription> getTreeConflicts() throws SVNException;

    public void setTreeConflictData(String conflictData) {
        treeConflictData = conflictData;
    }

    public void setTreeConflicts(Map treeConflicts) throws SVNException {
        String conflictData = SVNTreeConflictUtil.getTreeConflictData(treeConflicts);
        setTreeConflictData(conflictData);
    }

    public void setHasProperties(boolean hasProps) {
        this.hasProperties = hasProps;
    }

    public void setHasPropertiesModifications(boolean hasPropsMods) {
        this.hasPropertiesModifications = hasPropsMods;
    }

    public boolean hasPropertiesModifications() {
        return hasPropertiesModifications;
    }

    public boolean hasProperties() {
        return hasProperties;
    }

    public void setParentURL(String url) {
        parentURL = url;
    }
    
    public void setName(String name) {
        myName  = name;
    }

    public void applyChanges(Map attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        for(Iterator<?> names = attributes.keySet().iterator(); names.hasNext();) {
            String name = (String) names.next();
            setAttribute(name, attributes.get(name));
        }
    }

    private void setAttribute(String name, Object value) {
        if (SVNProperty.ABSENT.equals(name)) {
            setAbsent(SVNProperty.booleanValue((String) value));
        } else if (SVNProperty.CACHABLE_PROPS.equals(name)) {
            if (value instanceof String) {
              value = SVNAdminArea.fromString((String) value, " ");
            }
            if (Arrays.equals((String[]) value, SVNAdminArea14.getCachableProperties())) {
                value = SVNAdminArea14.getCachableProperties();
            }
            setCachableProperties((String[]) value);
        } else if (SVNProperty.CHANGELIST.equals(name)) {
            setChangelistName((String) value);
        } else if (SVNProperty.CHECKSUM.equals(name)) {
            setChecksum((String) value);
        } else if (SVNProperty.COMMITTED_DATE.equals(name)) {
            setCommittedDate((String) value);
        } else if (SVNProperty.COMMITTED_REVISION.equals(name)) {
            setCommittedRevision(SVNProperty.longValue((String) value));
        } else if (SVNProperty.CONFLICT_NEW.equals(name)) {
            setConflictNew((String) value);
        } else if (SVNProperty.CONFLICT_OLD.equals(name)) {
            setConflictOld((String) value);
        } else if (SVNProperty.CONFLICT_WRK.equals(name)) {
            setConflictWorking((String) value);
        } else if (SVNProperty.COPIED.equals(name)) {
            setCopied(SVNProperty.booleanValue((String) value));
        } else if (SVNProperty.COPYFROM_REVISION.equals(name)) {
            setCopyFromRevision(SVNProperty.longValue((String) value));
        } else if (SVNProperty.COPYFROM_URL.equals(name)) {
            setCopyFromURL((String) value);
        } else if (SVNProperty.DELETED.equals(name)) {
            setDeleted(SVNProperty.booleanValue((String) value));
        } else if (SVNProperty.DEPTH.equals(name)) {
            SVNDepth depth = null;
            if (value instanceof String) {
                depth = SVNDepth.fromString((String) value);
            }
            if (depth == null) {
                depth = SVNDepth.INFINITY;
            }
            setDepth(depth);
        } else if (SVNProperty.FILE_EXTERNAL_PATH.equals(name)) {
            setExternalFilePath((String) value);
        } else if (SVNProperty.FILE_EXTERNAL_PEG_REVISION.equals(name)) {
            setExternalFilePegRevision((SVNRevision) value);
        } else if (SVNProperty.FILE_EXTERNAL_REVISION.equals(name)) {
            setExternalFileRevision((SVNRevision) value);
        } else if (SVNProperty.HAS_PROP_MODS.equals(name)) {
            setHasPropertiesModifications(SVNProperty.booleanValue((String) value));
        } else if (SVNProperty.HAS_PROPS.equals(name)) {
            setHasProperties(SVNProperty.booleanValue((String) value));
        } else if (SVNProperty.INCOMPLETE.equals(name)) {
            setIncomplete(SVNProperty.booleanValue((String) value));
        } else if (SVNProperty.KEEP_LOCAL.equals(name)) {
            setKeepLocal(SVNProperty.booleanValue((String) value));
        } else if (SVNProperty.KIND.equals(name)) {
            SVNNodeKind kind = null;
            if (value instanceof String) {
                kind = SVNNodeKind.parseKind((String) value);
            }
            setKind(kind);
        } else if (SVNProperty.LAST_AUTHOR.equals(name)) {
            setAuthor((String) value);
        } else if (SVNProperty.LOCK_COMMENT.equals(name)) {
            setLockComment((String) value);
        } else if (SVNProperty.LOCK_CREATION_DATE.equals(name)) {
            setLockCreationDate((String) value);
        } else if (SVNProperty.LOCK_OWNER.equals(name)) {
            setLockOwner((String) value);
        } else if (SVNProperty.LOCK_TOKEN.equals(name)) {
            setLockToken((String) value);
        } else if (SVNProperty.NAME.equals(name)) {
            this.myName = (String) value;
        } else if (SVNProperty.PRESENT_PROPS.equals(name)) {
            if (value instanceof String) {
                value = SVNAdminArea.fromString((String) value, " ");
            }
            setPresentProperties((String[]) value);
        } else if (SVNProperty.PROP_REJECT_FILE.equals(name)) {
            setPropRejectFile((String) value);
        } else if (SVNProperty.PROP_TIME.equals(name)) {
            setPropTime((String) value);
        } else if (SVNProperty.REPOS.equals(name)) {
            setRepositoryRoot((String) value);
        } else if (SVNProperty.REVISION.equals(name)) {
            setRevision(SVNProperty.longValue((String) value));
        } else if (SVNProperty.SCHEDULE.equals(name)) {
            setSchedule((String) value);
        } else if (SVNProperty.TEXT_TIME.equals(name)) {
            setTextTime((String) value);
        } else if (SVNProperty.TREE_CONFLICT_DATA.equals(name)) {
            setTreeConflictData((String) value);
        } else if (SVNProperty.URL.equals(name)) {
            setURL((String) value);
        } else if (SVNProperty.UUID.equals(name)) {
            setUUID((String) value);
        } else if (SVNProperty.WORKING_SIZE.equals(name)) {            
            setWorkingSize(SVNProperty.longValue((String) value));
        }
    }

    public Map asMap() {
        Map map = new SVNHashMap();
        if (isAbsent()) {
            map.put(SVNProperty.ABSENT, Boolean.TRUE.toString());
        }
        if (getCachableProperties() != null) {
            map.put(SVNProperty.CACHABLE_PROPS, getCachableProperties());
        }
        if (getChangelistName() != null) {
            map.put(SVNProperty.CHANGELIST, getChangelistName());
        }
        if (getChecksum() != null) {
            map.put(SVNProperty.CHECKSUM, getChecksum());
        
        }
        if (getCommittedDate() != null) {
            map.put(SVNProperty.COMMITTED_DATE, getCommittedDate());
        }
        if (getCommittedRevision() >= 0) {
            map.put(SVNProperty.COMMITTED_REVISION, Long.toString(getCommittedRevision()));
        }
        if (getConflictNew() != null) {
            map.put(SVNProperty.CONFLICT_NEW, getConflictNew());
        }
        if (getConflictOld() != null) {
            map.put(SVNProperty.CONFLICT_OLD, getConflictOld());
        }
        if (getConflictWorking() != null) {
            map.put(SVNProperty.CONFLICT_WRK, getConflictWorking());
        }
        if (isCopied()) {
            map.put(SVNProperty.COPIED, Boolean.TRUE.toString());
        }
        if (getCopyFromRevision() >= 0) {
            map.put(SVNProperty.COPYFROM_REVISION, Long.toString(getCopyFromRevision()));
        }
        if (getCopyFromURL() != null) {
            map.put(SVNProperty.COPYFROM_URL, getCopyFromURL());
        }
        if (isDeleted()) {
            map.put(SVNProperty.DELETED, Boolean.TRUE.toString());
        }
        if (getDepth() != null) {
            map.put(SVNProperty.DEPTH, getDepth().toString());
        }
        if (getExternalFilePath() != null) {
            map.put(SVNProperty.FILE_EXTERNAL_PATH, getExternalFilePath());            
        }
        if (getExternalFileRevision() != null) {
            map.put(SVNProperty.FILE_EXTERNAL_REVISION, getExternalFileRevision());            
        }
        if (getExternalFilePegRevision() != null) {
            map.put(SVNProperty.FILE_EXTERNAL_PEG_REVISION, getExternalFilePegRevision());            
        }
        if (hasProperties()) {
            map.put(SVNProperty.HAS_PROPS, Boolean.TRUE.toString());
        }
        if (hasPropertiesModifications()) {
            map.put(SVNProperty.HAS_PROP_MODS, Boolean.TRUE.toString());
        }
        if (isIncomplete()) {
            map.put(SVNProperty.INCOMPLETE, Boolean.TRUE.toString());
        } 
        if (isKeepLocal()) {
            map.put(SVNProperty.KEEP_LOCAL, Boolean.TRUE.toString());
        }
        if (getKind() != null) {
            map.put(SVNProperty.KIND, getKind().toString());
        }
        if (getAuthor() != null) {
            map.put(SVNProperty.LAST_AUTHOR, getAuthor());
        }
        if (getLockComment() != null) {
            map.put(SVNProperty.LOCK_COMMENT, getLockComment());
        }
        if (getLockCreationDate() != null) {
            map.put(SVNProperty.LOCK_CREATION_DATE, getLockCreationDate());
        }
        if (getLockOwner() != null) {
            map.put(SVNProperty.LOCK_OWNER, getLockOwner());
        }
        if (getLockToken() != null) {
            map.put(SVNProperty.LOCK_TOKEN, getLockToken());
        }
        if (getName() != null) {
            map.put(SVNProperty.NAME, getName());
        }
        if (getPresentProperties() != null) {
            map.put(SVNProperty.PRESENT_PROPS, getPresentProperties());
        }
        if (getPropRejectFile() != null) {
            map.put(SVNProperty.PROP_REJECT_FILE, getPropRejectFile());
        }
        if (getPropTime() != null) {
            map.put(SVNProperty.PROP_TIME, getPropTime());
        }
        if (getRepositoryRoot() != null) {
            map.put(SVNProperty.REPOS, getRepositoryRoot());
        }
        if (getRevision() >= 0) {
            map.put(SVNProperty.REVISION, Long.toString(getRevision()));
        }
        if (getSchedule() != null) {
            map.put(SVNProperty.SCHEDULE, getSchedule());            
        }
        if (getTextTime() != null) {
            map.put(SVNProperty.TEXT_TIME, getTextTime());
        }
        if (getTreeConflictData() != null) {
            map.put(SVNProperty.TREE_CONFLICT_DATA, getTreeConflictData());
        }
        if (getURL() != null) {
            map.put(SVNProperty.URL, getURL());
        }
        if (getUUID() != null) {
            map.put(SVNProperty.UUID, getUUID());
        }
        if (getWorkingSize() >= 0) {
            map.put(SVNProperty.WORKING_SIZE, Long.toString(getWorkingSize()));
        }
        return map;
    }

}