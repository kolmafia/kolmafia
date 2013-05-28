package org.tmatesoft.svn.core.internal.wc17.db;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb.DirParsedInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.AdditionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.DeletionInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.NodeInfo;
import org.tmatesoft.svn.core.internal.wc17.db.StructureFields.RepositoryInfo;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.LOCK__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SvnWcDbRelocate extends SvnWcDbShared {
    
    public static interface ISvnRelocateValidator {
        void validateRelocation(String uuid, SVNURL newUrl, SVNURL newRepositoryRoot) throws SVNException;
    }
    
    public static void relocate(SVNWCContext context, File localAbspath, SVNURL from, SVNURL to, ISvnRelocateValidator validator) throws SVNException {
        if (!context.getDb().isWCRoot(localAbspath)) {
            try {
                File wcRoot = context.getDb().getWCRoot(localAbspath);
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_OP_ON_CWD, "Cannot relocate ''{0}'' as it is not the root of the working copy; " +
                		"try relocating ''{1}'' instead", localAbspath, wcRoot);
                SVNErrorManager.error(err, SVNLogType.WC);
            } catch (SVNException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_OP_ON_CWD, "Cannot relocate ''{0}'' as it is not the root of the working copy", localAbspath);
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            return;
        }
        Structure<NodeInfo> info = context.getDb().readInfo(localAbspath, NodeInfo.kind, NodeInfo.reposRelPath, NodeInfo.reposRootUrl, NodeInfo.reposUuid);
        if (info.<SVNWCDbKind>get(NodeInfo.kind) != SVNWCDbKind.Dir) {
            info.release();
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CLIENT_INVALID_RELOCATION, "Cannot relocate a single file");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        File relPath = info.<File>get(NodeInfo.reposRelPath);
        SVNURL oldUrl = info.<SVNURL>get(NodeInfo.reposRootUrl);
        String uuid = info.text(NodeInfo.reposUuid);
        info.release();

        oldUrl = SVNWCUtils.join(oldUrl, relPath);

        if (from != null && !oldUrl.toString().startsWith(from.toString())) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_RELOCATION, "Invalid source URL prefix: ''{0}'' (does not " +
            		"overlap target''s URL ''{1}''", from, oldUrl);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        SVNURL newUrl;
        if (from == null || oldUrl.equals(from)) {
            newUrl = to;
        } else {
            newUrl = SVNURL.parseURIEncoded(to.toString() + oldUrl.toString().substring(from.toString().length()));
        }
        String relPathStr = SVNFileUtil.getFilePath(relPath);
        String newReposRootPath = newUrl.getPath();
        if (!newReposRootPath.endsWith(relPathStr)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_RELOCATION, 
                    "Invalid relocation destination: ''{0}'' (does not " +
                    "point to target)", newUrl);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        newReposRootPath = newReposRootPath.substring(0, newReposRootPath.length() - relPathStr.length());
        SVNURL newReposRoot = newUrl.setPath(newReposRootPath, false);

        if (validator != null) {
            validator.validateRelocation(uuid, newUrl, newReposRoot);
        }
        relocate((SVNWCDb) context.getDb(), localAbspath, newReposRoot);
    }
    
    private static void relocate(SVNWCDb db, File localAbspath, SVNURL repositoryRootUrl) throws SVNException {
        DirParsedInfo dirInfo = db.obtainWcRoot(localAbspath);
        File localRelpath = dirInfo.localRelPath;
        SVNWCDbRoot root = dirInfo.wcDbDir.getWCRoot();
        
        Structure<NodeInfo> nodeInfo = SvnWcDbShared.readInfo(root, localRelpath, NodeInfo.status, NodeInfo.reposId, NodeInfo.haveBase);
        SVNWCDbStatus status = nodeInfo.<SVNWCDbStatus>get(NodeInfo.status);
        long oldReposId = nodeInfo.lng(NodeInfo.reposId);
        boolean haveBase = nodeInfo.is(NodeInfo.haveBase);
        nodeInfo.release();
        

        if (status == SVNWCDbStatus.Excluded) {
            File parentRelPath = SVNFileUtil.getFileDir(localRelpath);
            nodeInfo = SvnWcDbShared.readInfo(root, localRelpath, NodeInfo.status, NodeInfo.reposId);
            status = nodeInfo.<SVNWCDbStatus>get(NodeInfo.status);
            oldReposId = nodeInfo.lng(NodeInfo.reposId);
            localRelpath = parentRelPath;
            
            nodeInfo.release();
        }
        if (oldReposId == SVNWCContext.INVALID_REVNUM) {
            if (status == SVNWCDbStatus.Deleted) {
                Structure<DeletionInfo> deletionInfo = scanDeletion(root, localRelpath);
                if (deletionInfo.hasValue(DeletionInfo.workDelRelPath)) {
                    status = SVNWCDbStatus.Added;
                    localRelpath = SVNFileUtil.getFileDir(deletionInfo.<File>get(DeletionInfo.workDelRelPath));
                }
                deletionInfo.release();
            } 
            if (status == SVNWCDbStatus.Added) {
                Structure<AdditionInfo> additionInfo = scanAddition(root, localRelpath, AdditionInfo.reposId);
                oldReposId = additionInfo.lng(AdditionInfo.reposId);
                additionInfo.release();
            } else {
                Structure<NodeInfo> baseInfo = getDepthInfo(root, localRelpath, 0, NodeInfo.reposId);
                oldReposId = baseInfo.lng(NodeInfo.reposId);
                baseInfo.release();
            }
        }
        
        Structure<RepositoryInfo> repositoryInfo = db.fetchRepositoryInfo(root.getSDb(), oldReposId);
        String reposUuid = repositoryInfo.text(RepositoryInfo.reposUuid);
        repositoryInfo.release();
        
        begingWriteTransaction(root);
        try {
            relocate(root, localRelpath, repositoryRootUrl, reposUuid, haveBase, oldReposId);
        } catch(SVNException e) {            
            rollbackTransaction(root);
            throw e;
        } finally {
            commitTransaction(root);
        }
        
    }
    
    private static void relocate(SVNWCDbRoot root, File localRelPath, SVNURL reposRootUrl, String reposUuid, boolean haveBaseNode, long oldReposId) throws SVNException {
        long newReposId = root.getDb().createReposId(root.getSDb(), reposRootUrl, reposUuid);
        SVNSqlJetUpdateStatement stmt = new RecursiveUpdateNodeRepo(root.getSDb());
        try {
            stmt.bindf("isii", root.getWcId(), localRelPath, oldReposId, newReposId);
            stmt.done();
        } finally {
            stmt.reset();
        }
        
        if (haveBaseNode) {
            stmt = new UpdateLockReposId(root.getSDb());
            try {
                stmt.bindf("ii", oldReposId, newReposId);
                stmt.done();
            } finally {
                stmt.reset();
            }
        }
    }
    
    /**
     * UPDATE lock SET repos_id = ?2
     * WHERE repos_id = ?1
     */
    private static class UpdateLockReposId extends SVNSqlJetUpdateStatement {

        private Map<String, Object> updateValues;

        public UpdateLockReposId(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.LOCK);
        }
        
        @Override
        public Map<String, Object> getUpdateValues() throws SVNException {
            if (updateValues == null) {
                updateValues = new HashMap<String, Object>();
            } else {
                updateValues.clear();
            }
            updateValues.put(LOCK__Fields.repos_id.toString(), getBind(2));
            return updateValues;
        }
        
        @Override
        protected Object[] getWhere() throws SVNException {
            return new Object[0];
        }

        @Override
        protected boolean isFilterPassed() throws SVNException {
            long queryReposId = (Long) getBind(1);
            return getColumnLong(LOCK__Fields.repos_id) == queryReposId;
        }
    }

    /**
     * UPDATE nodes SET repos_id = ?4, dav_cache = NULL
     * WHERE wc_id = ?1
     * AND repos_id = ?3
     * AND (?2 = ''
     * OR local_relpath = ?2
     * OR (local_relpath > ?2 || '/' AND local_relpath < ?2 || '0'))
     *
     */
    private static class RecursiveUpdateNodeRepo extends SVNSqlJetUpdateStatement {
        
        private Map<String, Object> updateValues;

        public RecursiveUpdateNodeRepo(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES);
        }

        @Override
        public Map<String, Object> getUpdateValues() throws SVNException {
            if (updateValues == null) {
                updateValues = new HashMap<String, Object>();
            } else {
                updateValues.clear();
            }
            updateValues.put(NODES__Fields.repos_id.toString(), getBind(4));
            updateValues.put(NODES__Fields.dav_cache.toString(), null);
            updateValues.put(NODES__Fields.properties.toString(), getColumnBlob(NODES__Fields.properties));
            return updateValues;
        }

        @Override
        protected Object[] getWhere() throws SVNException {
            return new Object[] {getBind(1)};
        }

        @Override
        protected boolean isFilterPassed() throws SVNException {
            if (super.isFilterPassed()) {
                long queryReposId = (Long) getBind(3);
                if (getColumnLong(NODES__Fields.repos_id) == queryReposId) {
                    String queryPath = (String) getBind(2);
                    if ("".equals(queryPath)) {
                        return true;
                    }
                    String rowPath = getColumnString(NODES__Fields.local_relpath);
                    return rowPath.equals(queryPath) || rowPath.startsWith(queryPath + "/");
                }
            }
            return false;
        }
    }
}
