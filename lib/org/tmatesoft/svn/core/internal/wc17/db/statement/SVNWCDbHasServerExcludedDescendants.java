package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil;

/**
 * SELECT local_relpath FROM nodes
 * WHERE wc_id = ?1
 * AND IS_STRICT_DESCENDANT_OF(local_relpath, ?2)
 * AND op_depth = 0 AND presence = MAP_SERVER_EXCLUDED
 * LIMIT 1
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbHasServerExcludedDescendants extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {
    public SVNWCDbHasServerExcludedDescendants(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == 0 &&
                SvnWcDbStatementUtil.getColumnPresence(this, SVNWCDbSchema.NODES__Fields.presence) == ISVNWCDb.SVNWCDbStatus.ServerExcluded;
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    @Override
    protected String getPathScope() {
        return getBind(2).toString();
    }

    @Override
    protected ISqlJetCursor openCursor() throws SVNException {
        ISqlJetCursor cursor = super.openCursor();
        if (cursor != null) {
            try {
                cursor.setLimit(1);
            } catch (SqlJetException e) {
                cursor = null;
            }
        }
        return cursor;
    }
}
