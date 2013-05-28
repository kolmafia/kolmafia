package org.tmatesoft.svn.core.internal.wc17.db.statement;

import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;

public class SVNWCDDBUpdateActualClearChangelist extends SVNSqlJetUpdateStatement {
    
    @SuppressWarnings("serial")
    private static final Map<String, Object> eraser = new HashMap<String, Object>() {
        { 
            put(SVNWCDbSchema.ACTUAL_NODE__Fields.changelist.toString(), null);
        }
    };

    public SVNWCDDBUpdateActualClearChangelist(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    @Override
    public Map<String, Object> getUpdateValues() throws SVNException {
        return eraser;
    }

    protected Object[] getWhere() throws SVNException {
        return new Object[] {
                getBind(1), getBind(2)
        };
    }

}
