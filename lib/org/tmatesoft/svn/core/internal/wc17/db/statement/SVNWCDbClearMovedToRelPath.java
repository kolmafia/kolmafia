package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;

import java.util.HashMap;
import java.util.Map;

/*
 * UPDATE nodes SET moved_to = NULL
 * WHERE wc_id = ?1 AND local_relpath = ?2 AND op_depth = ?3
 */
public class SVNWCDbClearMovedToRelPath extends SVNSqlJetUpdateStatement {

    public SVNWCDbClearMovedToRelPath(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    public Map<String, Object> getUpdateValues() throws SVNException {
        final Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.NODES__Fields.moved_to.toString(), null);
        return values;
    }
}
