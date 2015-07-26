package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil;

/*
 * DELETE FROM nodes
 * WHERE wc_id = ?1
 *  AND (local_relpath = ?2 OR IS_STRICT_DESCENDANT_OF(local_relpath, ?2))
 *  AND op_depth = ?3
 *  AND NOT EXISTS (SELECT 1 FROM nodes n
 *                   WHERE n.wc_id = ?1
 *                   AND n.local_relpath = nodes.local_relpath
 *                   AND n.op_depth = ?4
 *                   AND n.presence IN (MAP_NORMAL, MAP_INCOMPLETE))
 *
 * @version 1.8
 */
public class SVNWCDbDeleteNoLowerLayer extends SVNSqlJetDeleteStatement {

    private boolean storedExistenceFlag;

    public SVNWCDbDeleteNoLowerLayer(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        storedExistenceFlag = false;
    }

    @Override
    protected String getPathScope() {
        return (String)getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return false;
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        long columnDepth = getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);

        if (columnDepth == 0 || !storedExistenceFlag) {
            ISVNWCDb.SVNWCDbStatus presence = SvnWcDbStatementUtil.getColumnPresence(this, SVNWCDbSchema.NODES__Fields.presence);
            storedExistenceFlag = (columnDepth == (Long)getBind(4)) &&
                    getBind(2).equals(getColumnString(SVNWCDbSchema.NODES__Fields.local_relpath)) &&
                    (presence == ISVNWCDb.SVNWCDbStatus.Normal || presence == ISVNWCDb.SVNWCDbStatus.Incomplete);
        }
        return columnDepth == (Long)getBind(3);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }
}
