package org.tmatesoft.svn.core.internal.wc17.db;

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNReporter17;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo.BaseInfoField;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;

public class SvnExternalFileReporter implements ISVNReporterBaton {
    
    private SVNWCContext context;
    private File localAbsPath;
    private boolean restoreFiles;
    private boolean useCommitTimes;

    public SvnExternalFileReporter(SVNWCContext context, File localAbsPath, boolean restoreFiles, boolean useCommitTimes) {
        this.context = context;
        this.localAbsPath = localAbsPath;
        this.restoreFiles = restoreFiles;
        this.useCommitTimes = useCommitTimes;
    }

    public void report(ISVNReporter reporter) throws SVNException {
        WCDbBaseInfo baseInfo = null;
        
        try {
            baseInfo = context.getDb().getBaseInfo(localAbsPath, BaseInfoField.kind, BaseInfoField.revision, BaseInfoField.reposRelPath, BaseInfoField.reposRootUrl, BaseInfoField.updateRoot, BaseInfoField.lock);
            if (baseInfo.kind == SVNWCDbKind.Dir || !baseInfo.updateRoot) {
                reporter.setPath("", null, 0, SVNDepth.INFINITY, false);
                reporter.deletePath("");
            } else {
                if (restoreFiles) {
                    SVNFileType ft = SVNFileType.getType(localAbsPath);
                    if (ft == SVNFileType.NONE) {
                        SVNReporter17.restoreNode(context, localAbsPath, SVNWCDbKind.File, baseInfo.revision, useCommitTimes);
                    }
                }
                reporter.setPath("", null, baseInfo.revision, SVNDepth.INFINITY, false);
                SVNURL url = baseInfo.reposRootUrl.appendPath(SVNFileUtil.getFilePath(baseInfo.reposRelPath), false);
                String lockToken = baseInfo.lock != null ? baseInfo.lock.token : null;
                reporter.linkPath(url, "", lockToken, baseInfo.revision, SVNDepth.INFINITY, false);
            }
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_PATH_NOT_FOUND) {
                throw e;
            }
            reporter.setPath("", null, 0, SVNDepth.INFINITY, false);
            reporter.deletePath("");
        }
        reporter.finishReport();
    }

}
