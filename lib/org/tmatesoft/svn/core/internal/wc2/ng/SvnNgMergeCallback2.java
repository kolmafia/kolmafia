package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNMergeInfoUtil;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.*;
import org.tmatesoft.svn.core.internal.wc17.SVNStatusEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNUpdateEditor17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCConflictDescription17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.Structure;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbConflicts;
import org.tmatesoft.svn.core.internal.wc2.SvnRepositoryAccess;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.io.InputStream;
import java.util.*;

public class SvnNgMergeCallback2 implements ISvnDiffCallback2 {

    private final SvnNgMergeDriver mergeDriver;
    private FileBaton currentFile;
//    private DirectoryBaton currentDirectory;
    private SVNWCContext context;
    private SvnNgRepositoryAccess repositoryAccess;

    public SvnNgMergeCallback2(SVNWCContext context, SvnNgMergeDriver mergeDriver, SvnNgRepositoryAccess repositoryAccess) {
        this.context = context;
        this.mergeDriver = mergeDriver;
        this.repositoryAccess = repositoryAccess;
    }

    public void fileOpened(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, SvnDiffSource copyFromSource, boolean createDirBaton, Object dirBaton) throws SVNException {
        DirectoryBaton currentDirectory = (DirectoryBaton) dirBaton;
        if (createDirBaton && currentDirectory == null) {
            currentDirectory = new DirectoryBaton();
            currentDirectory.treeConflictReason = null;
            currentDirectory.treeConflictAction = SVNConflictAction.EDIT;
            currentDirectory.skipReason = SVNStatusType.UNKNOWN;
            result.newBaton = currentDirectory;
        }
        File localAbsPath = SVNFileUtil.createFilePath(mergeDriver.targetAbsPath, relPath);

        currentFile = new FileBaton();
        currentFile.treeConflictReason = null;
        currentFile.treeConflictAction = SVNConflictAction.EDIT;
        currentFile.skipReason = SVNStatusType.UNKNOWN;

        if (currentDirectory != null) {
            currentFile.parentBaton = currentDirectory;
            currentFile.shadowed = currentDirectory.shadowed;
            currentFile.skipReason = currentDirectory.skipReason;
        }

        if (currentFile.shadowed) {
        } else if (leftSource != null) {
            SVNNodeKind kind;

            if (rightSource == null) {
                currentFile.treeConflictAction = SVNConflictAction.DELETE;
            }
            SVNWCContext.ObstructionData obstructionData = performObstructionCheck(localAbsPath);
            SVNStatusType obstructionState = obstructionData.obstructionState;
            boolean isDeleted = obstructionData.deleted;
            boolean isExcluded = obstructionData.excluded;
            kind = obstructionData.kind;
            SVNDepth parentDepth = obstructionData.parentDepth;

            if (obstructionState != SVNStatusType.INAPPLICABLE) {
                currentFile.shadowed = true;
                currentFile.treeConflictReason = SVNConflictReason.SKIP;
                currentFile.skipReason = obstructionState;
                return;
            }

            if (isDeleted) {
                kind = SVNNodeKind.NONE;
            }

            if (kind == SVNNodeKind.NONE) {
                currentFile.shadowed = true;

                if (currentDirectory != null && (isExcluded || (parentDepth != SVNDepth.UNKNOWN && parentDepth.compareTo(SVNDepth.FILES) < 0))) {
                    currentFile.shadowed = true;
                    currentFile.treeConflictReason = SVNConflictReason.SKIP;
                    currentFile.skipReason = SVNStatusType.MISSING;
                    return;
                }

                if (isDeleted) {
                    currentFile.treeConflictReason = SVNConflictReason.DELETED;
                } else {
                    currentFile.treeConflictReason = SVNConflictReason.MISSING;
                }

                result.skip = true;
                currentFile.markFileEdited(localAbsPath);
                return;
            } else if (kind != SVNNodeKind.FILE) {
                currentFile.shadowed = true;
                currentFile.treeConflictReason = SVNConflictReason.OBSTRUCTED;
                result.skip = true;
                currentFile.markFileEdited(localAbsPath);
                return;
            }

            if (rightSource == null) {
                currentFile.treeConflictAction = SVNConflictAction.DELETE;
                currentFile.markFileEdited(localAbsPath);
                if (currentFile.shadowed) {
                    return;
                }
                if (currentDirectory != null && currentDirectory.deleteState != null && currentDirectory.deleteState.foundEdit) {
                    result.skip = true;
                }
            }
        } else {
            SVNWCConflictDescription17 oldTreeConflict = null;

            currentFile.added = true;
            currentFile.treeConflictAction = SVNConflictAction.ADD;

            if (currentDirectory != null && currentDirectory.pendingDeletes != null && currentDirectory.pendingDeletes.containsKey(localAbsPath)) {
                currentFile.addIsReplace = true;
                currentFile.treeConflictAction = SVNConflictAction.REPLACE;
                currentDirectory.pendingDeletes.remove(localAbsPath);
            }

            if (currentDirectory != null && currentDirectory.newTreeConflicts != null && currentDirectory.newTreeConflicts.containsKey(localAbsPath)) {
                oldTreeConflict = currentDirectory.newTreeConflicts.get(localAbsPath);
                currentFile.treeConflictAction = SVNConflictAction.REPLACE;
                currentFile.treeConflictReason = oldTreeConflict.getReason();

                recordTreeConflict(localAbsPath, currentDirectory, SVNNodeKind.FILE, currentFile.treeConflictAction, currentFile.treeConflictReason, oldTreeConflict, false);

                if (oldTreeConflict.getReason() == SVNConflictReason.DELETED || oldTreeConflict.getReason() == SVNConflictReason.MOVED_AWAY) {
                } else {
                    result.skip = true;
                    return;
                }
            } else if (!(mergeDriver.dryRun && ((currentDirectory != null && currentDirectory.added) || currentFile.addIsReplace))) {
                SVNWCContext.ObstructionData obstructionData = performObstructionCheck(localAbsPath);

                SVNStatusType obstructionState = obstructionData.obstructionState;
                boolean isDeleted = obstructionData.deleted;
                boolean excluded = obstructionData.excluded;
                SVNNodeKind kind = obstructionData.kind;
                SVNDepth parentDepth = obstructionData.parentDepth;

                if (obstructionState != SVNStatusType.INAPPLICABLE) {
                    currentFile.shadowed = true;
                    currentFile.treeConflictReason = SVNConflictReason.SKIP;
                    currentFile.skipReason = obstructionState;
                } else if (kind != SVNNodeKind.NONE && !isDeleted) {
                    currentFile.shadowed = true;
                    currentFile.treeConflictReason = SVNConflictReason.OBSTRUCTED;
                }
            }

            currentFile.markFileEdited(localAbsPath);
        }
    }

