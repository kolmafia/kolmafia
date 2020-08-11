package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;

/*
SELECT s.op_depth, n.local_relpath, n.kind, n.repos_path, s.moved_to
FROM nodes n
JOIN nodes s ON s.wc_id = n.wc_id AND s.local_relpath = n.local_relpath
 AND s.op_depth = (SELECT MIN(d.op_depth)
                    FROM nodes d
                    WHERE d.wc_id = ?1
                      AND d.local_relpath = s.local_relpath
                      AND d.op_depth > ?3)
WHERE n.wc_id = ?1 AND n.op_depth = ?3
  AND (n.local_relpath = ?2 OR IS_STRICT_DESCENDANT_OF(n.local_relpath, ?2))
  AND s.moved_to IS NOT NULL


 */
public class SVNWCDbSelectMovedDescendantsSrc extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    private SVNWCDbNodesMinOpDepth minOpDepthSelect;
    private InternalSelect internalSelect;

    public SVNWCDbSelectMovedDescendantsSrc(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    public InternalSelect getInternalSelect() {
        return internalSelect;
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.op_depth);
        fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
        fields.add(SVNWCDbSchema.NODES__Fields.kind);
        fields.add(SVNWCDbSchema.NODES__Fields.repos_path);
        fields.add(SVNWCDbSchema.NODES__Fields.moved_to);
    }

    @Override
    protected String getRowPath() throws SVNException {
        return (String)(getRowValues().get(SVNWCDbSchema.NODES__Fields.local_relpath.name()));
    }

    @Override
    protected Enum<?> getRowPathField() throws SVNException {
        return SVNWCDbSchema.NODES__Fields.local_relpath;
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
        if (getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) != (Long)getBind(3)) {
            return false;
        }
        if (internalSelect != null) {
            internalSelect.reset();
        }
        internalSelect = new InternalSelect(sDb);
        internalSelect.bindf("isi",
                getColumnLong(SVNWCDbSchema.NODES__Fields.wc_id),
                getColumnString(SVNWCDbSchema.NODES__Fields.local_relpath),
                getNestedStatementMinOpDepth());
        boolean next = internalSelect.next();
        return next && !internalSelect.isColumnNull(SVNWCDbSchema.NODES__Fields.moved_to);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[] {getBind(1)};
    }

    @Override
    public void reset() throws SVNException {
        super.reset();
        if (internalSelect != null) {
            internalSelect.reset();
        }
    }

    private Long getNestedStatementMinOpDepth() throws SVNException {
        return getMinOpDepthSelect().getMinOpDepth((Long) getBind(1), getBind(2).toString());
    }

    private SVNWCDbNodesMinOpDepth getMinOpDepthSelect() throws SVNException {
        if (minOpDepthSelect == null) {
            minOpDepthSelect = new SVNWCDbNodesMinOpDepth(sDb, (Long)getBind(3) + 1); //add 1 to get strict > instead of >=
        }
        return minOpDepthSelect;
    }

    public static class InternalSelect extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

        public InternalSelect(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES);
        }

        @Override
        protected void defineFields() {
            fields.add(SVNWCDbSchema.NODES__Fields.op_depth);
            fields.add(SVNWCDbSchema.NODES__Fields.moved_to);
        }
    }
}
