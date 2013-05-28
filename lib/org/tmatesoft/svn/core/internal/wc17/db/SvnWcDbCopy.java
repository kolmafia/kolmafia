package org.tmatesoft.svn.core.internal.wc17.db;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.schema.SqlJetConflictAction;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb.InsertWorking;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb.ReposInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.AdditionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.DeletionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.ACTUAL_NODE__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbStatements;
import org.tmatesoft.svn.core.wc2.SvnChecksum;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.util.*;

import static org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil.*;

public class SvnWcDbCopy extends SvnWcDbShared {
    
    private enum CopyInfo {
        copyFromId, 
        copyFromRelpath,
        copyFromRev,
        status,
        kind,
        opRoot,
        haveWork
    }

    public static void copyFile(SVNWCDbDir pdh, File localRelpath,
            SVNProperties props, long changedRev, SVNDate changedDate,
            String changedAuthor, File originalReposRelPath,
            SVNURL originalRootUrl, String originalUuid, long originalRevision,
            SvnChecksum checksum, SVNSkel conflict, SVNSkel workItems) throws SVNException {

        InsertWorking iw = pdh.getWCRoot().getDb().new InsertWorking();
        iw.status = SVNWCDbStatus.Normal;
        iw.kind = SVNWCDbKind.File;
        iw.props = props;
        iw.changedAuthor = changedAuthor;
        iw.changedDate = changedDate;
        iw.changedRev = changedRev;
        
        if (originalRootUrl != null) {
            long reposId = pdh.getWCRoot().getDb().createReposId(pdh.getWCRoot().getSDb(), originalRootUrl, originalUuid);
            iw.originalReposId = reposId;
            iw.originalReposRelPath = originalReposRelPath;
            iw.originalRevision = originalRevision;
        }
        long[] depths = getOpDepthForCopy(pdh.getWCRoot(), localRelpath, iw.originalReposId, originalReposRelPath, originalRevision);
        iw.opDepth = depths[0];
        iw.notPresentOpDepth = depths[1];
        iw.checksum = checksum;
        iw.workItems = workItems;

        iw.wcId = pdh.getWCRoot().getWcId();
        iw.localRelpath = localRelpath;
        
        pdh.getWCRoot().getSDb().runTransaction(iw);
        pdh.flushEntries(pdh.getWCRoot().getAbsPath());
    }

    public static void copyDir(SVNWCDbDir pdh, File localRelpath,
            SVNProperties props, long changedRev, SVNDate changedDate, String changedAuthor, File originalReposRelPath, SVNURL originalRootUrl, String originalUuid,
            long originalRevision, List<File> children, SVNDepth depth, SVNSkel conflict, SVNSkel workItems) throws SVNException {

        InsertWorking iw = pdh.getWCRoot().getDb().new InsertWorking();
        iw.status = SVNWCDbStatus.Normal;
        iw.kind = SVNWCDbKind.Dir;
        iw.props = props;
        iw.changedAuthor = changedAuthor;
        iw.changedDate = changedDate;
        iw.changedRev = changedRev;
        
        if (originalRootUrl != null) {
            long reposId = pdh.getWCRoot().getDb().createReposId(pdh.getWCRoot().getSDb(), originalRootUrl, originalUuid);
            iw.originalReposId = reposId;
            iw.originalReposRelPath = originalReposRelPath;
            iw.originalRevision = originalRevision;
        }
        long[] depths = getOpDepthForCopy(pdh.getWCRoot(), localRelpath, iw.originalReposId, originalReposRelPath, originalRevision);
        iw.opDepth = depths[0];
        iw.notPresentOpDepth = depths[1];
        iw.children = children;
        iw.depth = depth;
        iw.workItems = workItems;

        iw.wcId = pdh.getWCRoot().getWcId();
        iw.localRelpath = localRelpath;
        
        pdh.getWCRoot().getSDb().runTransaction(iw);
        pdh.flushEntries(pdh.getWCRoot().getAbsPath());
    }

