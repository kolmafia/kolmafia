package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil;

/**
 *
 * SELECT n.checksum, md5_checksum, size
 *        FROM nodes_current n
 *     LEFT JOIN pristine p ON n.checksum = p.checksum
 * WHERE wc_id = ?1
 *     AND n.local_relpath = ?2
 *     AND n.checksum IS NOT NULL
 * UNION ALL
 * SELECT n.checksum, md5_checksum, size
 *        FROM nodes n
 *     LEFT JOIN pristine p ON n.checksum = p.checksum
 * WHERE wc_id = ?1
 *     AND IS_STRICT_DESCENDANT_OF(n.local_relpath, ?2)
 *     AND op_depth >=
 *     (SELECT MAX(op_depth) FROM nodes WHERE wc_id = ?1 AND local_relpath = ?2)
 *     AND n.checksum IS NOT NULL
 *
 * @version 1.8
 */
public class SVNWCDbSelectCopyPristines extends SVNSqlJetSelectStatement {
    private boolean firstPartOfUnion = true;
    private long maxOpDepth;
    private SVNSqlJetStatement joinedStatement;

    public SVNWCDbSelectCopyPristines(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        maxOpDepth = -1;
    }

    @Override
    protected String getPathScope() {
        return firstPartOfUnion ? null : (String)getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        if (isColumnNull(SVNWCDbSchema.NODES__Fields.checksum)) {
            return false;
        }
        if (firstPartOfUnion) {
            return true;
        } else {
            return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) >= getMaxOpDepth();
        }
    }

    private long getMaxOpDepth() throws SVNException {
        if (maxOpDepth == -1) {
            SVNWCDbNodesMaxOpDepth maxOpDepth = new SVNWCDbNodesMaxOpDepth(sDb, 0);
            try {
                this.maxOpDepth = maxOpDepth.getMaxOpDepth((Long) getBind(1), (String) getBind(2));
            } finally {
                maxOpDepth.reset();
            }
        }
        return maxOpDepth;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        if (firstPartOfUnion) {
            return new Object[] {getBind(1), getBind(2)};
        } else {
            return new Object[] {getBind(1)};
        }
    }

    @Override
    public SVNSqlJetStatement getJoinedStatement(String joinedTable) throws SVNException {
        return joinedStatement;
    }

    @Override
    public void reset() throws SVNException {
        if (joinedStatement != null) {
            joinedStatement.reset();
        }
        super.reset();
    }

    @Override
    public boolean next() throws SVNException {
        if (firstPartOfUnion) {
            boolean next = super.next();
            if (next) {
                if (joinedStatement != null) {
                    joinedStatement.reset();
                }
                joinedStatement = new JoinedStatement(sDb);
                joinedStatement.bindChecksum(1, SvnWcDbStatementUtil.getColumnChecksum(this, SVNWCDbSchema.NODES__Fields.checksum));
                joinedStatement.next();
                return true;
            } else {
                firstPartOfUnion = false;
                resetCursor();
            }
        }
        if (!firstPartOfUnion) {
            boolean next = super.next();
            if (next) {
                if (joinedStatement != null) {
                    joinedStatement.reset();
                }
                joinedStatement = new JoinedStatement(sDb);
                joinedStatement.bindChecksum(1, SvnWcDbStatementUtil.getColumnChecksum(this, SVNWCDbSchema.NODES__Fields.checksum));
                joinedStatement.next();
            }
            return next;
        }
        return false;
    }

    private void resetCursor() throws SVNException {
        try {
            getCursor().close();
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
        }
        setCursor(openCursor());
    }

    private static class JoinedStatement extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.PRISTINE__Fields> {
        public JoinedStatement(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.PRISTINE);
        }

        @Override
        protected void defineFields() {
            fields.add(SVNWCDbSchema.PRISTINE__Fields.md5_checksum);
            fields.add(SVNWCDbSchema.PRISTINE__Fields.size);
        }
    }
}
