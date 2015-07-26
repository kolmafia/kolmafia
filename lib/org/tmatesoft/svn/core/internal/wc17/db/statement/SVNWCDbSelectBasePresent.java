package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil;

/**
 * SELECT local_relpath, kind FROM nodes n
 * WHERE wc_id = ?1 AND IS_STRICT_DESCENDANT_OF(local_relpath, ?2)
 * AND op_depth = 0
 * AND presence in (MAP_NORMAL, MAP_INCOMPLETE)
 * AND NOT EXISTS(SELECT 1 FROM NODES w
 *                WHERE w.wc_id = ?1 AND w.local_relpath = n.local_relpath
 *                AND op_depth > 0)
 *  ORDER BY local_relpath DESC
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbSelectBasePresent extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    private SVNWCDbNodesMinOpDepth minOpDepthSelect;

    public SVNWCDbSelectBasePresent(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        minOpDepthSelect = new SVNWCDbNodesMinOpDepth(sDb, 1);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
        fields.add(SVNWCDbSchema.NODES__Fields.kind);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    @Override
    protected String getPathScope() {
        return getBind(2).toString();
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        ISVNWCDb.SVNWCDbStatus presence = SvnWcDbStatementUtil.getColumnPresence(this, SVNWCDbSchema.NODES__Fields.presence);
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == 0 &&
                (presence == ISVNWCDb.SVNWCDbStatus.Normal || presence == ISVNWCDb.SVNWCDbStatus.Incomplete) &&
                minOpDepthSelect.getMinOpDepth((Long)getBind(1), getColumnString(SVNWCDbSchema.NODES__Fields.local_relpath)) == null;
    }

    @Override
    protected ISqlJetCursor openCursor() throws SVNException {
        ISqlJetCursor cursor = super.openCursor();
        if (cursor != null) {
            try {
                cursor = cursor.reverse();
            } catch (SqlJetException e) {
                SVNSqlJetDb.createSqlJetError(e);
            }
        }
        return cursor;
    }

    //TODO: implement ORDER BY
}
