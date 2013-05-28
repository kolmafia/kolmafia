package org.tmatesoft.svn.core.internal.wc17.db.statement;

import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;

/**
 * -- STMT_COMMIT_DESCENDANT_TO_BASE
 * UPDATE NODES SET op_depth = 0, repos_id = ?4, repos_path = ?5, revision = ?6,
 *  moved_here = NULL, moved_to = NULL, dav_cache = NULL,
 * presence = CASE presence WHEN 'normal' THEN 'normal'
 *                          WHEN 'excluded' THEN 'excluded'
 *                          ELSE 'not-present' END
 * WHERE wc_id = ?1 AND local_relpath = ?2 and op_depth = ?3
 */
public class SVNWCDbCommitDescendantToBase extends SVNSqlJetUpdateStatement {

    public SVNWCDbCommitDescendantToBase(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    public Map<String, Object> getUpdateValues() throws SVNException {
        Map<String, Object> updateValues = getRowValues();
        
        updateValues.put(NODES__Fields.op_depth.toString(), 0l);
        updateValues.put(NODES__Fields.repos_id.toString(), getBind(4));
        updateValues.put(NODES__Fields.repos_path.toString(), getBind(5));
        updateValues.put(NODES__Fields.revision.toString(), getBind(6));
        updateValues.put(NODES__Fields.moved_here.toString(), null);
        updateValues.put(NODES__Fields.moved_to.toString(), null);
        updateValues.put(NODES__Fields.dav_cache.toString(), null);
        String presence = (String) updateValues.get(NODES__Fields.presence.toString());
        
        if (!"normal".equals(presence) && !"excluded".equals(presence)) {
            updateValues.put(NODES__Fields.presence.toString(), "not-present");
        }

        return updateValues;
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] { getBind(1), getBind(2), getBind(3) };
    }

}
