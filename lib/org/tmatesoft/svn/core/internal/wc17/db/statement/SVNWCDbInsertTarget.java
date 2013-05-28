package org.tmatesoft.svn.core.internal.wc17.db.statement;

import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetInsertStatement;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectStatement;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.NODES__Fields;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema.TARGETS_LIST__Fields;

public class SVNWCDbInsertTarget extends SVNSqlJetInsertStatement {

    private SVNSqlJetSelectStatement select;
    private Map<String, Object> insertValues;

    public SVNWCDbInsertTarget(SVNSqlJetDb sDb, SVNSqlJetSelectStatement select) throws SVNException {
        super(sDb, SVNWCDbSchema.TARGETS_LIST);
        this.select = select;
    }

    @Override
    protected Map<String, Object> getInsertValues() throws SVNException {
        Map<String,Object> selectedRow = select.getRowValues();
        if (insertValues == null) {
            insertValues = new HashMap<String, Object>();
        }
        insertValues.clear();
        insertValues.put(TARGETS_LIST__Fields.wc_id.toString(), selectedRow.get(NODES__Fields.wc_id.toString()));
        insertValues.put(TARGETS_LIST__Fields.local_relpath.toString(), selectedRow.get(NODES__Fields.local_relpath.toString()));
        insertValues.put(TARGETS_LIST__Fields.parent_relpath.toString(), selectedRow.get(NODES__Fields.parent_relpath.toString()));
        insertValues.put(TARGETS_LIST__Fields.kind.toString(), selectedRow.get(NODES__Fields.kind.toString()));
        return insertValues;
    }
    
    public long exec() throws SVNException {
        try {
            int n = 0;
            while (select.next()) {
                super.exec();
                n++;
            }
            return n;
        } finally {
            select.reset();
        }
    }

}
