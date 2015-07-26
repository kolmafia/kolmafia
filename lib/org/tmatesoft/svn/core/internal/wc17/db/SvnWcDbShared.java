package org.tmatesoft.svn.core.internal.wc17.db;

import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnBlob;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnBoolean;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnChecksum;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnDate;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnDepth;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnInt64;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnKind;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnPath;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnPresence;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnRevNum;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getColumnText;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.getLockFromColumns;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.hasColumnProperties;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.isColumnNull;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.parseDepth;
import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.reset;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb.Mode;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc16.SVNUpdateClient16;
import org.tmatesoft.svn.core.internal.wc17.SVNExternalsStore;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbLock;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb.DirParsedInfo;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb.ReposInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.AdditionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.DeletionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.MovedFromInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.MovedInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.RepositoryInfo;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbCollectTargets;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.LOCK__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbStatements;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnWcDbShared {
    
    public static final byte[] EMPTY_PROPS_BLOB = {40, 41};

    public static void begingReadTransaction(SVNWCDbRoot root) throws SVNException {
        root.getSDb().beginTransaction(SqlJetTransactionMode.READ_ONLY);
    }
    
    public static void begingWriteTransaction(SVNWCDbRoot root) throws SVNException {
        root.getSDb().beginTransaction(SqlJetTransactionMode.WRITE);
    }
    
    public static void commitTransaction(SVNWCDbRoot root) throws SVNException {
        root.getSDb().commit();
    }
    
    public static void rollbackTransaction(SVNWCDbRoot root) throws SVNException {
        root.getSDb().rollback();
    }
    
    protected static void nodeNotFound(File absolutePath) throws SVNException {
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", absolutePath);
        SVNErrorManager.error(err, SVNLogType.WC);
    }

    protected static void nodeIsNotInstallable(File absolutePath) throws SVNException {
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' is not installable.", absolutePath);
        SVNErrorManager.error(err, SVNLogType.WC);
    }
    
    protected static boolean doesNodeExists(SVNWCDbRoot wcDbRoot, File relpath) throws SVNException {
        SVNSqlJetStatement stmt = null;
        try {
            stmt = wcDbRoot.getSDb().getStatement(SVNWCDbStatements.DOES_NODE_EXIST);
            stmt.bindf("is", wcDbRoot.getWcId(), relpath);
            return stmt.next();
        } finally {
            if (stmt != null) {
                stmt.reset();
            }
        }
    }

    protected static void nodeNotFound(SVNWCDbRoot root, File relPath) throws SVNException {
        nodeNotFound(root.getAbsPath(relPath));
    }
    
    protected static void sqliteError(SqlJetException e) throws SVNException {
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.SQLITE_ERROR, e);
        SVNErrorManager.error(err, SVNLogType.WC);
    }
    
    protected static class Target {
    	public Target(long wcId, String relpath) {
    		this.relPath = relpath;
    		this.wcId = wcId;
    	}
    	
    	public String relPath;
    	public long wcId;
    }
    
    protected static Collection<Target> collectTargets(SVNWCDbRoot root, File relpath, SVNDepth depth, Collection<String> changelists) throws SVNException {
        SVNSqlJetStatement stmt = null;
        final Collection<Target> targets = new ArrayList<Target>();
        try {
        	stmt = new SVNWCDbCollectTargets(root.getSDb(), root.getWcId(), relpath, depth, changelists);
        	while(stmt.next()) {
        		final long wcId = getColumnInt64(stmt, NODES__Fields.wc_id);
        		final String path = getColumnText(stmt, NODES__Fields.local_relpath);
        		targets.add(new Target(wcId, path));
        	}
            if (depth == SVNDepth.FILES || depth == SVNDepth.IMMEDIATES) {
            	reset(stmt);
                stmt  = new SVNWCDbCollectTargets(root.getSDb(), root.getWcId(), relpath, SVNDepth.EMPTY, changelists);
            	while(stmt.next()) {
            		final long wcId = getColumnInt64(stmt, NODES__Fields.wc_id);
            		final String path = getColumnText(stmt, NODES__Fields.local_relpath);
            		targets.add(new Target(wcId, path));
            	}
            }
        } finally {
        	reset(stmt);
        }
        return targets;
    }

    public static Structure<AdditionInfo> scanAddition(SVNWCDb db, File localAbsPath) throws SVNException {
        DirParsedInfo parsed = db.parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        File localRelpath = parsed.localRelPath;

        Structure<AdditionInfo> additionInfo = scanAddition(pdh.getWCRoot(), localRelpath);
        if (additionInfo.hasField(AdditionInfo.reposRootUrl)) {
            ReposInfo reposInfo = db.fetchReposInfo(pdh.getWCRoot().getSDb(), additionInfo.lng(AdditionInfo.reposId));
            additionInfo.set(AdditionInfo.reposRootUrl, SVNURL.parseURIEncoded(reposInfo.reposRootUrl));
            additionInfo.set(AdditionInfo.reposUuid, reposInfo.reposUuid);
        }
        
        if (additionInfo.hasField(AdditionInfo.originalRootUrl) && additionInfo.lng(AdditionInfo.originalReposId) >= 0) {
            ReposInfo reposInfo = db.fetchReposInfo(pdh.getWCRoot().getSDb(), additionInfo.lng(AdditionInfo.originalReposId));
            additionInfo.set(AdditionInfo.originalRootUrl, SVNURL.parseURIEncoded(reposInfo.reposRootUrl));
            additionInfo.set(AdditionInfo.originalUuid, reposInfo.reposUuid);
        }
        return additionInfo;
    }
    
    public static Structure<MovedInfo> scanMoved(SVNWCDb db, File localAbsPath) throws SVNException {
        final Structure<MovedInfo> result = Structure.obtain(MovedInfo.class);
        final DirParsedInfo parsed = db.parseDir(localAbsPath, Mode.ReadOnly);
        final SVNWCDbDir pdh = parsed.wcDbDir;
        final File localRelpath = parsed.localRelPath;
        final SVNWCDbRoot root = pdh.getWCRoot();
        
        final Structure<AdditionInfo> additionInfo = scanAddition(pdh.getWCRoot(), localRelpath, AdditionInfo.values());
        if (additionInfo.get(AdditionInfo.status) != SVNWCDbStatus.MovedHere || additionInfo.get(AdditionInfo.movedFromRelPath) == null) {
            final SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Path ''{0}'' was not moved here", localAbsPath);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        result.set(MovedInfo.opRootAbsPath, additionInfo.get(AdditionInfo.opRootAbsPath));
        result.set(MovedInfo.movedFromAbsPath, root.getAbsPath(additionInfo.<File>get(AdditionInfo.movedFromRelPath)));
        result.set(MovedInfo.movedFromOpRootAbsPath, root.getAbsPath(additionInfo.<File>get(AdditionInfo.movedFromOpRootRelPath)));
        
        long movedFromOpDepth = additionInfo.lng(AdditionInfo.movedFromOpDepth);
        File tmp = additionInfo.<File>get(AdditionInfo.movedFromOpRootRelPath);
        while(SVNWCUtils.relpathDepth(tmp) > movedFromOpDepth) {
            tmp = SVNFileUtil.getFileDir(tmp);
        }
        result.set(MovedInfo.movedFromDeleteAbsPath, root.getAbsPath(tmp));
        return result;
    }
    
    protected static Structure<AdditionInfo> scanAddition(SVNWCDbRoot root, File localRelpath, AdditionInfo... fields) throws SVNException {
        Structure<AdditionInfo> info = Structure.obtain(AdditionInfo.class, fields);
        info.set(AdditionInfo.originalRevision, SVNWCDb.INVALID_REVNUM);
        info.set(AdditionInfo.originalReposId, SVNWCDb.INVALID_REPOS_ID);
        info.set(AdditionInfo.movedFromOpDepth, 0);
        
        begingReadTransaction(root);
        File buildRelpath = SVNFileUtil.createFilePath("");
        File currentRelpath = localRelpath;
        
        SVNSqlJetStatement stmt = null;
        try {
            SVNWCDbStatus presence;
            File reposPrefixPath = SVNFileUtil.createFilePath("");
            int i;

            stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_WORKING_NODE);

            stmt.bindf("is", root.getWcId(), localRelpath);
            if (!stmt.next()) {
                reset(stmt);
                nodeNotFound(root, localRelpath);
            }

            presence = getColumnPresence(stmt);
            long opDepth = getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.op_depth);

            if (opDepth == 0 || (presence != SVNWCDbStatus.Normal && presence != SVNWCDbStatus.Incomplete)) {
                reset(stmt);
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Expected node ''{0}'' to be added.", root.getAbsPath(localRelpath));
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            info.set(AdditionInfo.originalRevision, getColumnRevNum(stmt, NODES__Fields.revision));
            info.set(AdditionInfo.status, SVNWCDbStatus.Added);
            currentRelpath = localRelpath;
            for (i = SVNWCUtils.relpathDepth(localRelpath); i > opDepth; --i) {
                reposPrefixPath = SVNFileUtil.createFilePath(SVNFileUtil.createFilePath(SVNFileUtil.getFileName(currentRelpath)), reposPrefixPath);
                currentRelpath = SVNFileUtil.getFileDir(currentRelpath);
            }
            info.set(AdditionInfo.opRootRelPath, currentRelpath);
            info.set(AdditionInfo.opRootAbsPath, root.getAbsPath(currentRelpath));

            if (info.hasField(AdditionInfo.originalReposRelPath) 
                    || info.hasField(AdditionInfo.originalRootUrl) 
                    || info.hasField(AdditionInfo.originalUuid)
                    || (info.hasField(AdditionInfo.originalRevision) && info.lng(AdditionInfo.originalRevision) == SVNWCDb.INVALID_REVNUM) 
                    || info.hasField(AdditionInfo.status)) {
                if (!localRelpath.equals(currentRelpath)) {
                    reset(stmt);
                    stmt.bindf("is", root.getWcId(), currentRelpath);
                    if (!stmt.next()) {
                        reset(stmt);
                        nodeNotFound(root, currentRelpath);
                    }

                    if (info.hasField(AdditionInfo.originalRevision) 
                            && info.lng(AdditionInfo.originalRevision) == SVNWCDb.INVALID_REVNUM)
                        info.set(AdditionInfo.originalRevision, getColumnRevNum(stmt, NODES__Fields.revision));
                }
                info.set(AdditionInfo.originalReposRelPath, getColumnPath(stmt, NODES__Fields.repos_path));

                if (!isColumnNull(stmt, NODES__Fields.repos_id) 
                            && (
                                info.hasField(AdditionInfo.status) || 
                                info.hasField(AdditionInfo.originalReposId) ||
                                info.hasField(AdditionInfo.movedFromRelPath) ||
                                info.hasField(AdditionInfo.movedFromOpRootRelPath))) {
                        
                    info.set(AdditionInfo.originalReposId, getColumnInt64(stmt, NODES__Fields.repos_id));
                    final boolean movedHere = getColumnBoolean(stmt, NODES__Fields.moved_here); 
                    if (movedHere) {
                        info.set(AdditionInfo.status, SVNWCDbStatus.MovedHere);
                    } else {
                        info.set(AdditionInfo.status, SVNWCDbStatus.Copied);
                    }
                    if (movedHere &&
                            (info.hasField(AdditionInfo.movedFromRelPath) ||
                            info.hasField(AdditionInfo.movedFromOpRootRelPath))) {
                        final Structure<MovedFromInfo> movedFromInfo = getMovedFromInfo(root, currentRelpath, localRelpath);
                        if (movedFromInfo.hasValue(MovedFromInfo.opDepth)) {
                            info.set(AdditionInfo.movedFromOpDepth, movedFromInfo.lng(MovedFromInfo.opDepth));
                        }
                        info.set(AdditionInfo.movedFromOpRootRelPath, movedFromInfo.get(MovedFromInfo.movedFromOpRootRelPath));
                        info.set(AdditionInfo.movedFromRelPath, movedFromInfo.get(MovedFromInfo.movedFromRelPath));
                    }
                }
            }

            while (true) {
                reset(stmt);
                reposPrefixPath = SVNFileUtil.createFilePath(SVNFileUtil.createFilePath(SVNFileUtil.getFileName(currentRelpath)), reposPrefixPath);
                currentRelpath = SVNFileUtil.getFileDir(currentRelpath);
                stmt.bindf("is", root.getWcId(), currentRelpath);
                if (!stmt.next()) {
                    break;
                }
                opDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
                for (i = SVNWCUtils.relpathDepth(currentRelpath); i > opDepth; i--) {
                    reposPrefixPath = SVNFileUtil.createFilePath(SVNFileUtil.createFilePath(SVNFileUtil.getFileName(currentRelpath)), reposPrefixPath);
                    currentRelpath = SVNFileUtil.getFileDir(currentRelpath);
                }
            }
            reset(stmt);
            buildRelpath = reposPrefixPath;
            
            if (info.hasField(AdditionInfo.reposRelPath) || info.hasField(AdditionInfo.reposId)) {
                Structure<NodeInfo> baseInfo = getDepthInfo(root, currentRelpath, 0, NodeInfo.reposRelPath, NodeInfo.reposId);
                info.set(AdditionInfo.reposRelPath, SVNFileUtil.createFilePath(baseInfo.<File>get(NodeInfo.reposRelPath), buildRelpath));
                info.set(AdditionInfo.reposId, baseInfo.lng(NodeInfo.reposId));
                baseInfo.release();
            }
        } finally {
            reset(stmt);
            commitTransaction(root);
        }
        
        return info;
    }
    
    public static Structure<MovedFromInfo> getMovedFromInfo(SVNWCDbRoot root, File movedToOpRootRelPath, File localRelPath) throws SVNException {
        final Structure<MovedFromInfo> result = Structure.obtain(MovedFromInfo.class);
        SVNSqlJetStatement stmt = null;
        try {
            stmt = root.getSDb().getStatement(root.getFormat() == ISVNWCDb.WC_FORMAT_17 ? SVNWCDbStatements.SELECT_MOVED_FROM_RELPATH_17 : SVNWCDbStatements.SELECT_MOVED_FROM_RELPATH);
            stmt.bindf("is", root.getWcId(), movedToOpRootRelPath);
            if (!stmt.next()) {
                return result;
            }
            result.set(MovedFromInfo.opDepth, getColumnInt64(stmt, NODES__Fields.op_depth));
            final File deleteOpRootReplpath = getColumnPath(stmt, NODES__Fields.local_relpath);
            result.set(MovedFromInfo.movedFromOpRootRelPath, deleteOpRootReplpath);
            if (movedToOpRootRelPath.equals(localRelPath)) {
                result.set(MovedFromInfo.movedFromRelPath, deleteOpRootReplpath);
            } else {
                final File childRelPath = SVNWCUtils.skipAncestor(movedToOpRootRelPath, localRelPath);
                result.set(MovedFromInfo.movedFromRelPath, SVNFileUtil.createFilePath(deleteOpRootReplpath, childRelPath));
            }
        } finally {
            reset(stmt);
        }
        return result;
    }

    public static Structure<DeletionInfo> scanDeletion(SVNWCDb db, File localAbsPath) throws SVNException {
        DirParsedInfo parsed = db.parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        File localRelpath = parsed.localRelPath;

        return scanDeletion(pdh.getWCRoot(), localRelpath);
    }

    protected static Structure<DeletionInfo> scanDeletion(SVNWCDbRoot root, File localRelpath) throws SVNException {
        Structure<DeletionInfo> deletionInfoStructure = Structure.obtain(DeletionInfo.class);
        ISVNWCDb.WCDbDeletionInfo deletionInfo = root.getDb().scanDeletion(SVNFileUtil.createFilePath(root.getAbsPath(), localRelpath), ISVNWCDb.WCDbDeletionInfo.DeletionInfoField.values());
        deletionInfoStructure.set(DeletionInfo.baseDelRelPath, SVNFileUtil.skipAncestor(root.getAbsPath(), deletionInfo.baseDelAbsPath));
        deletionInfoStructure.set(DeletionInfo.movedToRelPath, SVNFileUtil.skipAncestor(root.getAbsPath(), deletionInfo.movedToAbsPath));
        deletionInfoStructure.set(DeletionInfo.movedToOpRootRelPath, SVNFileUtil.skipAncestor(root.getAbsPath(), deletionInfo.movedToOpRootAbsPath));
        deletionInfoStructure.set(DeletionInfo.workDelAbsPath, deletionInfo.workDelAbsPath);
        deletionInfoStructure.set(DeletionInfo.workDelRelPath, SVNFileUtil.skipAncestor(root.getAbsPath(), deletionInfo.baseDelAbsPath));
        return deletionInfoStructure;

//        Structure<DeletionInfo> info = Structure.obtain(DeletionInfo.class);
//
//        SVNWCDbStatus childPresence = SVNWCDbStatus.BaseDeleted;
//        boolean childHasBase = false;
//        boolean foundMovedTo = false;
//        long opDepth = 0, localOpDepth = 0;
//        File currentRelPath = localRelpath;
//        File childRelpath = null;
//
//        begingReadTransaction(root);
//        SVNSqlJetStatement stmt = null;
//        try {
//            while (true) {
//                stmt = root.getSDb().getStatement(SVNWCDbStatements.SELECT_DELETION_INFO);
//                stmt.bindf("is", root.getWcId(), SVNFileUtil.getFilePath(currentRelPath));
//                if (!stmt.next()) {
//                    try {
//                        if (currentRelPath == localRelpath) {
//                            nodeNotFound(root, localRelpath);
//                        }
//                        if (childHasBase) {
//                            info.set(DeletionInfo.baseDelRelPath, childRelpath);
//                        }
//                        break;
//                    } finally {
//                        reset(stmt);
//                    }
//                }
//
//                SVNSqlJetStatement baseStmt = null;
//                try {
//                    SVNWCDbStatus workPresence = getColumnPresence(stmt);
//                    if (currentRelPath.equals(localRelpath) && workPresence != SVNWCDbStatus.NotPresent && workPresence != SVNWCDbStatus.BaseDeleted) {
//                        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Expected node ''{0}'' to be deleted.", root.getAbsPath(localRelpath));
//                        SVNErrorManager.error(err, SVNLogType.WC);
//                    }
//                    assert (workPresence == SVNWCDbStatus.Normal || workPresence == SVNWCDbStatus.NotPresent || workPresence == SVNWCDbStatus.BaseDeleted || workPresence == SVNWCDbStatus.Incomplete);
//
//                    baseStmt = stmt.getJoinedStatement(SVN);
//                    boolean haveBase = false;
//                    haveBase = baseStmt != null && baseStmt.next() && !isColumnNull(baseStmt, SVNWCDbSchema.NODES__Fields.presence);
//                    if (haveBase) {
//                        SVNWCDbStatus basePresence = getColumnPresence(baseStmt);
//                        assert (basePresence == SVNWCDbStatus.Normal || basePresence == SVNWCDbStatus.NotPresent || basePresence == SVNWCDbStatus.Incomplete);
//                        if (basePresence == SVNWCDbStatus.Incomplete) {
//                            basePresence = SVNWCDbStatus.Normal;
//                        }
//                    }
//
//
//                    if (!foundMovedTo && !isColumnNull(stmt, SVNWCDbSchema.NODES__Fields.moved_to)) {
//                        foundMovedTo = true;
//                        info.set(DeletionInfo.baseDelRelPath, currentRelPath);
//                        info.set(DeletionInfo.movedToRelPath, getColumnPath(stmt, SVNWCDbSchema.NODES__Fields.moved_to));
//                    }
//
//                    opDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
//                    if (currentRelPath.equals(localRelpath)) {
//                        localOpDepth = opDepth;
//                    }
//                    if (!info.hasValue(DeletionInfo.workDelRelPath) &&
//                            ((opDepth < localOpDepth && opDepth > 0) || childPresence == SVNWCDbStatus.NotPresent)) {
//                        info.set(DeletionInfo.workDelRelPath, childRelpath);
//                        info.set(DeletionInfo.workDelAbsPath, root.getAbsPath(childRelpath));
//                    }
//
//                    childRelpath = currentRelPath;
//                    childPresence = workPresence;
//                    childHasBase = haveBase;
//
//                    currentRelPath = SVNFileUtil.getFileDir(currentRelPath);
//                } finally {
//                    reset(stmt);
//                    reset(baseStmt);
//                }
//            }
//        } finally {
//            commitTransaction(root);
//            reset(stmt);
//        }
//        return info;
    }

    public static Structure<NodeInfo> getBaseInfo(SVNWCDbRoot wcroot, File localRelPath, NodeInfo... fields) throws SVNException {
        return getDepthInfo(wcroot, localRelPath, 0, fields);
    }

    public static Structure<NodeInfo> getBaseInfo(SVNWCDb db, File localAbsPath, NodeInfo... fields) throws SVNException {
        DirParsedInfo parsed = db.parseDir(localAbsPath, Mode.ReadOnly);
        SVNWCDbDir pdh = parsed.wcDbDir;
        File localRelpath = parsed.localRelPath;

        return getDepthInfo(pdh.getWCRoot(), localRelpath, 0, fields);
    }
    
    public static Structure<NodeInfo> getDepthInfo(SVNWCDbRoot wcroot, File localRelPath, long opDepth, NodeInfo...fields) throws SVNException {
        Structure<NodeInfo> info = Structure.obtain(NodeInfo.class, fields);
        SVNSqlJetStatement stmt = wcroot.getSDb().getStatement(SVNWCDbStatements.SELECT_DEPTH_NODE);
        try {
            stmt.bindf("isi", wcroot.getWcId(), SVNFileUtil.getFilePath(localRelPath), opDepth);
            if (stmt.next()) {
                SVNWCDbKind node_kind = getColumnKind(stmt, NODES__Fields.kind);

                if (info.hasField(NodeInfo.kind)) {
                    info.set(NodeInfo.kind, node_kind);
                }
                if (info.hasField(NodeInfo.status)) {
                    info.set(NodeInfo.status, getColumnPresence(stmt));
                }
                if (info.hasField(NodeInfo.reposId)) {
                    info.set(NodeInfo.reposId, getColumnInt64(stmt, NODES__Fields.repos_id));
                }
                if (info.hasField(NodeInfo.revision)) {
                    info.set(NodeInfo.revision, getColumnRevNum(stmt, SVNWCDbSchema.NODES__Fields.revision));
                }
                if (info.hasField(NodeInfo.reposRelPath)) {
                    info.set(NodeInfo.reposRelPath, getColumnPath(stmt, SVNWCDbSchema.NODES__Fields.repos_path));
                }
                
                if (info.hasField(NodeInfo.reposRootUrl) || info.hasField(NodeInfo.reposUuid)) {
                    if (isColumnNull(stmt, SVNWCDbSchema.NODES__Fields.repos_id)) {
                        if (info.hasField(NodeInfo.reposRootUrl)) {
                            info.set(NodeInfo.reposRootUrl,null);
                        }
                        if (info.hasField(NodeInfo.reposUuid)) {
                            info.set(NodeInfo.reposUuid,null);
                        }
                    } else {
                        Structure<RepositoryInfo> repositoryInfo = wcroot.getDb().fetchRepositoryInfo(wcroot.getSDb(), getColumnInt64(stmt, SVNWCDbSchema.NODES__Fields.repos_id));
                        repositoryInfo.
                            from(RepositoryInfo.reposRootUrl, RepositoryInfo.reposUuid).
                            into(info, NodeInfo.reposRootUrl, NodeInfo.reposUuid);
                        repositoryInfo.release();
                    }
                }
                if (info.hasField(NodeInfo.changedRev)) {
                    info.set(NodeInfo.changedRev, getColumnRevNum(stmt, SVNWCDbSchema.NODES__Fields.changed_revision));
                }
                if (info.hasField(NodeInfo.changedDate)) {
                    info.set(NodeInfo.changedDate, getColumnDate(stmt, SVNWCDbSchema.NODES__Fields.changed_date));
                }
                if (info.hasField(NodeInfo.changedAuthor)) {
                    /* Result may be NULL. */
                    info.set(NodeInfo.changedAuthor,getColumnText(stmt, SVNWCDbSchema.NODES__Fields.changed_author));
                }
                if (info.hasField(NodeInfo.depth)) {
                    if (node_kind != SVNWCDbKind.Dir) {
                        info.set(NodeInfo.depth, SVNDepth.UNKNOWN);
                    } else {
                        String depth_str = getColumnText(stmt, SVNWCDbSchema.NODES__Fields.depth);

                        if (depth_str == null) {
                            info.set(NodeInfo.depth, SVNDepth.UNKNOWN);
                        } else {
                            info.set(NodeInfo.depth, parseDepth(depth_str));
                        }
                    }
                }
                if (info.hasField(NodeInfo.checksum)) {
                    if (node_kind != SVNWCDbKind.File) {
                        info.set(NodeInfo.checksum,null);
                    } else {
                        try {
                            info.set(NodeInfo.checksum, getColumnChecksum(stmt, SVNWCDbSchema.NODES__Fields.checksum));
                        } catch (SVNException e) {
                            SVNErrorMessage err = SVNErrorMessage.create(e.getErrorMessage().getErrorCode(), "The node ''{0}'' has a corrupt checksum value.", wcroot.getAbsPath(localRelPath));
                            SVNErrorManager.error(err, SVNLogType.WC);
                        }
                    }
                }
                if (info.hasField(NodeInfo.target)) {
                    if (node_kind != SVNWCDbKind.Symlink)
                        info.set(NodeInfo.target,null);
                    else
                        info.set(NodeInfo.target, getColumnPath(stmt, SVNWCDbSchema.NODES__Fields.symlink_target));
                }
                if (info.hasField(NodeInfo.updateRoot)) {
                    info.set(NodeInfo.updateRoot, getColumnText(stmt, SVNWCDbSchema.NODES__Fields.file_external) != null);
                }
                if (info.hasField(NodeInfo.hadProps)) {
                    info.set(NodeInfo.hadProps, hasColumnProperties(stmt, NODES__Fields.properties));
                }
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", wcroot.getAbsPath(localRelPath));
                SVNErrorManager.error(err, SVNLogType.WC);
            }

        } finally {
            stmt.reset();
        }

        return info;

    }

    protected static Structure<NodeInfo> readInfo(SVNWCDbRoot wcRoot, File localRelPath, NodeInfo... fields) throws SVNException {
        return readInfo(wcRoot, localRelPath,  false, fields);
    }

    protected static Structure<NodeInfo> readInfo(SVNWCDbRoot wcRoot, File localRelPath, boolean isAdditionMode, NodeInfo... fields) throws SVNException {
        Structure<NodeInfo> info = Structure.obtain(NodeInfo.class, fields);
    
        SVNSqlJetStatement stmtInfo = null;
        SVNSqlJetStatement stmtActual = null;
        
        try {
            stmtInfo = wcRoot.getSDb().getStatement(info.hasField(NodeInfo.lock) ? SVNWCDbStatements.SELECT_NODE_INFO_WITH_LOCK : SVNWCDbStatements.SELECT_NODE_INFO);
            stmtInfo.bindf("is", wcRoot.getWcId(), localRelPath);
            boolean haveInfo = stmtInfo.next();
            boolean haveActual = false;
            
            if (info.hasField(NodeInfo.changelist) || info.hasField(NodeInfo.conflicted) || info.hasField(NodeInfo.propsMod)) {
                stmtActual = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE);
                stmtActual.bindf("is", wcRoot.getWcId(), localRelPath);
                haveActual = stmtActual.next();
            }
            
            if (haveInfo) {
                long opDepth = getColumnInt64(stmtInfo, SVNWCDbSchema.NODES__Fields.op_depth);
                SVNWCDbKind nodeKind = getColumnKind(stmtInfo, NODES__Fields.kind);
                
                if (info.hasField(NodeInfo.status)) {
                    info.set(NodeInfo.status, getColumnPresence(stmtInfo));
                    if (opDepth != 0) {
                        info.set(NodeInfo.status, SVNWCDb.getWorkingStatus(info.<SVNWCDbStatus>get(NodeInfo.status)));
                    }
                }
                if (info.hasField(NodeInfo.kind)) {
                    info.set(NodeInfo.kind, nodeKind);
                }
                info.set(NodeInfo.reposId, opDepth != 0 ? SVNWCDb.INVALID_REPOS_ID : getColumnInt64(stmtInfo, SVNWCDbSchema.NODES__Fields.repos_id));
                if (info.hasField(NodeInfo.revision)) {
                    info.set(NodeInfo.revision, opDepth != 0 ? SVNWCDb.INVALID_REVNUM : getColumnRevNum(stmtInfo, SVNWCDbSchema.NODES__Fields.revision));
                }
                if (info.hasField(NodeInfo.reposRelPath)) {
                    info.set(NodeInfo.reposRelPath, opDepth != 0 ? null : SVNFileUtil.createFilePath(getColumnText(stmtInfo, SVNWCDbSchema.NODES__Fields.repos_path)));
                }
                if (info.hasField(NodeInfo.changedDate)) {
                    info.set(NodeInfo.changedDate, SVNWCUtils.readDate(getColumnInt64(stmtInfo, SVNWCDbSchema.NODES__Fields.changed_date)));
                }
                if (info.hasField(NodeInfo.changedRev)) {
                    info.set(NodeInfo.changedRev, getColumnRevNum(stmtInfo, SVNWCDbSchema.NODES__Fields.changed_revision));
                }
                if (info.hasField(NodeInfo.changedAuthor)) {
                    info.set(NodeInfo.changedAuthor, getColumnText(stmtInfo, SVNWCDbSchema.NODES__Fields.changed_author));                
                }
                if (info.hasField(NodeInfo.recordedTime)) {
                    info.set(NodeInfo.recordedTime, getColumnInt64(stmtInfo, SVNWCDbSchema.NODES__Fields.last_mod_time));
                }
                if (info.hasField(NodeInfo.depth)) {
                    if (nodeKind != SVNWCDbKind.Dir) {
                        info.set(NodeInfo.depth, SVNDepth.UNKNOWN);
                    } else {
                        info.set(NodeInfo.depth, getColumnDepth(stmtInfo, SVNWCDbSchema.NODES__Fields.depth));
                    }
                }
                if (info.hasField(NodeInfo.checksum)) {
                    if (nodeKind != SVNWCDbKind.File) {
                        info.set(NodeInfo.checksum, null);
                    } else {
                        try {
                            info.set(NodeInfo.checksum, getColumnChecksum(stmtInfo, SVNWCDbSchema.NODES__Fields.checksum));
                        } catch (SVNException e) {
                            SVNErrorMessage err = SVNErrorMessage.create(e.getErrorMessage().getErrorCode(), "The node ''{0}'' has a corrupt checksum value.", wcRoot.getAbsPath(localRelPath));
                            SVNErrorManager.error(err, SVNLogType.WC);
                        }
                    }                
                }
                if (info.hasField(NodeInfo.recordedSize)) {
                    info.set(NodeInfo.recordedSize, getColumnInt64(stmtInfo, SVNWCDbSchema.NODES__Fields.translated_size));
                }
                if (info.hasField(NodeInfo.target)) {
                    info.set(NodeInfo.target, SVNFileUtil.createFilePath(getColumnText(stmtInfo, SVNWCDbSchema.NODES__Fields.symlink_target)));
                }
                if (info.hasField(NodeInfo.changelist) && haveActual) {
                    info.set(NodeInfo.changelist, getColumnText(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.changelist));
                }
                info.set(NodeInfo.originalReposId, opDepth == 0 ? SVNWCDb.INVALID_REPOS_ID : getColumnInt64(stmtInfo, SVNWCDbSchema.NODES__Fields.repos_id));
                if (info.hasField(NodeInfo.originalRevision)) {
                    info.set(NodeInfo.originalRevision, opDepth == 0 ? SVNWCDb.INVALID_REVNUM : getColumnRevNum(stmtInfo, SVNWCDbSchema.NODES__Fields.revision));
                }
                if (info.hasField(NodeInfo.originalReposRelpath)) {
                    info.set(NodeInfo.originalReposRelpath, opDepth == 0 ? null : SVNFileUtil.createFilePath(getColumnText(stmtInfo, SVNWCDbSchema.NODES__Fields.repos_path)));
                }
                if (info.hasField(NodeInfo.propsMod)) {
                    info.set(NodeInfo.propsMod, haveActual && !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.properties));
                }
                if (info.hasField(NodeInfo.hadProps)) {
                    byte[] props = getColumnBlob(stmtInfo, SVNWCDbSchema.NODES__Fields.properties);
                    info.set(NodeInfo.hadProps, props != null && props.length > 2); 
                }
                if (info.hasField(NodeInfo.conflicted)) {
                    if (haveActual) {
                        if (wcRoot.getFormat() == ISVNWCDb.WC_FORMAT_17) {
                            info.set(NodeInfo.conflicted, !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data) ||
                                    !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject) ||
                                    !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old) ||
                                    !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new) ||
                                    !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working));
                        } else {
                            info.set(NodeInfo.conflicted, !isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data));
                        }
                    } else {
                        info.set(NodeInfo.conflicted, false);
                    }
                }
                if (info.hasField(NodeInfo.lock)) {
                    if (opDepth == 0) {
                        final SVNSqlJetStatement stmtBaseLock = stmtInfo.getJoinedStatement(SVNWCDbSchema.LOCK.toString());
                        SVNWCDbLock lock = getLockFromColumns(stmtBaseLock, LOCK__Fields.lock_token, LOCK__Fields.lock_owner, LOCK__Fields.lock_comment, LOCK__Fields.lock_date);
                        info.set(NodeInfo.lock, lock);
                    } else {
                        info.set(NodeInfo.lock, null);
                    }
                }
                if (info.hasField(NodeInfo.haveWork)) {
                    info.set(NodeInfo.haveWork, opDepth != 0);
                }
                if (info.hasField(NodeInfo.opRoot)) {
                    info.set(NodeInfo.opRoot, opDepth > 0 && opDepth == SVNWCUtils.relpathDepth(localRelPath));
                }
                if (info.hasField(NodeInfo.haveBase) || info.hasField(NodeInfo.haveMoreWork)) {
                    if (info.hasField(NodeInfo.haveMoreWork)) {
                        info.set(NodeInfo.haveMoreWork, false);
                    }
                    while(opDepth != 0) {
                        haveInfo = stmtInfo.next();
                        if (!haveInfo) {
                            break;
                        }
                        opDepth = getColumnInt64(stmtInfo, SVNWCDbSchema.NODES__Fields.op_depth);
                        if (info.hasField(NodeInfo.haveMoreWork)) {
                            if (opDepth > 0) {
                                info.set(NodeInfo.haveMoreWork, true);
                            }
                            if (!info.hasField(NodeInfo.haveBase)) {
                                break;
                            }
                        }
                    }
                    if (info.hasField(NodeInfo.haveBase)) {
                        info.set(NodeInfo.haveBase, opDepth == 0);
                    }
                }
            } else if (haveActual) {
                if (isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data) &&
                        isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.tree_conflict_data) &&
                        isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.prop_reject) &&
                        isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old) &&
                        isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new) &&
                        isColumnNull(stmtActual, SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working)) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_CORRUPT, "Corrupt data for ''{0}''", wcRoot.getAbsPath(localRelPath));
                    SVNErrorManager.error(err, SVNLogType.WC);
                }
                info.set(NodeInfo.conflicted, true);
                if (info.hasField(NodeInfo.opRoot)) {
                    info.set(NodeInfo.opRoot, false);
                }
                if (info.hasField(NodeInfo.status)) {
                    info.set(NodeInfo.status, SVNWCDbStatus.Normal);
                }
                if (info.hasField(NodeInfo.kind)) {
                    info.set(NodeInfo.kind, SVNWCDbKind.Unknown);
                }
                if (info.hasField(NodeInfo.revision)) {
                    info.set(NodeInfo.revision, SVNWCDb.INVALID_REVNUM);
                }
                info.set(NodeInfo.reposId, SVNWCDb.INVALID_REPOS_ID);
                if (info.hasField(NodeInfo.changedRev)) {
                    info.set(NodeInfo.changedRev, SVNWCDb.INVALID_REVNUM);
                }
                if (info.hasField(NodeInfo.depth)) {
                    info.set(NodeInfo.depth, SVNDepth.UNKNOWN);
                }
                if (info.hasField(NodeInfo.originalRevision)) {
                    info.set(NodeInfo.originalRevision, SVNWCDb.INVALID_REVNUM);
                }
                if (info.hasField(NodeInfo.originalReposId)) {
                    info.set(NodeInfo.originalReposId, SVNWCDb.INVALID_REPOS_ID);
                }
                if (info.hasField(NodeInfo.changelist)) {
                    info.set(NodeInfo.changelist, stmtActual.getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.changelist));
                }
                if (info.hasField(NodeInfo.originalRevision))
                    info.set(NodeInfo.originalRevision, SVNWCDb.INVALID_REVNUM);
            } else {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_NOT_FOUND, "The node ''{0}'' was not found.", wcRoot.getAbsPath(localRelPath));
                SVNErrorManager.error(err, SVNLogType.WC);
            }
        } finally {            
            try {
                if (stmtInfo != null) {
                    stmtInfo.reset();
                }
            } catch (SVNException e) {} 
            try {
                if (stmtActual != null) {
                    stmtActual.reset();
                }
            } catch (SVNException e) {} 
        }
    
        return info;
    }
    
    public static void canonicalizeURLs(SVNWCDbRoot wcRoot, boolean updateExternalProperties, final SVNExternalsStore store, final boolean omitDefaultPort) throws SVNException {        
        final Map<String, Object> values = new HashMap<String, Object>();
        
        try {
            begingWriteTransaction(wcRoot);
            final ISqlJetTable repositoryTable = wcRoot.getSDb().getDb().getTable(SVNWCDbSchema.REPOSITORY.toString());
            ISqlJetCursor cursor = repositoryTable.open();
            while(!cursor.eof()) {
                final String oldUrl = cursor.getString(SVNWCDbSchema.REPOSITORY__Fields.root.toString());
                final SVNURL canonicalUrl = SVNUpdateClient16.canonicalizeURL(SVNURL.parseURIEncoded(oldUrl), omitDefaultPort);
                if (canonicalUrl != null) {
                    String newUrl = canonicalUrl.toString();
                    if (!oldUrl.equals(newUrl)) {
                        values.put(SVNWCDbSchema.REPOSITORY__Fields.root.toString(), newUrl);                
                        cursor.updateByFieldNames(values);
                    }
                }
                cursor.next();
            }
            cursor.close();
        } catch (SqlJetException e) {
            rollbackTransaction(wcRoot);
        } finally {
            commitTransaction(wcRoot);
        }
        
        if (updateExternalProperties) {
            final Map<File, SVNProperties> newPropertyValues = new HashMap<File, SVNProperties>(); 
    
            SvnWcDbProperties.readPropertiesRecursively(wcRoot, new File(""), SVNDepth.INFINITY, false, false, null, new ISvnObjectReceiver<SVNProperties>() {
                public void receive(SvnTarget target, SVNProperties object) throws SVNException {
                    String externalsPropertyValue = object.getStringValue(SVNProperty.EXTERNALS);
                    if (externalsPropertyValue != null) {
                        String newValue = SVNUpdateClient16.canonicalizeExtenrals(externalsPropertyValue, omitDefaultPort);
                        if (!externalsPropertyValue.equals(newValue)) {
                            object.put(SVNProperty.EXTERNALS, newValue);
                            newPropertyValues.put(target.getFile(), object);
                        }
                        if (store != null) {
                            store.addExternal(target.getFile(), externalsPropertyValue, newValue);
                        }
                    }
                }
            });
            for (File path : newPropertyValues.keySet()) {
                wcRoot.getDb().opSetProps(path, newPropertyValues.get(path), null, false, null);
            }
        }
    }
}
