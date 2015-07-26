package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNStatusEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.ISVNWCNodeHandler;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext.SVNWCNodeReposInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.*;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbLock;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo.BaseInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeOriginInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbReader.ReplaceInfo;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.*;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgCommitUtil {
    
    private enum NodeCommitStatus {
        kind,
        added,
        deleted,
        notPresent,
        excluded,
        isOpRoot,
        isReplaceRoot,
        symlink,
        reposRelPath,
        revision,
        originalReposRelPath,
        originalRevision,
        changelist,
        conflicted,
        updateRoot,
        lockToken, propsMod
    }
    
    interface ISvnUrlKindCallback {
        public SVNNodeKind getUrlKind(SVNURL url, long revision) throws SVNException;
    }
    
    public static SvnCommitPacket harvestCopyCommitables(SVNWCContext context, File path, SVNURL dst, SvnCommitPacket packet, ISvnUrlKindCallback urlKindCallback, ISvnCommitParameters commitParameters, Map<File, String> externalsStorage) throws SVNException {
        SVNWCNodeReposInfo reposInfo = context.getNodeReposInfo(path);
        File commitRelPath = new File(SVNURLUtil.getRelativeURL(reposInfo.reposRootUrl, dst, false));
        
        harvestCommittables(context, path, packet, null, 
                reposInfo.reposRootUrl, 
                commitRelPath, 
                true, SVNDepth.INFINITY, 
                false, null, null,
                false, false, 
                urlKindCallback,
                commitParameters,
                externalsStorage,
                context.getEventHandler());
        
        return packet;
    }
    
    public static SvnCommitPacket harvestCommittables(SVNWCContext context, SvnCommitPacket packet, Map<SVNURL, String> lockTokens,
            File baseDirPath,
            Collection<String> targets, int depthEmptyStart,
            SVNDepth depth, boolean justLocked, Collection<String> changelists, ISvnUrlKindCallback urlKindCallback, ISvnCommitParameters commitParameters, Map<File, String> externalsStorage) throws SVNException {
        
        Map<File, File> danglers = new HashMap<File, File>();

        int i = -1;

        for (String target : targets) {
            i++;
            File targetPath = SVNFileUtil.createFilePath(baseDirPath, target);
            SVNNodeKind kind = context.readKind(targetPath, false);
            if (kind == SVNNodeKind.NONE) {
                try {
                    Structure<NodeInfo> nodeInfoStructure = context.getDb().readInfo(targetPath, NodeInfo.status);
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                        SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "''{0}'' is not under version control", targetPath);
                        SVNErrorManager.error(errorMessage, SVNLogType.WC);
                    } else {
                        throw e;
                    }
                }
                SVNTreeConflictDescription tc = context.getTreeConflict(targetPath);
                if (tc != null) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_FOUND_CONFLICT,
                            "Aborting commit: ''{0}'' remains in conflict", targetPath);
                    SVNErrorManager.error(err, SVNLogType.WC);
                } else {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET,
                            "''{0}'' is not under version control", targetPath);
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            }
            SVNWCNodeReposInfo reposInfo = context.getNodeReposInfo(targetPath);
            SVNURL repositoryRootUrl = reposInfo.reposRootUrl;

            boolean added = context.isNodeAdded(targetPath);
            if (added) {
                File parentPath = SVNFileUtil.getParentFile(targetPath);
                try {
                    boolean parentIsAdded = context.isNodeAdded(parentPath);
                    if (parentIsAdded) {
                        Structure<NodeOriginInfo> origin = context.getNodeOrigin(parentPath, false, NodeOriginInfo.copyRootAbsPath, NodeOriginInfo.isCopy);
                        if (origin.is(NodeOriginInfo.isCopy)) {
                            parentPath = origin.get(NodeOriginInfo.copyRootAbsPath);
                        }
                        origin.release();
                        danglers.put(parentPath, targetPath);
                    }
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT,
                                "''{0}'' is scheduled for addition within unversioned parent", targetPath);
                        SVNErrorManager.error(err, SVNLogType.WC);
                    }
                    throw e;
                }
            }
            bailOnTreeConflictedAncestor(context, targetPath);
            if (i == depthEmptyStart) {
                depth = SVNDepth.EMPTY;
            }
            harvestCommittables(context, targetPath, packet, lockTokens, repositoryRootUrl, null, false, depth, justLocked, changelists, danglers, false, false, urlKindCallback, commitParameters, externalsStorage, context.getEventHandler());
        }
        for(SVNURL root : packet.getRepositoryRoots()) {
            handleDescendants(context, packet, root, new ArrayList<SvnCommitItem>(packet.getItems(root)), urlKindCallback, context.getEventHandler());
        }
        
        for(File danglingParent : danglers.keySet()) {
            if (!packet.hasItem(danglingParent)) {
                // TODO fail no parent event
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, 
                        "''{0}'' is not known to exist in the repository and is not part of the commit, yet its child ''{1}'' is part of the commit",
                        danglingParent.getAbsolutePath(), danglers.get(danglingParent).getAbsolutePath());
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        }
        
        return packet;
    }

    private static void handleDescendants(SVNWCContext context, SvnCommitPacket packet, SVNURL rootUrl, Collection<SvnCommitItem> items, ISvnUrlKindCallback urlKindCallback, ISVNEventHandler eventHandler) throws SVNException {
        for (SvnCommitItem item : items) {
            if (!item.hasFlag(SvnCommitItem.ADD) || item.getCopyFromUrl() == null) {
                continue;
            }
            if (eventHandler != null) {
                eventHandler.checkCancelled();
            }
            Collection<File> notPresent = SvnWcDbReader.getNotPresentDescendants((SVNWCDb) context.getDb(), item.getPath());
            for (File absent : notPresent) {
                boolean itemFound = false;
                File localAbsPath = SVNFileUtil.createFilePath(item.getPath(), absent);
                for (SvnCommitItem i : items) {
                    if (i.getPath().equals(localAbsPath)) {
                        itemFound = true;
                        break; 
                    }
                }
                if (itemFound) {
                    continue;
                }
                SVNURL url = SVNWCUtils.join(item.getCopyFromUrl(), absent);
                SVNNodeKind kind = SVNNodeKind.UNKNOWN;
                if (urlKindCallback != null) {
                    kind = urlKindCallback.getUrlKind(url, item.getCopyFromRevision());
                    if (kind == SVNNodeKind.NONE) {
                        continue;
                    }
                }
                packet.addItem(localAbsPath, rootUrl, kind, SVNWCUtils.join(item.getUrl(), absent),  -1, null, -1, SvnCommitItem.DELETE);
            }
        }
    }

    public static void harvestCommittables(SVNWCContext context, File localAbsPath, SvnCommitPacket committables,
            Map<SVNURL, String> lockTokens, SVNURL repositoryRootUrl, File copyModeRelPath, boolean copyModeRoot,
            SVNDepth depth, boolean justLocked, Collection<String> changelists, Map<File, File> danglers, boolean skipFiles, boolean skipDirs,
            ISvnUrlKindCallback urlKindCallback, ISvnCommitParameters commitParameters, Map<File, String> externalsStorage, ISVNEventHandler eventHandler) throws SVNException {

        CommitStatusWalker commitStatusWalker = new CommitStatusWalker();
        commitStatusWalker.rootAbsPath = localAbsPath;
        commitStatusWalker.committables = committables;
        commitStatusWalker.lockTokens = lockTokens;
        commitStatusWalker.commitRelPath = copyModeRelPath;
        commitStatusWalker.depth = depth;
        commitStatusWalker.justLocked = justLocked;
        commitStatusWalker.changeLists = changelists;
        commitStatusWalker.danglers = danglers;
        commitStatusWalker.checkUrlCallback = urlKindCallback;
        commitStatusWalker.context = context;
        commitStatusWalker.eventHandler = eventHandler;
        commitStatusWalker.externalsStorage = externalsStorage;

        SVNStatusEditor17 editor = new SVNStatusEditor17(localAbsPath, context, context.getOptions(), false, copyModeRelPath != null, depth, commitStatusWalker);
        editor.walkStatus(localAbsPath, depth, copyModeRelPath != null, false, false, null);

/*
        if (committables.hasItem(localAbsPath)) {
            return;
        }
        
        boolean copyMode = commitRelPath != null;

        if (eventHandler != null) {
            eventHandler.checkCancelled();
        }
        
        Structure<NodeCommitStatus> commitStatus = getNodeCommitStatus(context, localAbsPath);
        if ((skipFiles && commitStatus.get(NodeCommitStatus.kind) == SVNNodeKind.FILE) || commitStatus.is(NodeCommitStatus.excluded)) {
            commitStatus.release();
            return;
        }
        if (commitStatus.get(NodeCommitStatus.reposRelPath) == null && commitRelPath != null) {
            commitStatus.set(NodeCommitStatus.reposRelPath, commitRelPath);
        }
        
        CheckSpecialInfo checkSpecial = SVNWCContext.checkSpecialPath(localAbsPath);
        SVNNodeKind workingKind = checkSpecial.kind;
        boolean isSpecial = checkSpecial.isSpecial;
        if ((workingKind != SVNNodeKind.FILE) && (workingKind != SVNNodeKind.DIR) && (workingKind != SVNNodeKind.NONE)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.NODE_UNKNOWN_KIND, "Unknown entry kind for ''{0}''", localAbsPath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        boolean matchesChangelists = context.isChangelistMatch(localAbsPath, changelists);
        if (workingKind != SVNNodeKind.DIR && workingKind != SVNNodeKind.NONE && !matchesChangelists) {
            commitStatus.release();
            return;
        }
        if ((!commitStatus.is(NodeCommitStatus.symlink) && isSpecial ||
                (SVNFileUtil.symlinksSupported() && (commitStatus.is(NodeCommitStatus.symlink) && !isSpecial))) &&
                workingKind != SVNNodeKind.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.NODE_UNEXPECTED_KIND, "Entry ''{0}'' has unexpectedly changed special status", localAbsPath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        if (copyMode && 
                commitStatus.is(NodeCommitStatus.updateRoot) && 
                commitStatus.get(NodeCommitStatus.kind) == SVNNodeKind.FILE) {
            if (copyMode) {
                commitStatus.release();
                return;
            }
        }
        
        if (commitStatus.is(NodeCommitStatus.conflicted) && matchesChangelists) {
            ConflictInfo ci = context.getConflicted(localAbsPath, true, true, true);
            if (ci.propConflicted || ci.textConflicted || ci.treeConflicted) {
                if (eventHandler != null) {
                    // TODO failed conflict event.
                }
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.WC_FOUND_CONFLICT, "Aborting commit: ''{0}'' remains in conflict", localAbsPath);
                SVNErrorManager.error(error, SVNLogType.WC);
            }
        }
        if (commitStatus.is(NodeCommitStatus.deleted) && !commitStatus.is(NodeCommitStatus.isOpRoot)) {
            commitStatus.release();
            return;
        }
        if (commitStatus.get(NodeCommitStatus.reposRelPath) == null) {
            File reposRelPath = context.getNodeReposRelPath(localAbsPath);
            commitStatus.set(NodeCommitStatus.reposRelPath, reposRelPath);
        }
        int stateFlags = 0;
        
        if (commitStatus.is(NodeCommitStatus.deleted) || commitStatus.is(NodeCommitStatus.isReplaceRoot)) {
            stateFlags |= SvnCommitItem.DELETE;
        } else if (commitStatus.is(NodeCommitStatus.notPresent)) {
            if (!copyMode) {
                commitStatus.release();
                return;
            }
            if (urlKindCallback != null) {
                Structure<NodeOriginInfo> originInfo = context.getNodeOrigin(SVNFileUtil.getParentFile(localAbsPath), false, NodeOriginInfo.revision, NodeOriginInfo.reposRelpath);
                File reposRelPath = SVNFileUtil.createFilePath(originInfo.<File>get(NodeOriginInfo.reposRelpath), SVNFileUtil.getFileName(localAbsPath));
                SVNURL url = SVNWCUtils.join(repositoryRootUrl, reposRelPath);
                long revision = originInfo.lng(NodeOriginInfo.revision);
                originInfo.release();
                
                if (urlKindCallback.getUrlKind(url, revision) == SVNNodeKind.NONE) {
                    commitStatus.release();
                    return;
                }
            }
            stateFlags |= SvnCommitItem.DELETE;
        }
        
        File copyFromPath = null;
        long copyFromRevision = -1;
        if (commitStatus.is(NodeCommitStatus.added) && commitStatus.is(NodeCommitStatus.isOpRoot)) {
            stateFlags |= SvnCommitItem.ADD;
            if (commitStatus.get(NodeCommitStatus.originalReposRelPath) != null) {
                stateFlags |= SvnCommitItem.COPY;
                copyFromPath = commitStatus.get(NodeCommitStatus.originalReposRelPath);
                copyFromRevision = commitStatus.lng(NodeCommitStatus.originalRevision);
            }
        }
        if (copyMode
                && (!commitStatus.is(NodeCommitStatus.added) || copyModeRoot)
                && !((stateFlags & SvnCommitItem.DELETE) != 0)) {
            long dirRevision = 0;
            if (!copyModeRoot) {
                dirRevision = context.getNodeBaseRev(SVNFileUtil.getParentFile(localAbsPath));
            }
            if (copyModeRoot || dirRevision != commitStatus.lng(NodeCommitStatus.revision)) {
                stateFlags |= SvnCommitItem.ADD;
                Structure<NodeOriginInfo> originInfo = context.getNodeOrigin(localAbsPath, false, NodeOriginInfo.revision, NodeOriginInfo.reposRelpath);
                copyFromPath = originInfo.get(NodeOriginInfo.reposRelpath);
                copyFromRevision = originInfo.lng(NodeOriginInfo.revision);
                originInfo.release();
                if (copyFromPath != null) {
                    stateFlags |= SvnCommitItem.COPY;
                }
            }
        }
        
        boolean textModified = false;
        if ((stateFlags & SvnCommitItem.ADD) != 0) {
            if (workingKind == SVNNodeKind.NONE) {
                // TODO failed missing event.
                SVNErrorMessage error = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "''{0}'' is scheduled for addition, but is missing", localAbsPath);
                SVNErrorManager.error(error, SVNLogType.WC);
            }
            if (commitStatus.get(NodeCommitStatus.kind) == SVNNodeKind.FILE) {
                if ((stateFlags & SvnCommitItem.COPY) != 0) {
                    textModified = context.isTextModified(localAbsPath, false);
                } else {
                    textModified = true;
                }
            }
        } else if ((stateFlags & SvnCommitItem.DELETE) == 0) {
            if (commitStatus.get(NodeCommitStatus.kind) == SVNNodeKind.FILE) {
                textModified = context.isTextModified(localAbsPath, false);
            }
        }
        
        boolean propsModified = commitStatus.is(NodeCommitStatus.propsMod);
        if (textModified) {
            stateFlags |= SvnCommitItem.TEXT_MODIFIED;
        }
        if (propsModified) {
            stateFlags |= SvnCommitItem.PROPS_MODIFIED;
        }
        if (commitStatus.get(NodeCommitStatus.lockToken) != null && lockTokens != null && (stateFlags != 0 || justLocked)) {
            stateFlags |= SvnCommitItem.LOCK;
        }
        
        if ((stateFlags & ~(SvnCommitItem.LOCK | SvnCommitItem.PROPS_MODIFIED | SvnCommitItem.COPY)) == 0 && matchesChangelists) {
            if (workingKind == SVNNodeKind.NONE) {
                if (commitParameters != null) {
                    ISvnCommitParameters.Action action = ISvnCommitParameters.Action.SKIP;
                    SVNNodeKind nodeKind = commitStatus.get(NodeCommitStatus.kind); 
                    if (nodeKind == SVNNodeKind.DIR) {
                        action = commitParameters.onMissingDirectory(localAbsPath);
                    } else if (nodeKind == SVNNodeKind.FILE) {
                        action = commitParameters.onMissingFile(localAbsPath);
                    }
                    if (action == Action.DELETE) {
                        stateFlags |= SvnCommitItem.DELETE;
                        // schedule file or dir for deletion!
                        SvnNgRemove.delete(context, localAbsPath, null, false, false, null);
                    } else if (action == Action.ERROR) {
                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_LOCKED, "Working copy {1} ''{0}'' is missing", localAbsPath, nodeKind == SVNNodeKind.DIR ? "directory" : "file");
                        SVNErrorManager.error(err, SVNLogType.WC);
                    }
                }
            }
        }
        
        if (stateFlags != 0 && matchesChangelists) {
            SvnCommitItem item = committables.addItem(localAbsPath, 
                    commitStatus.<SVNNodeKind>get(NodeCommitStatus.kind), 
                    repositoryRootUrl, 
                    SVNFileUtil.getFilePath(copyMode ? commitRelPath : commitStatus.<File>get(NodeCommitStatus.reposRelPath)), 
                    copyMode ? -1 : commitStatus.lng(NodeCommitStatus.revision), 
                    SVNFileUtil.getFilePath(copyFromPath), 
                    copyFromRevision, 
                    stateFlags);
            if (item.hasFlag(SvnCommitItem.LOCK) && lockTokens != null) {
                lockTokens.put(item.getUrl(), commitStatus.<String>get(NodeCommitStatus.lockToken));
            }
        }
        if (matchesChangelists) {            
            if (externalsStorage != null) {
                SVNProperties properties = context.getActualProps(localAbsPath);
                if (properties != null) {
                    String externalsProperty = properties.getStringValue(SVNProperty.EXTERNALS);
                    if (externalsProperty != null) {
                        externalsStorage.put(localAbsPath, externalsProperty);
                    }
                }
            }

        }
        if (lockTokens != null && (stateFlags & SvnCommitItem.DELETE) != 0) {
            collectLocks(context, localAbsPath, lockTokens);
        }
        try {
            if (commitStatus.get(NodeCommitStatus.kind) != SVNNodeKind.DIR || depth.compareTo(SVNDepth.EMPTY) <= 0) {
                return;
            }
            bailOnTreeConflictedChildren(context, localAbsPath, commitStatus.<SVNNodeKind>get(NodeCommitStatus.kind), depth, changelists);
        } finally {
            commitStatus.release();
        }
        
        if ((stateFlags & SvnCommitItem.DELETE) == 0 || (stateFlags & SvnCommitItem.ADD) != 0) {
            SVNDepth depthBelowHere = depth;
            if (depth.compareTo(SVNDepth.INFINITY) < 0) {
                depthBelowHere = SVNDepth.EMPTY;
            }
            List<File> children = context.getChildrenOfWorkingNode(localAbsPath, copyMode);
            for (File child : children) {
                String name = SVNFileUtil.getFileName(child);
                File childCommitRelPath = null;
                if (commitRelPath != null) {
                    childCommitRelPath = SVNFileUtil.createFilePath(commitRelPath, name);
                }
                harvestCommittables(context, child, committables, lockTokens, repositoryRootUrl, childCommitRelPath, false, depthBelowHere, justLocked, changelists, 
                        depth.compareTo(SVNDepth.FILES) < 0, 
                        depth.compareTo(SVNDepth.IMMEDIATES) < 0, 
                        urlKindCallback, commitParameters, externalsStorage, eventHandler);
            }
        }
        */
    }

    private static class CommitStatusWalker implements ISvnObjectReceiver<SvnStatus> {

        public File rootAbsPath;
        public File commitRelPath;
        public SVNDepth depth;
        public boolean justLocked;
        public Collection<String> changeLists;
        public Map<File, File> danglers;
        public ISvnUrlKindCallback checkUrlCallback;
        public ISVNEventHandler eventHandler;
        public SVNWCContext context;
        public File skipBelowAbsPath;
        public Map<SVNURL, String> lockTokens;
        public Map<File, String> externalsStorage;

        public SvnCommitPacket committables;

        public void receive(SvnTarget target, SvnStatus status) throws SVNException {
            File localAbsPath = target.getFile();

            int stateFlags = 0;

            boolean isHarvestRoot = rootAbsPath.equals(localAbsPath);
            boolean copyModeRoot = commitRelPath != null && isHarvestRoot;
            File movedFromAbsPath = null;
            File commitRelPath = null;

            if (this.commitRelPath != null) {
                commitRelPath = SVNFileUtil.createFilePath(this.commitRelPath, SVNFileUtil.skipAncestor(this.rootAbsPath, localAbsPath));
            }
            boolean copyMode = commitRelPath != null;

            if (this.skipBelowAbsPath != null && SVNPathUtil.isAncestor(SVNFileUtil.getFilePath(this.skipBelowAbsPath), SVNFileUtil.getFilePath(localAbsPath))) {
                return;
            } else {
                this.skipBelowAbsPath = null;
            }

            if (SVNStatusType.STATUS_UNVERSIONED.equals(status.getNodeStatus()) ||
                    SVNStatusType.STATUS_IGNORED.equals(status.getNodeStatus()) ||
                    SVNStatusType.STATUS_EXTERNAL.equals(status.getNodeStatus()) ||
                    SVNStatusType.STATUS_NONE.equals(status.getNodeStatus())) {
                if (isHarvestRoot) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "''{0}'' is not under version control", localAbsPath);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }
                return;
            } else if (SVNStatusType.STATUS_NORMAL.equals(status.getNodeStatus())) {
                if (!copyMode && !status.isConflicted() && !(this.justLocked && status.getLock() != null)) {
                    return;
                }
            }

            if (committables.hasItem(localAbsPath)) {
                return;
            }

            assert (copyMode && commitRelPath != null) || (!copyMode && commitRelPath == null);
            assert (copyModeRoot && copyMode) || !copyModeRoot;

            boolean matchesChangeLists = (changeLists == null) || (status.getChangelist() != null && changeLists.contains(status.getChangelist()));

            if (status.getKind() != SVNNodeKind.DIR && !matchesChangeLists) {
                return;
            }

            if (status.isConflicted() && matchesChangeLists) {
                if (this.eventHandler != null) {
                    this.eventHandler.handleEvent(SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.UNKNOWN, null, -1, SVNEventAction.FAILED_CONFLICT, SVNEventAction.FAILED_CONFLICT, null, null), ISVNEventHandler.UNKNOWN);
                }
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_FOUND_CONFLICT, "Aborting commit: ''{0}'' remains in conflict", localAbsPath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            } else if (status.getNodeStatus() == SVNStatusType.STATUS_OBSTRUCTED) {
                if (this.eventHandler != null) {
                    this.eventHandler.handleEvent(SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.UNKNOWN, null, -1, SVNEventAction.FAILED_OBSTRUCTION, SVNEventAction.FAILED_OBSTRUCTION, null, null), ISVNEventHandler.UNKNOWN);
                }
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.NODE_UNEXPECTED_KIND, "Node ''{0}'' has unexpectedly changed kind", localAbsPath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
            if (this.externalsStorage != null && matchesChangeLists) {
                SVNProperties properties = context.getActualProps(localAbsPath);
                if (properties != null) {
                    String externalsProperty = properties.getStringValue(SVNProperty.EXTERNALS);
                    if (externalsProperty != null) {
                        externalsStorage.put(localAbsPath, externalsProperty);
                    }
                }
            }
            if (status.isFileExternal() && !isHarvestRoot) {
                return;
            }

            Structure<NodeCommitStatus> nodeCommitStatus = getNodeCommitStatus(context, localAbsPath);
            boolean isAdded = nodeCommitStatus.is(NodeCommitStatus.added);
            boolean isDeleted = nodeCommitStatus.is(NodeCommitStatus.deleted);
            boolean isReplaced = nodeCommitStatus.is(NodeCommitStatus.isReplaceRoot);
            boolean isOpRoot = nodeCommitStatus.is(NodeCommitStatus.isOpRoot);
            long nodeRev = nodeCommitStatus.lng(NodeCommitStatus.revision);
            long originalRev = nodeCommitStatus.lng(NodeCommitStatus.originalRevision);
            File originalRelPath = nodeCommitStatus.get(NodeCommitStatus.originalReposRelPath);

            if (status.getNodeStatus() == SVNStatusType.STATUS_MISSING && matchesChangeLists) {
                if (isAdded && isOpRoot) {
                    if (this.eventHandler != null) {
                        this.eventHandler.handleEvent(SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.UNKNOWN, null, -1, SVNEventAction.FAILED_MISSING, SVNEventAction.FAILED_MISSING, null, null), ISVNEventHandler.UNKNOWN);
                    }
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "''{0}'' is scheduled for addition, but is missing", localAbsPath);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }
                return;
            }

            if (isDeleted && !isOpRoot) {
                return;
            }

            if (isDeleted || isReplaced) {
                stateFlags |= SvnCommitItem.DELETE;
            }

            File cfRelPath = null;
            long cfRev = -1;
            if (isAdded && isOpRoot) {
                stateFlags |= SvnCommitItem.ADD;
                if (originalRelPath != null) {
                    stateFlags |= SvnCommitItem.COPY;
                    cfRelPath = originalRelPath;
                    cfRev = originalRev;

                    if (status.getMovedFromPath() != null && !copyMode) {
                        stateFlags |= SvnCommitItem.MOVED_HERE;
                        movedFromAbsPath = status.getMovedFromPath();
                    }
                }
            } else if (copyMode && ((stateFlags & SvnCommitItem.DELETE) == 0)) {
                long dirRev = -1;
                if (!copyModeRoot && !status.isSwitched() && !isAdded) {
                    WCDbBaseInfo nodeBase = context.getNodeBase(SVNFileUtil.getFileDir(localAbsPath), false, false);
                    dirRev = nodeBase.revision;
                }
                if (copyModeRoot || status.isSwitched() || nodeRev != dirRev) {
                    stateFlags |= SvnCommitItem.ADD;
                    stateFlags |= SvnCommitItem.COPY;
                    if (status.isCopied()) {
                        cfRev = originalRev;
                        cfRelPath = originalRelPath;
                    } else {
                        cfRev = status.getRevision();
                        cfRelPath = SVNFileUtil.createFilePath(status.getRepositoryRelativePath());
                    }
                }
            }

            if ((((stateFlags & SvnCommitItem.DELETE) == 0) || ((stateFlags & SvnCommitItem.ADD) != 0))) {
                boolean textMod = false;
                boolean propMod = false;

                if (status.getKind() == SVNNodeKind.FILE) {
                    if (((stateFlags & SvnCommitItem.ADD) != 0) && (stateFlags & SvnCommitItem.COPY) == 0) {
                        textMod = true;
                    } else {
                        textMod = status.getTextStatus() != SVNStatusType.STATUS_NORMAL;
                    }
                }

                propMod = (status.getPropertiesStatus() != SVNStatusType.STATUS_NORMAL) && (status.getPropertiesStatus() != SVNStatusType.STATUS_NONE);

                if (textMod) {
                    stateFlags |= SvnCommitItem.TEXT_MODIFIED;
                }
                if (propMod) {
                    stateFlags |= SvnCommitItem.PROPS_MODIFIED;
                }
            }

            if (status.getLock() != null && lockTokens != null && (stateFlags != 0 || justLocked)) {
                stateFlags |= SvnCommitItem.LOCK;
            }

            if (matchesChangeLists && (stateFlags != 0)) {
                File reposRelPath = copyMode ? commitRelPath : SVNFileUtil.createFilePath(status.getRepositoryRelativePath());
                long revision = copyMode ? -1 : nodeRev;

                Map<SVNURL, String> lockTokens = this.lockTokens;
                SVNLock lock = status.getLock();

                assert status.getRepositoryRootUrl() != null && reposRelPath != null;

                SvnCommitItem item = committables.addItem(localAbsPath, status.getKind(), status.getRepositoryRootUrl(), SVNFileUtil.getFilePath(reposRelPath), revision, SVNFileUtil.getFilePath(cfRelPath), cfRev, movedFromAbsPath, stateFlags);
                if (lockTokens != null && lock != null && ((stateFlags & SvnCommitItem.LOCK) != 0)) {
                    lockTokens.put(item.getUrl(), lock.getID());
                }
            }

            if (matchesChangeLists && ((stateFlags & SvnCommitItem.DELETE) != 0) && !copyMode && SVNRevision.isValidRevisionNumber(nodeRev) && this.lockTokens != null) {
                Map<SVNURL, String> localRelPathTokens = context.getDb().getNodeLockTokensRecursive(localAbsPath);
                for (Map.Entry<SVNURL, String> entry : localRelPathTokens.entrySet()) {
                    SVNURL key = entry.getKey();
                    String value = entry.getValue();
                    if (value != null) {
                        this.lockTokens.put(key, value);
                    }
                }
            }

            if (matchesChangeLists && (isHarvestRoot || this.changeLists != null) && (stateFlags != 0) && isAdded && this.danglers != null) {
                Map<File, File> danglers = this.danglers;
                File parentAbsPath = SVNFileUtil.getParentFile(localAbsPath);

                boolean parentAdded;
                if (committables.hasItem(parentAbsPath)) {
                    parentAdded = false;
                } else {
                    parentAdded = context.isNodeAdded(parentAbsPath);
                }

                if (parentAdded) {
                    Structure<NodeOriginInfo> nodeOrigin = context.getNodeOrigin(parentAbsPath, false, NodeOriginInfo.isCopy, NodeOriginInfo.copyRootAbsPath);
                    boolean parentIsCopy = nodeOrigin.is(NodeOriginInfo.isCopy);
                    File copyRootAbsPath = nodeOrigin.get(NodeOriginInfo.copyRootAbsPath);

                    if (parentIsCopy) {
                        parentAbsPath = copyRootAbsPath;
                    }

                    if (!danglers.containsKey(parentAbsPath)) {
                        danglers.put(parentAbsPath, localAbsPath);
                    }
                }
            }

            if (isDeleted && !isAdded) {
                if (status.getKind() == SVNNodeKind.DIR) {
                    this.skipBelowAbsPath = localAbsPath;
                }
                return;
            }

            if (copyMode && !isAdded && !isDeleted && status.getKind() == SVNNodeKind.DIR) {
                harvestNotPresentForCopy(context, localAbsPath, committables, status.getRepositoryRootUrl(), commitRelPath, checkUrlCallback);
            }
        }
    }

    private static void harvestNotPresentForCopy(SVNWCContext context, File localAbsPath, SvnCommitPacket committables, SVNURL reposRootUrl, File commitRelPath, ISvnUrlKindCallback urlKindCallback) throws SVNException {
        List<File> children = context.getChildrenOfWorkingNode(localAbsPath, true);
        for (File thisAbsPath : children) {
            String name = SVNFileUtil.getFileName(thisAbsPath);
            SVNWCContext.NodePresence nodePresence = context.getNodePresence(thisAbsPath, false);
            if (!nodePresence.isNotPresent) {
                continue;
            }
            File thisCommitRelPath;
            if (commitRelPath == null) {
                thisCommitRelPath = null;
            } else {
                thisCommitRelPath = SVNFileUtil.createFilePath(commitRelPath, name);
            }
            SVNNodeKind kind;
            if (urlKindCallback != null) {
                Structure<NodeOriginInfo> nodeOrigin = context.getNodeOrigin(SVNFileUtil.getParentFile(thisAbsPath), false, NodeOriginInfo.revision, NodeOriginInfo.reposRelpath, NodeOriginInfo.reposRootUrl);
                long parentRev = nodeOrigin.lng(NodeOriginInfo.revision);
                File parentReposRelPath = nodeOrigin.get(NodeOriginInfo.reposRelpath);
                SVNURL parentReposRootUrl = nodeOrigin.get(NodeOriginInfo.reposRootUrl);

                SVNURL nodeUrl = parentReposRootUrl.appendPath(SVNFileUtil.getFilePath(parentReposRelPath), false).appendPath(SVNFileUtil.getFileName(thisAbsPath), false);

                kind = urlKindCallback.getUrlKind(nodeUrl, parentRev);
                if (kind == SVNNodeKind.NONE) {
                    continue;
                }
            } else {
                kind = context.readKind(thisAbsPath, true);
            }

            committables.addItem(thisAbsPath, kind, reposRootUrl, SVNFileUtil.getFilePath(thisCommitRelPath), -1, null, -1, null, SvnCommitItem.DELETE);
        }
    }

    private static Structure<NodeCommitStatus> getNodeCommitStatus(SVNWCContext context, File localAbsPath) throws SVNException {
        Structure<NodeCommitStatus> result = Structure.obtain(NodeCommitStatus.class);
        Structure<NodeInfo> nodeInfo = context.getDb().readInfo(localAbsPath, NodeInfo.status, NodeInfo.kind, NodeInfo.revision,
                NodeInfo.reposRelPath, NodeInfo.originalRevision, NodeInfo.originalReposRelpath, NodeInfo.lock,
                NodeInfo.changelist, NodeInfo.conflicted, NodeInfo.opRoot, NodeInfo.hadProps,
                NodeInfo.propsMod, NodeInfo.haveBase, NodeInfo.haveMoreWork);
        
        if (nodeInfo.get(NodeInfo.kind) == SVNWCDbKind.File) {
            result.set(NodeCommitStatus.kind, SVNNodeKind.FILE);
        } else if (nodeInfo.get(NodeInfo.kind) == SVNWCDbKind.Dir) {
            result.set(NodeCommitStatus.kind, SVNNodeKind.DIR);
        } else {
            result.set(NodeCommitStatus.kind, SVNNodeKind.UNKNOWN);
        }
        result.set(NodeCommitStatus.reposRelPath, nodeInfo.get(NodeInfo.reposRelPath));
        result.set(NodeCommitStatus.revision, nodeInfo.lng(NodeInfo.revision));
        result.set(NodeCommitStatus.originalReposRelPath, nodeInfo.get(NodeInfo.originalReposRelpath));
        result.set(NodeCommitStatus.originalRevision, nodeInfo.lng(NodeInfo.originalRevision));
        result.set(NodeCommitStatus.changelist, nodeInfo.get(NodeInfo.changelist));
        result.set(NodeCommitStatus.propsMod, nodeInfo.is(NodeInfo.propsMod));
        
        SVNWCDbStatus nodeStatus = nodeInfo.get(NodeInfo.status);
        
        result.set(NodeCommitStatus.added, nodeStatus == SVNWCDbStatus.Added);
        result.set(NodeCommitStatus.deleted, nodeStatus == SVNWCDbStatus.Deleted);
        result.set(NodeCommitStatus.notPresent, nodeStatus == SVNWCDbStatus.NotPresent);
        result.set(NodeCommitStatus.excluded, nodeStatus == SVNWCDbStatus.Excluded);
        result.set(NodeCommitStatus.isOpRoot, nodeInfo.is(NodeInfo.opRoot));
        result.set(NodeCommitStatus.conflicted, nodeInfo.is(NodeInfo.conflicted));
        
        if (nodeStatus == SVNWCDbStatus.Added && nodeInfo.is(NodeInfo.opRoot) &&
                (nodeInfo.is(NodeInfo.haveBase) || nodeInfo.is(NodeInfo.haveMoreWork))) {
            Structure<ReplaceInfo> replaceInfo = SvnWcDbReader.readNodeReplaceInfo((SVNWCDb) context.getDb(), localAbsPath, ReplaceInfo.replaceRoot);
            result.set(NodeCommitStatus.isReplaceRoot, replaceInfo.is(ReplaceInfo.replaceRoot));
            replaceInfo.release();
        } else {
            result.set(NodeCommitStatus.isReplaceRoot, false);
        }
        if (nodeInfo.get(NodeInfo.kind) == SVNWCDbKind.File && (nodeInfo.is(NodeInfo.hadProps) || nodeInfo.is(NodeInfo.propsMod))) {
            SVNProperties properties = context.getDb().readProperties(localAbsPath);
            result.set(NodeCommitStatus.symlink, properties.getStringValue(SVNProperty.SPECIAL) != null);
        } else {
            result.set(NodeCommitStatus.symlink, false);
        }
        
        if (nodeInfo.is(NodeInfo.haveBase) && (nodeInfo.lng(NodeInfo.revision) < 0 || nodeStatus == SVNWCDbStatus.Normal)) {
            WCDbBaseInfo baseInfo =context.getDb().getBaseInfo(localAbsPath, BaseInfoField.revision, BaseInfoField.updateRoot);
            result.set(NodeCommitStatus.revision, baseInfo.revision);
            result.set(NodeCommitStatus.updateRoot, baseInfo.updateRoot);
        } else {
            result.set(NodeCommitStatus.updateRoot, false);
        }
        SVNWCDbLock lock = nodeInfo.get(NodeInfo.lock);
        result.set(NodeCommitStatus.lockToken, lock != null ? lock.token : null);
        nodeInfo.release();
        return result;
    }


    private static void bailOnTreeConflictedChildren(SVNWCContext context, File localAbsPath, SVNNodeKind kind, SVNDepth depth, Collection<String> changelistsSet) throws SVNException {
        if ((depth == SVNDepth.EMPTY) || (kind != SVNNodeKind.DIR)) {
            return;
        }
        Map<String, SVNTreeConflictDescription> conflicts = context.getDb().opReadAllTreeConflicts(localAbsPath);
        if (conflicts == null || conflicts.isEmpty()) {
            return;
        }
        for (SVNTreeConflictDescription conflict : conflicts.values()) {
            if ((conflict.getNodeKind() == SVNNodeKind.DIR) && (depth == SVNDepth.FILES)) {
                continue;
            }
            if (!context.isChangelistMatch(localAbsPath, changelistsSet)) {
                continue;
            }
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_FOUND_CONFLICT, "Aborting commit: ''{0}'' remains in conflict", conflict.getPath());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }
    
    private static void collectLocks(final SVNWCContext context, File path, final Map<SVNURL, String> lockTokens) throws SVNException {
        ISVNWCNodeHandler nodeHandler = new ISVNWCNodeHandler() {
            public void nodeFound(File localAbspath, SVNWCDbKind kind) throws SVNException {
                SVNWCDbLock nodeLock = context.getNodeLock(localAbspath);
                if (nodeLock == null || nodeLock.token == null) {
                    return;
                }
                SVNURL url = context.getNodeUrl(localAbspath);
                if (url != null) {
                    lockTokens.put(url, nodeLock.token);
                }
            }
        };
        context.nodeWalkChildren(path, nodeHandler, false, SVNDepth.INFINITY, null);
    }
    
    private static void bailOnTreeConflictedAncestor(SVNWCContext context, File firstAbspath) throws SVNException {
        File localAbspath;
        File parentAbspath;
        boolean wcRoot;
        boolean treeConflicted;
        localAbspath = firstAbspath;
        while (true) {
            wcRoot = context.checkWCRoot(localAbspath, false).wcRoot;
            if (wcRoot) {
                break;
            }
            parentAbspath = SVNFileUtil.getFileDir(localAbspath);
            treeConflicted = context.getConflicted(parentAbspath, false, false, true).treeConflicted;
            if (treeConflicted) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_FOUND_CONFLICT, "Aborting commit: ''{0}'' remains in tree-conflict", localAbspath);
                SVNErrorManager.error(err, SVNLogType.WC);
                return;
            }
            localAbspath = parentAbspath;
        }
    }

    public static SVNURL translateCommitables(Collection<SvnCommitItem> items, Map<String, SvnCommitItem> decodedPaths) throws SVNException {
        Map<SVNURL, SvnCommitItem> itemsMap = new HashMap<SVNURL, SvnCommitItem>();

        for (SvnCommitItem item : items) {
            if (itemsMap.containsKey(item.getUrl())) {
                SvnCommitItem oldItem = (SvnCommitItem) itemsMap.get(item.getUrl());
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_DUPLICATE_COMMIT_URL, 
                        "Cannot commit both ''{0}'' and ''{1}'' as they refer to the same URL",
                        new Object[] {item.getPath(), oldItem.getPath()});
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            itemsMap.put(item.getUrl(), item);
        }

        Iterator<SVNURL> urls = itemsMap.keySet().iterator();
        SVNURL baseURL = urls.next();
        while (urls.hasNext()) {
            SVNURL url = (SVNURL) urls.next();
            baseURL = SVNURLUtil.getCommonURLAncestor(baseURL, url);
        }
        if (itemsMap.containsKey(baseURL)) {
            SvnCommitItem rootItem = (SvnCommitItem) itemsMap.get(baseURL);
            if (rootItem.getKind() != SVNNodeKind.DIR) {
                baseURL = baseURL.removePathTail();
            } else if (rootItem.getKind() == SVNNodeKind.DIR
                    && (rootItem.hasFlag(SvnCommitItem.ADD) 
                            || rootItem.hasFlag(SvnCommitItem.DELETE) 
                            || rootItem.hasFlag(SvnCommitItem.COPY) 
                            || rootItem.hasFlag(SvnCommitItem.LOCK))) {
                baseURL = baseURL.removePathTail();
            }
        }
        if (baseURL == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_URL, 
                    "Cannot compute base URL for commit operation");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        for (Iterator<SVNURL> iterator = itemsMap.keySet().iterator(); iterator.hasNext();) {
            SVNURL url = iterator.next();
            SvnCommitItem item = itemsMap.get(url);
            String realPath = url.equals(baseURL) ? "" : SVNPathUtil.getRelativePath(baseURL.getPath(), url.getPath());
            decodedPaths.put(realPath, item);
        }
        return baseURL;
    }

    public static Map<String, String> translateLockTokens(Map<SVNURL, String> lockTokens, SVNURL baseURL) {
        Map<String, String> translatedLocks = new TreeMap<String, String>();
        for (Iterator<SVNURL> urls = lockTokens.keySet().iterator(); urls.hasNext();) {
            SVNURL url = urls.next();
            if (!SVNURLUtil.isAncestor(baseURL, url)) {
                continue;
            }
            String token = lockTokens.get(url);
            String path = url.getPath().substring(baseURL.getPath().length());
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            translatedLocks.put(path, token);
        }
        return translatedLocks;
    }
}