    public void fileChanged(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, File leftFile, File rightFile, SVNProperties leftProps, SVNProperties rightProps, boolean fileModified, SVNProperties propChanges) throws SVNException {
        File localAbsPath = SVNFileUtil.createFilePath(mergeDriver.targetAbsPath, relPath);

        assert localAbsPath != null && SVNFileUtil.isAbsolute(localAbsPath);
        assert leftFile == null || SVNFileUtil.isAbsolute(leftFile);
        assert rightFile == null || SVNFileUtil.isAbsolute(rightFile);

        currentFile.markFileEdited(localAbsPath);

        if (currentFile.shadowed) {
            if (currentFile.treeConflictReason == null) {
                recordSkip(localAbsPath, SVNNodeKind.FILE, SVNEventAction.UPDATE_SHADOWED_UPDATE, currentFile.skipReason);
            }
            return;
        }

        SVNStatusType propertyState = SVNStatusType.UNCHANGED;
        SVNStatusType textState = SVNStatusType.UNCHANGED;

        propChanges = prepareMergePropsChanged(localAbsPath, propChanges);
        SVNConflictVersion[] conflictVersions = makeConflictVersions(localAbsPath, SVNNodeKind.FILE, mergeDriver.reposRootUrl, mergeDriver.mergeSource, mergeDriver.targetAbsPath);
        SVNConflictVersion left = conflictVersions[0];
        SVNConflictVersion right = conflictVersions[1];

        if ((mergeDriver.recordOnly || leftFile == null) && propChanges.size() > 0) {
            SVNWCContext.MergePropertiesInfo mergePropertiesInfo = context.mergeProperties(localAbsPath, left, right, leftProps, propChanges, mergeDriver.dryRun, null);
            propertyState = mergePropertiesInfo.mergeOutcome;

            if (propertyState == SVNStatusType.CONFLICTED) {
                if (mergeDriver.conflictedPaths == null) {
                    mergeDriver.conflictedPaths = new HashSet<File>();
                }
                mergeDriver.conflictedPaths.add(localAbsPath);
            }
        }

        SVNStatusType contentOutcome;

        if (mergeDriver.recordOnly) {

        } else if (leftFile != null) {
            String targetLabel = ".working";
            String leftLabel = ".merge-left.r" + leftSource.getRevision();
            String rightLabel = ".merge-right.r" + rightSource.getRevision();

            boolean hasLocalModifications = context.isTextModified(localAbsPath, false);

            MergeOutcome mergeOutcome = merge(leftFile, rightFile, localAbsPath,
                    leftLabel, rightLabel, targetLabel,
                    left, right,
                    mergeDriver.dryRun, mergeDriver.diff3Cmd,
                    mergeDriver.diffOptions,
                    leftProps, propChanges,
                    true, true, null);
            contentOutcome = mergeOutcome.mergeContentOutcome;
            propertyState = mergeOutcome.mergePropsOutcome;

            if (contentOutcome == SVNStatusType.CONFLICTED || propertyState == SVNStatusType.CONFLICTED) {
                if (mergeDriver.conflictedPaths == null) {
                    mergeDriver.conflictedPaths = new HashSet<File>();
                }
                mergeDriver.conflictedPaths.add(localAbsPath);
            }

            if (contentOutcome == SVNStatusType.CONFLICTED) {
                textState = SVNStatusType.CONFLICTED;
            } else if (hasLocalModifications && contentOutcome != SVNStatusType.UNCHANGED) {
                textState = SVNStatusType.MERGED;
            } else if (contentOutcome == SVNStatusType.MERGED) {
                textState = SVNStatusType.CHANGED;
            } else if (contentOutcome == SVNStatusType.NO_MERGE) {
                textState = SVNStatusType.MISSING;
            } else {
                textState = SVNStatusType.UNCHANGED;
            }
        }
        if (textState == SVNStatusType.CONFLICTED ||
                textState == SVNStatusType.MERGED ||
                textState == SVNStatusType.CHANGED ||
                propertyState == SVNStatusType.CONFLICTED ||
                propertyState == SVNStatusType.MERGED ||
                propertyState == SVNStatusType.CHANGED) {
            recordUpdateUpdate(localAbsPath, SVNNodeKind.FILE, textState, propertyState);
        }
    }

    public void fileAdded(SvnDiffCallbackResult result, File relPath, SvnDiffSource copyFromSource, SvnDiffSource rightSource, File copyFromFile, File rightFile, SVNProperties copyFromProps, SVNProperties rightProps) throws SVNException {
        File localAbsPath = SVNFileUtil.createFilePath(mergeDriver.targetAbsPath, relPath);
        assert SVNFileUtil.isAbsolute(localAbsPath);

        currentFile.markFileEdited(localAbsPath);

        if (currentFile.shadowed) {
            if (currentFile.treeConflictReason == null) {
                recordSkip(localAbsPath, SVNNodeKind.FILE, SVNEventAction.UPDATE_SHADOWED_ADD, currentFile.skipReason);
            }
            return;
        }

        if (mergeDriver.recordOnly) {
            return;
        }
        if ((mergeDriver.mergeSource.ancestral || mergeDriver.reintegrateMerge) && (currentFile.parentBaton == null || currentFile.parentBaton.added)) {
            if (mergeDriver.addedPaths == null) {
                mergeDriver.addedPaths = new HashSet<File>();
            }
            mergeDriver.addedPaths.add(localAbsPath);
        }
        InputStream pristineStream = null;
        InputStream newStream = null;
        SVNProperties pristineProps;
        SVNProperties newProps = null;
        SVNURL copyFromUrl;
        long copyFromRevision;
        try {
            if (!mergeDriver.dryRun) {
                if (mergeDriver.sameRepos) {
                    File child = SVNFileUtil.skipAncestor(mergeDriver.targetAbsPath, localAbsPath);
                    assert child != null;
                    copyFromUrl = mergeDriver.mergeSource.url2.appendPath(SVNFileUtil.getFilePath(child), false);
                    copyFromRevision = rightSource.getRevision();
                    checkReposMatch(mergeDriver.reposRootUrl, localAbsPath, copyFromUrl);
                    pristineStream = SVNFileUtil.openFileForReading(rightFile);
                    newStream = null;

                    pristineProps = rightProps;
                    newProps = null;

                    if (pristineProps.containsName(SVNProperty.MERGE_INFO)) {
                        if (mergeDriver.pathsWithNewMergeInfo == null) {
                            mergeDriver.pathsWithNewMergeInfo = new HashSet<File>();
                        }
                        mergeDriver.pathsWithNewMergeInfo.add(localAbsPath);
                    }
                } else {
                    copyFromUrl = null;
                    copyFromRevision = -1;

                    pristineStream = SVNFileUtil.DUMMY_IN;
                    newStream = SVNFileUtil.openFileForReading(rightFile);

                    pristineProps = new SVNProperties();
                    newProps = new SVNProperties();
                    SvnNgPropertiesManager.categorizeProperties(rightProps, newProps, null, null);

                    newProps.remove(SVNProperty.MERGE_INFO);
                }

                SvnNgReposToWcCopy.addFileToWc(context, localAbsPath, pristineStream, newStream, pristineProps, newProps, copyFromUrl, copyFromRevision);

                mergeDriver.useSleep = true;
            }

            recordUpdateAdd(localAbsPath, SVNNodeKind.FILE, currentFile.addIsReplace);
        } finally {
            SVNFileUtil.closeFile(pristineStream);
            SVNFileUtil.closeFile(newStream);
        }
    }

    public void fileDeleted(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, File leftFile, SVNProperties leftProps) throws SVNException {
        File localAbsPath = SVNFileUtil.createFilePath(mergeDriver.targetAbsPath, relPath);
        currentFile.markFileEdited(localAbsPath);
        if (currentFile.shadowed) {
            if (currentFile.treeConflictReason == null) {
                recordSkip(localAbsPath, SVNNodeKind.FILE, SVNEventAction.UPDATE_SHADOWED_DELETE, currentFile.skipReason);
            }
            return;
        }
        if (mergeDriver.recordOnly) {
            return;
        }
        boolean same;
        if (mergeDriver.forceDelete) {
            same = true;
        } else {
            same = areFilesSame(leftFile, leftProps, localAbsPath);
        }

        if (currentFile.parentBaton != null && currentFile.parentBaton.deleteState != null) {
            if (same) {
                currentFile.parentBaton.deleteState.comparedAbsPaths.add(localAbsPath);
            } else {
                currentFile.parentBaton.deleteState.foundEdit = true;
            }
            return;
        } else if (same) {
            if (!mergeDriver.dryRun) {
                SvnNgRemove.delete(context, localAbsPath, null, false, false, null);
            }
            if (mergeDriver.pathsWithDeletedMergeInfo == null) {
                mergeDriver.pathsWithDeletedMergeInfo = new HashSet<File>();
            }
            mergeDriver.pathsWithDeletedMergeInfo.add(localAbsPath);

            recordUpdateDelete(localAbsPath, SVNNodeKind.FILE, currentFile.parentBaton);
        } else {
            recordTreeConflict(localAbsPath, currentFile.parentBaton, SVNNodeKind.FILE, SVNConflictAction.DELETE, SVNConflictReason.EDITED, null, true);
        }
    }

