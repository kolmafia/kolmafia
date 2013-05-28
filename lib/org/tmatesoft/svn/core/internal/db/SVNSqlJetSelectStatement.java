/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.db;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetValueType;
import org.tmatesoft.sqljet.core.internal.ISqlJetMemoryPointer;
import org.tmatesoft.sqljet.core.internal.SqlJetUtility;
import org.tmatesoft.sqljet.core.schema.ISqlJetColumnDef;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbSchema;

/**
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNSqlJetSelectStatement extends SVNSqlJetTableStatement {

    private String indexName;
    private Map<String, Object> rowValues;

    public SVNSqlJetSelectStatement(SVNSqlJetDb sDb, Enum<?> fromTable) throws SVNException {
        this(sDb, fromTable.toString());
    }

    public SVNSqlJetSelectStatement(SVNSqlJetDb sDb, Enum<?> fromTable, Enum<?> indexName) throws SVNException {
        this(sDb, fromTable.toString(), indexName != null ? indexName.toString() : null);
    }

    public SVNSqlJetSelectStatement(SVNSqlJetDb sDb, String fromTable) throws SVNException {
        super(sDb, fromTable);
    }

    public SVNSqlJetSelectStatement(SVNSqlJetDb sDb, String fromTable, String indexName) throws SVNException {
        this(sDb, fromTable);
        this.indexName = indexName;
    }

    protected ISqlJetCursor openCursor() throws SVNException {
        try {
            Object[] where = getWhere();
            if (isPathScoped()) {
                where = new Object[] {where[0], getPathScope()};
                return getTable().scope(getIndexName(), where, null);
            }
            return getTable().lookup(getIndexName(), where);
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
            return null;
        }
    }

    private boolean isPathScoped() throws SVNException {
        Object[] where = getWhere();
        return getPathScope() != null && SVNWCDbSchema.NODES.toString().equals(getTableName()) && where.length == 1;
    }
    
    protected String getPathScope() {
        return null;
    }

    protected boolean isStrictiDescendant() {
        return false;
    }

    protected String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    protected Object[] getWhere() throws SVNException {
        if (binds.size() == 0) {
            return null;
        }
        return binds.toArray();
    }

    public boolean next() throws SVNException {
        boolean next = false;
        do {
            next = super.next();
            loadRowValues(next);
            if (next && !pathScopeMatches()) {
                return false;
            }
        } while(next && !pathIsDecendant());
        
        while (next && !isFilterPassed()) {
            do {
                next = super.next();
                loadRowValues(next);
                if (next && !pathScopeMatches()) {
                    return false;
                }
            } while(next && !pathIsDecendant());
        }
        return next;
    }

    private boolean pathScopeMatches() throws SVNException {
        if (isPathScoped()) {
            final String rowPath = getRowPath();
            if ("".equals(getPathScope()) && !(isStrictiDescendant() && "".equals(rowPath))) {
                return true;
            }
            if (rowPath != null) {
                return (!isStrictiDescendant() && getPathScope().equals(rowPath)) || rowPath.startsWith(getPathScope());
            }
            return false;
        }
        return true;
    }

    private boolean pathIsDecendant() throws SVNException {
        if (isPathScoped()) {
            final String rowPath = getRowPath();
            if (rowPath != null) {
                if ("".equals(getPathScope()) && !(isStrictiDescendant() && "".equals(rowPath))) {
                    return true;
                }
                return (!isStrictiDescendant() && getPathScope().equals(rowPath)) || rowPath.startsWith(getPathScope() + "/");
            }
            return false;
        }
        return true;
    }

    private String getRowPath() {
        if (SVNWCDbSchema.NODES__Indices.I_NODES_PARENT.toString().equals(getIndexName())) {
            return (String) rowValues.get(SVNWCDbSchema.NODES__Fields.parent_relpath.toString());
        }
        return (String) rowValues.get(SVNWCDbSchema.NODES__Fields.local_relpath.toString());
    }

    protected boolean isFilterPassed() throws SVNException {
        return true;
    }

    public boolean eof() throws SVNException {
        boolean eof = true;
        do {
            eof = eof ? super.eof() : !super.next();
            loadRowValues(!eof);
            if (!eof && !pathScopeMatches()) {
                return true;
            }
        } while(!eof && !pathIsDecendant());
            
        while (!eof && !isFilterPassed()) {
            do {
                eof = !super.next();
                loadRowValues(!eof);
                if (!eof && !pathScopeMatches()) {
                    return true;
                }
                
            } while(!eof && !pathIsDecendant());
        }
        return eof;
    }

    private void loadRowValues(boolean has) throws SVNException {
        if (has) {
            rowValues = getRowValues2(rowValues);
        } else if (rowValues != null) {
            rowValues.clear();
        }
    }

    public Map<String, Object> getRowValues2(Map<String, Object> v) throws SVNException {
        v = v == null ? new HashMap<String, Object>() : v;
        try {
            Object[] values = getCursor().getRowValues();
            List<ISqlJetColumnDef> columns = getTable().getDefinition().getColumns();
            for (int i = 0; i < values.length; i++) {
                String colName = columns.get(i).getName();
                v.put(colName, values[i]);
            }
            return v;
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
            return null;
        }
    }

    public Map<String, Object> getRowValues() throws SVNException {
        HashMap<String, Object> v = new HashMap<String, Object>();
        try {
            List<ISqlJetColumnDef> columns = getTable().getDefinition().getColumns();
            for (ISqlJetColumnDef column : columns) {
                String colName = column.getName();
                SqlJetValueType fieldType = getCursor().getFieldType(colName);
                if (fieldType == SqlJetValueType.NULL) {
                    v.put(colName, null);
                } else if (fieldType == SqlJetValueType.BLOB) {
                    v.put(colName, getCursor().getBlobAsArray(colName));
                } else {
                    v.put(colName, getCursor().getValue(colName));
                }
            }
            return v;
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
            return null;
        }
    }

    @Override
    protected Object getColumn(String f) throws SVNException {
        return rowValues != null ? rowValues.get(f) : null;
    }
    @Override
    protected long getColumnLong(String f) throws SVNException {
        if (rowValues == null) {
            return 0;
        }
        Object v = rowValues.get(f);
        if (v instanceof Long) {
            return (Long) v;
        } else if (v instanceof String) {
            try {
                return Long.parseLong((String) v);
            } catch (NumberFormatException nfe) {
                return 0;
            }
        }
        return 0;
    }

    @Override
    protected String getColumnString(String f) throws SVNException {
        if (rowValues == null) {
            return null;
        }
        Object v = rowValues.get(f);
        if (v == null) {
            return null;
        }
        return v instanceof String ? (String) v : v.toString();
    }

    @Override
    protected boolean isColumnNull(String f) throws SVNException {
        if (rowValues == null) {
            return true;
        }
        return rowValues.get(f) == null;
    }

    @Override
    protected byte[] getColumnBlob(String f) throws SVNException {
        if (rowValues == null) {
            return null;
        }
        Object v = rowValues.get(f);
        if (v instanceof ISqlJetMemoryPointer) {
            ISqlJetMemoryPointer buffer = (ISqlJetMemoryPointer) v;
            return buffer != null ? SqlJetUtility.readByteBuffer(buffer) : null;
        } else if (v instanceof String) {
            try {
                return ((String) v).getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                return ((String) v).getBytes();
            }
        } else if (v instanceof byte[]) {
            return (byte[]) v;
        }
        return null;
    }

    @Override
    public void reset() throws SVNException {
        if (rowValues != null) {
            rowValues.clear();
        }
        super.reset();
    }
}
