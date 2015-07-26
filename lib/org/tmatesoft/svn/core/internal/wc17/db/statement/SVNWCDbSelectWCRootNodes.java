package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.util.SVNLogType;

/*
 * SELECT l.wc_id, l.local_relpath FROM nodes as l
 * LEFT OUTER JOIN nodes as r
 * ON l.wc_id = r.wc_id
 *    AND r.local_relpath = l.parent_relpath
 *    AND r.op_depth = 0
 * WHERE l.op_depth = 0
 *   AND l.repos_path != ''
 *   AND ((l.repos_id IS NOT r.repos_id)
 *        OR (l.repos_path IS NOT RELPATH_SKIP_JOIN(r.local_relpath, r.repos_path, l.local_relpath)))
 *
 * @version 1.8
 */
public class SVNWCDbSelectWCRootNodes extends SVNSqlJetSelectStatement {

    public SVNWCDbSelectWCRootNodes(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        final String localRelpath = getColumnString(NODES__Fields.local_relpath);
        final String reposRelpath = getColumnString(NODES__Fields.repos_path);
        if ("".equals(localRelpath) && !"".equals(reposRelpath)) {
            return true;
        } else if (!"".equals(localRelpath)) {
            if (getColumnLong(NODES__Fields.op_depth) != 0) {
                return false;
            }
            final long wcId = getColumnLong(NODES__Fields.wc_id);
            final String localParentRelpath = getColumnString(NODES__Fields.parent_relpath);
            final String childName = localParentRelpath.length() == 0 ? localRelpath : localRelpath.substring(localParentRelpath.length() + 1);
            final String nodeReposRelpath = getNodeReposRelpath(wcId, localParentRelpath);
            if (nodeReposRelpath == null) {
                return true;
            }
            final String expectedChildReposPath = SVNPathUtil.append(nodeReposRelpath, childName);
            if (!expectedChildReposPath.equals(reposRelpath)) {
                return true;
            }
        }
        return false;
    }

    private String getNodeReposRelpath(long wcId, String path) throws SVNException {
        ISqlJetCursor cursor = null;
        try {
            cursor = getTable().lookup(null, wcId, path);
            if (!cursor.eof()) {
                return cursor.getString(NODES__Fields.repos_path.toString());
            }
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (SqlJetException e) {
                }
            }
        }
        return null;
    }
    
    

}
