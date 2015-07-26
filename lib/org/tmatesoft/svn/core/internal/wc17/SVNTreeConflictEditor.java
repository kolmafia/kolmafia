package org.tmatesoft.svn.core.internal.wc17;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.*;
import org.tmatesoft.svn.core.internal.wc17.db.*;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbStatements;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffEditor;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class SVNTreeConflictEditor implements ISVNEditor2 {

    private final SVNOperation operation;
    private final SVNConflictVersion oldVersion;
    private final SVNConflictVersion newVersion;
    private final SVNWCDb db;
    private final SVNWCDbRoot wcRoot;
    private final File moveRootDstRelPath;
    private ISVNEventHandler eventHandler;

    private File conflictRootRelPath;

    public SVNTreeConflictEditor(SVNWCDb db, SVNOperation operation, SVNConflictVersion oldVersion, SVNConflictVersion newVersion, SVNWCDbRoot wcRoot, File moveRootDstRelPath) {
        this.operation = operation;
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
        this.db = db;
        this.wcRoot = wcRoot;
        this.moveRootDstRelPath = moveRootDstRelPath;
    }

    public void addDir(String path, List<String> children, SVNProperties props, long replacesRev) throws SVNException {
        int opDepth = SVNWCUtils.relpathDepth(moveRootDstRelPath);
        File relPath = SVNFileUtil.createFilePath(path);
        db.extendParentDelete(wcRoot.getSDb(), wcRoot.getWcId(), relPath, SVNNodeKind.DIR, opDepth);

        SVNNodeKind oldKind;
        ISVNWCDb.SVNWCDbKind moveDstKind;
        File moveDstReposRelPath;
        try {
            Structure<StructureFields.NodeInfo> depthInfo = SvnWcDbShared.getDepthInfo(wcRoot, relPath, SVNWCUtils.relpathDepth(moveRootDstRelPath));
            moveDstKind = depthInfo.get(StructureFields.NodeInfo.kind);
            moveDstReposRelPath = depthInfo.get(StructureFields.NodeInfo.reposRelPath);

            oldKind = moveDstKind == ISVNWCDb.SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE;

        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                oldKind = SVNNodeKind.NONE;
                moveDstReposRelPath = null;
            } else {
                throw e;
            }
        }

        boolean isConflicted = checkTreeConflict(relPath, oldKind, SVNNodeKind.DIR, moveDstReposRelPath, SVNConflictAction.ADD);
        if (isConflicted) {
            return;
        }

        SVNEventAction action = SVNEventAction.ADD;
        File absPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), relPath);
        oldKind = SVNFileType.getNodeKind(SVNFileType.getType(absPath));

        if (oldKind == SVNNodeKind.FILE) {
            markTreeConflict(relPath, oldVersion, newVersion, moveRootDstRelPath, operation, oldKind, SVNNodeKind.DIR, moveDstReposRelPath,
                    SVNConflictReason.UNVERSIONED, SVNConflictAction.ADD, null);
            conflictRootRelPath = relPath;
            action = SVNEventAction.TREE_CONFLICT;
            isConflicted = true;
        } else if (oldKind == SVNNodeKind.NONE) {
            SVNSkel workItem = SVNWCContext.wqBuildDirInstall(db, absPath);
            db.wqAdd(wcRoot.getAbsPath(), workItem);
        } else if (oldKind == SVNNodeKind.DIR) {
        }

        if (!isConflicted) {
            SVNWCDb.updateMoveListAdd(wcRoot, relPath, action, SVNNodeKind.DIR, SVNStatusType.INAPPLICABLE, SVNStatusType.INAPPLICABLE);
        }
    }

    public void addFile(String path, SvnChecksum checksum, InputStream contents, SVNProperties props, long replacesRev) throws SVNException {
        File relPath = SVNFileUtil.createFilePath(path);
        int opDepth = SVNWCUtils.relpathDepth(moveRootDstRelPath);
        db.extendParentDelete(wcRoot.getSDb(), wcRoot.getWcId(), relPath, SVNNodeKind.FILE, opDepth);

        SVNNodeKind oldKind;
        File moveDstReposRelPath = null;

        try {
            Structure<StructureFields.NodeInfo> depthInfo = SvnWcDbShared.getDepthInfo(wcRoot, relPath, SVNWCUtils.relpathDepth(moveRootDstRelPath));
            ISVNWCDb.SVNWCDbKind moveDstKind = depthInfo.get(StructureFields.NodeInfo.kind);
            moveDstReposRelPath = depthInfo.get(StructureFields.NodeInfo.reposRelPath);

            oldKind = moveDstKind == ISVNWCDb.SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE;
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                oldKind = SVNNodeKind.NONE;
                moveDstReposRelPath = null;
            } else {
                throw e;
            }
        }

        boolean isConflicted = checkTreeConflict(relPath, oldKind, SVNNodeKind.FILE, moveDstReposRelPath, SVNConflictAction.ADD);
        if (isConflicted) {
            return;
        }

        File absPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), relPath);
        oldKind = SVNFileType.getNodeKind(SVNFileType.getType(absPath));

        if (oldKind != SVNNodeKind.NONE) {
            markTreeConflict(relPath, oldVersion, newVersion, moveRootDstRelPath, operation, oldKind, SVNNodeKind.FILE, moveDstReposRelPath,
                    SVNConflictReason.UNVERSIONED, SVNConflictAction.ADD, null);
            conflictRootRelPath = relPath;
        }

        SVNSkel workItem = SVNWCContext.wqBuildFileInstall(db, SVNFileUtil.createFilePath(wcRoot.getAbsPath(), relPath), null, false, true);
        db.wqAdd(wcRoot.getAbsPath(), workItem);

        SVNWCDb.updateMoveListAdd(wcRoot, relPath, SVNEventAction.UPDATE_ADD, SVNNodeKind.FILE, SVNStatusType.INAPPLICABLE, SVNStatusType.INAPPLICABLE);
    }

    public void addSymlink(String path, String target, SVNProperties props, long replacesRev) throws SVNException {
        SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE);
        SVNErrorManager.error(errorMessage, SVNLogType.WC);
    }

    public void addAbsent(String path, SVNNodeKind kind, long replacesRev) throws SVNException {
        SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE);
        SVNErrorManager.error(errorMessage, SVNLogType.WC);
    }

    public void alterDir(String path, long revision, List<String> children, SVNProperties props) throws SVNException {
        assert revision == oldVersion.getPegRevision();

        File dstRelPath = SVNFileUtil.createFilePath(path);

        WorkingNodeVersion oldNodeVersion = new WorkingNodeVersion();
        WorkingNodeVersion newNodeVersion = new WorkingNodeVersion();

        Structure<StructureFields.NodeInfo> depthInfo = SvnWcDbShared.getDepthInfo(wcRoot, dstRelPath, SVNWCUtils.relpathDepth(moveRootDstRelPath));
        ISVNWCDb.SVNWCDbStatus status = depthInfo.get(StructureFields.NodeInfo.status);
        ISVNWCDb.SVNWCDbKind moveDstKind = depthInfo.get(StructureFields.NodeInfo.kind);
        File moveDstReposRelPath = depthInfo.get(StructureFields.NodeInfo.reposRelPath);
        long moveDstRevision = depthInfo.lng(StructureFields.NodeInfo.revision);
        oldNodeVersion.checksum = depthInfo.get(StructureFields.NodeInfo.checksum);
        oldNodeVersion.props = depthInfo.get(StructureFields.NodeInfo.propsMod);

        if ((status == ISVNWCDb.SVNWCDbStatus.Deleted) && moveDstReposRelPath != null) {
            status = ISVNWCDb.SVNWCDbStatus.NotPresent;
        }
        assert(moveDstRevision == revision || status == ISVNWCDb.SVNWCDbStatus.NotPresent);
        assert(moveDstKind == ISVNWCDb.SVNWCDbKind.Dir);

        boolean isConflicted = checkTreeConflict(dstRelPath, moveDstKind == ISVNWCDb.SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE, SVNNodeKind.DIR, moveDstReposRelPath, SVNConflictAction.EDIT);

        if (isConflicted) {
            return;
        }

        oldNodeVersion.locationAndKind = oldVersion;
        newNodeVersion.locationAndKind = newVersion;

        newNodeVersion.checksum = null;
        newNodeVersion.props = props != null ? props : oldNodeVersion.props;

        if (props != null) {
            File dstAbsPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), dstRelPath);

            UpdateWorkingProps updateWorkingProps = updateWorkingProps(null, dstAbsPath, oldNodeVersion, newNodeVersion);
            SVNSkel conflictSkel = updateWorkingProps.conflictSkel;
            SVNProperties propChanges = updateWorkingProps.propChanges;
            SVNProperties actualProps = updateWorkingProps.actualProps;
            SVNStatusType propState = updateWorkingProps.propState;

            if (conflictSkel != null) {
                SVNSkel workItems = createConflictMarkers(dstAbsPath, moveDstReposRelPath, conflictSkel, operation,
                        oldNodeVersion, newNodeVersion, SVNNodeKind.DIR);
                db.markConflictInternal(wcRoot, dstRelPath, conflictSkel);
                db.wqAdd(wcRoot.getAbsPath(), workItems);
            }
            SVNWCDb.updateMoveListAdd(wcRoot, dstRelPath, SVNEventAction.UPDATE_UPDATE, SVNNodeKind.DIR, SVNStatusType.INAPPLICABLE, propState);
        }
    }

    public void alterFile(String path, long expectedMoveDstRevision, SVNProperties newProps, SvnChecksum newChecksum, InputStream newContents) throws SVNException {
        WorkingNodeVersion oldNodeVersion = new WorkingNodeVersion();
        WorkingNodeVersion newNodeVersion = new WorkingNodeVersion();

        File dstRelPath = SVNFileUtil.createFilePath(path);
        Structure<StructureFields.NodeInfo> depthInfo = SvnWcDbShared.getDepthInfo(wcRoot, dstRelPath, SVNWCUtils.relpathDepth(moveRootDstRelPath),
                StructureFields.NodeInfo.status, StructureFields.NodeInfo.kind, StructureFields.NodeInfo.revision,
                StructureFields.NodeInfo.reposRelPath, StructureFields.NodeInfo.checksum, StructureFields.NodeInfo.propsMod);
        ISVNWCDb.SVNWCDbStatus status = depthInfo.get(StructureFields.NodeInfo.status);
        ISVNWCDb.SVNWCDbKind moveDstKind = depthInfo.get(StructureFields.NodeInfo.kind);
        long moveDstRevision = depthInfo.lng(StructureFields.NodeInfo.revision);
        File moveDstReposRelPath = depthInfo.get(StructureFields.NodeInfo.reposRelPath);
        oldNodeVersion.checksum = depthInfo.get(StructureFields.NodeInfo.checksum);
        oldNodeVersion.props = depthInfo.get(StructureFields.NodeInfo.propsMod);

        if (status == ISVNWCDb.SVNWCDbStatus.Deleted && moveDstReposRelPath != null) {
            status = ISVNWCDb.SVNWCDbStatus.NotPresent;
        }

        assert moveDstRevision == expectedMoveDstRevision || status == ISVNWCDb.SVNWCDbStatus.NotPresent;
        assert moveDstKind == ISVNWCDb.SVNWCDbKind.File;

        boolean isConflicted = checkTreeConflict(dstRelPath,
                moveDstKind == ISVNWCDb.SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE,
                SVNNodeKind.FILE, moveDstReposRelPath, SVNConflictAction.EDIT);

        if (isConflicted) {
            return;
        }

        oldNodeVersion.locationAndKind = oldVersion;
        newNodeVersion.locationAndKind = newVersion;

        newNodeVersion.checksum = newChecksum != null ? newChecksum : oldNodeVersion.checksum;
        newNodeVersion.props = newProps != null ? newProps : oldNodeVersion.props;

        if (!SvnChecksum.match(newChecksum, oldNodeVersion.checksum) || newProps != null) {
            updateWorkingFile(dstRelPath, moveDstReposRelPath, operation, oldNodeVersion, newNodeVersion);
        }
    }

    public void alterSymlink(String path, long revision, SVNProperties props, String target) throws SVNException {
        SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE);
        SVNErrorManager.error(errorMessage, SVNLogType.WC);
    }

    public void delete(String path, long revision) throws SVNException {
        File relPath = SVNFileUtil.createFilePath(path);
        int opDepth = SVNWCUtils.relpathDepth(moveRootDstRelPath);
        boolean mustDeleteWorkingNode = false;

        File localAbsPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), relPath);
        File parentRelPath = SVNFileUtil.getFileDir(relPath);

        Structure<StructureFields.NodeInfo> depthInfo = SvnWcDbShared.getDepthInfo(wcRoot, relPath, SVNWCUtils.relpathDepth(moveRootDstRelPath),
                StructureFields.NodeInfo.kind, StructureFields.NodeInfo.reposRelPath);
        ISVNWCDb.SVNWCDbKind moveDstKind = depthInfo.get(StructureFields.NodeInfo.kind);
        File moveDstReposRelPath = depthInfo.get(StructureFields.NodeInfo.reposRelPath);

        boolean isConflicted = checkTreeConflict(relPath, moveDstKind == ISVNWCDb.SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE,
                SVNNodeKind.UNKNOWN, moveDstReposRelPath, SVNConflictAction.DELETE);
        if (!isConflicted) {
            SVNWCContext context = new SVNWCContext(db, eventHandler);
            SVNWCContext.TreeLocalModsInfo treeLocalModsInfo = context.hasLocalMods(localAbsPath, localAbsPath);
            boolean isModified = treeLocalModsInfo.modificationsFound;
            boolean isAllDeletes = !treeLocalModsInfo.nonDeleteModificationsFound;

            if (isModified) {
                SVNConflictReason reason;

                if (!isAllDeletes) {
                    SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.UPDATE_OP_DEPTH_RECURSIVE);
                    try {
                        stmt.bindf("isii", wcRoot.getWcId(), relPath, opDepth, SVNWCUtils.relpathDepth(relPath));
                        stmt.done();
                    } finally {
                        stmt.reset();
                    }

                    reason = SVNConflictReason.EDITED;
                } else {
                    SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.DELETE_WORKING_OP_DEPTH_ABOVE);
                    try {
                        stmt.bindf("isi", wcRoot.getWcId(), relPath, opDepth);
                        stmt.done();
                    } finally {
                        stmt.reset();
                    }

                    reason = SVNConflictReason.DELETED;
                    mustDeleteWorkingNode = true;
                }

                isConflicted = true;
                markTreeConflict(relPath, oldVersion, newVersion, moveRootDstRelPath, operation, moveDstKind == ISVNWCDb.SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE, SVNNodeKind.NONE,
                        moveDstReposRelPath, reason, SVNConflictAction.DELETE, null);

                conflictRootRelPath = relPath;
            }
        }


        ISVNWCDb.SVNWCDbKind delKind;

        SVNSqlJetStatement stmt;
        if (!isConflicted || mustDeleteWorkingNode) {
            stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_CHILDREN_OP_DEPTH);
            try {
                stmt.bindf("isi", wcRoot.getWcId(), relPath, opDepth);
                boolean haveRow = stmt.next();
                while (haveRow) {
                    delKind = SvnWcDbStatementUtil.getColumnKind(stmt, SVNWCDbSchema.NODES__Fields.kind);
                    File delAbsPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), stmt.getColumnString(SVNWCDbSchema.NODES__Fields.local_relpath));

                    SVNSkel workItem;
                    if (delKind == ISVNWCDb.SVNWCDbKind.Dir) {
                        workItem = SVNWCContext.wqBuildDirRemove(db, wcRoot.getAbsPath(), delAbsPath, false);
                    } else {
                        workItem = SVNWCContext.wqBuildFileRemove(db, wcRoot.getAbsPath(), delAbsPath);
                    }

                    db.wqAdd(wcRoot.getAbsPath(), workItem);

                    haveRow = stmt.next();
                }
            } finally {
                stmt.reset();
            }
            depthInfo = SvnWcDbShared.getDepthInfo(wcRoot, relPath, opDepth, StructureFields.NodeInfo.kind);
            delKind = depthInfo.get(StructureFields.NodeInfo.kind);

            SVNSkel workItem;
            if (delKind == ISVNWCDb.SVNWCDbKind.Dir) {
                workItem = SVNWCContext.wqBuildDirRemove(db, wcRoot.getAbsPath(), localAbsPath, false);
            } else {
                workItem = SVNWCContext.wqBuildFileRemove(db, wcRoot.getAbsPath(), localAbsPath);
            }
            db.wqAdd(wcRoot.getAbsPath(), workItem);

            if (!isConflicted) {
                SVNWCDb.updateMoveListAdd(wcRoot, relPath, SVNEventAction.UPDATE_DELETE, delKind == ISVNWCDb.SVNWCDbKind.Dir ? SVNNodeKind.DIR : SVNNodeKind.FILE,
                        SVNStatusType.INAPPLICABLE, SVNStatusType.INAPPLICABLE);
            }
        }

        int opDepthBelow = 0;
        boolean haveRow;
        stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_HIGHEST_WORKING_NODE);
        try {
            stmt.bindf("isi", wcRoot.getWcId(), parentRelPath, opDepth);
            haveRow = stmt.next();

            if (haveRow) {
                opDepthBelow = (int) stmt.getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);
            }

        } finally {
            stmt.reset();
        }

        if (haveRow) {
            stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.DELETE_NO_LOWER_LAYER);
            try {
                stmt.bindf("isii", wcRoot.getWcId(), parentRelPath, opDepth, opDepthBelow);
                stmt.done();
            } finally {
                stmt.reset();
            }

            stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.REPLACE_WITH_BASE_DELETED);
            try {
                stmt.bindf("isi", wcRoot.getWcId(), relPath, opDepth);
            } finally {
                stmt.reset();
            }
        } else {
            stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.DELETE_WORKING_OP_DEPTH);
            try {
                stmt.bindf("isi", wcRoot.getWcId(), relPath, opDepth);
                stmt.done();
            } finally {
                stmt.reset();
            }
        }

        db.retractParentDelete(wcRoot.getSDb(), wcRoot.getWcId(), relPath, opDepth);
    }

    public void copy(String srcPath, long srcRevision, String dstPath, long replacesRev) throws SVNException {
        SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE);
        SVNErrorManager.error(errorMessage, SVNLogType.WC);
    }

    public void move(String srcPath, long srcRevision, String dstPath, long replacesRev) throws SVNException {
        SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE);
        SVNErrorManager.error(errorMessage, SVNLogType.WC);
    }

    public void rotate(List<String> relPaths, List<String> revisions) throws SVNException {
        SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE);
        SVNErrorManager.error(errorMessage, SVNLogType.WC);
    }

    public void complete() {
    }

    public void abort() {
    }

    private static class WorkingNodeVersion {
        SVNConflictVersion locationAndKind;
        SVNProperties props;
        SvnChecksum checksum;
    }

    private boolean checkTreeConflict(File localRelPath, SVNNodeKind oldKind, SVNNodeKind newKind, File oldReposRelPath, SVNConflictAction action) throws SVNException {
        int dstOpDepth = SVNWCUtils.relpathDepth(moveRootDstRelPath);
        int opDepth = 0;

        if (this.conflictRootRelPath != null) {
            if (SVNPathUtil.isAncestor(SVNFileUtil.getFilePath(this.conflictRootRelPath), SVNFileUtil.getFilePath(localRelPath))) {
                return true;
            }
            this.conflictRootRelPath = null;
        }

        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_LOWEST_WORKING_NODE);
        boolean haveRow;
        try {
            stmt.bindf("isi", wcRoot.getWcId(), localRelPath, dstOpDepth);
            haveRow = stmt.next();
            if (haveRow) {
                opDepth = (int) stmt.getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);
            }
        } finally {
            stmt.reset();
        }

        if (!haveRow) {
            return false;
        }

        boolean isConflicted = true;
        File conflictRootRelPath = localRelPath;

        while (SVNWCUtils.relpathDepth(conflictRootRelPath) > opDepth) {
            conflictRootRelPath = SVNFileUtil.getFileDir(conflictRootRelPath);
            oldKind = newKind = SVNNodeKind.DIR;
            if (oldReposRelPath != null) {
                oldReposRelPath = SVNFileUtil.getFileDir(oldReposRelPath);
            }
            action = SVNConflictAction.EDIT;
        }

        SVNWCDb.BaseMovedTo movedTo = db.opDepthMovedTo(dstOpDepth, wcRoot, conflictRootRelPath);
        File moveDstRelPath = movedTo.moveDstRelPath;
        File moveSrcOpRootRelPath = movedTo.moveSrcOpRootRelPath;

        markTreeConflict(conflictRootRelPath, oldVersion, newVersion, moveRootDstRelPath, operation,
                oldKind, newKind, oldReposRelPath, moveDstRelPath != null ? SVNConflictReason.MOVED_AWAY : SVNConflictReason.DELETED,
                action, moveSrcOpRootRelPath);

        this.conflictRootRelPath = conflictRootRelPath;
        return isConflicted;
    }

    private UpdateWorkingProps updateWorkingProps(SVNSkel conflictSkel, File localAbsPath,
                                                  WorkingNodeVersion oldNodeVersion, WorkingNodeVersion newNodeVersion) throws SVNException {
        UpdateWorkingProps updateWorkingProps = new UpdateWorkingProps();

        if (oldNodeVersion.props == null) {
            oldNodeVersion.props = new SVNProperties();
        }
        if (newNodeVersion.props == null) {
            newNodeVersion.props = new SVNProperties();
        }

        updateWorkingProps.actualProps = db.readProperties(localAbsPath);
        updateWorkingProps.propChanges = SvnDiffEditor.computePropDiff(oldNodeVersion.props, newNodeVersion.props);

        SVNWCContext context = new SVNWCContext(db, eventHandler);

        SVNWCContext.MergePropertiesInfo mergePropertiesInfo = new SVNWCContext.MergePropertiesInfo();
        mergePropertiesInfo.conflictSkel = conflictSkel;
        mergePropertiesInfo = context.mergeProperties3(mergePropertiesInfo, localAbsPath, oldNodeVersion.props, oldNodeVersion.props, updateWorkingProps.actualProps, updateWorkingProps.propChanges);
        updateWorkingProps.conflictSkel = mergePropertiesInfo.conflictSkel;
        SVNProperties newActualProperties = mergePropertiesInfo.newActualProperties;
        updateWorkingProps.propState = mergePropertiesInfo.mergeOutcome;

        SVNProperties newPropChanges = SvnDiffEditor.computePropDiff(newNodeVersion.props, newActualProperties);
        if (newPropChanges.size() > 0) {
            newActualProperties = null;
        }

        db.opSetProps(localAbsPath, newActualProperties, null, SVNWCContext.hasMagicProperty(updateWorkingProps.propChanges), null);
        return updateWorkingProps;
    }

    private static class UpdateWorkingProps {
        SVNStatusType propState;
        SVNSkel conflictSkel;
        SVNProperties propChanges;
        SVNProperties actualProps;
    }

    private SVNSkel createConflictMarkers(File localAbsPath, File reposRelPath,
                                       SVNSkel conflictSkel, SVNOperation operation,
                                       WorkingNodeVersion oldNodeVersion, WorkingNodeVersion newNodeVersion,
                                       SVNNodeKind kind) throws SVNException {
        File part = SVNFileUtil.skipAncestor(SVNFileUtil.createFilePath(oldVersion.getPath()), reposRelPath);

        SVNConflictVersion originalVersion = new SVNConflictVersion(oldNodeVersion.locationAndKind.getRepositoryRoot(),
                SVNFileUtil.getFilePath(reposRelPath), oldNodeVersion.locationAndKind.getPegRevision(), kind);
        SVNConflictVersion conflictedVersion = new SVNConflictVersion(newNodeVersion.locationAndKind.getRepositoryRoot(),
                SVNPathUtil.append(oldNodeVersion.locationAndKind.getPath(), SVNFileUtil.getFilePath(part)), newNodeVersion.locationAndKind.getPegRevision(), kind);

        if (operation == SVNOperation.UPDATE) {
            SvnWcDbConflicts.conflictSkelOpUpdate(conflictSkel, originalVersion, conflictedVersion);
        } else {
            SvnWcDbConflicts.conflictSkelOpSwitch(conflictSkel, originalVersion, conflictedVersion);
        }

        SVNWCContext context = new SVNWCContext(db, eventHandler);
        return context.conflictCreateMarker(conflictSkel, localAbsPath);
    }

    private void updateWorkingFile(File localRelPath, File reposRelPath, SVNOperation operation,
                                   WorkingNodeVersion oldNodeVersion, WorkingNodeVersion newNodeVersion) throws SVNException {
        SVNSkel workItems = null;
        File localAbsPath = SVNFileUtil.createFilePath(wcRoot.getAbsPath(), localRelPath);
        SVNStatusType contentState;

        UpdateWorkingProps updateWorkingProps = updateWorkingProps(null, localAbsPath, oldNodeVersion, newNodeVersion);
        SVNSkel conflictSkel = updateWorkingProps.conflictSkel;
        SVNStatusType propState = updateWorkingProps.propState;
        SVNProperties propChanges = updateWorkingProps.propChanges;
        SVNProperties actualProps = updateWorkingProps.actualProps;

        if (!SvnChecksum.match(newNodeVersion.checksum, oldNodeVersion.checksum)) {
            SVNWCContext context = new SVNWCContext(db, eventHandler);
            boolean isLocallyModified = context.isTextModified(localAbsPath, false);

            if (!isLocallyModified) {
                SVNSkel workItem = context.wqBuildFileInstall(localAbsPath, null, false, true);
                workItems = SVNWCContext.wqMerge(workItems, workItem);
                contentState = SVNStatusType.CHANGED;
            } else {
                File oldPristineAbsPath = SvnWcDbPristines.getPristinePath(wcRoot, oldNodeVersion.checksum);
                File newPristineAbsPath = SvnWcDbPristines.getPristinePath(wcRoot, newNodeVersion.checksum);
                SVNWCContext.MergeInfo mergeInfo = context.merge(workItems, conflictSkel, oldPristineAbsPath, newPristineAbsPath, localAbsPath, localAbsPath,
                        null, null, null, actualProps, false, null, propChanges);
                conflictSkel = mergeInfo.conflictSkel;
                SVNSkel workItem = mergeInfo.workItems;
                SVNStatusType mergeOutcome = mergeInfo.mergeOutcome;

                workItems = SVNWCContext.wqMerge(workItems, workItem);
                if (mergeOutcome == SVNStatusType.CONFLICTED) {
                    contentState = SVNStatusType.CONFLICTED;
                } else {
                    contentState = SVNStatusType.MERGED;
                }


            }
        } else {
            contentState = SVNStatusType.UNCHANGED;
        }

        if (conflictSkel != null) {
            SVNSkel workItem = createConflictMarkers(localAbsPath, reposRelPath, conflictSkel, operation, oldNodeVersion, newNodeVersion, SVNNodeKind.FILE);
            db.markConflictInternal(wcRoot, localRelPath, conflictSkel);
            workItems = SVNWCContext.wqMerge(workItems, workItem);
        }

        db.wqAdd(wcRoot.getAbsPath(), workItems);

        SVNWCDb.updateMoveListAdd(wcRoot, localRelPath, SVNEventAction.UPDATE_UPDATE, SVNNodeKind.FILE, contentState, propState);
    }

    private void markTreeConflict(File localRelPath,
                                  SVNConflictVersion oldVersion, SVNConflictVersion newVersion,
                                  File moveRootDstRelPath, SVNOperation operation,
                                  SVNNodeKind oldKind, SVNNodeKind newKind,
                                  File oldReposRelPath,
                                  SVNConflictReason reason, SVNConflictAction action,
                                  File moveSrcOpRootRelPath) throws SVNException {
        File moveSrcOpRootAbsPath = moveSrcOpRootRelPath != null ? SVNFileUtil.createFilePath(wcRoot.getAbsPath(), moveSrcOpRootRelPath) : null;
        File oldReposRelPathPart = oldReposRelPath != null ? SVNFileUtil.skipAncestor(SVNFileUtil.createFilePath(oldVersion.getPath()), oldReposRelPath) : null;
        File newReposRelPath = oldReposRelPathPart != null ? SVNFileUtil.createFilePath(SVNFileUtil.createFilePath(newVersion.getPath()), oldReposRelPathPart) : null;

        if (newReposRelPath == null) {
            newReposRelPath = SVNFileUtil.createFilePath(newVersion.getPath(), SVNFileUtil.getFilePath(SVNFileUtil.skipAncestor(moveRootDstRelPath, localRelPath)));
        }

        SVNSkel conflictSkel;
        try {
            conflictSkel = db.readConflictInternal(wcRoot, localRelPath);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                conflictSkel = null;
            } else {
                throw e;
            }
        }

        if (conflictSkel != null) {
            Structure<SvnWcDbConflicts.ConflictInfo> conflictInfoStructure = SvnWcDbConflicts.readConflictInfo(conflictSkel);
            SVNOperation conflictOperation = conflictInfoStructure.get(SvnWcDbConflicts.ConflictInfo.conflictOperation);
            boolean treeConflicted = conflictInfoStructure.is(SvnWcDbConflicts.ConflictInfo.treeConflicted);

            if (conflictOperation != SVNOperation.UPDATE && conflictOperation != SVNOperation.SWITCH) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE,
                        "'{0}' already in conflict", localRelPath);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }

            if (treeConflicted) {
                Structure<SvnWcDbConflicts.TreeConflictInfo> treeConflictInfoStructure = SvnWcDbConflicts.readTreeConflict(db, wcRoot.getAbsPath(), conflictSkel);
                SVNConflictReason existingReason = treeConflictInfoStructure.get(SvnWcDbConflicts.TreeConflictInfo.localChange);
                SVNConflictAction existingAction = treeConflictInfoStructure.get(SvnWcDbConflicts.TreeConflictInfo.incomingChange);
                File existingAbsPath = treeConflictInfoStructure.get(SvnWcDbConflicts.TreeConflictInfo.moveSrcOpRootAbsPath);

                if (reason != existingReason || action != existingAction || (reason == SVNConflictReason.MOVED_AWAY && !moveSrcOpRootRelPath.equals(SVNFileUtil.skipAncestor(wcRoot.getAbsPath(), existingAbsPath)))) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_CONFLICT_RESOLVER_FAILURE, "'{0}' already in conflict", localRelPath);
                    SVNErrorManager.error(errorMessage, SVNLogType.WC);
                }

                return;
            }
        } else {
            conflictSkel = SvnWcDbConflicts.createConflictSkel();
        }

        SvnWcDbConflicts.addTreeConflict(conflictSkel, db, SVNFileUtil.createFilePath(wcRoot.getAbsPath(), localRelPath), reason, action, moveSrcOpRootAbsPath);

        SVNConflictVersion conflictOldVersion;
        SVNConflictVersion conflictNewVersion;

        if (reason != SVNConflictReason.UNVERSIONED && oldReposRelPath != null) {
            conflictOldVersion = new SVNConflictVersion(oldVersion.getRepositoryRoot(), SVNFileUtil.getFilePath(oldReposRelPath), oldVersion.getPegRevision(), oldKind);
        } else {
            conflictOldVersion = null;
        }

        conflictNewVersion = new SVNConflictVersion(newVersion.getRepositoryRoot(), SVNFileUtil.getFilePath(newReposRelPath), newVersion.getPegRevision(), newKind);

        if (operation == SVNOperation.UPDATE) {
            SvnWcDbConflicts.conflictSkelOpUpdate(conflictSkel, conflictOldVersion, conflictNewVersion);
        } else {
            assert operation == SVNOperation.SWITCH;
            SvnWcDbConflicts.conflictSkelOpSwitch(conflictSkel, conflictOldVersion, conflictNewVersion);
        }

        db.markConflictInternal(wcRoot, localRelPath, conflictSkel);

        SVNWCDb.updateMoveListAdd(wcRoot, localRelPath, SVNEventAction.TREE_CONFLICT, newKind, SVNStatusType.INAPPLICABLE, SVNStatusType.INAPPLICABLE);
    }
}
