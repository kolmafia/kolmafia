package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;

/*
 * SELECT u.local_relpath,
 *      u.presence, u.repos_id, u.repos_path, u.revision,
 *      l.presence, l.repos_id, l.repos_path, l.revision,
 *      u.moved_here, u.moved_to
 * FROM nodes u
 * LEFT OUTER JOIN nodes l ON l.wc_id = ?1
 *                      AND l.local_relpath = u.local_relpath
 *                      AND l.op_depth = ?3
 * WHERE u.wc_id = ?1
 *   AND u.local_relpath = ?2
 *   AND u.op_depth = ?4
 * UNION ALL
 *   SELECT u.local_relpath,
 *      u.presence, u.repos_id, u.repos_path, u.revision,
 *      l.presence, l.repos_id, l.repos_path, l.revision,
 *      u.moved_here, NULL
 * FROM nodes u
 * LEFT OUTER JOIN nodes l ON l.wc_id=?1
 *                      AND l.local_relpath=u.local_relpath
 *                      AND l.op_depth=?3
 * WHERE u.wc_id = ?1
 *   AND IS_STRICT_DESCENDANT_OF(u.local_relpath, ?2)
 *   AND u.op_depth = ?4
 *
 *   @version 1.8
 */

public class SVNWCDbSelectMovedBack extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    private boolean firstPartOfUnion = true;
    private SVNSqlJetStatement joinedStatement;

    public SVNWCDbSelectMovedBack(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    @Override
    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.local_relpath);
        fields.add(SVNWCDbSchema.NODES__Fields.presence);
        fields.add(SVNWCDbSchema.NODES__Fields.repos_path);
        fields.add(SVNWCDbSchema.NODES__Fields.revision);
        fields.add(SVNWCDbSchema.NODES__Fields.moved_here);
        fields.add(SVNWCDbSchema.NODES__Fields.moved_to);
    }

    @Override
    protected String getPathScope() {
        return firstPartOfUnion ? null : (String) getBind(2);
    }

    @Override
    protected boolean isStrictiDescendant() {
        return true;
    }

    @Override
    protected boolean isFilterPassed() throws SVNException {
        return firstPartOfUnion ? true : getColumnLong(SVNWCDbSchema.NODES__Fields.op_depth) == (Long) getBind(4);
    }

    @Override
    protected Object[] getWhere() throws SVNException {
        if (firstPartOfUnion) {
            return new Object[]{getBind(1), getBind(2), getBind(4)};
        } else {
            return new Object[]{getBind(1)};
        }
    }

    @Override
    public void reset() throws SVNException {
        super.reset();
        firstPartOfUnion = true;
        if (joinedStatement != null) {
            joinedStatement.reset();
        }
    }

    @Override
    public boolean next() throws SVNException {
        if (firstPartOfUnion) {
            boolean next = super.next();
            if (next) {
                if (joinedStatement != null) {
                    joinedStatement.reset();
                }
                joinedStatement = new JoinedStatement(sDb);
                joinedStatement.bindf("isi", getBind(1), getColumnString(SVNWCDbSchema.NODES__Fields.local_relpath), getBind(3));
                joinedStatement.next();
                return true;
            } else {
                firstPartOfUnion = false;
                resetCursor();
            }
        }
        if (!firstPartOfUnion) {
            boolean next = super.next();
            if (next) {
                if (joinedStatement != null) {
                    joinedStatement.reset();
                }
                joinedStatement = new JoinedStatement(sDb);
                joinedStatement.bindf("isi", getBind(1), getColumnString(SVNWCDbSchema.NODES__Fields.local_relpath), getBind(3));
                joinedStatement.next();
            }
            return next;
        }
        return false;
    }

    private void resetCursor() throws SVNException {
        try {
            getCursor().close();
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
        }
        setCursor(openCursor());
    }

    @Override
    public SVNSqlJetStatement getJoinedStatement(Enum<?> joinedTable) throws SVNException {
        return joinedStatement;
    }

    private static class JoinedStatement extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {
        public JoinedStatement(SVNSqlJetDb sDb) throws SVNException {
            super(sDb, SVNWCDbSchema.NODES);
        }

        @Override
        protected void defineFields() {
            fields.add(SVNWCDbSchema.NODES__Fields.presence);
            fields.add(SVNWCDbSchema.NODES__Fields.repos_id);
            fields.add(SVNWCDbSchema.NODES__Fields.repos_path);
            fields.add(SVNWCDbSchema.NODES__Fields.revision);
        }

        @Override
        protected Object[] getWhere() throws SVNException {
            return new Object[]{getBind(1), getBind(2), getBind(3)};
        }
    }
}
