package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;

import java.util.HashMap;
import java.util.Map;

/*
 * UPDATE actual_node SET conflict_data = ?3
 * WHERE wc_id = ?1 AND local_relpath = ?2
 *
 * @version 1.8
 */
public class SVNWCDbUpdateActualConflict extends SVNSqlJetUpdateStatement {

    public SVNWCDbUpdateActualConflict(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    @Override
    public Map<String, Object> getUpdateValues() throws SVNException {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_data.name(), getBind(3));
        return values;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1), getBind(2)};
    }
}
