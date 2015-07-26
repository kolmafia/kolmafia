package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil;

import java.util.EnumSet;

/**
 * DELETE FROM actual_node
 *   WHERE wc_id = ?1 AND IS_STRICT_DESCENDANT_OF(local_relpath, ?2)
 *   AND EXISTS(SELECT 1 FROM NODES b
 *              WHERE b.wc_id = ?1
 *              AND b.local_relpath = actual_node.local_relpath
 *              AND op_depth = 0)
 *   AND NOT EXISTS(SELECT 1 FROM NODES w
 *                  WHERE w.wc_id = ?1
 *                  AND w.local_relpath = actual_node.local_relpath
 *                  AND op_depth > 0
 *                  AND presence in (MAP_NORMAL, MAP_INCOMPLETE, MAP_NOT_PRESENT))
 * @author TMate Software Ltd.
 */
public class SVNWCDbDeleteActualForBaseRecursive extends SVNSqlJetDeleteStatement {

    private SVNWCDbNodesStrictOpDepth strictOpDepthSelect;
    private SelectFromNodesForPresence selectFromNodesForPresence;

    public SVNWCDbDeleteActualForBaseRecursive(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
        strictOpDepthSelect = new SVNWCDbNodesStrictOpDepth(sDb, 0);
        selectFromNodesForPresence = new SelectFromNodesForPresence(sDb, 1,
                EnumSet.of(ISVNWCDb.SVNWCDbStatus.Normal, ISVNWCDb.SVNWCDbStatus.Incomplete, ISVNWCDb.SVNWCDbStatus.NotPresent));
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return strictOpDepthSelect.existsOpDepth((Long)getBind(1), getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath)) &&
                !selectFromNodesForPresence.existsOpDepth((Long)getBind(1), getColumnString(SVNWCDbSchema.ACTUAL_NODE__Fields.local_relpath));
    }

    @Override
    protected String getPathScope() {
        return getBind(2).toString();
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    private static class SelectFromNodesForPresence extends SVNSqlJetSelectStatement {

        private long minOpDepth;
        private EnumSet<ISVNWCDb.SVNWCDbStatus> presence;

        public SelectFromNodesForPresence(SVNSqlJetDb sDb, long minOpDepth, EnumSet<ISVNWCDb.SVNWCDbStatus> presence) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES);
            this.minOpDepth = minOpDepth;
            this.presence = presence;
        }

        @Override
        protected boolean isFilterPassed() throws SVNException {
            return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) >= minOpDepth &&
                   presence.contains(SvnWcDbStatementUtil.getColumnPresence(this, SVNWCDbSchema.NODES__Fields.presence));
        }

        public boolean existsOpDepth(Long wcId, String localRelpath) throws SVNException {
            try {
                bindLong(1, wcId);
                bindString(2, localRelpath);
                return next();
            } finally {
                reset();
            }
        }
    }
}
