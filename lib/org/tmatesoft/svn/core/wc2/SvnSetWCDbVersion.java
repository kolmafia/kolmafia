package org.tmatesoft.svn.core.wc2;

import java.io.File;

import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb.Mode;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgUpgradeSDb;
import org.tmatesoft.svn.util.SVNLogType;

/**
 *             
 * @author TMate Software Ltd. 
 * @version 1.8            
 */
public class SvnSetWCDbVersion extends SvnOperation<Void> {
    
    public static final int WC_DB_17_VERSION = 29;
    public static final int WC_DB_18_VERSION = 31;
    
    private int version;
    
    protected SvnSetWCDbVersion(SvnOperationFactory factory) {
        super(factory);
    }

    public void setVersion(int version) {
        this.version = version;
    }
    
    public int getVersion() {
        return version;
    }
    
    @Override
    public boolean isChangesWorkingCopy() {
        return false;
    }

    @Override
    public Void run() throws SVNException {
        for (SvnTarget target : getTargets()) {
            if (!target.isFile()) {
                continue;
            }
            final File wc = getFirstTarget().getFile();
            final File wcDb = new File(wc, SVNFileUtil.getAdminDirectoryName() + "/wc.db");
            if (SVNFileType.getType(wcDb) != SVNFileType.FILE) { 
                continue;
            }
            final SVNSqlJetDb db = SVNSqlJetDb.open(wcDb, Mode.ReadWrite);
            db.beginTransaction(SqlJetTransactionMode.WRITE);
            try {
                SvnNgUpgradeSDb.setVersion(db, getVersion());
            } finally {
                db.commit();
            }
        }
        return null;
    }

    @Override
    protected void initDefaults() {
        super.initDefaults();
        setVersion(WC_DB_18_VERSION);
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        super.ensureArgumentsAreValid();
        if (getVersion() != WC_DB_17_VERSION && getVersion() != WC_DB_18_VERSION) {
            final SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.INCORRECT_PARAMS, "wc.db version could only be {1} or {2}.", 
                    new Object[] {WC_DB_17_VERSION, WC_DB_18_VERSION});
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }
}