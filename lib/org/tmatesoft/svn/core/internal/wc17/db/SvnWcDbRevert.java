package org.tmatesoft.svn.core.internal.wc17.db;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.db.*;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb.DirParsedInfo;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbCreateSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.REVERT_LIST__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbStatements;
import org.tmatesoft.svn.core.internal.wc17.db.statement17.SvnRevertActualNodesTrigger17;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.*;

public class SvnWcDbRevert extends SvnWcDbShared {
    
    public static void revert(SVNWCDbRoot root, File localRelPath) throws SVNException {
        File movedTo;
        boolean movedHere;

        SVNSqlJetDb sdb = root.getSDb();

        SvnRevertNodesTrigger nodesTableTrigger = new SvnRevertNodesTrigger(sdb);
        ISVNSqlJetTrigger actualNodesTableTrigger = root.getFormat() == ISVNWCDb.WC_FORMAT_17 ?
                new SvnRevertActualNodesTrigger17(sdb) :
                new SvnRevertActualNodesTrigger(sdb);

        SVNSqlJetStatement stmt = sdb.getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
        long opDepth;
        try {
            stmt.bindf("is", root.getWcId(), localRelPath);
            if (!stmt.next()) {
                reset(stmt);

                stmt = sdb.getStatement(SVNWCDbStatements.DELETE_ACTUAL_NODE);
                long affectedRows;
                try {
                    ((SVNSqlJetTableStatement) stmt).addTrigger(actualNodesTableTrigger);

                    stmt.bindf("is", root.getWcId(), localRelPath);
                    affectedRows = stmt.done();
                } finally {
                    stmt.reset();
                }
                if (affectedRows > 0) {
                    if (root.getFormat() == ISVNWCDb.WC_FORMAT_17) {
                        stmt = sdb.getStatement(SVNWCDbStatements.SELECT_ACTUAL_CHILDREN_INFO_17);
                    } else {
                        stmt = sdb.getStatement(SVNWCDbStatements.SELECT_ACTUAL_CHILDREN_INFO);
                    }
                    try {
                        stmt.bindf("is", root.getWcId(), localRelPath);
                        if (stmt.next()) {
                            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_OPERATION_DEPTH, "Can''t revert ''{0}'' without reverting children", root.getAbsPath(localRelPath));
                            SVNErrorManager.error(err, SVNLogType.WC);
                        }
                    } finally {
                        reset(stmt);
                    }
                    return;
                }
                nodeNotFound(root, localRelPath);
            }
            opDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
            movedTo = getColumnPath(stmt, NODES__Fields.moved_to);
            movedHere = getColumnBoolean(stmt, NODES__Fields.moved_here);
        } finally {
            reset(stmt);
        }
        if (movedTo != null) {
            SVNWCDb.ResolveBreakMovedAway resolveBreakMovedAway = new SVNWCDb.ResolveBreakMovedAway();
            resolveBreakMovedAway.wcRoot = root;
            resolveBreakMovedAway.localRelPath = localRelPath;
            try {
                resolveBreakMovedAway.transaction(sdb);
            } catch (SqlJetException e) {
                SVNSqlJetDb.createSqlJetError(e);
            }
        } else {
            SVNSkel conflict = root.getDb().readConflictInternal(root, localRelPath);
            if (conflict != null) {
                Structure<SvnWcDbConflicts.ConflictInfo> conflictInfoStructure = SvnWcDbConflicts.readConflictInfo(conflict);
                boolean treeConflicted = conflictInfoStructure.is(SvnWcDbConflicts.ConflictInfo.treeConflicted);
                SVNOperation operation = conflictInfoStructure.get(SvnWcDbConflicts.ConflictInfo.conflictOperation);
                if (treeConflicted && (operation == SVNOperation.UPDATE || operation == SVNOperation.SWITCH)) {
                    Structure<SvnWcDbConflicts.TreeConflictInfo> treeConflictInfoStructure = SvnWcDbConflicts.readTreeConflict(root.getDb(), root.getAbsPath(), conflict);
                    SVNConflictReason reason = treeConflictInfoStructure.get(SvnWcDbConflicts.TreeConflictInfo.localChange);
                    SVNConflictAction action = treeConflictInfoStructure.get(SvnWcDbConflicts.TreeConflictInfo.incomingChange);
                    if (reason == SVNConflictReason.DELETED) {
                        root.getDb().resolveDeleteRaiseMovedAway(SVNFileUtil.createFilePath(root.getAbsPath(), localRelPath), null);
                    }
                }
            }
        }

