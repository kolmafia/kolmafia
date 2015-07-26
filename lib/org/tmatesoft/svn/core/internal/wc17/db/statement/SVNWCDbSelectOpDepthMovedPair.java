package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;

/*
 * SELECT n.local_relpath, n.moved_to,
 *        (SELECT o.repos_path FROM nodes AS o
 *         WHERE o.wc_id = n.wc_id
 *           AND o.local_relpath = n.local_relpath
 *           AND o.op_depth < ?3 ORDER BY o.op_depth DESC LIMIT 1)
 * FROM nodes AS n
 * WHERE n.wc_id = ?1
 *   AND IS_STRICT_DESCENDANT_OF(n.local_relpath, ?2)
 *   AND n.op_depth = ?3
 *   AND n.moved_to IS NOT NULL
 */
public class SVNWCDbSelectOpDepthMovedPair extends SVNSqlJetSelectStatement {

    private String storedReposPath;

    public SVNWCDbSelectOpDepthMovedPair(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected String getPathScope() {
        return (String) getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        if (getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == (Long) getBind(3) && getColumn(SVNWCDbSchema.NODES__Fields.moved_to) != null) {
            return true;
        }
        storedReposPath = getColumnString(SVNWCDbSchema.NODES__Fields.repos_path);
        return false;
    }

    public String getReposPath() {
        return storedReposPath;
    }
}
