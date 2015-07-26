package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDeleteStatement;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;

/**
 * DELETE FROM nodes
 * WHERE wc_id = ?1 AND (((local_relpath) > (CASE (?2) WHEN '' THEN '' ELSE (?2) || '/' END)) AND ((local_relpath) < CASE (?2) WHEN '' THEN X'FFFF' ELSE (?2) || '0' END))
 *  AND op_depth > 0
 *
 * @author TMate Software Ltd.
 */
public class SVNWCDbDeleteWorkingRecursive extends SVNSqlJetDeleteStatement {

    public SVNWCDbDeleteWorkingRecursive(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        String localRelPath = getColumnString(SVNWCDbSchema.NODES__Fields.local_relpath);
        if (localRelPath == null) {
            localRelPath = "";
        }

        String deletedRelPath = (String) getBind(2);
        if (deletedRelPath == null) {
            deletedRelPath = "";
        }

        return (SVNPathUtil.isAncestor(deletedRelPath, localRelPath) || localRelPath.equals(deletedRelPath)) &&
                getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) > 0;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    @Override
    protected String getPathScope() {
        return (String) getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }
}