        if (opDepth > 0 && opDepth == SVNWCUtils.relpathDepth(localRelPath)) {

            boolean haveRow;
            stmt = sdb.getStatement(SVNWCDbStatements.SELECT_GE_OP_DEPTH_CHILDREN);
            try {
                stmt.bindf("isi", root.getWcId(), localRelPath, opDepth);
                haveRow = stmt.next();
            } finally {
                reset(stmt);
            }
            if (haveRow) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_OPERATION_DEPTH, "Can''t revert ''{0}'' without reverting children", root.getAbsPath(localRelPath));
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            if (root.getFormat() == ISVNWCDb.WC_FORMAT_17) {
                stmt = sdb.getStatement(SVNWCDbStatements.SELECT_ACTUAL_CHILDREN_INFO_17);
            } else {
                stmt = sdb.getStatement(SVNWCDbStatements.SELECT_ACTUAL_CHILDREN_INFO);
            }
            try {
                stmt.bindf("is", root.getWcId(), localRelPath);
                haveRow = stmt.next();
            } finally {
                reset(stmt);
            }
            if (haveRow) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_OPERATION_DEPTH, "Can''t revert ''{0}'' without reverting children", root.getAbsPath(localRelPath));
                SVNErrorManager.error(err, SVNLogType.WC);
            }

            stmt = sdb.getStatement(SVNWCDbStatements.UPDATE_OP_DEPTH_INCREASE_RECURSIVE);
            try {
                ((SVNSqlJetTableStatement) stmt).addTrigger(nodesTableTrigger);

                stmt.bindf("isi", root.getWcId(), localRelPath, opDepth);
                stmt.done();
            } finally {
                stmt.reset();
            }

            stmt = sdb.getStatement(SVNWCDbStatements.DELETE_WORKING_NODE);
            try {
                ((SVNSqlJetTableStatement) stmt).addTrigger(nodesTableTrigger);

                stmt.bindf("is", root.getWcId(), localRelPath);
                stmt.done();
            } finally {
                stmt.reset();
            }
            stmt = sdb.getStatement(SVNWCDbStatements.DELETE_WC_LOCK_ORPHAN);
            try {
                stmt.bindf("is", root.getWcId(), localRelPath);
                stmt.done();
            } finally {
                stmt.reset();
            }

            if (movedHere) {
                clearMovedTo(root, localRelPath, nodesTableTrigger);
            }
        }
        long affectedRows;
        stmt = sdb.getStatement(SVNWCDbStatements.DELETE_ACTUAL_NODE_LEAVING_CHANGELIST);
        try {
            ((SVNSqlJetTableStatement) stmt).addTrigger(actualNodesTableTrigger);
            stmt.bindf("is", root.getWcId(), localRelPath);

            affectedRows = stmt.done();
        } finally {
            stmt.reset();
        }
        if (affectedRows == 0) {
            stmt = sdb.getStatement(SVNWCDbStatements.CLEAR_ACTUAL_NODE_LEAVING_CHANGELIST);
            try {
                ((SVNSqlJetTableStatement) stmt).addTrigger(actualNodesTableTrigger);

                stmt.bindf("is", root.getWcId(), localRelPath);
                stmt.done();
            } finally {
                stmt.reset();
            }
        }
    }

    private static void clearMovedTo(SVNWCDbRoot root, File localRelPath, SvnRevertNodesTrigger nodesTableTrigger) throws SVNException {
        SVNSqlJetStatement stmt = root.getSDb().getStatement(root.getFormat() == ISVNWCDb.WC_FORMAT_17 ? SVNWCDbStatements.SELECT_MOVED_FROM_RELPATH_17 : SVNWCDbStatements.SELECT_MOVED_FROM_RELPATH);
        try {
            stmt.bindf("is", root.getWcId(), localRelPath);
            boolean haveRow = stmt.next();
            if (!haveRow) {
                return;
            }
            File movedFromRelPath = SvnWcDbStatementUtil.getColumnPath(stmt, NODES__Fields.local_relpath);
            stmt.reset();
            stmt = root.getSDb().getStatement(SVNWCDbStatements.CLEAR_MOVE_TO_RELPATH);
            stmt.bindf("isi", root.getWcId(), movedFromRelPath, SVNWCUtils.relpathDepth(movedFromRelPath));
            ((SVNSqlJetTableStatement) stmt).addTrigger(nodesTableTrigger);
            stmt.done();
        } finally {
            stmt.reset();
        }
    }

    public static void revertRecursive(SVNWCDbRoot root, File localRelPath) throws SVNException {
        boolean movedHere;

        SVNSqlJetDb sdb = root.getSDb();
        SvnRevertNodesTrigger nodesTableTrigger = new SvnRevertNodesTrigger(sdb);
        ISVNSqlJetTrigger actualNodesTableTrigger = root.getFormat() == ISVNWCDb.WC_FORMAT_17 ?
                new SvnRevertActualNodesTrigger17(sdb) :
                new SvnRevertActualNodesTrigger(sdb);


        SVNSqlJetStatement stmt = sdb.getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
        long opDepth;
        try {
            stmt.bindf("is", root.getWcId(), localRelPath);
            if (!stmt.next()) {
                reset(stmt);
                long affectedRows;
                stmt = sdb.getStatement(SVNWCDbStatements.DELETE_ACTUAL_NODE_RECURSIVE);
                try {
                    ((SVNSqlJetTableStatement) stmt).addTrigger(actualNodesTableTrigger);

                    stmt.bindf("is", root.getWcId(), localRelPath);
                    affectedRows = stmt.done();
                } finally {
                    stmt.reset();
                }
                if (affectedRows > 0) {
                    return;
                }
                nodeNotFound(root, localRelPath);
            }
            opDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
            movedHere = getColumnBoolean(stmt, NODES__Fields.moved_here);
        } finally {
            reset(stmt);
        }

        if (opDepth > 0 && opDepth != SVNWCUtils.relpathDepth(localRelPath)) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_OPERATION_DEPTH, "Can't revert ''{0}'' without" + " reverting parent", SVNFileUtil.createFilePath(root.getAbsPath(), localRelPath));
            SVNErrorManager.error(errorMessage, SVNLogType.WC);
        }

        stmt = sdb.getStatement(SVNWCDbStatements.SELECT_MOVED_OUTSIDE);
        try {
            stmt.bindf("isi", root.getWcId(), localRelPath, opDepth);
            boolean haveRow = stmt.next();
            while (haveRow) {
                File moveSrcRelPath = getColumnPath(stmt, NODES__Fields.local_relpath);

                SVNWCDb.ResolveBreakMovedAway resolveBreakMovedAway = new SVNWCDb.ResolveBreakMovedAway();
                resolveBreakMovedAway.wcRoot = root;
                resolveBreakMovedAway.localRelPath = moveSrcRelPath;
                try {
                    resolveBreakMovedAway.transaction(sdb);
                } catch (SqlJetException e) {
                    SVNSqlJetDb.createSqlJetError(e);
                }

                haveRow = stmt.next();
            }
        } finally {
            reset(stmt);
        }

        long selectOpDepth = opDepth != 0 ? opDepth : 1;
        stmt = sdb.getStatement(SVNWCDbStatements.DELETE_NODES_ABOVE_DEPTH_RECURSIVE);
        try {
            stmt.bindf("isi", root.getWcId(), localRelPath, selectOpDepth);
            ((SVNSqlJetTableStatement) stmt).addTrigger(nodesTableTrigger);
            stmt.done();
        } finally {
            reset(stmt);
        }

        if (opDepth > 0 && opDepth != SVNWCUtils.relpathDepth(localRelPath)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_OPERATION_DEPTH, "Can''t revert ''{0}'' without reverting parent", root.getAbsPath(localRelPath));
            SVNErrorManager.error(err, SVNLogType.WC);
        }

        stmt = sdb.getStatement(SVNWCDbStatements.DELETE_ACTUAL_NODE_LEAVING_CHANGELIST_RECURSIVE);
        try {
            ((SVNSqlJetTableStatement) stmt).addTrigger(actualNodesTableTrigger);
            stmt.bindf("is", root.getWcId(), localRelPath);
            stmt.done();
        } finally {
            stmt.reset();
        }

        stmt = sdb.getStatement(SVNWCDbStatements.CLEAR_ACTUAL_NODE_LEAVING_CHANGELIST_RECURSIVE);
        try {
            ((SVNSqlJetTableStatement) stmt).addTrigger(actualNodesTableTrigger);
            stmt.bindf("is", root.getWcId(), localRelPath);
            stmt.done();
        } finally {
            stmt.reset();
        }

        stmt = sdb.getStatement(SVNWCDbStatements.DELETE_WC_LOCK_ORPHAN_RECURSIVE);
        try {
            stmt.bindf("is", root.getWcId(), localRelPath);
            stmt.done();
        } finally {
            stmt.reset();
        }

        stmt = sdb.getStatement(SVNWCDbStatements.SELECT_MOVED_HERE_CHILDREN);
        try {
            stmt.bindf("is", root.getWcId(), localRelPath);
            boolean haveRow = stmt.next();
            while (haveRow) {
                File movedHereChildRelPath = getColumnPath(stmt, NODES__Fields.moved_to);
                clearMovedTo(root, movedHereChildRelPath, nodesTableTrigger);
                haveRow = stmt.next();
            }
        } finally {
            stmt.reset();
        }

        if (opDepth > 0 && opDepth == SVNWCUtils.relpathDepth(localRelPath) && movedHere) {
            clearMovedTo(root, localRelPath, nodesTableTrigger);
        }
    }
    
    public enum RevertInfo {
        reverted, 
        conflictOld, 
        conflictNew,
        conflictWorking,
        propReject,
        copiedHere,
        kind,
        markerFiles
    }
    
    public static Map<File, SVNWCDbKind> readRevertCopiedChildren(SVNWCContext context, File localAbsPath) throws SVNException {
        Map<File, SVNWCDbKind> result = new TreeMap<File, ISVNWCDb.SVNWCDbKind>(new Comparator<File>() {
            public int compare(File o1, File o2) {
                String path1 = o1.getAbsolutePath();
                String path2 = o2.getAbsolutePath();
                
                return -SVNPathUtil.PATH_COMPARATOR.compare(path1, path2);
            }
        });
        
        
        SVNWCDb db = (SVNWCDb) context.getDb();
        DirParsedInfo dirInfo = db.obtainWcRoot(localAbsPath);
        File localRelpath = dirInfo.localRelPath;
        SVNWCDbRoot root = dirInfo.wcDbDir.getWCRoot();

        root.getSDb().getTemporaryDb().beginTransaction(SqlJetTransactionMode.READ_ONLY);
        SVNSqlJetStatement stmt = null;
        try {
            stmt = root.getSDb().getTemporaryDb().getStatement(SVNWCDbStatements.SELECT_REVERT_LIST_COPIED_CHILDREN);
            stmt.bindf("si", localRelpath, SVNWCUtils.relpathDepth(localRelpath));
            while(stmt.next()) {
                String relpath = getColumnText(stmt, REVERT_LIST__Fields.local_relpath);
                File childFile = SVNFileUtil.createFilePath(root.getAbsPath(), relpath);
                result.put(childFile, getColumnKind(stmt, REVERT_LIST__Fields.kind));
            }
        } finally {
            reset(stmt);
            root.getSDb().getTemporaryDb().commit();
        }
        
        return result;
    }
    
    public static Structure<RevertInfo> readRevertInfo(SVNWCContext context, File localAbsPath) throws SVNException {
        SVNWCDb db = (SVNWCDb) context.getDb();
        DirParsedInfo dirInfo = db.obtainWcRoot(localAbsPath);
        File localRelpath = dirInfo.localRelPath;
        SVNWCDbRoot root = dirInfo.wcDbDir.getWCRoot();

        root.getSDb().getTemporaryDb().beginTransaction(SqlJetTransactionMode.WRITE);
        Structure<RevertInfo> result = Structure.obtain(RevertInfo.class);
        result.set(RevertInfo.kind, SVNWCDbKind.Unknown);
        result.set(RevertInfo.reverted, false);
        result.set(RevertInfo.copiedHere, false);
        
        try {            
            /**
             * SELECT conflict_old, conflict_new, conflict_working, prop_reject, notify,
             *  actual, op_depth, repos_id, kind
             *   FROM revert_list
             * WHERE local_relpath = ?1
             * ORDER BY actual DESC
             */
            SVNSqlJetStatement stmt = new SVNSqlJetSelectStatement(root.getSDb().getTemporaryDb(), SVNWCDbSchema.REVERT_LIST) {
                @Override
                protected ISqlJetCursor openCursor() throws SVNException {
                    try {
                        return super.openCursor().reverse();
                    } catch (SqlJetException e) {
                        SVNSqlJetDb.createSqlJetError(e);
                    }
                    return null;
                }

                @Override
                protected boolean isFilterPassed() throws SVNException {
                    return getBind(1).equals(getColumnString(REVERT_LIST__Fields.local_relpath));
                }

                @Override
                protected Object[] getWhere() throws SVNException {
                    return new Object[]{};
                }
            };
            stmt.bindf("s", localRelpath);
            boolean haveRow = stmt.next();
            if (haveRow) {
                boolean isActual = getColumnBoolean(stmt, REVERT_LIST__Fields.actual);
                boolean anotherRow = false;
                if (isActual) {
                    byte[] conflictData = getColumnBlob(stmt, REVERT_LIST__Fields.conflict_data);
                    if (conflictData != null) {
                        SVNSkel conflicts = SVNSkel.parse(conflictData);
                        result.set(RevertInfo.markerFiles, SvnWcDbConflicts.readConflictMarkers((SVNWCDb) context.getDb(), root.getAbsPath(), conflicts));
                    }
                    if (!isColumnNull(stmt, REVERT_LIST__Fields.notify)) {
                        result.set(RevertInfo.reverted, true);
                    }

                    anotherRow = stmt.next();
                }
                if (!isActual || anotherRow) {
                    result.set(RevertInfo.reverted, true);
                    if (!isColumnNull(stmt, REVERT_LIST__Fields.repos_id)) {
                        long opDepth = getColumnInt64(stmt, REVERT_LIST__Fields.op_depth);
                        result.set(RevertInfo.copiedHere, opDepth == SVNWCUtils.relpathDepth(localRelpath));
                    }
                    result.set(RevertInfo.kind, getColumnKind(stmt, REVERT_LIST__Fields.kind));
                }
            }
            reset(stmt);
            if (haveRow) {
                stmt = new SVNSqlJetDeleteStatement(root.getSDb().getTemporaryDb(), SVNWCDbSchema.REVERT_LIST) {
                    @Override
                    protected boolean isFilterPassed() throws SVNException {
                        return getBind(1).equals(getColumnString(REVERT_LIST__Fields.local_relpath));
                    }

                    @Override
                    protected Object[] getWhere() throws SVNException {
                        return new Object[]{};
                    }
                };
                try {
                    stmt.bindf("s", localRelpath);
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }
        } catch (SVNException e) {
            root.getSDb().getTemporaryDb().rollback();
            throw e;
        } finally {
            root.getSDb().getTemporaryDb().commit();
        }
        return result;
    }

    public static void dropRevertList(SVNWCContext context, File localAbsPath) throws SVNException {
        SVNWCDb db = (SVNWCDb) context.getDb();
        DirParsedInfo dirInfo = db.obtainWcRoot(localAbsPath);
        SVNWCDbRoot root = dirInfo.wcDbDir.getWCRoot();
        
        SVNSqlJetStatement stmt = new SVNWCDbCreateSchema(root.getSDb().getTemporaryDb(), SVNWCDbCreateSchema.DROP_REVERT_LIST, -1, false);
        try {
            stmt.done();
        } finally {
            stmt.reset();
        }
    }

    public static void notifyRevert(SVNWCContext context, File localAbsPath, ISVNEventHandler eventHandler) throws SVNException {
        SVNWCDb db = (SVNWCDb) context.getDb();
        DirParsedInfo dirInfo = db.obtainWcRoot(localAbsPath);
        File localRelpath = dirInfo.localRelPath;
        SVNWCDbRoot root = dirInfo.wcDbDir.getWCRoot();
        
        SVNSqlJetStatement stmt = new SVNSqlJetSelectStatement(root.getSDb().getTemporaryDb(), SVNWCDbSchema.REVERT_LIST) {

            @Override
            protected boolean isFilterPassed() throws SVNException {
                String rowPath = getColumnString(REVERT_LIST__Fields.local_relpath);
                String selectPath = (String) getBind(1);
                if (selectPath.equals(rowPath) || "".equals(selectPath) || rowPath.startsWith(selectPath + "/")) {
                    return !isColumnNull(REVERT_LIST__Fields.notify) || getColumnLong(REVERT_LIST__Fields.actual) == 0;
                }
                return false;
            }
            @Override
            protected Object[] getWhere() throws SVNException {
                return new Object[] {};
            }
        };
        stmt.bindf("s", localRelpath);
        try {
            if (eventHandler != null) {
                File previousPath = null;
                while(stmt.next()) {
                    File notifyRelPath = getColumnPath(stmt, REVERT_LIST__Fields.local_relpath);
                    if (previousPath != null && notifyRelPath.equals(previousPath)) {
                        continue;
                    }
                    previousPath = notifyRelPath;
                    File notifyAbsPath = SVNFileUtil.createFilePath(root.getAbsPath(), notifyRelPath);
                    eventHandler.handleEvent(SVNEventFactory.createSVNEvent(notifyAbsPath, SVNNodeKind.NONE, null, -1, SVNEventAction.REVERT, 
                            SVNEventAction.REVERT, null, null, -1, -1), -1);
                }
            }
        } finally {
            reset(stmt);
        }

        stmt = new SVNSqlJetDeleteStatement(root.getSDb().getTemporaryDb(), SVNWCDbSchema.REVERT_LIST) {
            @Override
            protected Object[] getWhere() throws SVNException {
                return new Object[0];
            }

            @Override
            protected boolean isFilterPassed() throws SVNException {
                String selectPath = (String) getBind(1);
                if ("".equals(selectPath)) {
                    return true;
                }
                String rowPath = getColumnString(REVERT_LIST__Fields.local_relpath);
                return rowPath.equals(selectPath) || rowPath.startsWith(selectPath + "/");
            }
        };
        try {
            stmt.bindf("s", localRelpath);
            stmt.done();
        } finally {
            stmt.reset();
        }
    }
}
