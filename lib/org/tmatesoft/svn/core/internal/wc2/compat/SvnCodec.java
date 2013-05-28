package org.tmatesoft.svn.core.internal.wc2.compat;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCConflictDescription17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo.BaseInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc2.ISvnCommitRunner;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc.admin.*;
import org.tmatesoft.svn.core.wc2.*;
import org.tmatesoft.svn.core.wc2.hooks.ISvnCommitHandler;
import org.tmatesoft.svn.core.wc2.hooks.ISvnExternalsHandler;
import org.tmatesoft.svn.core.wc2.hooks.ISvnFileListHook;
import org.tmatesoft.svn.core.wc2.hooks.ISvnPropertyValueProvider;

import java.io.File;
import java.util.*;

public class SvnCodec {
    
    public static SVNDiffStatus diffStatus(SvnDiffStatus diffStatus) {
        return new SVNDiffStatus(diffStatus.getFile(), diffStatus.getUrl(), diffStatus.getPath(), diffStatus.getModificationType(), diffStatus.isPropertiesModified(), diffStatus.getKind());
    }
    
    public static SvnDiffStatus diffStatus(SVNDiffStatus diffStatus) {
        SvnDiffStatus result = new SvnDiffStatus();
        result.setFile(diffStatus.getFile());
        result.setUrl(diffStatus.getURL());
        result.setPath(diffStatus.getPath());
        result.setModificationType(diffStatus.getModificationType());
        result.setPropertiesModified(diffStatus.isPropertiesModified());
        result.setKind(diffStatus.getKind());
        result.setUserData(diffStatus);
        return result;
    }
    
    public static ISvnObjectReceiver<SVNAdminPath> treeReceiver(final ISVNTreeHandler handler) {
        return new ISvnObjectReceiver<SVNAdminPath>() {
            public void receive(SvnTarget target, SVNAdminPath path) throws SVNException {
                if (handler != null) {
                    handler.handlePath(path);
                }
            }
        };
    }
    
    public static ISvnObjectReceiver<SVNAdminPath> changedHistoryReceiver(final ISVNHistoryHandler handler) {
        return new ISvnObjectReceiver<SVNAdminPath>() {
            public void receive(SvnTarget target, SVNAdminPath path) throws SVNException {
                if (handler != null) {
                    handler.handlePath(path);
                }
            }
        };
    }
    
    public static ISvnObjectReceiver<String> changedDirectoriesReceiver(final ISVNChangedDirectoriesHandler handler) {
        return new ISvnObjectReceiver<String>() {
            public void receive(SvnTarget target, String path) throws SVNException {
                if (handler != null) {
                    handler.handleDir(path);
                }
            }
        };
    }
    
    public static ISvnObjectReceiver<SVNChangeEntry> changeEntryReceiver(final ISVNChangeEntryHandler handler) {
        return new ISvnObjectReceiver<SVNChangeEntry>() {
            public void receive(SvnTarget target, SVNChangeEntry entry) throws SVNException {
                if (handler != null) {
                    handler.handleEntry(entry);
                }
            }
        };
    }
    
    
    
    public static ISvnObjectReceiver<String> changelistReceiver(final ISVNChangelistHandler handler) {
        return new ISvnObjectReceiver<String>() {
            public void receive(SvnTarget target, String object) throws SVNException {
                if (handler != null) {
                    handler.handle(target.getFile(), object);
                }
            }
        };
    }
    
    public static ISvnObjectReceiver<SVNPropertyData> propertyReceiver(final ISVNPropertyHandler handler) {
        return new ISvnObjectReceiver<SVNPropertyData>() {
            public void receive(SvnTarget target, SVNPropertyData object) throws SVNException {
                if (handler != null) {
                    handler.handleProperty(target.getFile(), object);
                }
            }
        };
    }
    
    public static ISvnObjectReceiver<SVNLogEntry> logReceiver(final ISVNLogEntryHandler handler) {
        return new ISvnObjectReceiver<SVNLogEntry>() {
            public void receive(SvnTarget target, SVNLogEntry object) throws SVNException {
                if (handler != null) {
                    handler.handleLogEntry(object);
                }
            }
        };
    }
    
    public static ISvnObjectReceiver<SvnAnnotateItem> annotateReceiver(final ISVNAnnotateHandler handler) {
        return new ISvnObjectReceiver<SvnAnnotateItem>() {
            public void receive(SvnTarget target, SvnAnnotateItem item) throws SVNException {
                if (handler != null) {
                	if (item.isEof())
                		handler.handleEOF();
                	else if (item.isLine()) 
                		handler.handleLine(item.getDate(), item.getRevision(), item.getAuthor(), item.getLine(), item.getMergedDate(), 
                				item.getMergedRevision(), item.getMergedAuthor(), item.getMergedPath(), item.getLineNumber());
                	else if (item.isRevision())
                		item.setReturnResult(handler.handleRevision(item.getDate(), item.getRevision(), item.getAuthor(), item.getContents()));

                }
            }
        };
    }

