package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.Set;
import java.util.logging.Level;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.SVNWCUtils;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbOpenMode;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo.InfoField;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc2.SvnCleanup;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgCleanup extends SvnNgOperationRunner<Void, SvnCleanup> {

    @Override
    protected Void run(SVNWCContext context) throws SVNException {
        
        if (getOperation().getFirstTarget().isURL()) {
        	SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET,
        			"''{0}'' is not a local path", getOperation().getFirstTarget().getURL());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        File localAbsPath = getOperation().getFirstTarget().getFile().getAbsoluteFile();
        SVNWCDb db = new SVNWCDb();
    	SVNWCContext wcContext = new SVNWCContext(db, context.getEventHandler());
        cleanup(wcContext, localAbsPath);
        sleepForTimestamp();
        
        return null;
    }
    
    private void cleanup(SVNWCContext wcContext, File localAbsPath) throws SVNException {
    	try {
    		wcContext.getDb().open(SVNWCDbOpenMode.ReadWrite, (ISVNOptions)null, true, false);
    		cleanupInternal(wcContext, localAbsPath);
    		wcContext.getDb().clearDavCacheRecursive(localAbsPath);
    	}
    	finally {
    		wcContext.getDb().close();
    	}
    }
    
    private int canBeCleaned(SVNWCContext wcContext, File localAbsPath) throws SVNException {
    	int wcFormat = wcContext.checkWC(localAbsPath);
    	if (wcFormat == 0) {
    		SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_NOT_WORKING_COPY,
        			"''{0}'' is not a working copy directory", localAbsPath);
            SVNErrorManager.error(err, SVNLogType.WC);
    	}
    	if (wcFormat < SVNWCContext.WC_NG_VERSION) {
    		SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_UNSUPPORTED_FORMAT,
        			"Log format too old, please use Subversion 1.6 or earlier");
            SVNErrorManager.error(err, SVNLogType.WC);
    	}
    	return wcFormat;
    }
    
    private void cleanupInternal(SVNWCContext wcContext, File localAbsPath) throws SVNException {
    	int wcFormat = canBeCleaned(wcContext, localAbsPath);
    	wcContext.getDb().obtainWCLock(localAbsPath, -1, true);
    	if (wcFormat >= ISVNWCDb.WC_HAS_WORK_QUEUE) {
    		wcContext.wqRun(localAbsPath);
    	}
    	File cleanupWCRoot = wcContext.getDb().getWCRoot(localAbsPath);
    	if (cleanupWCRoot.equals(localAbsPath)) {
    		SVNWCUtils.admCleanupTmpArea(wcContext, localAbsPath);
    		wcContext.getDb().cleanupPristine(localAbsPath);
    	}
    	repairTimestamps(wcContext, localAbsPath);
    	wcContext.getDb().releaseWCLock(localAbsPath);
    }
    
    public static void repairTimestamps(SVNWCContext wcContext, File localAbsPath) throws SVNException {
    	wcContext.checkCancelled();
    	WCDbInfo info = wcContext.getDb().readInfo(localAbsPath, InfoField.status, InfoField.kind);
    	if (info.status == ISVNWCDb.SVNWCDbStatus.ServerExcluded
    			|| info.status == ISVNWCDb.SVNWCDbStatus.Deleted
    			|| info.status == ISVNWCDb.SVNWCDbStatus.Excluded
    			|| info.status == ISVNWCDb.SVNWCDbStatus.NotPresent
    			) {
    		return;
    	}
    	if (info.kind == ISVNWCDb.SVNWCDbKind.File || info.kind == ISVNWCDb.SVNWCDbKind.Symlink) {
    	    try {
    	        wcContext.isTextModified(localAbsPath, false);
    	    } catch (SVNException e) {
    	        SVNDebugLog.getDefaultLog().log(SVNLogType.WC, e, Level.WARNING);
    	    }
    	}
    	else if (info.kind == ISVNWCDb.SVNWCDbKind.Dir) {
    		Set<String> children = wcContext.getDb().readChildren(localAbsPath);
    		for (String childPath : children) {
    			File childAbsPath = SVNFileUtil.createFilePath(localAbsPath, childPath);
    			repairTimestamps(wcContext, childAbsPath);
    		}
    	}
    	return;
    }
}
