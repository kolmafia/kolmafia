package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNStatusEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbOpenMode;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo.InfoField;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.*;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgCleanup extends SvnNgOperationRunner<Void, SvnCleanup> {

    @Override
    protected Void run(SVNWCContext context) throws SVNException {
        if (getOperation().getFirstTarget().isURL()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET,
                    "''{0}'' is not a local path", getOperation().getFirstTarget().getURL());
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        File localAbsPath = getOperation().getFirstTarget().getFile().getAbsoluteFile();
        SVNWCContext wcContext;
        if (getOperation().isBreakLocks()) {
            SVNWCDb db = new SVNWCDb();
            wcContext = new SVNWCContext(db, context.getEventHandler());
        } else {
            wcContext = context;
        }
        ISVNOptions options = wcContext.getOptions();
        String[] ignores = options == null ? new String[]{} : options.getIgnorePatterns();

        doCleanup(localAbsPath, wcContext, Arrays.asList(ignores));
        return null;
    }

    private void doCleanup(File localAbsPath, SVNWCContext wcContext, List<String> ignores) throws SVNException {
        cleanup(wcContext, localAbsPath);
        sleepForTimestamp();

        if (getOperation().isRemoveUnversionedItems() || getOperation().isRemoveIgnoredItems() || getOperation().isIncludeExternals()) {
            CleanupStatusWalk cleanupStatusWalk = new CleanupStatusWalk();
//            cleanupStatusWalk.breakLocks = getOperation().isBreakLocks();
//            cleanupStatusWalk.clearDavCache = getOperation().isDeleteWCProperties();
//            cleanupStatusWalk.vacuumPristines = getOperation().isVacuumPristines();
            cleanupStatusWalk.removeUnversionedItems = getOperation().isRemoveUnversionedItems();
            cleanupStatusWalk.removeIgnoredItems = getOperation().isRemoveIgnoredItems();
            cleanupStatusWalk.includeExternals = getOperation().isIncludeExternals();

            cleanupStatusWalk.eventHandler = wcContext.getEventHandler();
            cleanupStatusWalk.wcContext = wcContext;
            cleanupStatusWalk.ignores = ignores;
            cleanupStatusWalk.operation = getOperation();

            File lockPath = wcContext.acquireWriteLock(localAbsPath, false, true);
            try {
                SVNStatusEditor17 statusEditor17 = new SVNStatusEditor17(localAbsPath, getWcContext(), getOperation().getOptions(), getOperation().isRemoveIgnoredItems(), true, SVNDepth.INFINITY, cleanupStatusWalk);
                statusEditor17.walkStatus(localAbsPath, SVNDepth.INFINITY, true, getOperation().isRemoveIgnoredItems(), true, ignores);
            } finally {
                wcContext.releaseWriteLock(lockPath);
            }
        }
    }

    private void cleanup(SVNWCContext wcContext, File localAbsPath) throws SVNException {
        boolean breakLocks = getOperation().isBreakLocks();
        try {
            if (breakLocks) {
                wcContext.getDb().open(SVNWCDbOpenMode.ReadWrite, (ISVNOptions) null, true, false);
            }
            cleanupInternal(wcContext, localAbsPath);
            if (getOperation().isDeleteWCProperties()) {
                wcContext.getDb().clearDavCacheRecursive(localAbsPath);
            }
        } finally {
            if (breakLocks) {
                wcContext.getDb().close();
            }
        }
    }

    private int canBeCleaned(SVNWCContext wcContext, File localAbsPath) throws SVNException {
        int wcFormat = wcContext.checkWC(localAbsPath);
        if (wcFormat == 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_WORKING_COPY,
                    "''{0}'' is not a working copy directory", localAbsPath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        if (wcFormat < SVNWCContext.WC_NG_VERSION) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_UNSUPPORTED_FORMAT,
                    "Log format too old, please use Subversion 1.6 or earlier");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        return wcFormat;
    }

    private void cleanupInternal(SVNWCContext wcContext, File localAbsPath) throws SVNException {
        int wcFormat = canBeCleaned(wcContext, localAbsPath);
        wcContext.getDb().obtainWCLock(localAbsPath, -1, getOperation().isBreakLocks());
        if (wcFormat >= ISVNWCDb.WC_HAS_WORK_QUEUE) {
            wcContext.wqRun(localAbsPath);
        }
        File cleanupWCRoot = wcContext.getDb().getWCRoot(localAbsPath);
        if (cleanupWCRoot.equals(localAbsPath) && getOperation().isVacuumPristines()) {
            SVNWCUtils.admCleanupTmpArea(wcContext, localAbsPath);
            wcContext.getDb().cleanupPristine(localAbsPath);
        }
        repairTimestamps(wcContext, localAbsPath);
        wcContext.getDb().releaseWCLock(localAbsPath);
    }

    public static void repairTimestamps(SVNWCContext wcContext, File localAbsPath) throws SVNException {
        wcContext.checkCancelled();
        WCDbInfo info = wcContext.getDb().readInfo(localAbsPath, InfoField.status, InfoField.kind);
        if (info.status == ISVNWCDb.SVNWCDbStatus.ServerExcluded
                || info.status == ISVNWCDb.SVNWCDbStatus.Deleted
                || info.status == ISVNWCDb.SVNWCDbStatus.Excluded
                || info.status == ISVNWCDb.SVNWCDbStatus.NotPresent
                ) {
            return;
        }
        if (info.kind == ISVNWCDb.SVNWCDbKind.File || info.kind == ISVNWCDb.SVNWCDbKind.Symlink) {
            try {
                wcContext.isTextModified(localAbsPath, false);
            } catch (SVNException e) {
                SVNDebugLog.getDefaultLog().log(SVNLogType.WC, e, Level.WARNING);
            }
        } else if (info.kind == ISVNWCDb.SVNWCDbKind.Dir) {
            Set<String> children = wcContext.getDb().readChildren(localAbsPath);
            for (String childPath : children) {
                File childAbsPath = SVNFileUtil.createFilePath(localAbsPath, childPath);
                repairTimestamps(wcContext, childAbsPath);
            }
        }
        return;
    }

    private static class CleanupStatusWalk implements ISvnObjectReceiver<SvnStatus> {

//        private boolean breakLocks;
//        private boolean clearDavCache;
//        private boolean vacuumPristines;
        private boolean removeUnversionedItems;
        private boolean removeIgnoredItems;
        private boolean includeExternals;

        private SVNWCContext context;
        private ISVNEventHandler eventHandler;
        private SVNWCContext wcContext;
        private List<String> ignores;
        private SvnCleanup operation;

        public void receive(SvnTarget target, SvnStatus status) throws SVNException {
            File localAbsPath = target.getFile();

            if (status.getNodeStatus() == SVNStatusType.STATUS_EXTERNAL && includeExternals) {
                SVNFileType kindOnDisk = SVNFileType.getType(localAbsPath);
                if (kindOnDisk == SVNFileType.DIRECTORY) {
                    if (eventHandler != null) {
                        SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.UNKNOWN, null, SVNRepository.INVALID_REVISION, SVNEventAction.CLEANUP_EXTERNAL, SVNEventAction.CLEANUP_EXTERNAL, null, null);
                        eventHandler.handleEvent(event, -1);
                    }
                    try {
                        SvnNgCleanup svnNgCleanup = new SvnNgCleanup();
                        svnNgCleanup.setOperation(operation);
                        svnNgCleanup.doCleanup(localAbsPath, wcContext, ignores);
                    } catch (SVNException e) {
                        if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_NOT_WORKING_COPY) {
                            return;
                        } else {
                            throw e;
                        }

                    }
                }
                return;
            }
            if (status.getNodeStatus() == SVNStatusType.STATUS_IGNORED) {
                if (!removeIgnoredItems) {
                    return;
                }
            } else if (status.getNodeStatus() == SVNStatusType.STATUS_UNVERSIONED) {
                if (!removeUnversionedItems) {
                    return;
                }
            } else {
                return;
            }
            SVNFileType kindOnDisk = SVNFileType.getType(localAbsPath);
            if (kindOnDisk == SVNFileType.FILE || kindOnDisk == SVNFileType.SYMLINK) {
                SVNFileUtil.deleteFile(localAbsPath);
            } else if (kindOnDisk == SVNFileType.DIRECTORY) {
                SVNFileUtil.deleteAll(localAbsPath, true, eventHandler);
            } else {
                return;
            }

            if (eventHandler != null) {
                SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, SVNFileType.getNodeKind(kindOnDisk), null, SVNRepository.INVALID_REVISION, SVNEventAction.DELETE, SVNEventAction.DELETE, null, null);
                eventHandler.handleEvent(event, -1);
            }
        }
    }
}