    public void fileClosed(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource) throws SVNException {
        SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE);
        SVNErrorManager.error(errorMessage, SVNLogType.WC);
    }

    public void dirOpened(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, SvnDiffSource copyFromSource, Object dirBaton) throws SVNException {
        DirectoryBaton currentDirectory = (DirectoryBaton) dirBaton;

        File localAbsPath = SVNFileUtil.createFilePath(mergeDriver.targetAbsPath, relPath);
        DirectoryBaton db = new DirectoryBaton();
        db.treeConflictReason = null;
        db.treeConflictAction = SVNConflictAction.EDIT;
        db.skipReason = SVNStatusType.UNKNOWN;

        DirectoryBaton pdb = currentDirectory;
        result.newBaton = db;

        if (pdb != null) {
            db.parentBaton = pdb;
            db.shadowed = pdb.shadowed;
            db.skipReason = pdb.skipReason;
        }

        if (db.shadowed) {
            if (leftSource == null) {
                db.added = true;
            }
        } else if (leftSource != null) {
            if (rightSource == null) {
                db.treeConflictAction = SVNConflictAction.DELETE;
            }

            SVNWCContext.ObstructionData obstructionData = performObstructionCheck(localAbsPath);
            SVNStatusType obstructionState = obstructionData.obstructionState;
            boolean isDeleted = obstructionData.deleted;
            boolean excluded = obstructionData.excluded;
            SVNNodeKind kind = obstructionData.kind;
            SVNDepth parentDepth = obstructionData.parentDepth;

            if (obstructionState != SVNStatusType.INAPPLICABLE) {
                db.shadowed = true;

                if (obstructionState == SVNStatusType.OBSTRUCTED) {
                    if (context.getDb().isWCRoot(localAbsPath)) {
                        db.treeConflictReason = SVNConflictReason.WC_SKIP;
                        return;
                    }
                }

                db.treeConflictReason = SVNConflictReason.SKIP;
                db.skipReason = obstructionState;

                if (rightSource == null) {
                    result.skip = result.skipChildren = true;
                    db.markDirectoryEdited(localAbsPath);
                }

                return;
            }
            if (isDeleted) {
                kind = SVNNodeKind.NONE;
            }
            if (kind == SVNNodeKind.NONE) {
                db.shadowed = true;

                if (pdb != null && (excluded || (parentDepth != SVNDepth.UNKNOWN && parentDepth.compareTo(SVNDepth.IMMEDIATES) < 0))) {
                    db.shadowed = true;
                    db.treeConflictReason = SVNConflictReason.SKIP;
                    db.skipReason = SVNStatusType.MISSING;
                    return;
                }

                if (isDeleted) {
                    db.treeConflictReason = SVNConflictReason.DELETED;
                } else {
                    db.treeConflictReason = SVNConflictReason.MISSING;
                }

                result.skip = true;
                result.skipChildren = true;
                db.markDirectoryEdited(localAbsPath);
                return;
            } else if (kind != SVNNodeKind.DIR) {
                db.shadowed = true;
                db.treeConflictReason = SVNConflictReason.OBSTRUCTED;
                result.skip = true;
                result.skipChildren = true;
                db.markDirectoryEdited(localAbsPath);
                return;
            }

            if (rightSource == null) {
                db.treeConflictAction = SVNConflictAction.DELETE;
                db.markDirectoryEdited(localAbsPath);
                if (db.shadowed) {
                    result.skipChildren = true;
                    return;
                }
                db.deleteState = pdb != null ? pdb.deleteState : null;
                if (db.deleteState != null && db.deleteState.foundEdit) {
                    result.skip = true;
                    result.skipChildren = true;
                } else if (mergeDriver.forceDelete) {
                    result.skipChildren = true;
                } else if (db.deleteState == null) {
                    db.deleteState = new DirectoryDeleteBaton();
                    db.deleteState.delRoot = db;
                    db.deleteState.comparedAbsPaths = new HashSet<File>();
                }
            }
        } else {
            SVNWCConflictDescription17 oldTc = null;
            db.added = true;
            db.treeConflictAction = SVNConflictAction.ADD;
            if (pdb != null && pdb.pendingDeletes != null && pdb.pendingDeletes.containsKey(localAbsPath)) {
                db.addIsReplace = true;
                db.treeConflictAction = SVNConflictAction.REPLACE;
                pdb.pendingDeletes.remove(localAbsPath);
            }

            if (pdb != null && pdb.newTreeConflicts != null && pdb.newTreeConflicts.containsKey(localAbsPath)) {
                oldTc = pdb.newTreeConflicts.get(localAbsPath);
                db.treeConflictAction = SVNConflictAction.REPLACE;
                db.treeConflictReason = oldTc.getReason();

                if (oldTc.getReason() == SVNConflictReason.DELETED || oldTc.getReason() == SVNConflictReason.MOVED_AWAY) {

                } else {
                    result.skip = true;
                    result.skipChildren = true;

                    recordTreeConflict(localAbsPath, pdb, SVNNodeKind.DIR, db.treeConflictAction, db.treeConflictReason, oldTc, false);
                    return;
                }
            }

            if (! (mergeDriver.dryRun && ((pdb != null && pdb.added) || db.addIsReplace))) {
                SVNWCContext.ObstructionData obstructionData = performObstructionCheck(localAbsPath);
                SVNStatusType obstructionState = obstructionData.obstructionState;
                boolean isDeleted = obstructionData.deleted;
                SVNNodeKind kind = obstructionData.kind;

                if (obstructionState == SVNStatusType.OBSTRUCTED && (isDeleted || kind == SVNNodeKind.NONE)) {
                    if (SVNFileType.getType(localAbsPath) == SVNFileType.DIRECTORY) {
                        obstructionState = SVNStatusType.INAPPLICABLE;
                        db.addExisting = true;
                    }
                }

                if (obstructionState != SVNStatusType.INAPPLICABLE) {
                    db.shadowed = true;
                    db.treeConflictReason = SVNConflictReason.SKIP;
                    db.skipReason = obstructionState;
                } else if (kind != SVNNodeKind.NONE && !isDeleted) {
                    db.shadowed = true;
                    db.treeConflictReason = SVNConflictReason.OBSTRUCTED;
                }
            }

            db.markDirectoryEdited(localAbsPath);

            if (db.shadowed) {

            } else if (mergeDriver.recordOnly) {
                result.skip = true;
                result.skipChildren = true;
            } else if (!mergeDriver.dryRun) {
                if (!db.addExisting) {
                    SVNFileUtil.ensureDirectoryExists(localAbsPath);
                }
                if (oldTc != null) {
                    context.deleteTreeConflict(localAbsPath);
                }
                if (mergeDriver.sameRepos) {
                    SVNURL originalUrl = mergeDriver.mergeSource.url2.appendPath(SVNFileUtil.getFilePath(relPath), false);

                    SvnNgAdd svnNgAdd = new SvnNgAdd();
                    svnNgAdd.setWcContext(context);
                    svnNgAdd.add(localAbsPath, SVNDepth.INFINITY, originalUrl, rightSource.getRevision(), false);

                } else {
                    SvnNgAdd svnNgAdd = new SvnNgAdd();
                    svnNgAdd.setWcContext(context);
                    svnNgAdd.addFromDisk(localAbsPath, null, false);
                }

                if (oldTc != null) {
                    recordTreeConflict(localAbsPath, pdb, SVNNodeKind.DIR, db.treeConflictAction, db.treeConflictReason, oldTc, false);
                }
            }

            if (!db.shadowed && !mergeDriver.recordOnly) {
                recordUpdateAdd(localAbsPath, SVNNodeKind.DIR, db.addIsReplace);
            }
        }
    }

    public void dirChanged(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, SVNProperties leftProps, SVNProperties rightProps, SVNProperties propChanges, Object dirBaton) throws SVNException {
        DirectoryBaton currentDirectory = (DirectoryBaton) dirBaton;

        File localAbsPath = SVNFileUtil.createFilePath(mergeDriver.targetAbsPath, relPath);

        DirectoryBaton db = currentDirectory;
        handlePendingNotifications(db);

        db.markDirectoryEdited(localAbsPath);

        if (db.shadowed) {
            if (db.treeConflictReason == null) {
                recordSkip(localAbsPath, SVNNodeKind.DIR, SVNEventAction.UPDATE_SHADOWED_UPDATE, db.skipReason);
            }
            return;
        }

        SVNProperties props = prepareMergePropsChanged(localAbsPath, propChanges);

        if (props.size() > 0) {
            SVNConflictVersion[] conflictVersions = makeConflictVersions(localAbsPath, SVNNodeKind.DIR, mergeDriver.reposRootUrl, mergeDriver.mergeSource, mergeDriver.targetAbsPath);
            SVNConflictVersion left = conflictVersions[0];
            SVNConflictVersion right = conflictVersions[1];

            SVNWCContext.MergePropertiesInfo mergePropertiesInfo = context.mergeProperties(localAbsPath, left, right, leftProps, props, mergeDriver.dryRun, null);
            SVNStatusType propState = mergePropertiesInfo.mergeOutcome;

            if (propState == SVNStatusType.CONFLICTED) {
                if (mergeDriver.conflictedPaths == null) {
                    mergeDriver.conflictedPaths = new HashSet<File>();
                }
                mergeDriver.conflictedPaths.add(localAbsPath);
            }

            if (propState == SVNStatusType.CONFLICTED || propState == SVNStatusType.MERGED || propState == SVNStatusType.CHANGED) {
                recordUpdateUpdate(localAbsPath, SVNNodeKind.FILE, SVNStatusType.INAPPLICABLE, propState);
            }
        }
    }

    public void dirDeleted(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SVNProperties leftProps, Object dirBaton) throws SVNException {
        DirectoryBaton currentDirectory = (DirectoryBaton) dirBaton;

        File localAbsPath = SVNFileUtil.createFilePath(mergeDriver.targetAbsPath, relPath);
        DirectoryBaton db = currentDirectory;
        handlePendingNotifications(db);
        db.markDirectoryEdited(localAbsPath);
        if (db.shadowed) {
            if (db.treeConflictReason == null) {
                recordSkip(localAbsPath, SVNNodeKind.DIR, SVNEventAction.UPDATE_SHADOWED_DELETE, db.skipReason);
            }
            return;
        }
        if (mergeDriver.recordOnly) {
            return;
        }
        boolean same;
        SVNProperties workingProps = context.getActualProps(localAbsPath);
        if (mergeDriver.forceDelete) {
            same = true;
        } else {
            same = arePropertiesSame(leftProps, workingProps);

            DirectoryDeleteBaton delBaton = db.deleteState;
            assert delBaton != null;

            if (!same) {
                delBaton.foundEdit = true;
            } else {
                delBaton.comparedAbsPaths.add(localAbsPath);
            }
            if (delBaton.delRoot != db) {
                return;
            }
            if (delBaton.foundEdit) {
                same = false;
            } else if (mergeDriver.forceDelete) {
                same = true;
            } else {
                try {
                    SVNStatusEditor17 statusEditor17 = new SVNStatusEditor17(localAbsPath, context, context.getOptions(), false, true, SVNDepth.INFINITY, new VerifyTouchedByDelCheck(delBaton));
                    statusEditor17.walkStatus(localAbsPath, SVNDepth.INFINITY, true, false, true, null);
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() != SVNErrorCode.CEASE_INVOCATION) {
                        throw e;
                    }
                }

                same = !delBaton.foundEdit;
            }
        }
        if (same && !mergeDriver.dryRun) {
            try {
                SvnNgRemove.delete(context, localAbsPath, null, false, false, null);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_LEFT_LOCAL_MOD) {
                    throw e;
                }
                same = false;
            }
        }

        if (!same) {
            recordTreeConflict(localAbsPath, db.parentBaton, SVNNodeKind.DIR, SVNConflictAction.DELETE, SVNConflictReason.EDITED, null, true);
        } else {
            if (workingProps != null && workingProps.containsName(SVNProperty.MERGE_INFO)) {
                if (mergeDriver.pathsWithDeletedMergeInfo == null) {
                    mergeDriver.pathsWithDeletedMergeInfo = new HashSet<File>();
                }
                mergeDriver.pathsWithDeletedMergeInfo.add(localAbsPath);
            }

            recordUpdateDelete(localAbsPath, SVNNodeKind.DIR, db.parentBaton);
        }
    }

    public void dirAdded(SvnDiffCallbackResult result, File relPath, SvnDiffSource copyFromSource, SvnDiffSource rightSource, SVNProperties copyFromProps, SVNProperties rightProps, Object dirBaton) throws SVNException {
        DirectoryBaton currentDirectory = (DirectoryBaton) dirBaton;

        File localAbsPath = SVNFileUtil.createFilePath(mergeDriver.targetAbsPath, relPath);

        DirectoryBaton db = currentDirectory;
        handlePendingNotifications(db);

        db.markDirectoryEdited(localAbsPath);
        if (db.shadowed) {
            if (db.treeConflictReason == null) {
                recordSkip(localAbsPath, SVNNodeKind.DIR, SVNEventAction.UPDATE_SHADOWED_ADD, db.skipReason);
            }
            return;
        }

        assert db.edited && !mergeDriver.recordOnly;

        if ((mergeDriver.mergeSource.ancestral || mergeDriver.reintegrateMerge) && (db.parentBaton == null || !db.parentBaton.added)) {
            if (mergeDriver.addedPaths == null) {
                mergeDriver.addedPaths = new HashSet<File>();
            }
            mergeDriver.addedPaths.add(localAbsPath);
        }

        if (mergeDriver.sameRepos) {
            SVNProperties newPristineProps = rightProps;
            File parentAbsPath = SVNFileUtil.getParentFile(localAbsPath);
            File child = SVNFileUtil.skipAncestor(mergeDriver.targetAbsPath, localAbsPath);
            assert child != null;

            SVNURL copyFromUrl = mergeDriver.mergeSource.url2.appendPath(SVNFileUtil.getFilePath(child), false);
            long copyFromRevision = rightSource.getRevision();

            checkReposMatch(mergeDriver.reposRootUrl, localAbsPath, copyFromUrl);
            if (!mergeDriver.dryRun) {
                completeDirectoryAdd(localAbsPath, newPristineProps, copyFromUrl, copyFromRevision);
            }

            if (newPristineProps.containsName(SVNProperty.MERGE_INFO)) {
                if (mergeDriver.pathsWithNewMergeInfo == null) {
                    mergeDriver.pathsWithNewMergeInfo = new HashSet<File>();
                }
                mergeDriver.pathsWithNewMergeInfo.add(localAbsPath);
            }
        } else {
            SVNProperties newProps = new SVNProperties();
            SvnNgPropertiesManager.categorizeProperties(rightProps, newProps, null, null);
            newProps.remove(SVNProperty.MERGE_INFO);

            SVNWCContext.MergePropertiesInfo mergePropertiesInfo = context.mergeProperties(localAbsPath, null, null, new SVNProperties(), newProps, mergeDriver.dryRun, null);
            if (mergePropertiesInfo.mergeOutcome == SVNStatusType.CONFLICTED) {
                if (mergeDriver.conflictedPaths == null) {
                    mergeDriver.conflictedPaths = new HashSet<File>();
                }
                mergeDriver.conflictedPaths.add(localAbsPath);
            }
        }
    }

    public void dirPropsChanged(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, SVNProperties leftProps, SVNProperties rightProps, SVNProperties propChanges) throws SVNException {
    }

    public void dirClosed(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, Object dirBaton) throws SVNException {
        DirectoryBaton currentDirectory = (DirectoryBaton) dirBaton;
        handlePendingNotifications(currentDirectory);
    }

    public void nodeAbsent(SvnDiffCallbackResult result, File relPath, Object dirBaton) throws SVNException {
        File localAbsPath = SVNFileUtil.createFilePath(mergeDriver.targetAbsPath, relPath);
        recordSkip(localAbsPath, SVNNodeKind.UNKNOWN, SVNEventAction.SKIP, SVNStatusType.MISSING);
    }

    private void handlePendingNotifications(DirectoryBaton db) throws SVNException {
        if (context.getEventHandler() != null && db.pendingDeletes != null) {
            for (Map.Entry<File, SVNNodeKind> entry : db.pendingDeletes.entrySet()) {
                File delAbsPath = entry.getKey();
                SVNNodeKind kind = entry.getValue();
                SVNEvent event = SVNEventFactory.createSVNEvent(delAbsPath, kind, null, -1, SVNEventAction.UPDATE_DELETE, SVNEventAction.UPDATE_DELETE, null, null);
                context.getEventHandler().handleEvent(event, ISVNEventHandler.UNKNOWN);
            }
            db.pendingDeletes = null;
        }
    }

    private static class MergeOutcome {
        private SVNStatusType mergeContentOutcome;
        private SVNStatusType mergePropsOutcome;
    }

    private MergeOutcome merge(File leftAbsPath, File rightAbsPath, File targetAbsPath,
                               String leftLabel, String rightLabel, String targetLabel,
                               SVNConflictVersion leftVersion, SVNConflictVersion rightVersion,
                               boolean dryRun, String diff3Cmd, SVNDiffOptions mergeOptions,
                               SVNProperties originalProps,
                               SVNProperties propChanges,
                               boolean mergeContentNeeded, boolean mergePropsNeeded, ISVNConflictHandler conflictResolver) throws SVNException {
        assert SVNFileUtil.isAbsolute(leftAbsPath);
        assert SVNFileUtil.isAbsolute(rightAbsPath);
        assert SVNFileUtil.isAbsolute(targetAbsPath);

        SVNSkel conflictSkel = null;
        SVNSkel workItems = null;

        MergeOutcome result = new MergeOutcome();

        File dirAbsPath = SVNFileUtil.getParentFile(targetAbsPath);

        if (!dryRun) {
            context.writeCheck(dirAbsPath);
        }

        Structure<StructureFields.NodeInfo> nodeInfoStructure = context.getDb().readInfo(targetAbsPath, StructureFields.NodeInfo.status, StructureFields.NodeInfo.kind, StructureFields.NodeInfo.conflicted,
                StructureFields.NodeInfo.hadProps, StructureFields.NodeInfo.propsMod);
        ISVNWCDb.SVNWCDbStatus status = nodeInfoStructure.get(StructureFields.NodeInfo.status);
        ISVNWCDb.SVNWCDbKind kind = nodeInfoStructure.get(StructureFields.NodeInfo.kind);
        boolean conflicted = nodeInfoStructure.is(StructureFields.NodeInfo.conflicted);
        boolean hadProps = nodeInfoStructure.is(StructureFields.NodeInfo.hadProps);
        boolean propMods = nodeInfoStructure.is(StructureFields.NodeInfo.propsMod);

        if (kind != ISVNWCDb.SVNWCDbKind.File || (status == ISVNWCDb.SVNWCDbStatus.Normal && status == ISVNWCDb.SVNWCDbStatus.Added)) {
            result.mergeContentOutcome = SVNStatusType.NO_MERGE;
            if (mergePropsNeeded) {
                result.mergePropsOutcome = SVNStatusType.UNCHANGED;
            }
            return result;
        }

        if (conflicted) {
            SVNWCContext.ConflictInfo conflictInfo = context.getConflicted(targetAbsPath, true, true, true);
            boolean textConflicted = conflictInfo.textConflicted;
            boolean propConflicted = conflictInfo.propConflicted;
            boolean treeConflicted = conflictInfo.treeConflicted;

            if (textConflicted || propConflicted || treeConflicted) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Can''t merge into conflicted node ''{0}''", targetAbsPath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }

        }
        SVNProperties pristineProps = null;
        if (mergePropsNeeded && hadProps) {
            pristineProps = context.getDb().readPristineProperties(targetAbsPath);
        } else if (mergePropsNeeded) {
            pristineProps = new SVNProperties();
        }

        SVNProperties oldActualProps;
        if (propMods) {
            oldActualProps = context.getDb().readProperties(targetAbsPath);
        } else if (pristineProps != null) {
            oldActualProps = pristineProps;
        } else {
            oldActualProps = new SVNProperties();
        }

        SVNProperties newActualProps = null;
        if (mergePropsNeeded) {
            Set<String> propChangesNames = propChanges.nameSet();
            for (String propName : propChangesNames) {
                if (!SVNProperty.isRegularProperty(propName)) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.BAD_PROP_KIND, "The property ''{0}'' may not be merged into ''{1}''.", propName, targetAbsPath);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }
            }
            SVNWCContext.MergePropertiesInfo mergePropertiesInfo = new SVNWCContext.MergePropertiesInfo();
            mergePropertiesInfo.conflictSkel = conflictSkel;
            context.mergeProperties3(mergePropertiesInfo, targetAbsPath, originalProps, pristineProps, oldActualProps, propChanges);
            conflictSkel = mergePropertiesInfo.conflictSkel;
            newActualProps = mergePropertiesInfo.newActualProperties;
            result.mergePropsOutcome = mergePropertiesInfo.mergeOutcome;
        }

        SVNWCContext.MergeInfo mergeInfo = context.merge(workItems, conflictSkel, leftAbsPath, rightAbsPath, targetAbsPath, targetAbsPath,
                leftLabel, rightLabel, targetLabel,
                oldActualProps, dryRun, mergeOptions, propChanges);
        conflictSkel = mergeInfo.conflictSkel;
        workItems = mergeInfo.workItems;
        result.mergeContentOutcome = mergeInfo.mergeOutcome;

        if (!dryRun) {
            if (conflictSkel != null) {
                SvnWcDbConflicts.conflictSkelOpMerge(conflictSkel, leftVersion, rightVersion);
                SVNSkel workItem = SvnWcDbConflicts.createConflictMarkers(context.getDb(), targetAbsPath, conflictSkel);
                workItems = SVNWCContext.wqMerge(workItems, workItem);
            }
            if (newActualProps != null) {
                context.getDb().opSetProps(targetAbsPath, newActualProps, conflictSkel, SVNWCContext.hasMagicProperty(propChanges), workItems);
            } else if (conflictSkel != null) {
                context.getDb().opMarkConflict(targetAbsPath, conflictSkel, workItems);
            } else if (workItems != null) {
                context.getDb().addWorkQueue(targetAbsPath, workItems);
            }
            if (workItems != null) {
                context.wqRun(targetAbsPath);
            }
            if (conflictSkel != null && conflictResolver != null) {
                context.invokeConflictResolver(targetAbsPath, conflictSkel, conflictResolver, context.getEventHandler());
            }
            SVNWCContext.ConflictInfo conflictInfo = context.getConflicted(targetAbsPath, true, true, false);
            if (result.mergePropsOutcome == SVNStatusType.CONFLICTED && !conflictInfo.propConflicted) {
                result.mergePropsOutcome = SVNStatusType.MERGED;
            }
            if (result.mergeContentOutcome == SVNStatusType.CONFLICTED && !conflictInfo.textConflicted) {
                result.mergeContentOutcome = SVNStatusType.MERGED;
            }
        }

        return result;
    }

    private void recordUpdateUpdate(File localAbsPath, SVNNodeKind kind, SVNStatusType contentState, SVNStatusType propState) throws SVNException {
        if (mergeDriver.mergeSource.ancestral || mergeDriver.reintegrateMerge) {
            if (mergeDriver.mergedPaths == null) {
                mergeDriver.mergedPaths = new HashSet<File>();
            }
            mergeDriver.mergedPaths.add(localAbsPath);
        }

        ISVNEventHandler eventHandler = context.getEventHandler();
        if (eventHandler != null) {
            notifyMergeBegin(localAbsPath, false);

            SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, kind, null, -1, contentState, propState, SVNStatusType.LOCK_UNKNOWN, SVNEventAction.UPDATE_UPDATE, SVNEventAction.UPDATE_UPDATE, null, null);
            eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);
        }
    }

    private void recordUpdateAdd(File localAbsPath, SVNNodeKind kind, boolean notifyReplaced) throws SVNException {
        if (mergeDriver.mergeSource.ancestral || mergeDriver.reintegrateMerge) {
            if (mergeDriver.mergedPaths == null) {
                mergeDriver.mergedPaths = new HashSet<File>();
            }
            mergeDriver.mergedPaths.add(localAbsPath);
        }
        ISVNEventHandler eventHandler = context.getEventHandler();
        if (eventHandler != null) {
            notifyMergeBegin(localAbsPath, false);

            SVNEventAction action = SVNEventAction.UPDATE_ADD;
            if (notifyReplaced) {
                action = SVNEventAction.UPDATE_REPLACE;
            }

            SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, kind, null, -1, SVNStatusType.UNKNOWN, SVNStatusType.UNKNOWN, SVNStatusType.LOCK_UNKNOWN, action, action, null, null);
            eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);
        }
    }

    private void recordUpdateDelete(File localAbsPath, SVNNodeKind kind, DirectoryBaton parentBaton) throws SVNException {
        if (mergeDriver.mergeSource.ancestral || mergeDriver.reintegrateMerge) {
            if (mergeDriver.addedPaths != null) {
                mergeDriver.addedPaths.remove(localAbsPath);
            }
            if (mergeDriver.mergedPaths == null) {
                mergeDriver.mergedPaths = new HashSet<File>();
            }
            mergeDriver.mergedPaths.add(localAbsPath);
        }
        notifyMergeBegin(localAbsPath, true);
        if (parentBaton != null) {
            if (parentBaton.pendingDeletes == null) {
                parentBaton.pendingDeletes = new HashMap<File, SVNNodeKind>();
            }
            parentBaton.pendingDeletes.put(localAbsPath, kind);
        }
    }

    private void notifyMergeBegin(File localAbsPath, boolean deleteAction) throws SVNException {
        SVNMergeRange nRange = new SVNMergeRange(-1, -1, true);
        if (context.getEventHandler() == null) {
            return;
        }
        File notifyAbsPath;

        if (mergeDriver.mergeSource.ancestral) {
            long[] nRangeRevisions = new long[2];
            SvnNgMergeDriver.MergePath child = SvnNgMergeDriver.findNearestAncestorWithIntersectingRanges(nRangeRevisions, mergeDriver.notifyBegin.nodesWithMergeInfo, !deleteAction, localAbsPath);
            nRange.setStartRevision(nRangeRevisions[0]);
            nRange.setEndRevision(nRangeRevisions[1]);
            if (child == null && deleteAction) {
                child = SvnNgMergeDriver.findNearestAncestor(mergeDriver.notifyBegin.nodesWithMergeInfo, true, localAbsPath);
            }
            assert child != null;

            if (child == null) {
                return;
            }

            if (mergeDriver.notifyBegin.lastAbsPath != null && child.absPath.equals(mergeDriver.notifyBegin.lastAbsPath)) {
                return;
            }

            mergeDriver.notifyBegin.lastAbsPath = child.absPath;

            if (child.absent || child.remainingRanges.getSize() == 0 || !SVNRevision.isValidRevisionNumber(nRange.getStartRevision())) {
                return;
            }

            notifyAbsPath = child.absPath;
        } else {
            if (mergeDriver.notifyBegin.lastAbsPath != null) {
                return;
            }
            notifyAbsPath = mergeDriver.targetAbsPath;
            mergeDriver.notifyBegin.lastAbsPath = mergeDriver.targetAbsPath;
        }
        SVNMergeRange mergeRange;
        if (SVNRevision.isValidRevisionNumber(nRange.getStartRevision())) {
            removeSourceGap(nRange, mergeDriver.implicitSrcGap);
            mergeRange = nRange;
        } else {
            mergeRange = null;
        }
        SVNEvent event = SVNEventFactory.createSVNEvent(notifyAbsPath, SVNNodeKind.UNKNOWN, null, -1, mergeDriver.sameRepos ? SVNEventAction.MERGE_BEGIN : SVNEventAction.FOREIGN_MERGE_BEGIN, mergeDriver.sameRepos ? SVNEventAction.MERGE_BEGIN : SVNEventAction.FOREIGN_MERGE_BEGIN, null, mergeRange);
        context.getEventHandler().handleEvent(event, ISVNEventHandler.UNKNOWN);
    }

    private void checkReposMatch(SVNURL reposRootUrl, File localAbsPath, SVNURL url) throws SVNException {
        if (!SVNPathUtil.isAncestor(reposRootUrl.toString(), url.toString())) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "URL ''{0}'' of ''{1}'' is not in repository ''{2}''", url, localAbsPath, reposRootUrl);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
    }

    private void removeSourceGap(SVNMergeRange range, SVNMergeRangeList implicitSrcGap) {
        if (implicitSrcGap != null) {
            SVNMergeRange gapRange = implicitSrcGap.getRanges()[0];
            if (range.getStartRevision() < range.getEndRevision()) {
                if (gapRange.getStartRevision() == range.getStartRevision()) {
                    range.setStartRevision(gapRange.getEndRevision());
                }
            } else {
                if (gapRange.getStartRevision() == range.getEndRevision()) {
                    range.setEndRevision(gapRange.getEndRevision());
                }
            }
        }
    }

    private SVNProperties prepareMergePropsChanged(File localAbsPath, SVNProperties propChanges) throws SVNException {
        assert SVNFileUtil.isAbsolute(localAbsPath);

        SVNProperties props = new SVNProperties();
        SvnNgPropertiesManager.categorizeProperties(propChanges, props, null, null);

        if (mergeDriver.recordOnly && props.size() != 0) {
            SVNProperties mergeInfoProps = new SVNProperties();

            for (String propName : props.nameSet()) {
                SVNPropertyValue propertyValue = props.getSVNPropertyValue(propName);
                  if (propName.equals(SVNProperty.MERGE_INFO)) {
                      mergeInfoProps.put(propName, propertyValue);
                  }
            }

            props = mergeInfoProps;
        }

        if (props.size() > 0) {
            if (!mergeDriver.sameRepos) {
                props = SvnNgMergeCallback.omitMergeInfoChanges(props);
            }
            if (mergeDriver.mergeSource.rev1 < mergeDriver.mergeSource.rev2 || !mergeDriver.mergeSource.ancestral) {
                if (mergeDriver.isHonorMergeInfo() || mergeDriver.reintegrateMerge) {
                    props = filterSelfReferentialMergeInfo(props, localAbsPath, mergeDriver.repos2);
                }
            }
        }
        SVNProperties propUpdates = props;

        if (props.size() > 0) {
            for (String propName : props.nameSet()) {
                if (propName.equals(SVNProperty.MERGE_INFO)) {
                    boolean hasPristineMergeInfo = false;
                    SVNProperties pristineProps = context.getPristineProps(localAbsPath);
                    if (pristineProps != null && pristineProps.containsName(SVNProperty.MERGE_INFO)) {
                        hasPristineMergeInfo = true;
                    }

                    SVNPropertyValue propertyValue = props.getSVNPropertyValue(propName);
                    if (!hasPristineMergeInfo && propertyValue != null) {
                        if (mergeDriver.pathsWithNewMergeInfo == null) {
                            mergeDriver.pathsWithNewMergeInfo = new HashSet<File>();
                        }
                        mergeDriver.pathsWithNewMergeInfo.add(localAbsPath);
                    } else if (hasPristineMergeInfo && propertyValue == null) {
                        if (mergeDriver.pathsWithDeletedMergeInfo == null) {
                            mergeDriver.pathsWithDeletedMergeInfo = new HashSet<File>();
                        }
                        mergeDriver.pathsWithDeletedMergeInfo.add(localAbsPath);
                    }
                }
            }
        }

        return propUpdates;
    }

    private SVNConflictVersion[] makeConflictVersions(File victimAbsPath, SVNNodeKind nodeKind, SVNURL rootUrl, SvnNgMergeDriver.MergeSource mergeSource, File targetAbsPath) {
        File child = SVNFileUtil.skipAncestor(targetAbsPath, victimAbsPath);
        assert child != null;

        String leftPath = SVNPathUtil.getRelativePath(rootUrl.toDecodedString(), mergeSource.url1.toDecodedString());
        String rightPath = SVNPathUtil.getRelativePath(rootUrl.toDecodedString(), mergeSource.url2.toDecodedString());

        SVNConflictVersion[] svnConflictVersions = new SVNConflictVersion[2];
        svnConflictVersions[0] = new SVNConflictVersion(rootUrl, SVNPathUtil.append(leftPath, SVNFileUtil.getFilePath(child)), mergeSource.rev1, nodeKind);
        svnConflictVersions[1] = new SVNConflictVersion(rootUrl, SVNPathUtil.append(rightPath, SVNFileUtil.getFilePath(child)), mergeSource.rev2, nodeKind);
        return svnConflictVersions;
    }

    private void recordSkip(File localAbsPath, SVNNodeKind kind, SVNEventAction action, SVNStatusType state) throws SVNException {
        if (mergeDriver.recordOnly) {
            return;
        }

        if (mergeDriver.mergeSource.ancestral || mergeDriver.reintegrateMerge) {
            if (mergeDriver.skippedPaths == null) {
                mergeDriver.skippedPaths = new HashSet<File>();
            }
            mergeDriver.skippedPaths.add(localAbsPath);
        }

        ISVNEventHandler eventHandler = context.getEventHandler();
        if (eventHandler != null) {
            notifyMergeBegin(localAbsPath, false);

            SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, kind, null, -1, state, state, SVNStatusType.LOCK_UNKNOWN, action, action, null, null);
            eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);
        }
    }

    private SVNWCContext.ObstructionData performObstructionCheck(File localAbsPath) throws SVNException {
        return context.checkForObstructions(localAbsPath, localAbsPath.equals(mergeDriver.targetAbsPath));
    }

    private void recordTreeConflict(File localAbsPath, DirectoryBaton parentBaton, SVNNodeKind nodeKind, SVNConflictAction action, SVNConflictReason reason, SVNWCConflictDescription17 existingConflict, boolean notifyTreeConflict) throws SVNException {
        if (mergeDriver.mergeSource.ancestral || mergeDriver.reintegrateMerge) {
            if (mergeDriver.treeConflictedPaths == null) {
                mergeDriver.treeConflictedPaths = new HashSet<File>();
            }
            mergeDriver.treeConflictedPaths.add(localAbsPath);
        }
        if (mergeDriver.conflictedPaths == null) {
            mergeDriver.conflictedPaths = new HashSet<File>();
        }
        mergeDriver.conflictedPaths.add(localAbsPath);

        if (!mergeDriver.recordOnly && !mergeDriver.dryRun) {
            if (reason == SVNConflictReason.DELETED) {
                SVNWCContext.NodeMovedAway nodeMovedAway = context.nodeWasMovedAway(localAbsPath);
                File movedToAbsPath = nodeMovedAway.movedToAbsPath;
                if (movedToAbsPath != null) {
                    reason = SVNConflictReason.MOVED_AWAY;
                }
            } else if (reason == SVNConflictReason.ADDED) {
                SVNWCContext.NodeMovedHere nodeMovedHere = context.nodeWasMovedHere(localAbsPath);
                File movedFromAbsPath = nodeMovedHere.movedFromAbsPath;
                if (movedFromAbsPath != null) {
                    reason = SVNConflictReason.MOVED_HERE;
                }
            }

            SVNConflictVersion[] conflictVersions = makeConflictVersions(localAbsPath, nodeKind, mergeDriver.reposRootUrl, mergeDriver.mergeSource, mergeDriver.targetAbsPath);
            SVNConflictVersion left = conflictVersions[0];
            SVNConflictVersion right = conflictVersions[1];

            if (existingConflict != null && existingConflict.getSrcLeftVersion() != null) {
                left = existingConflict.getSrcLeftVersion();
            }

            SVNWCConflictDescription17 conflict = new SVNWCConflictDescription17();
            conflict.setLocalAbspath(localAbsPath);
            conflict.setNodeKind(nodeKind);
            conflict.setOperation(SVNOperation.MERGE);
            conflict.setSrcLeftVersion(left);
            conflict.setSrcRightVersion(right);
            conflict.setAction(action);
            conflict.setReason(reason);

            if (existingConflict != null) {
                context.deleteTreeConflict(localAbsPath);
            }

            context.addTreeConflict(conflict);

            if (parentBaton != null) {
                if (parentBaton.newTreeConflicts == null) {
                    parentBaton.newTreeConflicts = new HashMap<File, SVNWCConflictDescription17>();
                }
                parentBaton.newTreeConflicts.put(localAbsPath, conflict);
            }
        }

        ISVNEventHandler eventHandler = context.getEventHandler();
        if (eventHandler != null && notifyTreeConflict) {
            notifyMergeBegin(localAbsPath, false);

            SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, nodeKind, null, -1, SVNEventAction.TREE_CONFLICT, SVNEventAction.TREE_CONFLICT, null, null);
            eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);
        }
    }

    private SVNProperties filterSelfReferentialMergeInfo(SVNProperties props, File targetAbsPath, SVNRepository svnRepository) throws SVNException {
        Structure<StructureFields.NodeOriginInfo> nodeOrigin = context.getNodeOrigin(targetAbsPath, false,
                StructureFields.NodeOriginInfo.isCopy, StructureFields.NodeOriginInfo.revision,
                StructureFields.NodeOriginInfo.reposRelpath, StructureFields.NodeOriginInfo.reposRootUrl, StructureFields.NodeOriginInfo.reposUuid);
        boolean isCopy = nodeOrigin.is(StructureFields.NodeOriginInfo.isCopy);
        long revision = nodeOrigin.lng(StructureFields.NodeOriginInfo.revision);
        File reposRelPath = nodeOrigin.get(StructureFields.NodeOriginInfo.reposRelpath);
        SVNURL reposRootUrl = nodeOrigin.get(StructureFields.NodeOriginInfo.reposRootUrl);
        String reposUuid = nodeOrigin.get(StructureFields.NodeOriginInfo.reposUuid);

        if (isCopy || reposRelPath == null) {
            return null;
        }
        SVNURL url = reposRootUrl.appendPath(SVNFileUtil.getFilePath(reposRelPath), false);
        SVNProperties adjustedProps = new SVNProperties();

        Set<String> propNames = props.nameSet();
        for (String propName : propNames) {
            SVNPropertyValue propertyValue = props.getSVNPropertyValue(propName);
            if (!propName.equals(SVNProperty.MERGE_INFO) || propertyValue == null || SVNPropertyValue.getPropertyAsBytes(propertyValue).length == 0) {
                adjustedProps.put(propName, propertyValue);
                continue;
            }
            Map<String, SVNMergeRangeList> mergeInfo;
            try {
                mergeInfo = SVNMergeInfoUtil.parseMergeInfo(new StringBuffer(SVNPropertyValue.getPropertyAsString(propertyValue)), null);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.MERGE_INFO_PARSE_ERROR) {
                    adjustedProps.put(propName, propertyValue);
                    continue;
                } else {
                    throw e;
                }
            }

            SplitMergeInfo splitMergeInfo = splitMergeInfoOnRevision(mergeInfo, revision);
            mergeInfo = splitMergeInfo.mergeInfo;
            Map<String, SVNMergeRangeList> youngerMergeInfo = splitMergeInfo.youngerMergeInfo;
            Map<String, SVNMergeRangeList> filteredYoungerMergeInfo = null;
            if (youngerMergeInfo != null) {
                SVNURL mergeSourceRootUrl = svnRepository.getRepositoryRoot(true);
                for (Map.Entry<String, SVNMergeRangeList> entry : youngerMergeInfo.entrySet()) {
                    String sourcePath = entry.getKey();
                    SVNMergeRangeList rangeList = entry.getValue();


                    SVNMergeRangeList adjustedRangeList = new SVNMergeRangeList(new SVNMergeRange[0]);
                    SVNURL mergeSourceUrl = mergeSourceRootUrl.appendPath(sourcePath.substring(1), false);

                    SVNMergeRange[] ranges = rangeList.getRanges();
                    for (SVNMergeRange range : ranges) {
                        try {
                            SVNURL startUrl = reposLocations(url, revision, reposRootUrl, reposUuid, range.getStartRevision() + 1, svnRepository);
                            if (!startUrl.equals(mergeSourceUrl)) {
                                adjustedRangeList.pushRange(range.getStartRevision(), range.getEndRevision(), range.isInheritable());
                            }
                        } catch (SVNException e) {
                            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.CLIENT_UNRELATED_RESOURCES ||
                                    e.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND ||
                                    e.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NO_SUCH_REVISION) {
                                adjustedRangeList.pushRange(range.getStartRevision(), range.getEndRevision(), range.isInheritable());
                            }
                        }
                    }

                    if (adjustedRangeList.getSize() != 0) {
                        if (filteredYoungerMergeInfo == null) {
                            filteredYoungerMergeInfo = new HashMap<String, SVNMergeRangeList>();
                        }
                        filteredYoungerMergeInfo.put(sourcePath, adjustedRangeList);
                    }
                }
            }

            Map<String, SVNMergeRangeList> filteredMergeInfo = null;
            if (mergeInfo != null) {
                SVNURL oldUrl = svnRepository.getLocation();
                try {
                    svnRepository.setLocation(url, false);
                    Map<String, SVNMergeRangeList> implicitMergeInfo = repositoryAccess.getHistoryAsMergeInfo(svnRepository, SvnTarget.fromURL(url), revision, -1);
                    filteredMergeInfo = SVNMergeInfoUtil.removeMergeInfo(implicitMergeInfo, mergeInfo, true);
                } finally {
                    if (oldUrl != null) {
                        svnRepository.setLocation(oldUrl, false);
                    }
                }
            }
            if (filteredMergeInfo != null && filteredYoungerMergeInfo != null) {
                filteredMergeInfo = SVNMergeInfoUtil.mergeMergeInfos(filteredMergeInfo, filteredYoungerMergeInfo);
            } else if (filteredYoungerMergeInfo != null) {
                filteredMergeInfo = filteredYoungerMergeInfo;
            }
            if (filteredMergeInfo != null && filteredMergeInfo.size() > 0) {
                String filteredMergeInfoString = SVNMergeInfoUtil.formatMergeInfoToString(filteredMergeInfo, null);
                adjustedProps.put(SVNProperty.MERGE_INFO, filteredMergeInfoString);
            }
       }
       return adjustedProps;
    }

    private SVNURL reposLocations(SVNURL url, long pegRevision, SVNURL reposRootUrl, String reposUuid, long opRevision, SVNRepository svnRepository) throws SVNException {
        SVNURL oldUrl = SvnNgMergeDriver.ensureSessionURL(svnRepository, url);
        try {
            Structure<SvnRepositoryAccess.LocationsInfo> locations = repositoryAccess.getLocations(svnRepository, SvnTarget.fromURL(url), SVNRevision.create(pegRevision), SVNRevision.create(opRevision), SVNRevision.HEAD);
            SVNURL startUrl = locations.get(SvnRepositoryAccess.LocationsInfo.startUrl);
            return startUrl;
        } finally {
            svnRepository.setLocation(oldUrl, false);
        }
    }

    private SplitMergeInfo splitMergeInfoOnRevision(Map<String, SVNMergeRangeList> mergeInfo, long revision) {
        SplitMergeInfo splitMergeInfo = new SplitMergeInfo();
        splitMergeInfo.youngerMergeInfo = null;
        splitMergeInfo.mergeInfo = mergeInfo;
        for (Map.Entry<String, SVNMergeRangeList> entry : mergeInfo.entrySet()) {
            String mergeSourcePath = entry.getKey();
            SVNMergeRangeList rangeList = entry.getValue();

            SVNMergeRange[] ranges = rangeList.getRanges();
            for (int i = 0; i < ranges.length; i++) {
                final SVNMergeRange range = ranges[i];
                if (range.getEndRevision() < revision) {
                    continue;
                } else {
                    SVNMergeRangeList youngerRangeList = new SVNMergeRangeList(new SVNMergeRange[0]);
                    for (int j = i; j < ranges.length; j++) {
                        final SVNMergeRange youngerRange = ranges[j].dup();
                        if (i == j && range.getStartRevision() + 1 <= revision) {
                            youngerRange.setStartRevision(revision);
                            range.setEndRevision(revision);
                        }
                        youngerRangeList.pushRange(youngerRange.getStartRevision(), youngerRange.getEndRevision(), youngerRange.isInheritable());
                    }

                    if (splitMergeInfo.youngerMergeInfo == null) {
                        splitMergeInfo.youngerMergeInfo = new HashMap<String, SVNMergeRangeList>();
                    }
                    splitMergeInfo.youngerMergeInfo.put(mergeSourcePath, youngerRangeList);
                    splitMergeInfo.mergeInfo = SVNMergeInfoUtil.removeMergeInfo(splitMergeInfo.youngerMergeInfo, splitMergeInfo.mergeInfo);
                    break;
                }
            }
        }

        return splitMergeInfo;
    }

    private boolean areFilesSame(File olderAbsPath, SVNProperties originalProps, File mineAbsPath) throws SVNException {
        SVNProperties workingProps = context.getActualProps(mineAbsPath);
        boolean same = arePropertiesSame(originalProps, workingProps);
        if (same) {
            return !context.compareAndVerify(mineAbsPath, olderAbsPath, workingProps != null && workingProps.size() > 0, false, false);
        }
        return same;
    }

    private boolean arePropertiesSame(SVNProperties originalProps, SVNProperties workingProps) {
        SVNProperties propDiff = originalProps == null ? workingProps : originalProps.compareTo(workingProps);

        Set<String> propNames = propDiff.nameSet();
        for (String propName : propNames) {
            if (SVNProperty.isRegularProperty(propName) && !SVNProperty.MERGE_INFO.equals(propName)) {
                return false;
            }
        }
        return true;
    }

    private void completeDirectoryAdd(File localAbsPath, SVNProperties newOriginalProps, SVNURL copyFromUrl, long copyFromRevision) throws SVNException {
        Structure<StructureFields.NodeInfo> nodeInfoStructure = context.getDb().readInfo(localAbsPath, StructureFields.NodeInfo.status, StructureFields.NodeInfo.kind,
                StructureFields.NodeInfo.originalReposRelpath, StructureFields.NodeInfo.originalRootUrl, StructureFields.NodeInfo.originalUuid,
                StructureFields.NodeInfo.originalRevision,
                StructureFields.NodeInfo.hadProps, StructureFields.NodeInfo.propsMod);
        ISVNWCDb.SVNWCDbStatus status = nodeInfoStructure.get(StructureFields.NodeInfo.status);
        ISVNWCDb.SVNWCDbKind kind = nodeInfoStructure.get(StructureFields.NodeInfo.kind);
        File originalReposRelPath = nodeInfoStructure.get(StructureFields.NodeInfo.originalReposRelpath);
        SVNURL originalRootUrl = nodeInfoStructure.get(StructureFields.NodeInfo.originalRootUrl);
        String originalUuid = nodeInfoStructure.get(StructureFields.NodeInfo.originalUuid);
        long originalRevision = nodeInfoStructure.lng(StructureFields.NodeInfo.originalRevision);
        boolean hadProps = nodeInfoStructure.is(StructureFields.NodeInfo.hadProps);
        boolean propsMod = nodeInfoStructure.is(StructureFields.NodeInfo.propsMod);

        if (status != ISVNWCDb.SVNWCDbStatus.Added || kind != ISVNWCDb.SVNWCDbKind.Dir || hadProps || propsMod || originalReposRelPath == null) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "''{0}'' is not an unmodified copied directory", localAbsPath);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        if (originalRevision != copyFromRevision || !copyFromUrl.equals(originalRootUrl.appendPath(SVNFileUtil.getFilePath(originalReposRelPath), false))) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_COPYFROM_PATH_NOT_FOUND, "Copyfrom ''{0}'' doesn''t match original location of ''{1}''", copyFromUrl, localAbsPath);
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }
        SVNProperties regularProps = new SVNProperties();
        SVNProperties entryProps = new SVNProperties();
        SvnNgPropertiesManager.categorizeProperties(newOriginalProps, regularProps, entryProps, null);

        newOriginalProps = regularProps;

        SVNUpdateEditor17.AccumulatedChangeInfo accumulatedChangeInfo = SVNUpdateEditor17.accumulateLastChange(null, entryProps);
        String changedAuthor = accumulatedChangeInfo.changedAuthor;
        SVNDate changedDate = accumulatedChangeInfo.changedDate;
        long changedRev = accumulatedChangeInfo.changedRev;

        context.getDb().opCopyDir(localAbsPath, newOriginalProps, changedRev, changedDate, changedAuthor,
                originalReposRelPath, originalRootUrl, originalUuid, originalRevision,
                null, false, SVNDepth.INFINITY, null, null);
    }

    private static class SplitMergeInfo {
        private Map<String, SVNMergeRangeList> youngerMergeInfo;
        private Map<String, SVNMergeRangeList> mergeInfo;
    }

    private static class DirectoryDeleteBaton {
        private DirectoryBaton delRoot;
        private boolean foundEdit;
        private Set<File> comparedAbsPaths;
    }

    private class DirectoryBaton {
        private DirectoryBaton parentBaton;
        private boolean shadowed;
        private boolean edited;
        private SVNConflictReason treeConflictReason;
        private SVNConflictAction treeConflictAction;
        private SVNStatusType skipReason;
        private boolean added;
        private boolean addIsReplace;
        private boolean addExisting;
        private Map<File, SVNNodeKind> pendingDeletes;
        private Map<File, SVNWCConflictDescription17> newTreeConflicts;
        private DirectoryDeleteBaton deleteState;

        public void markDirectoryEdited(File localAbsPath) throws SVNException {
            if (edited) {
                return;
            }
            if (parentBaton != null && !parentBaton.edited) {
                File dirAbsPath = SVNFileUtil.getFileDir(localAbsPath);
                parentBaton.markDirectoryEdited(dirAbsPath);
            }
            edited = true;

            if (!shadowed) {
                return;
            }

            if (parentBaton != null && parentBaton.deleteState != null && treeConflictReason != null) {
                parentBaton.deleteState.foundEdit = true;
            } else if (treeConflictReason == SVNConflictReason.SKIP || treeConflictReason == SVNConflictReason.WC_SKIP) {
                ISVNEventHandler eventHandler = context.getEventHandler();
                if (eventHandler != null) {
                    notifyMergeBegin(localAbsPath, false);

                    SVNEventAction eventAction = treeConflictReason == SVNConflictReason.SKIP ? SVNEventAction.SKIP : SVNEventAction.UPDATE_SKIP_OBSTRUCTION;
                    SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.DIR, null, -1, skipReason, skipReason, SVNStatusType.LOCK_UNKNOWN, eventAction, eventAction, null, null);
                    eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);
                }
                if (mergeDriver.mergeSource.ancestral || mergeDriver.reintegrateMerge) {
                    if (mergeDriver.skippedPaths == null) {
                        mergeDriver.skippedPaths = new HashSet<File>();
                    }
                    mergeDriver.skippedPaths.add(localAbsPath);
                }
            } else if (treeConflictReason != null) {
                recordTreeConflict(localAbsPath, parentBaton, SVNNodeKind.FILE, treeConflictAction, treeConflictReason, null, true);
            }
        }
    }

    private class FileBaton {
        private DirectoryBaton parentBaton;
        private boolean shadowed;
        private boolean edited;
        private SVNConflictReason treeConflictReason;
        private SVNConflictAction treeConflictAction;
        private SVNStatusType skipReason;
        private boolean added;
        private boolean addIsReplace;

        public void markFileEdited(File localAbsPath) throws SVNException {
            if (edited) {
                return;
            }
            if (parentBaton != null && !parentBaton.edited) {
                File dirAbsPath = SVNFileUtil.getFileDir(localAbsPath);
                parentBaton.markDirectoryEdited(dirAbsPath);
            }
            edited = true;

            if (!shadowed) {
                return;
            }

            if (parentBaton != null && parentBaton.deleteState != null && treeConflictReason != null) {
                parentBaton.deleteState.foundEdit = true;
            } else if (treeConflictReason == SVNConflictReason.SKIP || treeConflictReason == SVNConflictReason.WC_SKIP) {
                ISVNEventHandler eventHandler = context.getEventHandler();
                if (eventHandler != null) {
                    notifyMergeBegin(localAbsPath, false);

                    SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.FILE, null, -1, skipReason, skipReason, SVNStatusType.LOCK_UNKNOWN, SVNEventAction.SKIP, SVNEventAction.SKIP, null, null);
                    eventHandler.handleEvent(event, ISVNEventHandler.UNKNOWN);
                }
                if (mergeDriver.mergeSource.ancestral || mergeDriver.reintegrateMerge) {
                    if (mergeDriver.skippedPaths == null) {
                        mergeDriver.skippedPaths = new HashSet<File>();
                    }
                    mergeDriver.skippedPaths.add(localAbsPath);
                }
            } else if (treeConflictReason != null) {
                recordTreeConflict(localAbsPath, parentBaton, SVNNodeKind.FILE, treeConflictAction, treeConflictReason, null, true);
            }
        }
    }

    private static class VerifyTouchedByDelCheck implements ISvnObjectReceiver<SvnStatus> {

        private final DirectoryDeleteBaton deleteBaton;

        private VerifyTouchedByDelCheck(DirectoryDeleteBaton deleteBaton) {
            this.deleteBaton = deleteBaton;
        }

        public void receive(SvnTarget target, SvnStatus status) throws SVNException {
            File localAbsPath = target.getFile();

            if (deleteBaton.comparedAbsPaths.contains(localAbsPath)) {
                return;
            }

            if (status.getNodeStatus().equals(SVNStatusType.STATUS_DELETED) ||
                    status.getNodeStatus().equals(SVNStatusType.STATUS_IGNORED) ||
                    status.getNodeStatus().equals(SVNStatusType.STATUS_NONE)) {
                return;
            } else {
                deleteBaton.foundEdit = true;
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.CEASE_INVOCATION);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
        }
    }
}
