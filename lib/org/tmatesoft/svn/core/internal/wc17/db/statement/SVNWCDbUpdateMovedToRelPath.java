package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;

import java.util.HashMap;
import java.util.Map;

/**
 * UPDATE nodes SET moved_to = ?4
 * WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth = ?3
 *
 * @version 1.8
 */
public class SVNWCDbUpdateMovedToRelPath extends SVNSqlJetUpdateStatement {
    public SVNWCDbUpdateMovedToRelPath(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    public Map<String, Object> getUpdateValues() throws SVNException {
        HashMap<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.NODES__Fields.moved_to.name(), getBind(4));
        return values;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1), getBind(2), getBind(3)};
    }
}
