package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * SELECT local_relpath, moved_to, op_depth,
 *   (SELECT CASE WHEN r.moved_here THEN r.op_depth END FROM nodes r
 *    WHERE r.wc_id = ?1
 *      AND r.local_relpath = n.local_relpath
 *      AND r.op_depth < n.op_depth
 *     BY r.op_depth DESC LIMIT 1) AS moved_here_op_depth
 * FROM nodes n
 * WHERE wc_id = ?1
 *   AND (local_relpath = ?2 OR IS_STRICT_DESCENDANT_OF(local_relpath, ?2))
 *   AND moved_to IS NOT NULL
 *   AND op_depth >= ?3
 *
 * @version 1.8
 */
public class SVNWCDbSelectMovedForDelete extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    private InternalStatement internalStatement;

    public SVNWCDbSelectMovedForDelete(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
        fields.add(SVNWCDbSchema.NODES__Fields.moved_to);
        fields.add(SVNWCDbSchema.NODES__Fields.op_depth);
    }

    @Override
    public void reset() throws SVNException {
        if (internalStatement != null) {
            internalStatement.reset();
            internalStatement = null;
        }
        super.reset();
    }

    @Override
    protected String getPathScope() {
        return (String) getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return false;
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        long opDepth = getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);
        if (internalStatement == null) {
            internalStatement = new InternalStatement(sDb);
            internalStatement.bindf("isi", getBind(1), getBind(2), opDepth);
            internalStatement.next();
        }
        return !isColumnNull(SVNWCDbSchema.NODES__Fields.moved_to) && opDepth >= (Long)getBind(3);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[]{getBind(1)};
    }

    public int getMovedHereDepth() throws SVNException {
        if (internalStatement == null) {
            return -1;
        } else {
            return (int) internalStatement.getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);
        }
    }

    private static class InternalStatement extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

        public InternalStatement(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES);
        }

        @Override
        protected void defineFields() {
            fields.add(SVNWCDbSchema.NODES__Fields.op_depth);
        }

        @Override
        protected ISqlJetCursor openCursor() throws SVNException {
            try {
                ISqlJetCursor cursor = super.openCursor().reverse();
                cursor.setLimit(1);
                return cursor;
            } catch (SqlJetException e) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.WC_DB_ERROR, e);
                SVNErrorManager.error(errorMessage, SVNLogType.WC);
            }
            return null;
        }

        @Override
        protected boolean isFilterPassed() throws SVNException {
            return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) < (Long)getBind(3) && !isColumnNull(SVNWCDbSchema.NODES__Fields.moved_here);
        }

        @Override
        protected Object[] getWhere() throws SVNException {
            return new Object[] {getBind(1), getBind(2)};
        }
    }
}