    public static ISvnObjectReceiver<SvnDiffStatus> diffStatusReceiver(final ISVNDiffStatusHandler handler) {
        return new ISvnObjectReceiver<SvnDiffStatus>() {
            public void receive(SvnTarget target, SvnDiffStatus svnDiffStatus) throws SVNException {
                if (handler != null) {
                    handler.handleDiffStatus(diffStatus(svnDiffStatus));
                }
            }
        };
    }
    
    public static SvnStatus status(SVNStatus status) {
        SvnStatus result = new SvnStatus();
        result.setUserData(status);
        
        result.setPath(status.getFile());
        result.setChangedAuthor(status.getAuthor());
        result.setChangedDate(SVNDate.fromDate(status.getCommittedDate()));
        result.setChangedRevision(revisionNumber(status.getCommittedRevision()));
        result.setChangelist(status.getChangelistName());
        result.setConflicted(status.isConflicted());
        result.setCopied(status.isCopied());
        result.setDepth(status.getDepth());
        result.setFileExternal(status.isFileExternal());
        // TODO
        //result.setFileSize()
        result.setKind(status.getKind());
        result.setLock(status.getLocalLock());
        
        // combine node and contents?
        result.setNodeStatus(status.getNodeStatus());
        result.setTextStatus(status.getContentsStatus());
        result.setPropertiesStatus(status.getPropertiesStatus());
        
        result.setRepositoryChangedAuthor(status.getRemoteAuthor());
        result.setRepositoryChangedDate(SVNDate.fromDate(status.getRemoteDate()));
        result.setRepositoryChangedRevision(revisionNumber(status.getRemoteRevision()));
        result.setRepositoryKind(status.getRemoteKind());
        result.setRepositoryLock(status.getRemoteLock());
        
        // combine node and contents?
        result.setRepositoryNodeStatus(status.getRemoteNodeStatus());
        result.setRepositoryTextStatus(status.getRemoteContentsStatus());
        result.setRepositoryPropertiesStatus(status.getRemotePropertiesStatus());
        
        result.setRepositoryRelativePath(status.getRepositoryRelativePath());
        result.setRepositoryRootUrl(status.getRepositoryRootURL());
        result.setRepositoryUuid(status.getRepositoryUUID());
        
        result.setRevision(revisionNumber(status.getRevision()));
        result.setSwitched(status.isSwitched());
        result.setVersioned(status.isVersioned());
        result.setWcLocked(status.isLocked());

        result.setWorkingCopyFormat(status.getWorkingCopyFormat());
        
        try {
            result.setCopyFromUrl(status.getCopyFromURL() != null ? SVNURL.parseURIEncoded(status.getCopyFromURL()) : null);
            result.setCopyFromRevision(status.getCopyFromRevision() != null ? status.getCopyFromRevision().getNumber() : -1);
        } catch (SVNException e) {
            result.setCopyFromUrl(null);
        }
        return result;
    }
    
    public static long revisionNumber(SVNRevision revision) {
        if (revision == null) {
            return SVNWCContext.INVALID_REVNUM;
        }
        return revision.getNumber();
    } 
    