    private static void copyShadowedLayer(SVNWCDbDir srcPdh, File srcRelpath, long srcOpDepth, 
            SVNWCDbDir dstPdh, File dstRelpath, long dstOpDepth, long delOpDepth, long reposId, File reposRelPath, long revision) throws SVNException {
        Structure<NodeInfo> depthInfo = null;
        try {
            depthInfo = SvnWcDbReader.getDepthInfo(srcPdh.getWCRoot(), srcRelpath, srcOpDepth, 
                    NodeInfo.status, NodeInfo.kind, NodeInfo.revision, NodeInfo.reposRelPath, NodeInfo.reposId);
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            return;
        }
        SVNWCDbStatus status = depthInfo.get(NodeInfo.status);
        if (srcOpDepth == 0) {
            long nodeRevision = depthInfo.lng(NodeInfo.revision);
            long nodeReposId = depthInfo.lng(NodeInfo.reposId);
            File nodeReposRelPath = depthInfo.get(NodeInfo.reposRelPath);
            
            if (status == SVNWCDbStatus.NotPresent
                    || status == SVNWCDbStatus.Excluded
                    || status == SVNWCDbStatus.ServerExcluded
                    || nodeRevision != revision
                    || nodeReposId != reposId
                    || !nodeReposRelPath.equals(reposRelPath)) {
                
                ReposInfo reposInfo = srcPdh.getWCRoot().getDb().fetchReposInfo(srcPdh.getWCRoot().getSDb(), nodeReposId);
                nodeReposId = dstPdh.getWCRoot().getDb().createReposId(dstPdh.getWCRoot().getSDb(), 
                        SVNURL.parseURIEncoded(reposInfo.reposRootUrl), reposInfo.reposUuid);
                
                InsertWorking iw = dstPdh.getWCRoot().getDb().new InsertWorking();
                iw.opDepth = dstOpDepth;
                if (status != SVNWCDbStatus.Excluded) {
                    iw.status = SVNWCDbStatus.NotPresent;
                } else {
                    iw.status = SVNWCDbStatus.Excluded;
                }
                iw.kind = depthInfo.get(NodeInfo.kind);
                iw.originalReposId = nodeReposId;
                iw.originalRevision = nodeRevision;
                iw.originalReposRelPath = nodeReposRelPath;
                
                iw.changedRev = -1;
                iw.depth = SVNDepth.INFINITY;

                iw.wcId = dstPdh.getWCRoot().getWcId();
                iw.localRelpath = dstRelpath;
                
                dstPdh.getWCRoot().getSDb().runTransaction(iw);

                return;
            }
        }
        SVNWCDbStatus dstPresence = null;
        switch (status) {
        case Normal:
        case Added:
        case MovedHere:
        case Copied:
            dstPresence = SVNWCDbStatus.Normal;
            break;
        case Deleted:
        case NotPresent:
            dstPresence = SVNWCDbStatus.NotPresent;
            break;
        case Excluded:
            dstPresence = SVNWCDbStatus.Excluded;
            break;
        case ServerExcluded:
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Cannot copy ''{0}'' excluded by server", srcPdh.getWCRoot().getAbsPath(srcRelpath));
            SVNErrorManager.error(err, SVNLogType.WC);
            break;
        default:
            SVNErrorMessage err2 = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Cannot handle status of ''{0}''", srcPdh.getWCRoot().getAbsPath(srcRelpath));
            SVNErrorManager.error(err2, SVNLogType.WC);
        }
        
        if (dstPresence == SVNWCDbStatus.Normal && srcPdh.getWCRoot().getSDb() == dstPdh.getWCRoot().getSDb()) {
            SVNSqlJetStatement stmt;
            if (srcOpDepth > 0) {
                stmt = new InsertWorkingNodeCopy(srcPdh.getWCRoot().getSDb(), srcOpDepth);
            } else {
                stmt = new InsertWorkingNodeCopy(srcPdh.getWCRoot().getSDb(), 0);                
            }
            try {
                stmt.bindf("issist",
                        srcPdh.getWCRoot().getWcId(),
                        srcRelpath,
                        dstRelpath,
                        dstOpDepth,
                        SVNFileUtil.getFileDir(dstRelpath),
                        SvnWcDbStatementUtil.getPresenceText(dstPresence));
                stmt.done();
            } finally {
                stmt.reset();
            }
            
            InsertWorking iw = dstPdh.getWCRoot().getDb().new InsertWorking();
            iw.opDepth = delOpDepth;
            iw.status = SVNWCDbStatus.BaseDeleted;
            iw.kind = depthInfo.get(NodeInfo.kind);
            iw.changedRev = -1;
            iw.depth = SVNDepth.INFINITY;
            iw.wcId = dstPdh.getWCRoot().getWcId();
            iw.localRelpath = dstRelpath;
            
            dstPdh.getWCRoot().getSDb().runTransaction(iw);
        } else {
            if (dstPresence == SVNWCDbStatus.Normal) {
                dstPresence = SVNWCDbStatus.NotPresent;
            }

            InsertWorking iw = dstPdh.getWCRoot().getDb().new InsertWorking();
            iw.opDepth = dstOpDepth;
            iw.status = dstPresence;
            iw.kind = depthInfo.get(NodeInfo.kind);
            iw.changedRev = -1;
            iw.depth = SVNDepth.INFINITY;
            iw.wcId = dstPdh.getWCRoot().getWcId();
            iw.localRelpath = dstRelpath;
            
            dstPdh.getWCRoot().getSDb().runTransaction(iw);            
        }
        List<String> children = srcPdh.getWCRoot().getDb().gatherRepoChildren(srcPdh, srcRelpath, srcOpDepth);
        for (String name : children) {
            File srcChildRelpath = SVNFileUtil.createFilePath(srcRelpath, name);
            File dstChildRelpath = SVNFileUtil.createFilePath(dstRelpath, name);
            File childReposRelPath = null;
            if (reposRelPath != null) {
                childReposRelPath = SVNFileUtil.createFilePath(reposRelPath, name);
            }
            copyShadowedLayer(srcPdh, srcChildRelpath, srcOpDepth, dstPdh, dstChildRelpath, dstOpDepth, delOpDepth, reposId, childReposRelPath, revision);
        }
    }

