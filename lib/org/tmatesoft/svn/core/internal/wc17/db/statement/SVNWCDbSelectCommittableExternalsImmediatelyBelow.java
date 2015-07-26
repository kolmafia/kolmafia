package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbStatementUtil;

/**
 * SELECT local_relpath, kind, def_repos_relpath,
 * (SELECT root FROM repository AS r WHERE r.id = e.repos_id)
 * FROM externals e
 *  WHERE wc_id = ?1
 *    AND IS_STRICT_DESCENDANT_OF(e.local_relpath, ?2)
 *    AND parent_relpath = ?2
 *    AND def_revision IS NULL
 *    AND repos_id = (SELECT repos_id
 *          FROM nodes AS n
 *           WHERE n.wc_id = ?1
 *              AND n.local_relpath = ''
 *              AND n.op_depth = 0)
 *              AND ((kind='dir')
 *              OR EXISTS (SELECT 1 FROM nodes
 *                     WHERE nodes.wc_id = e.wc_id
 *                        AND nodes.local_relpath = e.parent_relpath))
 *
 * @version 1.8
 */
public class SVNWCDbSelectCommittableExternalsImmediatelyBelow extends SVNSqlJetSelectStatement {

    private InternalStatement1 internalStatement1;
    private InternalStatement2 internalStatement2;

    public SVNWCDbSelectCommittableExternalsImmediatelyBelow(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.EXTERNALS);
        internalStatement2 = new InternalStatement2(sDb);
    }

    public InternalStatement1 getInternalStatement1() {
        return internalStatement1;
    }

    @Override
    protected String getPathScope() {
        return (String)getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return checkForImmediate() &&
                isColumnNull(SVNWCDbSchema.EXTERNALS__Fields.def_revision) &&
                getColumnLong(SVNWCDbSchema.EXTERNALS__Fields.repos_id) == getInternalStatement2().getColumnLong(SVNWCDbSchema.NODES__Fields.repos_id);
    }

    private InternalStatement2 getInternalStatement2() throws SVNException {
        if (internalStatement2 == null) {
            internalStatement2 = new InternalStatement2(sDb);
            internalStatement2.bindf("isi", getBind(1), getBind(2), 0);
            internalStatement2.next();
        }
        return internalStatement2;
    }

    protected boolean checkForImmediate() throws SVNException {
        return getColumnString(SVNWCDbSchema.EXTERNALS__Fields.parent_relpath).equals(getBind(2));
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        return new Object[]{getBind(1)};
    }

    @Override
    public boolean next() throws SVNException {
        if (internalStatement1 != null) {
            internalStatement1.reset();
            internalStatement1 = null;
        }
        if (internalStatement2 != null) {
            internalStatement2.reset();
            internalStatement2 = null;
        }
        boolean next = super.next();
        if (next) {
            if (internalStatement1 == null) {
                internalStatement1 = new InternalStatement1(sDb);
                internalStatement1.bindf("i", getColumnLong(SVNWCDbSchema.EXTERNALS__Fields.repos_id));
                internalStatement1.next();
            }
            if (internalStatement2 != null) {
                internalStatement2.reset();
                internalStatement2 = null;
            }
        }
        return next;
    }

    @Override
    public void reset() throws SVNException {
        if (internalStatement1 != null) {
            internalStatement1.reset();
        }
        if (internalStatement2 != null) {
            internalStatement2.reset();
        }
        super.reset();
    }

    private static class InternalStatement1 extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.REPOSITORY__Fields> {

        public InternalStatement1(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.REPOSITORY);
        }

        @Override
        protected void defineFields() {
            fields.add(SVNWCDbSchema.REPOSITORY__Fields.root);
        }
    }

    private static class InternalStatement2 extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

        private InternalStatement3 internalStatement3;

        public InternalStatement2(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES);
        }

        @Override
        protected void defineFields() {
            fields.add(SVNWCDbSchema.NODES__Fields.repos_id);
        }

        @Override
        protected boolean isFilterPassed() throws SVNException {
            return SvnWcDbStatementUtil.getColumnKind(this, SVNWCDbSchema.NODES__Fields.kind) == ISVNWCDb.SVNWCDbKind.Dir || !getInternalStatement3().isColumnNull(SVNWCDbSchema.NODES__Fields.wc_id);
        }

        private InternalStatement3 getInternalStatement3() throws SVNException {
            if (internalStatement3 == null) {
                internalStatement3 = new InternalStatement3(sDb);
                internalStatement3.bindf("is", getColumnLong(SVNWCDbSchema.NODES__Fields.wc_id), getBind(2));
                internalStatement3.next();
            }
            return internalStatement3;
        }

        @Override
        public boolean next() throws SVNException {
            boolean next = super.next();
            if (next) {
                if (internalStatement3 != null) {
                    internalStatement3.reset();
                    internalStatement3 = null;
                }
            }
            return next;
        }

        @Override
        public void reset() throws SVNException {
            if (internalStatement3 != null) {
                internalStatement3.reset();
            }
            super.reset();
        }

        @Override
        protected Object[] getWhere() throws SVNException {
            return new Object[] {getBind(1), "", 0};
        }
    }

    private static class InternalStatement3 extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

        public InternalStatement3(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES);
        }

        @Override
        protected void defineFields() {
        }
    }
}