    public static SVNStatus status(SVNWCContext context, SvnStatus status) throws SVNException {
        if (status.getUserData() instanceof SVNStatus) {
            return (SVNStatus) status.getUserData();
        }
        
        SVNStatus result = new SVNStatus();
        result.setFile(status.getPath());
        result.setKind(status.getKind());
        // TODO filesize
        result.setIsVersioned(status.isVersioned());
        result.setIsConflicted(status.isConflicted());
        
        result.setNodeStatus(status.getNodeStatus());
        result.setContentsStatus(status.getTextStatus());
        result.setPropertiesStatus(status.getPropertiesStatus());


        if (status.getKind() == SVNNodeKind.DIR) {
            result.setIsLocked(status.isWcLocked());
        }
        result.setIsFileExternal(status.isFileExternal());
        result.setIsCopied(status.isCopied());
        result.setRevision(SVNRevision.create(status.getRevision()));
        
        result.setCommittedRevision(SVNRevision.create(status.getChangedRevision()));
        result.setAuthor(status.getChangedAuthor());
        result.setCommittedDate(status.getChangedDate());
        
        result.setRepositoryRootURL(status.getRepositoryRootUrl());
        result.setRepositoryRelativePath(status.getRepositoryRelativePath());
        result.setRepositoryUUID(status.getRepositoryUuid());
        
        result.setIsSwitched(status.isSwitched());
        if (status.isVersioned() && status.isSwitched() && status.getKind() == SVNNodeKind.FILE) {
           // TODO fileExternal
        }
        result.setLocalLock(status.getLock());
        result.setChangelistName(status.getChangelist());
        result.setDepth(status.getDepth());
        
        result.setRemoteKind(status.getRepositoryKind());
        result.setRemoteNodeStatus(status.getRepositoryNodeStatus());
        result.setRemoteContentsStatus(status.getRepositoryTextStatus());
        result.setRemotePropertiesStatus(status.getRepositoryPropertiesStatus());
        result.setRemoteLock(status.getRepositoryLock());
        
        result.setRemoteAuthor(status.getRepositoryChangedAuthor());
        result.setRemoteRevision(SVNRevision.create(status.getRepositoryChangedRevision()));
        result.setRemoteDate(status.getRepositoryChangedDate());
        
        // do all that on demand in SVNStatus class later.
        // compose URL on demand in SVNStatus
        
        if (status.isVersioned() && status.isConflicted()) {
            SVNWCContext.ConflictInfo info = context.getConflicted(status.getPath(), true, true, true);
            if (info.textConflicted) {
                result.setContentsStatus(SVNStatusType.STATUS_CONFLICTED);
            }
            if (info.propConflicted) {
                result.setPropertiesStatus(SVNStatusType.STATUS_CONFLICTED);
            }
            if (info.textConflicted || info.propConflicted) {
                result.setNodeStatus(SVNStatusType.STATUS_CONFLICTED);
            }
        }
        
        if (status.getRepositoryRootUrl() != null && status.getRepositoryRelativePath() != null) {
            SVNURL url = status.getRepositoryRootUrl().appendPath(status.getRepositoryRelativePath(), false);
            if (status.isVersioned()) {
                result.setURL(url);
            }
            result.setRemoteURL(url);
        }
        
        if (context != null && status.isVersioned() && status.getRevision() == SVNWCContext.INVALID_REVNUM && !status.isCopied()) {
            if (status.getNodeStatus() == SVNStatusType.STATUS_REPLACED) {
                fetchStatusRevision(context, status, result);
            } else if (status.getNodeStatus() == SVNStatusType.STATUS_DELETED ) {
                fetchStatusRevision(context, status, result);
            }
        }
        
        if (context != null && status.isConflicted()) {
            boolean hasTreeConflict = false;
            SVNWCContext.ConflictInfo conflictedInfo = null;
            if (status.isVersioned()) {
                try {
                    conflictedInfo = context.getConflicted(status.getPath(), true, true, true);
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_UPGRADE_REQUIRED) {
                    } else {
                        throw e;
                    }
                }
                hasTreeConflict = conflictedInfo != null && conflictedInfo.treeConflicted;
            } else {
                hasTreeConflict = true;
            }
        
            if (hasTreeConflict) {
                SVNTreeConflictDescription treeConflictDescription = context.getTreeConflict(status.getPath());
                result.setTreeConflict(treeConflictDescription);
            }
            
            if (conflictedInfo != null) {
                result.setConflictWrkFile(conflictedInfo.localFile);
                result.setConflictOldFile(conflictedInfo.baseFile);
                result.setConflictNewFile(conflictedInfo.repositoryFile);                    
                result.setPropRejectFile(conflictedInfo.propRejectFile);                    
            }
        }
        
        if (result.getNodeStatus() == SVNStatusType.STATUS_ADDED) {
            result.setPropertiesStatus(SVNStatusType.STATUS_NONE);
        }
        result.setWorkingCopyFormat(status.getWorkingCopyFormat());
        
        result.setCopyFromRevision(status.getCopyFromRevision() >= 0 ? SVNRevision.create(status.getCopyFromRevision()) : SVNRevision.UNDEFINED);
        result.setCopyFromURL(status.getCopyFromUrl() != null ? status.getCopyFromUrl().toString() : null);
        