    public static void copyShadowedLayer(SVNWCDbDir srcPdh, File localSrcRelpath, SVNWCDbDir dstPdh, File localDstRelpath) throws SVNException {
        boolean dstLocked = false;
        begingWriteTransaction(srcPdh.getWCRoot());
        try {
            if (srcPdh.getWCRoot().getSDb() != dstPdh.getWCRoot().getSDb()) {
                begingWriteTransaction(dstPdh.getWCRoot());
                dstLocked = true;
            }
            File srcParentRelPath = SVNFileUtil.getFileDir(localSrcRelpath);
            File dstParentRelPath = SVNFileUtil.getFileDir(localDstRelpath);
            
            long srcOpDepth = getOpDepthOf(srcPdh.getWCRoot(), srcParentRelPath);
            long dstOpDepth = getOpDepthOf(dstPdh.getWCRoot(), dstParentRelPath);
            long delOpDepth = SVNWCUtils.relpathDepth(localDstRelpath);
            
            Structure<NodeInfo> depthInfo = SvnWcDbReader.getDepthInfo(srcPdh.getWCRoot(), srcParentRelPath, srcOpDepth, 
                    NodeInfo.revision, NodeInfo.reposRelPath, NodeInfo.reposId);
            File reposRelpath = depthInfo.get(NodeInfo.reposRelPath);
            if (reposRelpath == null) {
                return;
            }
            reposRelpath = SVNFileUtil.createFilePath(reposRelpath, SVNFileUtil.getFileName(localSrcRelpath));
            copyShadowedLayer(srcPdh, localSrcRelpath, srcOpDepth, dstPdh, localDstRelpath, dstOpDepth, delOpDepth, 
                    depthInfo.lng(NodeInfo.reposId), reposRelpath, depthInfo.lng(NodeInfo.revision));
            
            depthInfo.release();
        } catch (SVNException e) {
            try {
                rollbackTransaction(srcPdh.getWCRoot());
            } finally {
                if (dstLocked) {
                    rollbackTransaction(dstPdh.getWCRoot());
                }
            }
        } finally {
            try {
                commitTransaction(srcPdh.getWCRoot());
            } finally {
                if (dstLocked) {
                    commitTransaction(dstPdh.getWCRoot());
                }
            }
        }
    }

