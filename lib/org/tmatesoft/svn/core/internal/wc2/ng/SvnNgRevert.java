package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.*;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbRevert.RevertInfo;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc2.SvnRevert;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.util.*;

public class SvnNgRevert extends SvnNgOperationRunner<Void, SvnRevert> {

    @Override
    protected Void run(SVNWCContext context) throws SVNException {
        boolean useCommitTimes = getOperation().getOptions().isUseCommitTimes();
        
        for (SvnTarget target : getOperation().getTargets()) {
            checkCancelled();
            
            boolean isWcRoot = context.getDb().isWCRoot(target.getFile());
            File lockTarget = isWcRoot ? target.getFile() : SVNFileUtil.getParentFile(target.getFile());
            File lockRoot = context.acquireWriteLock(lockTarget, false, true);
            try {
                revert(target.getFile(), getOperation().getDepth(), useCommitTimes, getOperation().getApplicableChangelists());
            } catch (SVNException e) {
                SVNErrorMessage err = e.getErrorMessage();
                if (err.getErrorCode() == SVNErrorCode.ENTRY_NOT_FOUND 
                        || err.getErrorCode() == SVNErrorCode.UNVERSIONED_RESOURCE 
                        || err.getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                    SVNEvent event = SVNEventFactory.createSVNEvent(target.getFile(), SVNNodeKind.NONE, null, -1, SVNEventAction.SKIP, SVNEventAction.REVERT, err, null, -1, -1);
                    handleEvent(event);
                    continue;
                }
                if (!useCommitTimes) {
                    sleepForTimestamp();
                }
                throw e;
            } finally {
                context.releaseWriteLock(lockRoot);
            }
        }
        if (!useCommitTimes) {
            sleepForTimestamp();
        }
        return null;
    }