        return result;
    }
    
    private static void fetchStatusRevision(SVNWCContext context, SvnStatus source, SVNStatus result) throws SVNException {
        Structure<NodeInfo> info = context.getDb().readInfo(source.getPath(), NodeInfo.revision, NodeInfo.changedAuthor, NodeInfo.changedDate, NodeInfo.changedRev, 
                NodeInfo.haveBase, NodeInfo.haveWork, NodeInfo.haveMoreWork, NodeInfo.status);
        
        if (source.getNodeStatus() == SVNStatusType.STATUS_DELETED) {
            result.setAuthor(info.text(NodeInfo.changedAuthor));
            result.setCommittedDate(info.<SVNDate>get(NodeInfo.changedDate));
            result.setCommittedRevision(SVNRevision.create(info.lng(NodeInfo.changedRev)));
        }
        result.setRevision(SVNRevision.create(info.lng(NodeInfo.revision)));
        SVNWCDbStatus st = info.<SVNWCDbStatus>get(NodeInfo.status); 
        if (info.is(NodeInfo.haveWork) || info.lng(NodeInfo.revision) == SVNWCContext.INVALID_REVNUM || 
                (source.getNodeStatus() == SVNStatusType.STATUS_DELETED && info.lng(NodeInfo.changedRev) == SVNWCContext.INVALID_REVNUM) || 
                (st != SVNWCDbStatus.Added && st != SVNWCDbStatus.Deleted)) {
            info.release();
            
            try {
                ISVNWCDb.WCDbBaseInfo binfo = context.getDb().getBaseInfo(source.getPath(), BaseInfoField.revision, BaseInfoField.changedRev, BaseInfoField.changedAuthor, BaseInfoField.changedDate);            
                if (source.getNodeStatus() == SVNStatusType.STATUS_DELETED) {
                    result.setAuthor(binfo.changedAuthor);
                    result.setCommittedDate(binfo.changedDate);
                    result.setCommittedRevision(SVNRevision.create(binfo.changedRev));
                }
                result.setRevision(SVNRevision.create(binfo.revision));
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                    throw e;
                }
            }
        } else {
            info.release();
        }
    }
    
    public static SvnInfo info(SVNInfo info) {
        SvnInfo result = new SvnInfo();
        result.setUserData(info);
        result.setKind(info.getKind());
        result.setLastChangedAuthor(info.getAuthor());
        result.setLastChangedDate(SVNDate.fromDate(info.getCommittedDate()));
        result.setLastChangedRevision(info.getCommittedRevision().getNumber());
        result.setLock(info.getLock());
        result.setRepositoryRootURL(info.getRepositoryRootURL());
        result.setRepositoryUuid(info.getRepositoryUUID());
        result.setRevision(info.getRevision().getNumber());
        result.setSize(-1);
        result.setUrl(info.getURL());
        
        SvnWorkingCopyInfo wcInfo = new SvnWorkingCopyInfo();
        
        result.setWcInfo(wcInfo);
        wcInfo.setChangelist(info.getChangelistName());
        if (info.getChecksum() != null) {
            SvnChecksum checksum = new SvnChecksum(SvnChecksum.Kind.md5, info.getChecksum());
            wcInfo.setChecksum(checksum);
        }
        
        if (info.getTreeConflict() != null || 
                info.getConflictWrkFile() != null || info.getConflictNewFile() != null || info.getConflictOldFile() != null || 
                info.getPropConflictFile() != null) {
            Collection<SVNConflictDescription> conflicts = new ArrayList<SVNConflictDescription>();
            if (info.getTreeConflict() != null) {
                conflicts.add(info.getTreeConflict());
            }
            if (info.getConflictWrkFile() != null || info.getConflictNewFile() != null || info.getConflictOldFile() != null) {
                SVNWCConflictDescription17 cd = SVNWCConflictDescription17.createText(info.getFile());
                cd.setTheirFile(info.getConflictNewFile());
                cd.setBaseFile(info.getConflictOldFile());
                cd.setMyFile(info.getConflictWrkFile());
                conflicts.add(cd.toConflictDescription());
            }
            if (info.getPropConflictFile() != null) {
                SVNWCConflictDescription17 cd = SVNWCConflictDescription17.createProp(info.getFile(), info.getKind(), null);
                cd.setTheirFile(info.getPropConflictFile());
                conflicts.add(cd.toConflictDescription());
            }
            wcInfo.setConflicts(conflicts);
        }
        
        wcInfo.setCopyFromRevision(info.getCommittedRevision().getNumber());
        wcInfo.setCopyFromUrl(info.getCopyFromURL());
        wcInfo.setDepth(info.getDepth());
        wcInfo.setPath(info.getFile());
        wcInfo.setRecordedSize(info.getWorkingSize());
        if (info.getTextTime() != null) {
            wcInfo.setRecordedTime(info.getTextTime().getTime());
        }
        wcInfo.setSchedule(SvnSchedule.fromString(info.getSchedule()));
        
        File wcRoot = null;
        try {
            wcRoot = SVNWCUtil.getWorkingCopyRoot(info.getFile(), true);
        } catch (SVNException e) {
        }
        wcInfo.setWcRoot(wcRoot);
        
        return result;
    }
    
    public static SVNInfo info(SvnInfo info) {
        if (info.getUserData() instanceof SVNInfo) {
            return ((SVNInfo) info.getUserData());
        }
        if (info.getWcInfo() == null) {
            String rootPath = info.getRepositoryRootUrl().getPath();
            String itemPath = info.getUrl().getPath();
            itemPath = SVNPathUtil.getPathAsChild(rootPath, itemPath);
            if (itemPath == null) {
                itemPath = "";
            }
            return new SVNInfo(itemPath, info.getUrl(), SVNRevision.create(info.getRevision()), info.getKind(), info.getRepositoryUuid(), info.getRepositoryRootUrl(), 
                    info.getLastChangedRevision(), info.getLastChangedDate(), info.getLastChangedAuthor(), info.getLock(), SVNDepth.UNKNOWN, info.getSize());
        }
        SvnWorkingCopyInfo wcInfo = info.getWcInfo();
        
        String conflictOld = null;
        String conflictNew = null;
        String conflictWorking = null;
        String propRejectFile = null;
        SVNTreeConflictDescription treeConflict = null;
        
        Collection<SVNConflictDescription> conflicts = wcInfo.getConflicts();
        if (conflicts != null) {
            for (SVNConflictDescription conflictDescription : conflicts) {
                if (conflictDescription.isTreeConflict() && conflictDescription instanceof SVNTreeConflictDescription) {
                    treeConflict = (SVNTreeConflictDescription) conflictDescription;
                } else if (conflictDescription.isTextConflict()) {
                    if (conflictDescription.getMergeFiles() != null) {
                        if (conflictDescription.getMergeFiles().getBaseFile() != null) {
                            conflictOld = conflictDescription.getMergeFiles().getBaseFile().getName();
                        }
                        if (conflictDescription.getMergeFiles().getRepositoryFile() != null) {
                            conflictNew = conflictDescription.getMergeFiles().getRepositoryFile().getName();
                        }
                        if (conflictDescription.getMergeFiles().getLocalFile() != null) {
                            conflictWorking = conflictDescription.getMergeFiles().getLocalFile().getName();
                        }
                    }
                } else if (conflictDescription.isPropertyConflict()) {
                    if (conflictDescription.getMergeFiles() != null) {
                        propRejectFile = conflictDescription.getMergeFiles().getRepositoryFile().getName();
                    }
                }
            }
        }
        
        String schedule = wcInfo.getSchedule() != null ? wcInfo.getSchedule().asString() : null;
        SVNInfo i = new SVNInfo(wcInfo.getPath(), 
                info.getUrl(), 
                info.getRepositoryRootUrl(), 
                info.getRevision(), 
                info.getKind(), 
                info.getRepositoryUuid(), 
                info.getLastChangedRevision(),
                info.getLastChangedDate() != null ? info.getLastChangedDate().format() : null, 
                info.getLastChangedAuthor(), 
                schedule, 
                wcInfo.getCopyFromUrl(), 
                wcInfo.getCopyFromRevision(), 
                wcInfo.getRecordedTime() > 0 ? SVNWCUtils.readDate(wcInfo.getRecordedTime()).format() : null, 
                null,
                wcInfo.getChecksum() != null ? wcInfo.getChecksum().getDigest() : null, 
                conflictOld, 
                conflictNew, 
                conflictWorking, 
                propRejectFile, 
                info.getLock(), 
                wcInfo.getDepth(), 
                wcInfo.getChangelist(), 
                wcInfo.getRecordedSize(), 
                treeConflict);
        i.setWorkingCopyRoot(wcInfo.getWcRoot());
        return i;
    }
    
    public static ISvnFileListHook fileListHook(final ISVNStatusFileProvider provider) {
        if (provider == null) {
            return null;
        }
        return new ISvnFileListHook() {
            public Map<String, File> listFiles(File parent) {
                return provider.getChildrenFiles(parent);
            }
        };
    }
    
    public static ISVNStatusFileProvider fileListProvider(final ISvnFileListHook hook) {
        if (hook == null) {
            return null;
        }
        return new ISVNStatusFileProvider() {
            public Map<String, File> getChildrenFiles(File parent) {
                return hook.listFiles(parent);
            }
        };
    }
    
    public static SVNCommitItem commitItem(SvnCommitItem item) {
        return new SVNCommitItem(item.getPath(), 
                item.getUrl(), item.getCopyFromUrl(), 
                item.getKind(), 
                SVNRevision.create(item.getRevision()), 
                SVNRevision.create(item.getCopyFromRevision()), 
                item.hasFlag(SvnCommitItem.ADD), 
                item.hasFlag(SvnCommitItem.DELETE), 
                item.hasFlag(SvnCommitItem.PROPS_MODIFIED), 
                item.hasFlag(SvnCommitItem.TEXT_MODIFIED), 
                item.hasFlag(SvnCommitItem.COPY), 
                item.hasFlag(SvnCommitItem.LOCK));
    }

    public static SvnCommitItem commitItem(SVNCommitItem item) {
        SvnCommitItem i = new SvnCommitItem();
        i.setPath(item.getFile());
        i.setUrl(item.getURL());
        i.setKind(item.getKind());
        if (item.getCopyFromRevision() != null) {
            i.setCopyFromRevision(item.getCopyFromRevision().getNumber());
        } else {
            i.setCopyFromRevision(-1);
        }        
        i.setCopyFromUrl(i.getCopyFromUrl());
        i.setRevision(item.getRevision() != null ? item.getRevision().getNumber() : -1);
        int flags = 0;
        if (item.isAdded()) {
            flags |= SvnCommitItem.ADD;
        }
        if (item.isContentsModified()) {
            flags |= SvnCommitItem.TEXT_MODIFIED;
        }
        if (item.isPropertiesModified()) {
            flags |= SvnCommitItem.PROPS_MODIFIED;
        }
        if (item.isCopied()) {
            flags |= SvnCommitItem.COPY;
        }
        if (item.isDeleted()) {
            flags |= SvnCommitItem.DELETE;
        }
        if (item.isLocked()) {
            flags |= SvnCommitItem.LOCK;
        }
        i.setFlags(flags);
        return i;
    }
    
    public static class SVNCommitPacketWrapper extends SVNCommitPacket {

        private SvnCommitPacket packet;
        private SvnCommit operation;

        public SVNCommitPacketWrapper(SvnCommit operation, SvnCommitPacket packet, SVNCommitItem[] items, Map<String, String> lockTokens) {
            super(null, items, lockTokens);
            this.operation = operation;
            this.packet = packet;
        }

        @Override
        public void dispose() throws SVNException {
            packet.dispose();
        }
        
        public SvnCommitPacket getPacket() {
            return this.packet;
        }
        
        public SvnCommit getOperation() {
            return operation;
        }

        @Override
        public void setCommitItemSkipped(SVNCommitItem item, boolean skipped) {
            super.setCommitItemSkipped(item, skipped);
            packet.setItemSkipped(item.getFile(), skipped);
        }

        @Override
        public SVNCommitPacket removeSkippedItems() {
            packet.removeSkippedItems();

            if (this == EMPTY) {
                return EMPTY;
            }
            Collection items = new ArrayList();
            Map lockTokens = getLockTokens() == null ? null : new SVNHashMap(getLockTokens());
            SVNCommitItem[] filteredItems = filterSkippedItemsAndLockTokens(items, lockTokens);
            return new SVNCommitPacketWrapper(getOperation(), packet, filteredItems, lockTokens);
        }
    }
    
    public static SVNCommitPacket commitPacket(final SvnCommit operation, final SvnCommitPacket packet) {
        Collection<SVNCommitItem> skippedItems = new ArrayList<SVNCommitItem>();
        Collection<SVNCommitItem> oldItems = new ArrayList<SVNCommitItem>();
        for (SVNURL reposRoot : packet.getRepositoryRoots()) {
            for (SvnCommitItem item : packet.getItems(reposRoot)) {
                SVNCommitItem oldItem = commitItem(item);
                oldItems.add(oldItem);

                if (packet.isItemSkipped(item.getPath())) {
                    skippedItems.add(oldItem);
                }
            }
        }
        final SVNCommitItem[] allItems = oldItems.toArray(new SVNCommitItem[oldItems.size()]);
        
        Map<String, String> oldLockTokens = new HashMap<String, String>();
        if (packet.getLockTokens() != null) {
            for (SVNURL url : packet.getLockTokens().keySet()) {
                String token = packet.getLockTokens().get(url);
                oldLockTokens.put(url.toString(), token);
            }
        }

        final SVNCommitPacketWrapper packetWrapper = new SVNCommitPacketWrapper(operation, packet, allItems, oldLockTokens);
        for (SVNCommitItem skippedItem : skippedItems) {
            packetWrapper.setCommitItemSkipped(skippedItem, true);
        }
        return packetWrapper;
    }

    public static SvnCommitPacket commitPacket(ISvnCommitRunner runner, SVNCommitPacket oldPacket) {
        SvnCommitPacket packet = new SvnCommitPacket();
        packet.setLockingContext(runner, oldPacket);
        Map<SVNURL, String> lockTokens = new HashMap<SVNURL, String>();
        SVNCommitItem[] items = oldPacket.getCommitItems();
        @SuppressWarnings("unchecked")
        Map<String, String> locks = oldPacket.getLockTokens();
        if (locks != null) {
            for (String url : locks.keySet()) {
                try {
                    lockTokens.put(SVNURL.parseURIEncoded(url), locks.get(url));
                } catch (SVNException e) {
                    //
                }
            }
        }
        Collection<SVNURL> allUrl = new HashSet<SVNURL>();
        SVNURL rootUrl = null;
        for (int j = 0; j < items.length; j++) {
            SVNCommitItem item = items[j];
            allUrl.add(item.getURL());
            if (item.getCopyFromURL() != null) {
                allUrl.add(item.getCopyFromURL());
            }
        }
        for (SVNURL svnurl : allUrl) {
            if (rootUrl == null) {
                rootUrl = svnurl;
            } else {
                rootUrl = SVNURLUtil.getCommonURLAncestor(rootUrl, svnurl);
            }
        }
        for (int j = 0; j < items.length; j++) {
            SVNCommitItem item = items[j];
            int flags = 0;
            if (item.isAdded()) {
                flags |= SvnCommitItem.ADD;
            }
            if (item.isContentsModified()) {
                flags |= SvnCommitItem.TEXT_MODIFIED;
            }
            if (item.isCopied()) {
                flags |= SvnCommitItem.COPY;
            }
            if (item.isDeleted()) {
                flags |= SvnCommitItem.DELETE;
            }
            if (item.isLocked()) {
                flags |= SvnCommitItem.LOCK;
            }
            if (item.isPropertiesModified()) {
                flags |= SvnCommitItem.PROPS_MODIFIED;
            }
            try {
                SvnCommitItem newItem = packet.addItem(item.getFile(),
                        rootUrl,
                        item.getKind(),
                        item.getURL(), item.getRevision() != null ? item.getRevision().getNumber() : -1,
                        item.getCopyFromURL(),
                        item.getCopyFromRevision() != null ? item.getCopyFromRevision().getNumber() : -1,
                        flags);
                if (oldPacket.isCommitItemSkipped(item)) {
                    packet.setItemSkipped(newItem.getPath(), true);
                }
            } catch (SVNException e) {
                //
            }
        }
        packet.setLockTokens(lockTokens);
        return packet;
    }
    
    public static SVNRevisionRange revisionRange(SvnRevisionRange range) {
        return new SVNRevisionRange(range.getStart(), range.getEnd());
    }
    
    public static SvnRevisionRange revisionRange(SVNRevisionRange range) {
        return SvnRevisionRange.create(range.getStartRevision(), range.getEndRevision());
    }

    public static Collection<SvnRevisionRange> revisionRanges(Collection<SVNRevisionRange> ranges) {
        Collection<SvnRevisionRange> result = new ArrayList<SvnRevisionRange>();
        if (ranges != null) {
            for (SVNRevisionRange range : ranges) {
                result.add(revisionRange(range));
            }
        }
        return result;
    }

    public static Collection<SVNRevisionRange> oldRevisionRanges(Collection<SvnRevisionRange> ranges) {
        Collection<SVNRevisionRange> result = new ArrayList<SVNRevisionRange>();
        if (ranges != null) {
            for (SvnRevisionRange range : ranges) {
                result.add(revisionRange(range));
            }
        }
        return result;
    }

    public static SVNCopySource copySource(SvnCopySource newSource) {
        if (newSource.getSource().getURL() != null) {
            final SVNCopySource copySource = new SVNCopySource(newSource.getSource().getResolvedPegRevision(), newSource.getRevision(), newSource.getSource().getURL());
            copySource.setCopyContents(newSource.isCopyContents());
            return copySource;
        }
        final SVNCopySource copySource = new SVNCopySource(newSource.getSource().getResolvedPegRevision(), newSource.getRevision(), newSource.getSource().getFile());
        copySource.setCopyContents(newSource.isCopyContents());
        return copySource;
    }

    public static SvnCopySource copySource(SVNCopySource oldSource) {
        SvnTarget target;
        if (oldSource.isURL()) {
            target = SvnTarget.fromURL(oldSource.getURL(), oldSource.getPegRevision());
        } else {
            target = SvnTarget.fromFile(oldSource.getFile(), oldSource.getPegRevision());
        }
        final SvnCopySource copySource = SvnCopySource.create(target, oldSource.getRevision());
        copySource.setCopyContents(oldSource.isCopyContents());
        return copySource;
    }
    
    public static ISvnCommitHandler commitHandler(final ISVNCommitHandler target) {
        if (target == null) {
            return null;
        }
        return new ISvnCommitHandler() {
            
            public SVNProperties getRevisionProperties(String message, SvnCommitItem[] commitables, SVNProperties revisionProperties) throws SVNException {
                SVNCommitItem[] targetItems = new SVNCommitItem[commitables.length];
                for (int i = 0; i < targetItems.length; i++) {
                    targetItems[i] = commitItem(commitables[i]);
                }
                return target.getRevisionProperties(message, targetItems, revisionProperties);
            }
            
            public String getCommitMessage(String message, SvnCommitItem[] commitables) throws SVNException {
                SVNCommitItem[] targetItems = new SVNCommitItem[commitables.length];
                for (int i = 0; i < targetItems.length; i++) {
                    targetItems[i] = commitItem(commitables[i]);
                }
                return target.getCommitMessage(message, targetItems);
            }
        };
    }

    public static ISVNCommitHandler commitHandler(final ISvnCommitHandler target) {
        if (target == null) {
            return null;
        }
        return new ISVNCommitHandler() {
            public SVNProperties getRevisionProperties(String message, SVNCommitItem[] commitables, SVNProperties revisionProperties) throws SVNException {
                SvnCommitItem[] targetItems = new SvnCommitItem[commitables.length];
                for (int i = 0; i < targetItems.length; i++) {
                    targetItems[i] = commitItem(commitables[i]);
                }
                return target.getRevisionProperties(message, targetItems, revisionProperties);
            }
            
            public String getCommitMessage(String message, SVNCommitItem[] commitables) throws SVNException {
                SvnCommitItem[] targetItems = new SvnCommitItem[commitables.length];
                for (int i = 0; i < targetItems.length; i++) {
                    targetItems[i] = commitItem(commitables[i]);
                }
                return target.getCommitMessage(message, targetItems);
            }
        };
        
    }
    
    public static ISVNExternalsHandler externalsHandler(final ISvnExternalsHandler target) {
        if (target == null) {
            return ISVNExternalsHandler.DEFAULT;
        }
        return new ISVNExternalsHandler() {
            public SVNRevision[] handleExternal(File externalPath, SVNURL externalURL,
                    SVNRevision externalRevision, SVNRevision externalPegRevision,
                    String externalsDefinition, SVNRevision externalsWorkingRevision) {
                return target.handleExternal(externalPath, externalURL, externalRevision, externalPegRevision, externalsDefinition, externalsWorkingRevision);
            }
        };
    }

    public static ISvnExternalsHandler externalsHandler(final ISVNExternalsHandler target) {
        if (target == null) {
            return new ISvnExternalsHandler() {
                public SVNRevision[] handleExternal(File externalPath, SVNURL externalURL, SVNRevision externalRevision, SVNRevision externalPegRevision, String externalsDefinition, SVNRevision externalsWorkingRevision) {
                    return new SVNRevision[] {externalRevision, externalPegRevision};
                }
            };
        }
        return new ISvnExternalsHandler() {
            public SVNRevision[] handleExternal(File externalPath, SVNURL externalURL,
                    SVNRevision externalRevision, SVNRevision externalPegRevision,
                    String externalsDefinition, SVNRevision externalsWorkingRevision) {
                return target.handleExternal(externalPath, externalURL, externalRevision, externalPegRevision, externalsDefinition, externalsWorkingRevision);
            }
        };
    }
    
    public static ISvnCommitParameters commitParameters(final ISVNCommitParameters old) {
        if (old == null) {
            return null;
        }
        return new ISvnCommitParameters() {

            public Action onMissingFile(File file) {
                ISVNCommitParameters.Action a = old.onMissingFile(file);
                return action(a);
            }

            public Action onMissingDirectory(File file) {
                ISVNCommitParameters.Action a = old.onMissingDirectory(file);
                return action(a);
            }

            private Action action(ISVNCommitParameters.Action a) {
                if (a == ISVNCommitParameters.DELETE) {
                    return Action.DELETE;
                } else if (a == ISVNCommitParameters.ERROR) {
                    return Action.ERROR;
                }
                return Action.SKIP;
            }

            public boolean onDirectoryDeletion(File directory) {
                return old.onDirectoryDeletion(directory);
            }

            public boolean onFileDeletion(File file) {
                return old.onFileDeletion(file);
            }
        };
    }
    
    public static ISVNCommitParameters commitParameters(final ISvnCommitParameters old) {
        if (old == null) {
            return null;
        }
        return new ISVNCommitParameters() {

            public ISVNCommitParameters.Action onMissingFile(File file) {
                ISvnCommitParameters.Action a = old.onMissingFile(file);
                return action(a);
            }

            public Action onMissingDirectory(File file) {
                ISvnCommitParameters.Action a = old.onMissingDirectory(file);
                return action(a);
            }

            private ISVNCommitParameters.Action action(ISvnCommitParameters.Action a) {
                if (a == ISvnCommitParameters.Action.DELETE) {
                    return ISVNCommitParameters.DELETE;
                } else if (a == ISvnCommitParameters.Action.ERROR) {
                    return ISVNCommitParameters.ERROR;
                }
                return ISVNCommitParameters.SKIP;
            }

            public boolean onDirectoryDeletion(File directory) {
                return old.onDirectoryDeletion(directory);
            }

            public boolean onFileDeletion(File file) {
                return old.onFileDeletion(file);
            }
        };
    }
    
    public static ISvnAddParameters addParameters(final ISVNAddParameters old) {
        if (old == null) {
            return null;
        }
        return new ISvnAddParameters() {

            public Action onInconsistentEOLs(File file) {
                ISVNAddParameters.Action a = old.onInconsistentEOLs(file);
                if (a == ISVNAddParameters.ADD_AS_BINARY) {
                    return Action.ADD_AS_BINARY;
                } else if (a == ISVNAddParameters.ADD_AS_IS) {
                    return Action.ADD_AS_IS;
                }
                return Action.REPORT_ERROR;
            }
        };
    }
    
    public static ISVNAddParameters addParameters(final ISvnAddParameters old) {
        if (old == null) {
            return null;
        }
        return new ISVNAddParameters() {
            public Action onInconsistentEOLs(File file) {
                ISvnAddParameters.Action a = old.onInconsistentEOLs(file);
                if (a == ISvnAddParameters.Action.ADD_AS_BINARY) {
                    return ISVNAddParameters.ADD_AS_BINARY;
                } else if (a == ISvnAddParameters.Action.ADD_AS_IS) {
                    return ISVNAddParameters.ADD_AS_IS;
                }
                return ISVNAddParameters.REPORT_ERROR;
            }
        };
    }

    public static ISvnPropertyValueProvider propertyValueProvider(final ISVNPropertyValueProvider propertyValueProvider) {
        if (propertyValueProvider == null) {
            return null;
        }
        return new ISvnPropertyValueProvider() {
            public SVNProperties providePropertyValues(File path, SVNProperties properties) throws SVNException {
                return propertyValueProvider.providePropertyValues(path, properties);
            }
        };
    }

    public static ISVNPropertyValueProvider propertyValueProvider(final ISvnPropertyValueProvider propertyValueProvider) {
        if (propertyValueProvider == null) {
            return null;
        }
        return new ISVNPropertyValueProvider() {
            public SVNProperties providePropertyValues(File path, SVNProperties properties) throws SVNException {
                return propertyValueProvider.providePropertyValues(path, properties);
            }
        };
    }
}
