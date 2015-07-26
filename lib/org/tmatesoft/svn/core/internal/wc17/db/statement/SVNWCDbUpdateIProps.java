package org.tmatesoft.svn.core.internal.wc17.db.statement;

import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;

public class SVNWCDbUpdateIProps extends SVNSqlJetUpdateStatement {
    
    private Map<String, Object> values;

    public SVNWCDbUpdateIProps(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        values = new HashMap<String, Object>();
    }

    @Override
    public Map<String, Object> getUpdateValues() throws SVNException {
        values.put(SVNWCDbSchema.NODES__Fields.inherited_props.toString(), getBind(3));
        return values;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1), getBind(2), 0};
    }

}