    private void revert(File localAbsPath, SVNDepth depth, boolean useCommitTimes, Collection<String> changelists) throws SVNException {
        if (changelists != null && changelists.size() > 0) {
            revertChangelist(localAbsPath, depth, useCommitTimes, changelists);
            return;
        }
        if (depth == SVNDepth.EMPTY || depth == SVNDepth.INFINITY) {
            revert(localAbsPath, depth, useCommitTimes);
            return;
        }
        if (depth == SVNDepth.IMMEDIATES || depth == SVNDepth.FILES) {
            revert(localAbsPath, SVNDepth.EMPTY, useCommitTimes);
            Set<String> children = ((SVNWCDb) getWcContext().getDb()).getWorkingChildren(localAbsPath);
            for (String childName : children) {
                File childAbsPath = SVNFileUtil.createFilePath(localAbsPath, childName);
                if (depth == SVNDepth.FILES) {
                    SVNNodeKind childKind = getWcContext().readKind(childAbsPath, true);
                    if (childKind != SVNNodeKind.FILE) {
                        continue;
                    }
                }
                revert(childAbsPath, SVNDepth.EMPTY, useCommitTimes);
            }
            return;
        }
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_OPERATION_DEPTH);
        SVNErrorManager.error(err, SVNLogType.WC);
        
    }

    private void revertChangelist(File localAbsPath, SVNDepth depth, boolean useCommitTimes, Collection<String> changelists) throws SVNException {
        checkCancelled();
        if (getWcContext().isChangelistMatch(localAbsPath, changelists)) {
            revert(localAbsPath, SVNDepth.EMPTY, useCommitTimes);
        }
        if (depth == SVNDepth.EMPTY) {
            return;
        }
        if (depth == SVNDepth.FILES || depth == SVNDepth.IMMEDIATES) {
            depth = SVNDepth.EMPTY;
        }
        Set<String> children = ((SVNWCDb) getWcContext().getDb()).getWorkingChildren(localAbsPath);
        for (String childName : children) {
            File childAbsPath = SVNFileUtil.createFilePath(localAbsPath, childName);
            revertChangelist(childAbsPath, depth, useCommitTimes, changelists);
        }
    }

    private void revert(File localAbsPath, SVNDepth depth, boolean useCommitTimes) throws SVNException {
        File wcRoot = getWcContext().getDb().getWCRoot(localAbsPath);
        if (!localAbsPath.equals(wcRoot)) {
            getWcContext().writeCheck(SVNFileUtil.getParentFile(localAbsPath));
        } else {
            getWcContext().writeCheck(localAbsPath);
        }
        try {

            //we should detect that the copy was modified here
            //after opRevert() call we won't be able to do that
            final Set<File> modifiedCopiesThatShouldBePreserved = new HashSet<File>();
            populateModifiedCopiesThatShouldBePreserved(localAbsPath, wcRoot, modifiedCopiesThatShouldBePreserved);

            getWcContext().getDb().opRevert(localAbsPath, depth);
            restore(getWcContext(), localAbsPath, depth, useCommitTimes, getWcContext().getEventHandler(), modifiedCopiesThatShouldBePreserved);
        } finally {
            SvnWcDbRevert.dropRevertList(getWcContext(), localAbsPath);
        }
    }

    private void populateModifiedCopiesThatShouldBePreserved(File localAbsPath, File wcRoot, Set<File> modifiedCopiesThatShouldBePreserved) throws SVNException {
        if (!getOperation().isPreserveModifiedCopies()) {
            return;
        }
        try {
            final ISVNWCDb.WCDbInfo wcDbInfo = getWcContext().getDb().readInfo(localAbsPath, ISVNWCDb.WCDbInfo.InfoField.checksum,
                    ISVNWCDb.WCDbInfo.InfoField.originalRootUrl, ISVNWCDb.WCDbInfo.InfoField.propsMod,
                    ISVNWCDb.WCDbInfo.InfoField.lastModTime, ISVNWCDb.WCDbInfo.InfoField.translatedSize,
                    ISVNWCDb.WCDbInfo.InfoField.kind);

            final SVNNodeKind nodeKind = SVNFileType.getNodeKind(SVNFileType.getType(localAbsPath));
            if (nodeKind == SVNNodeKind.NONE ) {
                //there's nothing to preserve
            } else if (nodeKind == SVNNodeKind.DIR) {
                if (wcDbInfo.kind == SVNWCDbKind.Dir) {
                    final List<File> children = getWcContext().getChildrenOfWorkingNode(localAbsPath, false);
                    for (File child : children) {
                        populateModifiedCopiesThatShouldBePreserved(child, wcRoot, modifiedCopiesThatShouldBePreserved);
                    }
                }
            } else {
                if (wcDbInfo.kind != SVNWCDbKind.File) {
                    //obstruction, nothing to preserve
                    return;
                }


                if (wcDbInfo.propsMod) {
                    modifiedCopiesThatShouldBePreserved.add(localAbsPath);
                    return;
                }
                if (wcDbInfo.originalRootUrl != null && wcDbInfo.checksum != null) {
                    //now we are sure that the file is copied
                    long fileSize = SVNFileUtil.getFileLength(localAbsPath);
                    long fileTime = SVNFileUtil.getFileLastModified(localAbsPath);

                    if (wcDbInfo.translatedSize != -1 && wcDbInfo.lastModTime != 0 &&
                            wcDbInfo.translatedSize == fileSize && wcDbInfo.lastModTime == fileTime) {
                        //not modified
                    } else {
                        SVNWCDb db = (SVNWCDb) getWcContext().getDb();
                        SVNWCDb.DirParsedInfo dirParsedInfo = db.parseDir(wcRoot, SVNSqlJetDb.Mode.ReadOnly);
                        File pristineFileName = SvnWcDbPristines.getPristineFileName(dirParsedInfo.wcDbDir.getWCRoot(), wcDbInfo.checksum, false);
                        if (getWcContext().compareAndVerify(localAbsPath, pristineFileName, true, false, false)) {
                            modifiedCopiesThatShouldBePreserved.add(localAbsPath);
                        }
                    }
                }
            }
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.ENTRY_NOT_FOUND && e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
        }
    }

    public static void restore(SVNWCContext context, File localAbsPath, SVNDepth depth, boolean useCommitTimes, ISVNEventHandler notifier) throws SVNException {
        restore(context, localAbsPath, depth, useCommitTimes, notifier, Collections.<File>emptySet());
    }

    public static void restore(SVNWCContext context, File localAbsPath, SVNDepth depth, boolean useCommitTimes, ISVNEventHandler notifier, Set<File> modifiedCopiesThatShouldBePreserved) throws SVNException {
        context.checkCancelled();
        
        Structure<RevertInfo> revertInfo = SvnWcDbRevert.readRevertInfo(context, localAbsPath);
        ISVNWCDb.SVNWCDbStatus status = SVNWCDbStatus.Normal;
        ISVNWCDb.SVNWCDbKind kind = SVNWCDbKind.Unknown;
        long recordedSize = -1;
        long recordedTime = 0;
        boolean notifyRequired = revertInfo.is(RevertInfo.reverted);
        
        try {
            Structure<NodeInfo> nodeInfo = context.getDb().readInfo(localAbsPath, NodeInfo.status, NodeInfo.kind,
                    NodeInfo.recordedSize, NodeInfo.recordedTime);
            status = nodeInfo.get(NodeInfo.status);
            kind = nodeInfo.get(NodeInfo.kind);
            recordedSize = nodeInfo.lng(NodeInfo.recordedSize);
            recordedTime = nodeInfo.lng(NodeInfo.recordedTime);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                if (!revertInfo.is(RevertInfo.copiedHere)) {
                    if (notifyRequired && notifier != null) {
                        notifier.handleEvent(SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.NONE, null, -1, SVNEventAction.REVERT, 
                                SVNEventAction.REVERT, null, null, -1, -1), -1);
                    }
                    SvnWcDbRevert.notifyRevert(context, localAbsPath, notifier);
                    return;
                } 
            } else {
                throw e;
            }
        }
        
        SVNFileType filetype = SVNFileType.getType(localAbsPath);
        SVNNodeKind onDisk = null;
        boolean special = false;
        if (filetype == SVNFileType.NONE) {
            onDisk = SVNNodeKind.NONE;
            special = false;
        } else {
            if (filetype == SVNFileType.FILE || filetype == SVNFileType.SYMLINK) {
                onDisk = SVNNodeKind.FILE;
            } else if (filetype == SVNFileType.DIRECTORY) {
                onDisk = SVNNodeKind.DIR;
            } else {
                onDisk = SVNNodeKind.UNKNOWN;
            }
            special = filetype == SVNFileType.SYMLINK;
        }
        
        if (revertInfo.is(RevertInfo.copiedHere)) {
            if (revertInfo.get(RevertInfo.kind) == SVNWCDbKind.File && onDisk == SVNNodeKind.FILE) {
                if (!modifiedCopiesThatShouldBePreserved.contains(localAbsPath)) {
                    SVNFileUtil.deleteFile(localAbsPath);
                }
                onDisk = SVNNodeKind.NONE;
            } else if (revertInfo.get(RevertInfo.kind) == SVNWCDbKind.Dir && onDisk == SVNNodeKind.DIR) {
                boolean removed = restoreCopiedDirectory(context, localAbsPath, true, modifiedCopiesThatShouldBePreserved);
                if (removed) {
                    onDisk = SVNNodeKind.NONE;
                }
            }
        }
        
        if (onDisk != SVNNodeKind.NONE
                && status != SVNWCDbStatus.ServerExcluded
                && status != SVNWCDbStatus.Deleted
                && status != SVNWCDbStatus.Excluded
                && status != SVNWCDbStatus.NotPresent) {
            if (onDisk == SVNNodeKind.DIR && kind != SVNWCDbKind.Dir) {
                SVNFileUtil.deleteAll(localAbsPath, true, notifier);
                onDisk = SVNNodeKind.NONE;
            } else if (onDisk == SVNNodeKind.FILE && kind != SVNWCDbKind.File) {
                SVNFileUtil.deleteFile(localAbsPath);
                onDisk = SVNNodeKind.NONE;
            } else if (onDisk == SVNNodeKind.FILE) {
                SVNProperties pristineProperties = context.getDb().readPristineProperties(localAbsPath);
                boolean modified = false;
                String specialProperty = pristineProperties.getStringValue(SVNProperty.SPECIAL);
                if (SVNFileUtil.symlinksSupported() && (specialProperty != null) != special) {
                    SVNFileUtil.deleteFile(localAbsPath);
                    onDisk = SVNNodeKind.NONE;                    
                } else {
                    long lastModified = SVNFileUtil.getFileLastModified(localAbsPath);
                    long size = SVNFileUtil.getFileLength(localAbsPath);
                    if (recordedSize != -1
                            && recordedTime != 0
                            && recordedSize == size 
                            && recordedTime/1000 == lastModified) {
                        modified = false;
                    } else {
                        modified = context.isTextModified(localAbsPath, true);
                    }
                }
                
                if (modified) {
                    SVNFileUtil.deleteFile(localAbsPath);
                    onDisk = SVNNodeKind.NONE;
                } else {
                    boolean isReadOnly = filetype != SVNFileType.SYMLINK && !localAbsPath.canWrite();
                    boolean needsLock = pristineProperties.getStringValue(SVNProperty.NEEDS_LOCK) != null;
                    if (needsLock && !isReadOnly) {
                        SVNFileUtil.setReadonly(localAbsPath, true);
                        notifyRequired = true;
                    } else if (!needsLock && isReadOnly) {
                        SVNFileUtil.setReadonly(localAbsPath, false);
                        notifyRequired = true;
                    }
                }
                if (!(SVNFileUtil.isWindows || SVNFileUtil.isOpenVMS) && (!SVNFileUtil.symlinksSupported() || !special)) {
                    boolean executable = SVNFileUtil.isExecutable(localAbsPath);
                    boolean executableProperty = pristineProperties.getStringValue(SVNProperty.EXECUTABLE) != null;
                    if (executableProperty && !executable) {
                        SVNFileUtil.setExecutable(localAbsPath, true);
                        notifyRequired = true;
                    } else if (!executableProperty && executable) {
                        SVNFileUtil.setExecutable(localAbsPath, false);
                        notifyRequired = true;
                    }
                }
            }
        }

        if (onDisk == SVNNodeKind.NONE
                && status != SVNWCDbStatus.ServerExcluded
                && status != SVNWCDbStatus.Deleted
                && status != SVNWCDbStatus.Excluded
                && status != SVNWCDbStatus.NotPresent) {
            if (kind == SVNWCDbKind.Dir) {
                SVNFileUtil.ensureDirectoryExists(localAbsPath);
            } else if (kind == SVNWCDbKind.File) {
                SVNSkel workItem = context.wqBuildFileInstall(localAbsPath, null, useCommitTimes, true);
                context.getDb().addWorkQueue(localAbsPath, workItem);
                context.wqRun(localAbsPath);
            }
            notifyRequired = true;
        }
        
        if (revertInfo.hasValue(RevertInfo.conflictOld)) {
            notifyRequired |= SVNFileUtil.deleteFile(revertInfo.<File>get(RevertInfo.conflictOld));
        }
        if (revertInfo.hasValue(RevertInfo.conflictNew)) {
            notifyRequired |= SVNFileUtil.deleteFile(revertInfo.<File>get(RevertInfo.conflictNew));
        }
        if (revertInfo.hasValue(RevertInfo.conflictWorking)) {
            notifyRequired |= SVNFileUtil.deleteFile(revertInfo.<File>get(RevertInfo.conflictWorking));
        }
        if (revertInfo.hasValue(RevertInfo.propReject)) {
            notifyRequired |= SVNFileUtil.deleteFile(revertInfo.<File>get(RevertInfo.propReject));
        }

        if (notifyRequired && notifier != null) {
            notifier.handleEvent(SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.NONE, null, -1, SVNEventAction.REVERT, 
                    SVNEventAction.REVERT, null, null, -1, -1), -1);
        }
        
        if (depth == SVNDepth.INFINITY && kind == SVNWCDbKind.Dir) {
            restoreCopiedDirectory(context, localAbsPath, false, modifiedCopiesThatShouldBePreserved);
            
            Set<String> children = ((SVNWCDb) context.getDb()).getChildrenOfWorkingNode(localAbsPath);
            for (String childName : children) {
                File childAbsPath = SVNFileUtil.createFilePath(localAbsPath, childName);
                restore(context, childAbsPath, depth, useCommitTimes, notifier, modifiedCopiesThatShouldBePreserved);
            }
        }
        
        SvnWcDbRevert.notifyRevert(context, localAbsPath, notifier);
    }

    private static boolean restoreCopiedDirectory(SVNWCContext context, File localAbsPath, boolean removeSelf, Set<File> modifiedCopiesThatShouldBePreserved) throws SVNException {
        boolean selfRemoved = false;
        
        Map<File, SVNWCDbKind> children = SvnWcDbRevert.readRevertCopiedChildren(context, localAbsPath);
        
        for (File child : children.keySet()) {
            context.checkCancelled();
            SVNWCDbKind childKind = children.get(child);
            if (childKind != SVNWCDbKind.File) {
                continue;
            }
            SVNFileType childFileType = SVNFileType.getType(child);
            if (childFileType != SVNFileType.FILE && childFileType != SVNFileType.SYMLINK) {
                continue;
            }
            if (!modifiedCopiesThatShouldBePreserved.contains(child)) {
                SVNFileUtil.deleteFile(child);
            } else {
                selfRemoved = false;
                removeSelf = false;
            }
        }

        for (File child : children.keySet()) {
            context.checkCancelled();
            SVNWCDbKind childKind = children.get(child);
            if (childKind != SVNWCDbKind.Dir) {
                continue;
            }
            if (!modifiedCopiesThatShouldBePreserved.contains(child)) {
                SVNFileUtil.deleteFile(child);
            } else {
                selfRemoved = false;
                removeSelf = false;
            }
        }
        
        if (removeSelf) {
            SVNFileUtil.deleteFile(localAbsPath);
            if (SVNFileType.getType(localAbsPath) == SVNFileType.NONE) {
                selfRemoved = true;
            }
        }        
        return selfRemoved;
    }

}
