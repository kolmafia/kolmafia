package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;

/*
 * SELECT nodes.repos_id, nodes.repos_path, lock_token
 * FROM nodes
 * LEFT JOIN lock ON nodes.repos_id = lock.repos_id
 *   AND nodes.repos_path = lock.repos_relpath
 * WHERE wc_id = ?1 AND op_depth = 0
 *   AND IS_STRICT_DESCENDANT_OF(local_relpath, ?2)
 *
 * @version 1.8
 */
public class SVNWCDbSelectBaseNodeLockTokensRecursive extends SVNSqlJetSelectStatement {

    private JoinedStatement joinedStatement;

    public SVNWCDbSelectBaseNodeLockTokensRecursive(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected String getPathScope() {
        return (String)getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == 0;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[]{getBind(1)};
    }

    @Override
    public SVNSqlJetStatement getJoinedStatement(Enum<?> joinedTable) throws SVNException {
        return joinedStatement;
    }

    @Override
    public boolean next() throws SVNException {
        boolean next = super.next();
        if (joinedStatement != null) {
            joinedStatement.reset();
            joinedStatement = null;
        }
        joinedStatement = new JoinedStatement(sDb);
        joinedStatement.bindf("is", getColumnLong(SVNWCDbSchema.NODES__Fields.repos_id), getColumnString(SVNWCDbSchema.NODES__Fields.repos_path));
        joinedStatement.next();
        return next;
    }

    @Override
    public void reset() throws SVNException {
        super.reset();
        if (joinedStatement != null) {
            joinedStatement.reset();
        }
    }

    private static class JoinedStatement extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.LOCK__Fields> {

        public JoinedStatement(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.LOCK);
        }

        @Override
        protected void defineFields() {
            fields.add(SVNWCDbSchema.LOCK__Fields.lock_token);
        }
    }
}
