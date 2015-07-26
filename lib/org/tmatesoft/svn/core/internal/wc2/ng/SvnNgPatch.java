package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.patch.SVNPatch;
import org.tmatesoft.svn.core.internal.wc.patch.SVNPatchFileStream;
import org.tmatesoft.svn.core.internal.wc.patch.SVNPatchTargetInfo;
import org.tmatesoft.svn.core.internal.wc17.SVNStatusEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc2.patch.SvnPatchFile;
import org.tmatesoft.svn.core.internal.wc2.patch.SvnPatchTarget;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnPatch;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SvnNgPatch extends SvnNgOperationRunner<Void, SvnPatch> {
    @Override
    protected Void run(SVNWCContext context) throws SVNException {
        if (getOperation().getStripCount() < 0) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS, "strip count must be positive");
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        final SvnTarget firstTarget = getOperation().getFirstTarget();
        if (firstTarget.isURL()) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "''{0}'' is not a local path", firstTarget);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        final File patchFile = getOperation().getPatchFile();
        SVNNodeKind kind = SVNFileType.getNodeKind(SVNFileType.getType(patchFile));
        if (kind == SVNNodeKind.NONE) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "''{0}'' does not exist", patchFile);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        if (kind != SVNNodeKind.FILE) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "''{0}'' is not a file", patchFile);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        final File workingCopyDirectory = firstTarget.getFile();
        kind = SVNFileType.getNodeKind(SVNFileType.getType(workingCopyDirectory));
        if (kind == SVNNodeKind.NONE) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "''{0}'' does not exist", workingCopyDirectory);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        if (kind != SVNNodeKind.DIR) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "''{0}'' is not a directory", workingCopyDirectory);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        File lockAbsPath = null;
        try {
            lockAbsPath = context.acquireWriteLock(workingCopyDirectory, false, false);
            applyPatches(patchFile, workingCopyDirectory,
                    getOperation().isDryRun(), getOperation().getStripCount(),
                    getOperation().isReverse(), getOperation().isIgnoreWhitespace(), getOperation().isRemoveTempFiles(),
                    context);
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        } finally {
            if (lockAbsPath != null) {
                context.releaseWriteLock(lockAbsPath);
            }
        }
        return null;
    }

    private void applyPatches(File patchFile, File workingCopyDirectory, boolean dryRun, int stripCount, boolean reverse, boolean ignoreWhitespace, boolean removeTempFiles, SVNWCContext context) throws SVNException, IOException {
        final SvnPatchFile svnPatchFile = SvnPatchFile.openReadOnly(patchFile);
        try {
            final List<SVNPatchTargetInfo> targetInfos = new ArrayList<SVNPatchTargetInfo>(); //can we use SVNPatchTarget instead of SVNPatchTargetInfo
            org.tmatesoft.svn.core.internal.wc2.patch.SvnPatch patch;
            do {
                checkCancelled();
                patch = org.tmatesoft.svn.core.internal.wc2.patch.SvnPatch.parseNextPatch(svnPatchFile, reverse, ignoreWhitespace);
                if (patch != null) {
                    SvnPatchTarget target = SvnPatchTarget.applyPatch(patch, workingCopyDirectory, stripCount, context, ignoreWhitespace, removeTempFiles);
                    if (!target.isFiltered()) {
                        SVNPatchTargetInfo targetInfo = new SVNPatchTargetInfo(target.getAbsPath(), target.isDeleted());
                        if (!target.isSkipped()) {
                            targetInfos.add(targetInfo);
                            if (target.hasTextChanges() || target.isAdded() || target.getMoveTargetAbsPath() != null || target.isDeleted()) {
                                target.installPatchedTarget(workingCopyDirectory, dryRun, context);
                            }
                            if (target.hasPropChanges() && !target.isDeleted()) {
                                target.installPatchedPropTarget(dryRun, context);
                            }
                            target.writeOutRejectedHunks(dryRun);
                        }
                        target.sendPatchNotification(context);
                        if (target.isDeleted() && !target.isSkipped()) {
                            checkAncestorDelete(targetInfo.getLocalAbsPath(), targetInfos, workingCopyDirectory, context, dryRun);
                        }
//                        target.getPatch().close();
                    }
                }
            } while (patch != null);
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        } finally {
            if (svnPatchFile != null) {
                svnPatchFile.close();
            }
        }
    }

    private void checkAncestorDelete(File deletedTarget, List<SVNPatchTargetInfo> targetsInfo, File applyRoot, SVNWCContext context, boolean dryRun) throws SVNException {
        File dirAbsPath = SVNFileUtil.getFileDir(deletedTarget);
        Collection<String> globalIgnores = SVNStatusEditor17.getGlobalIgnores(context.getOptions());

        while (SVNPathUtil.isAncestor(SVNFileUtil.getFilePath(applyRoot), SVNFileUtil.getFilePath(dirAbsPath)) && !applyRoot.equals(dirAbsPath)) {
            final CanDeleteBaton statusWalker = new CanDeleteBaton();
            statusWalker.localAbsPath = dirAbsPath;
            statusWalker.mustKeep = false;
            statusWalker.targetsInfo = targetsInfo;

            SVNStatusEditor17 editor = new SVNStatusEditor17(dirAbsPath, context, context.getOptions(), false, true, SVNDepth.INFINITY, statusWalker);
            try {
                editor.walkStatus(dirAbsPath, SVNDepth.INFINITY, true, false, false, globalIgnores);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.CEASE_INVOCATION) {
                    throw e;
                }
            }
            if (statusWalker.mustKeep) {
                break;
            }
            if (!dryRun) {
                SvnNgRemove.delete(context, dirAbsPath, null, false, false, null);
            }
            SVNPatchTargetInfo targetInfo = new SVNPatchTargetInfo(dirAbsPath, true);
            targetsInfo.add(targetInfo);

            final ISVNEventHandler eventHandler = context.getEventHandler();
            if (eventHandler != null) {
                eventHandler.handleEvent(SVNEventFactory.createSVNEvent(dirAbsPath, SVNNodeKind.DIR, null, -1, SVNEventAction.DELETE, SVNEventAction.DELETE, null, null), UNKNOWN);
            }

            dirAbsPath = SVNFileUtil.getFileDir(dirAbsPath);
        }
    }


    private class CanDeleteBaton implements ISvnObjectReceiver<SvnStatus> {
        public File localAbsPath;
        public boolean mustKeep;
        public List<SVNPatchTargetInfo> targetsInfo;

        public void receive(SvnTarget target, SvnStatus status) throws SVNException {
            if (status.getNodeStatus() == SVNStatusType.STATUS_NONE || status.getNodeStatus() == SVNStatusType.STATUS_DELETED) {
                //do nothing
                return;
            } else {
                if (localAbsPath.equals(target.getFile())) {
                    //do nothing
                    return;
                }
                for (SVNPatchTargetInfo targetInfo : targetsInfo) {
                    if (targetInfo.getLocalAbsPath().equals(target.getFile())) {
                        if (targetInfo.isDeleted()) {
                            return;
                        }
                        break;
                    }
                }
                mustKeep = true;

                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.CEASE_INVOCATION);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        }
    }
}
