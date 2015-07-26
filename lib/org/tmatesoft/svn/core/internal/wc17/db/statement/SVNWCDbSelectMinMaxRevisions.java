package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * SELECT MIN(revision), MAX(revision),
 * MIN(changed_revision), MAX(changed_revision) FROM nodes
 * WHERE wc_id = ?1
 * AND (local_relpath = ?2
 * OR IS_STRICT_DESCENDANT_OF(local_relpath, ?2))
 * AND presence IN (MAP_NORMAL, MAP_INCOMPLETE)
 * AND file_external IS NULL
 * AND op_depth = 0
 *
 * @version 1.8
 */
public class SVNWCDbSelectMinMaxRevisions extends SVNSqlJetSelectStatement {

    private long minRevision, maxRevision;
    private long minChangedRevision, maxChangedRevision;

    private Set<ISVNWCDb.SVNWCDbStatus> expectedPresence;

    public SVNWCDbSelectMinMaxRevisions(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
        minRevision = maxRevision = minChangedRevision = maxChangedRevision = -1;
        expectedPresence = new HashSet<ISVNWCDb.SVNWCDbStatus>(2);
        expectedPresence.add(ISVNWCDb.SVNWCDbStatus.Normal);
        expectedPresence.add(ISVNWCDb.SVNWCDbStatus.Incomplete);
    }

    @Override
    public void reset() throws SVNException {
        super.reset();
        minRevision = maxRevision = minChangedRevision = maxChangedRevision = -1;
    }

    public long getMinRevision() {
        return minRevision;
    }

    public long getMaxRevision() {
        return maxRevision;
    }

    public long getMinChangedRevision() {
        return minChangedRevision;
    }

    public long getMaxChangedRevision() {
        return maxChangedRevision;
    }

    @Override
    public boolean next() throws SVNException {
        boolean next = false;
        while (super.next()) {
            next = true;
            long revision = getColumnLong(SVNWCDbSchema.NODES__Fields.revision);
            if (minRevision == -1 || revision < minRevision) {
                minRevision = revision;
            }
            if (maxRevision == -1 || revision > maxRevision) {
                maxRevision = revision;
            }

            long changedRevision = getColumnLong(SVNWCDbSchema.NODES__Fields.changed_revision);
            if (minChangedRevision == -1 || changedRevision < minChangedRevision) {
                minChangedRevision = changedRevision;
            }
            if (maxChangedRevision == -1 || changedRevision > maxChangedRevision) {
                maxChangedRevision = changedRevision;
            }
        }
        return next;
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
        return getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == 0 &&
                expectedPresence.contains(SvnWcDbStatementUtil.getColumnPresence(this)) &&
                isColumnNull(SVNWCDbSchema.NODES__Fields.file_external);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }
}
