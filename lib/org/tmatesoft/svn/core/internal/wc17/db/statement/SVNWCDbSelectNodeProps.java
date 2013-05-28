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
package org.tmatesoft.svn.core.internal.wc17.db.statement;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;


/**
  SELECT properties, presence FROM nodes
  WHERE wc_id = ?1 AND local_relpath = ?2
  ORDER BY op_depth DESC;

 * @author  TMate Software Ltd.
 */
public class SVNWCDbSelectNodeProps extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.NODES__Fields> {

    public SVNWCDbSelectNodeProps(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.NODES);
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.NODES__Fields.properties);
        fields.add(SVNWCDbSchema.NODES__Fields.presence);
    }

    @Override
    protected ISqlJetCursor openCursor() throws SVNException {
        try {
            return super.openCursor().reverse();
        } catch (SqlJetException e) {
            SVNSqlJetDb.createSqlJetError(e);
        }
        return null;
    }
    
    

}