    public static void copy(SVNWCDbDir srcPdh, File localSrcRelpath, SVNWCDbDir dstPdh, File localDstRelpath, SVNSkel workItems) throws SVNException {
        boolean dstLocked = false;
        begingWriteTransaction(srcPdh.getWCRoot());
        try {
            if (srcPdh.getWCRoot().getSDb() != dstPdh.getWCRoot().getSDb()) {
                begingWriteTransaction(dstPdh.getWCRoot());
                dstLocked = true;
            }
            doCopy(srcPdh, localSrcRelpath, dstPdh, localDstRelpath, workItems);
        } catch (SVNException e) {
            try {
                rollbackTransaction(srcPdh.getWCRoot());
            } finally {
                if (dstLocked) {
                    rollbackTransaction(dstPdh.getWCRoot());
                }
            }
        } finally {
            try {
                commitTransaction(srcPdh.getWCRoot());
            } finally {
                if (dstLocked) {
                    commitTransaction(dstPdh.getWCRoot());
                }
            }
        }
        
    }

    private static void doCopy(SVNWCDbDir srcPdh, File localSrcRelpath, SVNWCDbDir dstPdh, File localDstRelpath, SVNSkel workItems) throws SVNException {
        Structure<CopyInfo> copyInfo = getCopyInfo(srcPdh.getWCRoot(), localSrcRelpath);
        long[] dstOpDepths = getOpDepthForCopy(dstPdh.getWCRoot(), localDstRelpath, 
                copyInfo.lng(CopyInfo.copyFromId), copyInfo.<File>get(CopyInfo.copyFromRelpath), copyInfo.lng(CopyInfo.copyFromRev));
        
        SVNWCDbStatus status = copyInfo.get(CopyInfo.status);
        SVNWCDbStatus dstPresence = null;
        boolean opRoot = copyInfo.is(CopyInfo.opRoot);
        
        switch (status) {
        case Normal:
        case Added:
        case MovedHere:
        case Copied:
            dstPresence = SVNWCDbStatus.Normal;
            break;
        case Deleted:
            if (opRoot) {
                try {
                    Structure<NodeInfo> dstInfo = SvnWcDbReader.readInfo(dstPdh.getWCRoot(), localDstRelpath, NodeInfo.status);
                    SVNWCDbStatus dstStatus = dstInfo.get(NodeInfo.status);
                    dstInfo.release();
                    if (dstStatus == SVNWCDbStatus.Deleted) {
                        dstPdh.getWCRoot().getDb().addWorkQueue(dstPdh.getWCRoot().getAbsPath(), workItems);
                        return;
                    }
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                        throw e;
                    }
                }
            }
        case NotPresent:
        case Excluded:
            if (dstOpDepths[1] > 0) {
                dstOpDepths[0] = dstOpDepths[1];
                dstOpDepths[1] = -1;
            }
            if (status == SVNWCDbStatus.Excluded) {
                dstPresence = SVNWCDbStatus.Excluded;
            } else {
                dstPresence = SVNWCDbStatus.NotPresent;
            }
            break;
        case ServerExcluded:
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Cannot copy ''{0}'' excluded by server", srcPdh.getWCRoot().getAbsPath(localSrcRelpath));
            SVNErrorManager.error(err, SVNLogType.WC);
        default:
            SVNErrorMessage err1 = SVNErrorMessage.create(SVNErrorCode.WC_PATH_UNEXPECTED_STATUS, "Cannot handle status of ''{0}''", srcPdh.getWCRoot().getAbsPath(localSrcRelpath));
            SVNErrorManager.error(err1, SVNLogType.WC);
        }
        
        SVNWCDbKind kind = copyInfo.get(CopyInfo.kind);
        List<String> children = null;
        if (kind == SVNWCDbKind.Dir) {
            long opDepth = getOpDepthOf(srcPdh.getWCRoot(), localSrcRelpath);
            children = srcPdh.getWCRoot().getDb().gatherRepoChildren(srcPdh, localSrcRelpath, opDepth);
        }
        
