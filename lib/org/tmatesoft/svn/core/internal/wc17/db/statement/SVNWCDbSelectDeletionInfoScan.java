package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;

/*
 * SELECT (SELECT b.presence FROM nodes AS b
 *        WHERE b.wc_id = ?1 AND b.local_relpath = ?2 AND b.op_depth = 0),
 *      work.presence, work.op_depth, moved.moved_to
 * FROM nodes_current AS work
 * LEFT OUTER JOIN nodes AS moved
 *  ON moved.wc_id = work.wc_id
 *  AND moved.local_relpath = work.local_relpath
 *  AND moved.moved_to IS NOT NULL
 * WHERE work.wc_id = ?1 AND work.local_relpath = ?2 AND work.op_depth > 0
 * LIMIT 1
 *
 * @version 1.8
 */
public class SVNWCDbSelectDeletionInfoScan extends SVNWCDbSelectDeletionInfo {

    private JoinedStatement joinedStatement;

    public SVNWCDbSelectDeletionInfoScan(SVNSqlJetDb sDb) throws SVNException {
        super(sDb);
    }

    @Override
    public SVNSqlJetStatement getJoinedStatement(Enum<?> joinedTable) throws SVNException {
        if (joinedStatement == null) {
            joinedStatement = new JoinedStatement(sDb);
            joinedStatement.bindf("is", getBind(1), getBind(2));
            joinedStatement.next();
        }
        return joinedStatement;
    }

    @Override
    public void reset() throws SVNException {
        if (joinedStatement != null) {
            joinedStatement.reset();
        }
        super.reset();
    }

    private static class JoinedStatement extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

        public JoinedStatement(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES);
        }

        @Override
        protected void defineFields() {
            fields.add(SVNWCDbSchema.NODES__Fields.moved_to);
        }

        @Override
        protected boolean isFilterPassed() throws SVNException {
            return !isColumnNull(SVNWCDbSchema.NODES__Fields.moved_to);
        }

        @Override
        protected Object[] getWhere() throws SVNException {
            return new Object[] {getBind(1), getBind(2)};
        }
    }
}
