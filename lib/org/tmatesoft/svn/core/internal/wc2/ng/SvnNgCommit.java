package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.SVNCommitUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNPropertiesManager;
import org.tmatesoft.svn.core.internal.wc17.SVNCommitMediator17;
import org.tmatesoft.svn.core.internal.wc17.SVNCommitter17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc2.ISvnCommitRunner;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgCommitUtil.ISvnUrlKindCallback;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.core.wc2.SvnCommit;
import org.tmatesoft.svn.core.wc2.SvnCommitItem;
import org.tmatesoft.svn.core.wc2.SvnCommitPacket;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgCommit extends SvnNgOperationRunner<SVNCommitInfo, SvnCommit> implements ISvnCommitRunner, ISvnUrlKindCallback {

    public SvnCommitPacket collectCommitItems(SvnCommit operation) throws SVNException {
        int depthEmptyAfter = -1;
        setOperation(operation);

        SvnCommitPacket packet = new SvnCommitPacket();
        Collection<String> targets = new ArrayList<String>();

        String[] validatedPaths = new String[getOperation().getTargets().size()];
        int i = 0;
        for(SvnTarget target : getOperation().getTargets()) {
            validatedPaths[i] = target.getFile().getAbsolutePath();
            validatedPaths[i] = validatedPaths[i].replace(File.separatorChar, '/');
            i++;
        }
        String rootPath = SVNPathUtil.condencePaths(validatedPaths, targets, false);
        if (rootPath == null) {
            return packet;
        }
        File baseDir = new File(rootPath).getAbsoluteFile();
        if (targets.isEmpty()) {
            targets.add("");
        }
        if (getOperation().isIncludeFileExternals() || getOperation().isIncludeDirectoryExternals()) {
            if (getOperation().getDepth() != SVNDepth.UNKNOWN && getOperation().getDepth() != SVNDepth.INFINITY) {
                depthEmptyAfter = targets.size();
            }
            appendExternalsAsExplicitTargets(targets, baseDir, getOperation().isIncludeFileExternals(), getOperation().isIncludeDirectoryExternals(), getOperation().getDepth(), getWcContext());
        }
        Collection<File> lockTargets = determineLockTargets(baseDir, targets);
        Collection<File> lockedRoots = new HashSet<File>();
        try {
            for (File lockTarget : lockTargets) {
                File lockRoot = getWcContext().acquireWriteLock(lockTarget, false, true);
                lockedRoots.add(lockRoot);
            }
            packet.setLockingContext(this, lockedRoots);

            Map<SVNURL, String> lockTokens = new HashMap<SVNURL, String>();
            SvnNgCommitUtil.harvestCommittables(getWcContext(), packet, lockTokens,
                    baseDir, targets, depthEmptyAfter, getOperation().getDepth(),
                    !getOperation().isKeepLocks(), getOperation().getApplicableChangelists(),
                    this, getOperation().getCommitParameters(), null);
            packet.setLockTokens(lockTokens);

            if (getOperation().isFailOnMultipleRepositories() && packet.getRepositoryRoots().size() > 1) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE,
                        "Commit can only commit to a single repository at a time.\n" +
                        "Are all targets part of the same working copy?");
                SVNErrorManager.error(err, SVNLogType.WC);
            }

            if (!packet.isEmpty()) {
                return packet;
            } else {
                packet.dispose();
                return new SvnCommitPacket();
            }
        } catch (SVNException e) {
            packet.dispose();
            SVNErrorMessage err = e.getErrorMessage().wrap("Commit failed (details follow):");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return null;
    }

    private void appendExternalsAsExplicitTargets(Collection<String> targets, File baseAbsPath, boolean includeFileExternals, boolean includeDirectoryExternals, SVNDepth depth, SVNWCContext context) throws SVNException {
        if (!(includeFileExternals||includeDirectoryExternals)) {
            return;
        }
        if (depth == SVNDepth.EMPTY) {
            return;
        }
        List<String> newTargets = new ArrayList<String>();
        for (String target : targets) {
            File targetAbsPath = SVNFileUtil.createFilePath(baseAbsPath, target);
            List<SVNWCContext.CommittableExternalInfo> externals = context.committableExternalsBelow(null, targetAbsPath, depth);
            if (externals != null) {
                for (SVNWCContext.CommittableExternalInfo xInfo : externals) {
                    if ((xInfo.kind == SVNNodeKind.FILE && !includeFileExternals)||
                        (xInfo.kind == SVNNodeKind.DIR && !includeDirectoryExternals)) {
                        continue;
                    }
                    File targetRelPath = SVNFileUtil.skipAncestor(baseAbsPath, xInfo.localAbsPath);

                    assert targetRelPath != null && SVNFileUtil.getFilePath(targetRelPath).length() != 0;

                    newTargets.add(SVNFileUtil.getFilePath(targetRelPath));
                }
            }
        }
        targets.addAll(newTargets);
    }

    @Override
    protected SVNCommitInfo run(SVNWCContext context) throws SVNException {
        final SvnCommitPacket[] packets = getOperation().splitCommitPackets(getOperation().isCombinePackets());
        SVNCommitInfo result = SVNCommitInfo.NULL;
        for(int i = 0; i < packets.length; i++) {
            if (packets[i] == null || packets[i].isEmpty()) {
                continue;
            }
            packets[i] = packets[i].removeSkippedItems();
            final SVNURL repositoryRoot = packets[i].getRepositoryRoots().iterator().next();
            result = doRun(context, packets[i]);
            if (result != null) {
                getOperation().receive(SvnTarget.fromURL(repositoryRoot), result);
            }
        }
        return result;
    }

    protected SVNCommitInfo doRun(SVNWCContext context, SvnCommitPacket packet) throws SVNException {
        SVNProperties revisionProperties = getOperation().getRevisionProperties();
        SVNPropertiesManager.validateRevisionProperties(revisionProperties);

        SVNException bumpError = null;
        SVNCommitInfo info = null;
        try {
            for (SVNURL repositoryRoot : packet.getRepositoryRoots()) {
                Collection<SvnCommitItem> items = packet.getItems(repositoryRoot);
                for (SvnCommitItem item : items) {
                    if (item.hasFlag(SvnCommitItem.MOVED_HERE)) {
                        SVNWCContext.NodeMovedHere nodeMovedHere = context.nodeWasMovedHere(item.getPath());
                        File movedFromAbsPath = nodeMovedHere.movedFromAbsPath;
                        File deleteOpRootAbsPath = nodeMovedHere.deleteOpRootAbsPath;

                        if (movedFromAbsPath != null && deleteOpRootAbsPath != null && movedFromAbsPath.equals(deleteOpRootAbsPath)) {
                            boolean foundDeleteHalf = packet.getItem(deleteOpRootAbsPath) != null;
                            if (!foundDeleteHalf) {
                                File deleteHalfParentAbsPath = SVNFileUtil.getFileDir(deleteOpRootAbsPath);
                                if (!deleteOpRootAbsPath.equals(deleteHalfParentAbsPath)) {
                                    File parentDeleteOpRootAbsPath = context.getNodeDeletedAncestor(deleteHalfParentAbsPath);
                                    if (parentDeleteOpRootAbsPath != null) {
                                        foundDeleteHalf = packet.getItem(parentDeleteOpRootAbsPath) != null;
                                    }
                                }
                            }

                            if (!foundDeleteHalf) {
                                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "Cannot commit ''{0}'' because it was moved from " +
                                        "''{1}'' which is not part of the commit; both " + "sides of the move must be committed together", item.getPath(), deleteOpRootAbsPath);
                                SVNErrorManager.error(errorMessage, SVNLogType.WC);
                            }
                        }
                    }

                    if (item.hasFlag(SvnCommitItem.DELETE)) {
                        SVNWCContext.NodeMovedAway nodeMovedAway = context.nodeWasMovedAway(item.getPath());
                        File movedToAbsPath = nodeMovedAway.movedToAbsPath;
                        File copyOpRootAbsPath = nodeMovedAway.opRootAbsPath;

                        if (movedToAbsPath != null && copyOpRootAbsPath != null &&
                            movedToAbsPath.equals(copyOpRootAbsPath) && packet.getItem(copyOpRootAbsPath) == null) {
                            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "Cannot commit ''{0}'' because it was moved to ''{1}'' " +
                                    "which is not part of the commit; both sides of the move must be committed together", item.getPath(), copyOpRootAbsPath);
                            SVNErrorManager.error(errorMessage, SVNLogType.WC);
                        }
                    }
                }
            }

            String commitMessage = getOperation().getCommitMessage();
            if (getOperation().getCommitHandler() != null) {
                Collection<SvnCommitItem> items = new ArrayList<SvnCommitItem>();
                for (SVNURL rootUrl : packet.getRepositoryRoots()) {
                    items.addAll(packet.getItems(rootUrl));
                }
                SvnCommitItem[] itemsArray = items.toArray(new SvnCommitItem[items.size()]);
                try {
                    commitMessage = getOperation().getCommitHandler().getCommitMessage(commitMessage, itemsArray);
                    if (commitMessage == null) {
                        return SVNCommitInfo.NULL;
                    }
                    revisionProperties = getOperation().getCommitHandler().getRevisionProperties(commitMessage, itemsArray, revisionProperties);
                } catch (SVNException e) {
                    SVNErrorMessage err = e.getErrorMessage().wrap("Commit failed (details follow):");
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            }
            commitMessage = commitMessage == null ? "" : SVNCommitUtil.validateCommitMessage(commitMessage);

            boolean keepLocks = getOperation().isKeepLocks();

            SVNURL repositoryRootUrl = packet.getRepositoryRoots().iterator().next();
            if (packet.isEmpty(repositoryRootUrl)) {
                return SVNCommitInfo.NULL;
            }

            Map<String, SvnCommitItem> committables = new TreeMap<String, SvnCommitItem>();
            Map<File, SvnChecksum> md5Checksums = new HashMap<File, SvnChecksum>();
            Map<File, SvnChecksum> sha1Checksums = new HashMap<File, SvnChecksum>();
            SVNURL baseURL = SvnNgCommitUtil.translateCommitables(packet.getItems(repositoryRootUrl), committables);
            Map<String, String> lockTokens = SvnNgCommitUtil.translateLockTokens(packet.getLockTokens(), baseURL);

            SvnCommitItem firstItem = packet.getItems(repositoryRootUrl).iterator().next();
            SVNRepository repository = getRepositoryAccess().createRepository(baseURL, firstItem.getPath());
            SVNCommitMediator17 mediator = new SVNCommitMediator17(context, committables);

            ISVNEditor commitEditor = null;

            try {
                commitEditor = repository.getCommitEditor(commitMessage, lockTokens, keepLocks, revisionProperties, mediator);
                SVNCommitter17 committer = new SVNCommitter17(context, committables, repositoryRootUrl, mediator.getTmpFiles(), md5Checksums, sha1Checksums);
                SVNCommitUtil.driveCommitEditor(committer, committables.keySet(), commitEditor, -1);
                committer.sendTextDeltas(commitEditor);
                info = commitEditor.closeEdit();
                commitEditor = null;
                if (info.getErrorMessage() == null || info.getErrorMessage().getErrorCode() == SVNErrorCode.REPOS_POST_COMMIT_HOOK_FAILED) {
                    // do some post processing, make sure not to unlock wc (to dipose packet) in case there
                    // is an error on post processing.
                    SvnCommittedQueue queue = new SvnCommittedQueue();
                    try {
                        for (SvnCommitItem item : packet.getItems(repositoryRootUrl)) {
                            postProcessCommitItem(queue, item, getOperation().isKeepChangelists(), getOperation().isKeepLocks(), sha1Checksums.get(item.getPath()));
                        }
                        processCommittedQueue(queue, info.getNewRevision(), info.getDate(), info.getAuthor());
                        deleteDeleteFiles(committer, getOperation().getCommitParameters());
                    } catch (SVNException e) {
                        // this is bump error.
                        bumpError = e;
                        throw e;
                    } finally {
                        // only for the last packet in chain.
                        if (packet.isLastPacket()) {
                            sleepForTimestamp();
                        }
                    }
                }
                handleEvent(SVNEventFactory.createSVNEvent(null, SVNNodeKind.NONE, null, info.getNewRevision(), SVNEventAction.COMMIT_COMPLETED,
                        SVNEventAction.COMMIT_COMPLETED, null, null, -1, -1));
            } catch (SVNException e) {
                if (e instanceof SVNCancelException) {
                    throw e;
                }
                SVNErrorMessage err = e.getErrorMessage().wrap("Commit failed (details follow):");
                info = new SVNCommitInfo(-1, null, null, err);
                handleEvent(SVNEventFactory.createErrorEvent(err, SVNEventAction.COMMIT_COMPLETED), ISVNEventHandler.UNKNOWN);
                if (packet.getRepositoryRoots().size() == 1) {
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
            } finally {
                if (commitEditor != null) {
                    try {
                        commitEditor.abortEdit();
                    } catch (SVNException e) {
                        SVNDebugLog.getDefaultLog().log(SVNLogType.CLIENT, e, Level.WARNING);
                        //the exception should not mask original exception
                    }
                }
                for (File tmpFile : mediator.getTmpFiles()) {
                    SVNFileUtil.deleteFile(tmpFile);
                }
            }
        } finally {
            if (bumpError == null) {
                packet.dispose();
            }
        }

        return info;
    }

    private void postProcessCommitItem(SvnCommittedQueue queue, SvnCommitItem item, boolean keepChangelists, boolean keepLocks, SvnChecksum sha1Checksum) throws SVNException {
        boolean removeLock = !keepLocks && item.hasFlag(SvnCommitItem.LOCK);
        Map<String, SVNPropertyValue> wcPropChanges = item.getIncomingProperties();
        SVNProperties wcProps = null;
        if (wcPropChanges != null) {
            try {
                wcProps = getWcContext().getDb().getBaseDavCache(item.getPath());
            } catch (SVNException e) {
                // missing properties.
            }
            if (wcProps == null) {
                wcProps = new SVNProperties();
            }
            for (String name : wcPropChanges.keySet()) {
                SVNPropertyValue pv = wcPropChanges.get(name);
                if (pv == null) {
                    wcProps.remove(name);
                } else {
                    wcProps.put(name, pv);
                }
            }
        }
        final boolean unlockOnly = item.getFlags() == SvnCommitItem.LOCK && wcPropChanges == null;
        queueCommitted(queue, item.getPath(), false, wcProps, unlockOnly, removeLock, !keepChangelists, sha1Checksum);
    }

    public SVNNodeKind getUrlKind(SVNURL url, long revision) throws SVNException {
        return getRepositoryAccess().createRepository(url, null).checkPath("", revision);
    }

    private Collection<File> determineLockTargets(File baseDirectory, Collection<String> targets) throws SVNException {
        Map<File, Collection<File>> wcItems = new HashMap<File, Collection<File>>();
        for (String t: targets) {
            File target = SVNFileUtil.createFilePath(baseDirectory, t);
            File wcRoot = null;
            try {
                wcRoot = getWcContext().getDb().getWCRoot(target);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                    continue;
                }
                throw e;
            }
            Collection<File> wcTargets = wcItems.get(wcRoot);
            if (wcTargets == null) {
                wcTargets = new HashSet<File>();
                wcItems.put(wcRoot, wcTargets);
            }
            wcTargets.add(target);
        }
        Collection<File> lockTargets = new HashSet<File>();
        for (File wcRoot : wcItems.keySet()) {
            Collection<File> wcTargets = wcItems.get(wcRoot);
            if (wcTargets.size() == 1) {
                if (wcRoot.equals(wcTargets.iterator().next())) {
                    lockTargets.add(wcRoot);
                } else {
                    lockTargets.add(SVNFileUtil.getParentFile(wcTargets.iterator().next()));
                }
            } else if (wcTargets.size() > 1) {
                lockTargets.add(wcRoot);
            }
        }
        return lockTargets;
    }

    public Object splitLockingContext(Object lockingContext, SvnCommitPacket newPacket) {
        if (!(lockingContext instanceof Collection)) {
            return lockingContext;
        }
        @SuppressWarnings("unchecked")
        final Collection<File> lockedPaths = (Collection<File>) lockingContext;
        final Collection<File> newLockedPaths = new ArrayList<File>();

        for (SVNURL root : newPacket.getRepositoryRoots()) {
            for(SvnCommitItem item : newPacket.getItems(root)) {
                final File path = item.getPath();
                for (File lockedPath : lockedPaths) {
                    if (path.equals(lockedPath) || (path.isFile() && path.getParentFile().equals(lockedPath))) {
                        newLockedPaths.add(lockedPath);
                        break;
                    }
                }
            }
        }
        return newLockedPaths;
    };

    public void disposeCommitPacket(Object lockingContext, boolean disposeParentContext) throws SVNException {
        if (!(lockingContext instanceof Collection)) {
            if (disposeParentContext) {
                getWcContext().close();
            }
            return;
        }
        if (disposeParentContext) {
            @SuppressWarnings("unchecked")
            Collection<File> lockedPaths = (Collection<File>) lockingContext;

            for (File lockedPath : lockedPaths) {
                try {
                    getWcContext().releaseWriteLock(lockedPath);
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_LOCKED) {
                        throw e;
                    }
                }
            }
            getWcContext().close();
        }
    }

    private void queueCommitted(SvnCommittedQueue queue, File localAbsPath, boolean recurse,
            SVNProperties wcPropChanges, boolean unlockOnly, boolean removeLock, boolean removeChangelist,
            SvnChecksum sha1Checksum) {

        SvnCommittedQueueItem cqi = new SvnCommittedQueueItem();
        cqi.localAbspath = localAbsPath;
        cqi.recurse = recurse;
        cqi.noUnlock = !removeLock;
        cqi.keepChangelist = !removeChangelist;
        cqi.sha1Checksum = sha1Checksum;
        cqi.newDavCache = wcPropChanges;
        cqi.unlockOnly = unlockOnly;
        queue.queue.put(localAbsPath, cqi);
    }

    private void processCommittedQueue(SvnCommittedQueue queue, long newRevision, Date revDate, String revAuthor) throws SVNException {
        final Map<File, Collection<SvnCommittedQueueItem>> itemsMap = new HashMap<File, Collection<SvnCommittedQueueItem>>();
        for (final SvnCommittedQueueItem cqi : queue.queue.values()) {
            final File root = getWcContext().getDb().getWCRoot(cqi.localAbspath);
            if (!itemsMap.containsKey(root)) {
                itemsMap.put(root, new ArrayList<SvnNgCommit.SvnCommittedQueueItem>());
            }
            itemsMap.get(root).add(cqi);
        }
        queue.queue.clear();

        for (final File root : itemsMap.keySet()) {
            final SVNSqlJetDb db = getWcContext().getDb().getSDb(root);
            db.beginTransaction(SqlJetTransactionMode.WRITE);
            try {
                for (SvnCommittedQueueItem cqi : itemsMap.get(root)) {
                    processCommittedInternal(cqi.localAbspath, cqi.recurse, true, newRevision, new SVNDate(revDate.getTime(), 0), revAuthor, cqi.newDavCache, cqi.unlockOnly, cqi.noUnlock, cqi.keepChangelist, cqi.sha1Checksum, queue);
                }
                getWcContext().wqRun(root);
            } catch (SVNException th) {
                db.rollback();
                throw th;
            } finally {
                db.commit();
            }
        }
    }

    private void processCommittedInternal(File localAbspath, boolean recurse, boolean topOfRecurse, long newRevision, SVNDate revDate, String revAuthor, SVNProperties newDavCache, boolean unlockOnly, boolean noUnlock,
            boolean keepChangelist, SvnChecksum sha1Checksum, SvnCommittedQueue queue) throws SVNException {
        processCommittedLeaf(localAbspath, !topOfRecurse, newRevision, revDate, revAuthor, newDavCache, unlockOnly, noUnlock, keepChangelist, sha1Checksum);
    }

    private void processCommittedLeaf(File localAbspath, boolean viaRecurse, long newRevnum, SVNDate newChangedDate, String newChangedAuthor, SVNProperties newDavCache, boolean unlockOnly, boolean noUnlock,
            boolean keepChangelist, SvnChecksum checksum) throws SVNException {
        long newChangedRev = newRevnum;
        assert (SVNFileUtil.isAbsolute(localAbspath));

        Structure<NodeInfo> nodeInfo = getWcContext().getDb().readInfo(localAbspath,
                NodeInfo.status, NodeInfo.kind, NodeInfo.checksum, NodeInfo.hadProps, NodeInfo.propsMod, NodeInfo.haveBase, NodeInfo.haveWork);
        File admAbspath;
        if (nodeInfo.get(NodeInfo.kind) == SVNWCDbKind.Dir) {
            admAbspath = localAbspath;
        } else {
            admAbspath = SVNFileUtil.getFileDir(localAbspath);
        }
        getWcContext().writeCheck(admAbspath);

        if (nodeInfo.get(NodeInfo.status) == SVNWCDbStatus.Deleted) {
            getWcContext().getDb().removeBase(localAbspath, false, false, true, !viaRecurse ? newRevnum : SVNRepository.INVALID_REVISION, null, null);
            nodeInfo.release();
            return;
        } else if (nodeInfo.get(NodeInfo.status) == SVNWCDbStatus.NotPresent) {
            nodeInfo.release();
            return;
        }
        if (unlockOnly) {
            if (!noUnlock) {
                getWcContext().getDb().removeLock(localAbspath);
            }
        } else {
            SVNSkel workItem = null;
            SVNWCDbKind kind = nodeInfo.get(NodeInfo.kind);
            if (kind != SVNWCDbKind.Dir) {
                if (checksum == null) {
                    checksum = nodeInfo.get(NodeInfo.checksum);
                    if (viaRecurse && !nodeInfo.is(NodeInfo.propsMod)) {
                        Structure<NodeInfo> moreInfo = getWcContext().getDb().
                                readInfo(localAbspath, NodeInfo.changedRev, NodeInfo.changedDate, NodeInfo.changedAuthor);
                        newChangedRev = moreInfo.lng(NodeInfo.changedRev);
                        newChangedDate = moreInfo.get(NodeInfo.changedDate);
                        newChangedAuthor = moreInfo.get(NodeInfo.changedAuthor);
                        moreInfo.release();
                    }
                }
                workItem = getWcContext().wqBuildFileCommit(localAbspath, nodeInfo.is(NodeInfo.propsMod));
            }
            getWcContext().getDb().globalCommit(localAbspath, newRevnum, newChangedRev, newChangedDate, newChangedAuthor, checksum, null, newDavCache, keepChangelist, noUnlock, workItem);
        }
    }

    private static class SvnCommittedQueue {
        @SuppressWarnings("unchecked")
        public Map<File, SvnCommittedQueueItem> queue = new TreeMap<File, SvnCommittedQueueItem>(SVNCommitUtil.FILE_COMPARATOR);
    };

    private static class SvnCommittedQueueItem {

        public File localAbspath;
        public boolean recurse;
        public boolean noUnlock;
        public boolean keepChangelist;
        public SvnChecksum sha1Checksum;
        public SVNProperties newDavCache;
        public boolean unlockOnly;
    }

}
