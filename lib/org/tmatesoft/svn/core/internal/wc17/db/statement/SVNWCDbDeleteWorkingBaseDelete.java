package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;

/**
 * DELETE FROM nodes
 * WHERE wc_id = ?1 AND (((local_relpath) > (CASE (?2) WHEN '' THEN '' ELSE (?2) || '/' END)) AND ((local_relpath) < CASE (?2) WHEN '' THEN X'FFFF' ELSE (?2) || '0' END))
 *  AND presence = 'base-deleted'
 *  AND op_depth > 0
 *  AND op_depth = (SELECT MIN(op_depth) FROM nodes n
 *                    WHERE n.wc_id = ?1
 *                      AND n.local_relpath = nodes.local_relpath
 *                      AND op_depth > 0)
 */

public class SVNWCDbDeleteWorkingBaseDelete extends SVNSqlJetDeleteStatement {

    private SVNWCDbNodesMinOpDepth minOpDepthSelect;
    private Long minOpDepth = null;

    public SVNWCDbDeleteWorkingBaseDelete(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        minOpDepthSelect = new SVNWCDbNodesMinOpDepth(sDb, 1);
    }

    @Override
    public void reset() throws SVNException {
        minOpDepth = null;
        super.reset();
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        String presenceString = getColumnString(SVNWCDbSchema.NODES__Fields.presence);
        long opDepth = getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth);

        return "base-deleted".equals(presenceString) && opDepth > 0 && opDepth == minOpDepth;

    }

    @Override
    protected Object[] getWhere() throws SVNException {
        minOpDepth = minOpDepthSelect.getMinOpDepth((Long)getBind(1), getBind(2).toString());
        return new Object[]{getBind(1)};
    }

    @Override
    protected String getPathScope() {
        return getBind(2).toString();
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }
}