        if (srcPdh.getWCRoot() == dstPdh.getWCRoot()) {
            File dstParentRelpath = SVNFileUtil.getFileDir(localDstRelpath);
            SVNSqlJetStatement stmt = new InsertWorkingNodeCopy(srcPdh.getWCRoot().getSDb(), !copyInfo.is(CopyInfo.haveWork));
            try {
                stmt.bindf("issist", srcPdh.getWCRoot().getWcId(), localSrcRelpath, localDstRelpath, dstOpDepths[0], dstParentRelpath,
                        SvnWcDbStatementUtil.getPresenceText(dstPresence));
                stmt.done();
            } finally {
                stmt.reset();
            }
            
            copyActual(srcPdh, localSrcRelpath, dstPdh, localDstRelpath);
            
            if (dstOpDepths[1] > 0) {
                stmt = srcPdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.INSERT_NODE);
                try {
                    stmt.bindf("isisisrtnt",
                            srcPdh.getWCRoot().getWcId(),
                            localDstRelpath,
                            dstOpDepths[1],
                            dstParentRelpath,
                            copyInfo.lng(CopyInfo.copyFromId),
                            copyInfo.get(CopyInfo.copyFromRelpath),
                            copyInfo.lng(CopyInfo.copyFromRev),
                            SvnWcDbStatementUtil.getPresenceText(SVNWCDbStatus.NotPresent),
                            SvnWcDbStatementUtil.getKindText(kind));
                    stmt.done();
                } finally {
                    stmt.reset();
                }
            }
            if (kind == SVNWCDbKind.Dir && dstPresence == SVNWCDbStatus.Normal) {
                List<File> fileChildren = new LinkedList<File>();
                for (String childName : children) {
                    fileChildren.add(new File(childName));
                }
                srcPdh.getWCRoot().getDb().insertIncompleteChildren(srcPdh.getWCRoot().getSDb(), srcPdh.getWCRoot().getWcId(), 
                        localDstRelpath, copyInfo.lng(CopyInfo.copyFromRev), fileChildren, dstOpDepths[0]);
            }
        } else {
            crossDbCopy(srcPdh, localSrcRelpath, dstPdh, localDstRelpath, dstPresence, dstOpDepths[0], dstOpDepths[1], kind, children, 
                    copyInfo.lng(CopyInfo.copyFromId), copyInfo.<File>get(CopyInfo.copyFromRelpath), copyInfo.lng(CopyInfo.copyFromRev));
        }
        dstPdh.getWCRoot().getDb().addWorkQueue(dstPdh.getWCRoot().getAbsPath(), workItems);
    }
    
    private static void crossDbCopy(SVNWCDbDir srcPdh, File localSrcRelpath,
            SVNWCDbDir dstPdh, File localDstRelpath, SVNWCDbStatus dstPresence,
            long dstOpDepth, long dstNpOpDepth, SVNWCDbKind kind, List<String> children, long copyFromId,
            File copyFromRelpath, long copyFromRev) throws SVNException {
        Structure<NodeInfo> nodeInfo = 
                SvnWcDbShared.readInfo(srcPdh.getWCRoot(), localSrcRelpath, NodeInfo.changedRev, NodeInfo.changedDate, NodeInfo.changedAuthor,
                        NodeInfo.depth, NodeInfo.checksum);
        SVNProperties properties = SvnWcDbProperties.readPristineProperties(srcPdh.getWCRoot(), localSrcRelpath);
        
        InsertWorking iw = dstPdh.getWCRoot().getDb().new InsertWorking();
        iw.status = dstPresence;
        iw.kind = kind;
        iw.props = properties;
        iw.changedRev = nodeInfo.lng(NodeInfo.changedRev);
        iw.changedDate = nodeInfo.get(NodeInfo.changedDate);
        iw.changedAuthor = nodeInfo.text(NodeInfo.changedAuthor);
        iw.opDepth = dstOpDepth;
        iw.checksum = nodeInfo.get(NodeInfo.checksum);
        List<File> childrenAsFiles = null;
        if (children != null) {
            childrenAsFiles = new ArrayList<File>();
            for (String name : children) {
                childrenAsFiles.add(new File(name));
            }
        }
        iw.children = childrenAsFiles;
        iw.depth = nodeInfo.get(NodeInfo.depth);
        iw.notPresentOpDepth = dstNpOpDepth;

        iw.originalReposId = copyFromId;
        iw.originalRevision = copyFromRev;
        iw.originalReposRelPath = copyFromRelpath;
        
        iw.wcId = dstPdh.getWCRoot().getWcId();
        iw.localRelpath = localDstRelpath;
        
        dstPdh.getWCRoot().getSDb().runTransaction(iw);
        
        copyActual(srcPdh, localSrcRelpath, dstPdh, localDstRelpath);
        nodeInfo.release();
    }

    private static void copyActual(SVNWCDbDir srcPdh, File localSrcRelpath, SVNWCDbDir dstPdh, File localDstRelpath) throws SVNException {
        SVNSqlJetStatement stmt = srcPdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.SELECT_ACTUAL_NODE);
        stmt.bindf("is", srcPdh.getWCRoot().getWcId(), localSrcRelpath);
        try {
            if (stmt.next()) {
                String changelist = getColumnText(stmt, ACTUAL_NODE__Fields.changelist);
                byte[] properties = getColumnBlob(stmt, ACTUAL_NODE__Fields.properties);
                
                if (changelist != null || properties != null) {
                    reset(stmt);
                    stmt = dstPdh.getWCRoot().getSDb().getStatement(SVNWCDbStatements.INSERT_ACTUAL_NODE);
                    try {
                        stmt.bindf("issbssssss",
                                dstPdh.getWCRoot().getWcId(),
                                localDstRelpath,
                                SVNFileUtil.getFileDir(localDstRelpath),
                                properties,
                                null, null, null,
                                null, changelist, null);
                        stmt.done();
                    } finally {
                        stmt.reset();
                    }
                }
            }
        } finally {
            reset(stmt);
        }
    }

    private static Structure<CopyInfo> getCopyInfo(SVNWCDbRoot wcRoot, File localRelPath) throws SVNException {
        Structure<CopyInfo> result = Structure.obtain(CopyInfo.class);
        result.set(CopyInfo.haveWork, false);
        
        Structure<NodeInfo> nodeInfo = SvnWcDbReader.readInfo(wcRoot, localRelPath, NodeInfo.status, NodeInfo.kind, NodeInfo.revision, NodeInfo.reposRelPath,
                NodeInfo.reposId, NodeInfo.opRoot, NodeInfo.haveWork);

        nodeInfo.from(NodeInfo.kind, NodeInfo.status, NodeInfo.reposId, NodeInfo.haveWork, NodeInfo.opRoot)
            .into(result, CopyInfo.kind, CopyInfo.status, CopyInfo.copyFromId, CopyInfo.haveWork, CopyInfo.opRoot);
        SVNWCDbStatus status = nodeInfo.get(NodeInfo.status);
        File reposRelpath = nodeInfo.get(NodeInfo.reposRelPath);
        long revision = nodeInfo.lng(NodeInfo.revision);
        
        nodeInfo.release();

        if (status == SVNWCDbStatus.Excluded) {
            File parentRelpath = SVNFileUtil.getFileDir(localRelPath);
            String name = SVNFileUtil.getFileName(localRelPath);
            
            Structure<CopyInfo> parentCopyInfo = getCopyInfo(wcRoot, parentRelpath);
            parentCopyInfo.from(CopyInfo.copyFromId, CopyInfo.copyFromRev)
                .into(result, CopyInfo.copyFromId, CopyInfo.copyFromRev);
            
            if (parentCopyInfo.get(CopyInfo.copyFromRelpath) != null) {
                result.set(CopyInfo.copyFromRelpath, 
                        SVNFileUtil.createFilePath(parentCopyInfo.<File>get(CopyInfo.copyFromRelpath), name));
            }
            
            parentCopyInfo.release();
        } else if (status == SVNWCDbStatus.Added) {
            Structure<AdditionInfo> additionInfo = scanAddition(wcRoot, localRelPath, AdditionInfo.opRootRelPath, 
                    AdditionInfo.originalReposRelPath, AdditionInfo.originalReposId, AdditionInfo.originalRevision);
            additionInfo.from(AdditionInfo.originalReposRelPath, AdditionInfo.originalReposId, AdditionInfo.originalRevision)
                .into(result, CopyInfo.copyFromRelpath, CopyInfo.copyFromId, CopyInfo.copyFromRev);
            
            if (additionInfo.get(AdditionInfo.originalReposRelPath) != null) {
                File opRootRelPath = additionInfo.get(AdditionInfo.opRootRelPath);
                File copyFromRelPath = additionInfo.get(AdditionInfo.originalReposRelPath); 
                File relPath = SVNFileUtil.createFilePath(copyFromRelPath, SVNWCUtils.skipAncestor(opRootRelPath, localRelPath));
                result.set(CopyInfo.copyFromRelpath, relPath);
            }
            
            additionInfo.release();
        } else if (status == SVNWCDbStatus.Deleted) {
            Structure<DeletionInfo> deletionInfo = scanDeletion(wcRoot, localRelPath);
            if (deletionInfo.get(DeletionInfo.workDelRelPath) != null) {
                File parentDelRelpath = SVNFileUtil.getFileDir(deletionInfo.<File>get(DeletionInfo.workDelRelPath));

                Structure<AdditionInfo> additionInfo = scanAddition(wcRoot, parentDelRelpath, AdditionInfo.opRootRelPath, 
                        AdditionInfo.originalReposRelPath, AdditionInfo.originalReposId, AdditionInfo.originalRevision);
                
                additionInfo.from(AdditionInfo.originalReposRelPath, AdditionInfo.originalReposId, AdditionInfo.originalRevision)
                    .into(result, CopyInfo.copyFromRelpath, CopyInfo.copyFromId, CopyInfo.copyFromRev);
                File opRootRelPath = additionInfo.get(AdditionInfo.opRootRelPath);
                File copyFromRelPath = additionInfo.get(AdditionInfo.originalReposRelPath); 
                File relPath = SVNFileUtil.createFilePath(copyFromRelPath, SVNWCUtils.skipAncestor(opRootRelPath, localRelPath));
                result.set(CopyInfo.copyFromRelpath, relPath);

                additionInfo.release();
            } else if (deletionInfo.get(DeletionInfo.baseDelRelPath) != null) {
                Structure<NodeInfo> baseInfo = getDepthInfo(wcRoot, localRelPath, 0, NodeInfo.revision, NodeInfo.reposRelPath, NodeInfo.reposId);
                baseInfo.from(NodeInfo.revision, NodeInfo.reposRelPath, NodeInfo.reposId).
                    into(result, CopyInfo.copyFromRev, CopyInfo.copyFromRelpath, CopyInfo.copyFromId);
                baseInfo.release();
            }
            deletionInfo.release();
        } else {
            result.set(CopyInfo.copyFromRelpath, reposRelpath);
            result.set(CopyInfo.copyFromRev, revision);
        }
        
        return result;
    }
    
    private static long[] getOpDepthForCopy(SVNWCDbRoot wcRoot, File localRelpath, long copyFromReposId, File copyFromRelpath, long copyFromRevision) throws SVNException {
        long[] result = new long[] {SVNWCUtils.relpathDepth(localRelpath), -1};
        if (copyFromRelpath == null) {
            return result;
        }
        
        long minOpDepth = 1;
        long incompleteOpDepth = -1;
        
        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_WORKING_NODE);
        try {
            bindf(stmt, "is", wcRoot.getWcId(), localRelpath);
            if (stmt.next()) {
                SVNWCDbStatus status = getColumnPresence(stmt);
                minOpDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
                if (status == SVNWCDbStatus.Incomplete) {
                    incompleteOpDepth = minOpDepth;
                }
            }
        } finally {
            reset(stmt);
        }
        File parentRelpath = SVNFileUtil.getFileDir(localRelpath);
        bindf(stmt, "is", wcRoot.getWcId(), parentRelpath);
        if (stmt.next()) {
            long parentOpDepth = getColumnInt64(stmt, NODES__Fields.op_depth);
            if (parentOpDepth < minOpDepth) {
                reset(stmt);
                return result;
            }
            if (incompleteOpDepth < 0 || incompleteOpDepth == parentOpDepth) {
                long parentCopyFromReposId = getColumnInt64(stmt, NODES__Fields.repos_id);
                File parentCopyFromRelpath = getColumnPath(stmt, NODES__Fields.repos_path);
                long parentCopyFromRevision = getColumnInt64(stmt, NODES__Fields.revision);
                if (parentCopyFromReposId == copyFromReposId) {
                    if (copyFromRevision == parentCopyFromRevision &&
                            copyFromRelpath.equals(SVNFileUtil.createFilePath(parentCopyFromRelpath, localRelpath.getName()))) {
                        result[0] = parentOpDepth;
                    } else if (incompleteOpDepth > 0) {
                        result[1] = incompleteOpDepth;
                    }
                }
            }
        }
        reset(stmt);
        return result;
    }
    
    private static long getOpDepthOf(SVNWCDbRoot wcRoot, File localRelpath) throws SVNException {
        SVNSqlJetStatement stmt = wcRoot.getSDb().getStatement(SVNWCDbStatements.SELECT_NODE_INFO);
        bindf(stmt, "is", wcRoot.getWcId(), localRelpath);
        try {
            if (stmt.next()) {
                return getColumnInt64(stmt, NODES__Fields.op_depth); 
            }
        } finally {
            reset(stmt);
        }        
        return 0;
        
    }

    private static class InsertWorkingNodeCopy extends SVNSqlJetInsertStatement {

        private SelectNodeToCopy select;

        public InsertWorkingNodeCopy(SVNSqlJetDb sDb, boolean base) throws SVNException {
            this(sDb, base ? 0 : -1);
        }

        public InsertWorkingNodeCopy(SVNSqlJetDb sDb, long depth) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES, SqlJetConflictAction.REPLACE);
            select = new SelectNodeToCopy(sDb, depth);
        }

        @Override
        protected Map<String, Object> getInsertValues() throws SVNException {
            // run select once and return values.
            select.bindf("is", getBind(1), getBind(2));
            try {
                if (select.next()) {
                    Map<String, Object> values = new HashMap<String, Object>();
                    values.put(NODES__Fields.wc_id.toString(), select.getColumn(NODES__Fields.wc_id));
                    values.put(NODES__Fields.local_relpath.toString(), getBind(3));
                    values.put(NODES__Fields.op_depth.toString(), getBind(4));
                    values.put(NODES__Fields.parent_relpath.toString(), getBind(5));
                    values.put(NODES__Fields.repos_id.toString(), select.getColumn(NODES__Fields.repos_id));
                    values.put(NODES__Fields.repos_path.toString(), select.getColumn(NODES__Fields.repos_path));
                    values.put(NODES__Fields.revision.toString(), select.getColumn(NODES__Fields.revision));
                    values.put(NODES__Fields.presence.toString(), getBind(6));
                    values.put(NODES__Fields.depth.toString(), select.getColumn(NODES__Fields.depth));
                    values.put(NODES__Fields.kind.toString(), select.getColumn(NODES__Fields.kind));
                    
                    values.put(NODES__Fields.changed_revision.toString(), select.getColumn(NODES__Fields.changed_revision));
                    values.put(NODES__Fields.changed_date.toString(), select.getColumn(NODES__Fields.changed_date));
                    values.put(NODES__Fields.changed_author.toString(), select.getColumn(NODES__Fields.changed_author));
                    values.put(NODES__Fields.checksum.toString(), select.getColumn(NODES__Fields.checksum));
                    values.put(NODES__Fields.properties.toString(), select.getColumn(NODES__Fields.properties));
                    values.put(NODES__Fields.translated_size.toString(), select.getColumn(NODES__Fields.translated_size));
                    values.put(NODES__Fields.last_mod_time.toString(), select.getColumn(NODES__Fields.last_mod_time));
                    values.put(NODES__Fields.symlink_target.toString(), select.getColumn(NODES__Fields.symlink_target));
                    return values;
                }                
            } finally {
                select.reset();
            }
            return null;
        }
    }

    /**
     * SELECT wc_id, ?3 (local_relpath), ?4 (op_depth), ?5 (parent_relpath),
     * repos_id, repos_path, revision, ?6 (presence), depth,
     * kind, changed_revision, changed_date, changed_author, checksum, properties,
     * translated_size, last_mod_time, symlink_target
     * FROM nodes
     * 
     * WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth > 0
     * ORDER BY op_depth DESC
     * LIMIT 1
     * 
     * or for base:
     * 
     * FROM nodes
     * WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth = 0

     * @author alex
     *
     */
    private static class SelectNodeToCopy extends SVNSqlJetSelectStatement {

        private long limit;
        private long depth;

        public SelectNodeToCopy(SVNSqlJetDb sDb, long depth) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES);
            this.depth = depth;
        }

        @Override
        protected Object[] getWhere() throws SVNException {
            if (depth >= 0) {
                return new Object[] {getBind(1), getBind(2), depth};
            } 
            return super.getWhere();
        }
        
        @Override
        protected boolean isFilterPassed() throws SVNException {
            limit++;
            return super.isFilterPassed() && limit == 1;
        }

        @Override
        protected ISqlJetCursor openCursor() throws SVNException {
            if (depth == 0) {
                return super.openCursor();
            }
            try {
                return super.openCursor().reverse();
            } catch (SqlJetException e) {
                SVNSqlJetDb.createSqlJetError(e);
            }
            return null;
        }
        
        
    }
}
