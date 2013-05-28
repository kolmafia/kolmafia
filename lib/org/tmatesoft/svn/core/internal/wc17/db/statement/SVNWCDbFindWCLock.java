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

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetSelectFieldsStatement;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;

/**
 * SELECT local_dir_relpath FROM wc_lock WHERE wc_id = ?1 AND local_dir_relpath
 * LIKE ?2 ESCAPE '#';
 *
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbFindWCLock extends SVNSqlJetSelectFieldsStatement<SVNWCDbSchema.WC_LOCK__Fields> {

    public SVNWCDbFindWCLock(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.WC_LOCK);
    }

    protected void defineFields() {
        fields.add(SVNWCDbSchema.WC_LOCK__Fields.local_dir_relpath);
    }

    protected boolean isFilterPassed() throws SVNException {
        if (getBinds().size() < 2) {
            return super.isFilterPassed();
        }
        Object bind = getBind(2);
        if (bind == null) {
            return isColumnNull(SVNWCDbSchema.WC_LOCK__Fields.local_dir_relpath);
        }
        if (bind instanceof File) {
            if (isColumnNull(SVNWCDbSchema.WC_LOCK__Fields.local_dir_relpath)) {
                return false;
            }
            File path = (File) bind;
            File localDirPath = SVNFileUtil.createFilePath(getColumnString(SVNWCDbSchema.WC_LOCK__Fields.local_dir_relpath));
            return SVNWCUtils.isAncestor(path, localDirPath);
        }
        return super.isFilterPassed();
    }

    protected Object[] getWhere() throws SVNException {
        return new Object[] {
            getBind(1)
        };
    }

}
