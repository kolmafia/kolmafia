package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.wc.ISVNDiffStatusHandler;
import org.tmatesoft.svn.core.wc.SVNDiffStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver;
import org.tmatesoft.svn.core.wc2.SvnDiffStatus;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class SvnDiffSummarizeCallback implements ISvnDiffCallback {

    private final ISVNDiffStatusHandler handler;
    private final Set<File> propChanges;
    private final boolean reversed;
    private final SVNURL baseUrl;
    private final File baseDirectory;
    private final File target;

    public SvnDiffSummarizeCallback(File targetAbsPath, boolean reversed, SVNURL baseUrl, File baseDirectory, ISVNDiffStatusHandler handler) {
        this.handler = handler;
        this.reversed = reversed;
        this.target = targetAbsPath;
        this.baseUrl = baseUrl;
        this.baseDirectory = baseDirectory;
        this.propChanges = new HashSet<File>();
    }

    public void fileOpened(SvnDiffCallbackResult result, File path, long revision) throws SVNException {
    }

    public void fileChanged(SvnDiffCallbackResult result, File path, File leftFile, File rightFile, long rev1, long rev2, String mimeType1, String mimeType2, SVNProperties propChanges, SVNProperties originalProperties) throws SVNException {
        boolean textChange = rightFile != null;
        SVNProperties regularPropChanges = new SVNProperties();
        SvnNgPropertiesManager.categorizeProperties(propChanges, regularPropChanges, null, null);
        boolean propChange = regularPropChanges.size() > 0;

        if (textChange || propChange) {
            sendSummary(path, textChange ? SVNStatusType.STATUS_MODIFIED : SVNStatusType.STATUS_NORMAL, propChange, SVNNodeKind.FILE);
        }
    }

    public void fileAdded(SvnDiffCallbackResult result, File path, File leftFile, File rightFile, long rev1, long rev2, String mimeType1, String mimeType2, File copyFromPath, long copyFromRevision, SVNProperties propChanges, SVNProperties originalProperties) throws SVNException {
        SVNProperties regularPropChanges = new SVNProperties();
        SvnNgPropertiesManager.categorizeProperties(propChanges, regularPropChanges, null, null);
        sendSummary(path, SVNStatusType.STATUS_ADDED, regularPropChanges.size() > 0, SVNNodeKind.FILE);
    }

    public void fileDeleted(SvnDiffCallbackResult result, File path, File leftFile, File rightFile, String mimeType1, String mimeType2, SVNProperties originalProperties) throws SVNException {
        sendSummary(path, SVNStatusType.STATUS_DELETED, false, SVNNodeKind.FILE);
    }

    public void dirDeleted(SvnDiffCallbackResult result, File path) throws SVNException {
        sendSummary(path, SVNStatusType.STATUS_DELETED, false, SVNNodeKind.DIR);
    }

    public void dirOpened(SvnDiffCallbackResult result, File path, long revision) throws SVNException {
    }

    public void dirAdded(SvnDiffCallbackResult result, File path, long revision, String copyFromPath, long copyFromRevision) throws SVNException {
    }

    public void dirPropsChanged(SvnDiffCallbackResult result, File path, boolean isAdded, SVNProperties propChanges, SVNProperties originalProperties) throws SVNException {
        SVNProperties regularPropChanges = new SVNProperties();
        SvnNgPropertiesManager.categorizeProperties(propChanges, regularPropChanges, null, null);
        if (regularPropChanges.size() > 0) {
            this.propChanges.add(path);
        }
    }

    public void dirClosed(SvnDiffCallbackResult result, File path, boolean isAdded) throws SVNException {
        if (SVNFileUtil.skipAncestor(this.target, path) == null) {
            return;
        }
        boolean propChange = this.propChanges.contains(path);
        if (isAdded || propChange) {
            sendSummary(path, isAdded ? SVNStatusType.STATUS_ADDED : SVNStatusType.STATUS_NORMAL, propChange, SVNNodeKind.DIR);
        }
    }

    private void sendSummary(File path, SVNStatusType summarizeKind, boolean propChanged, SVNNodeKind nodeKind) throws SVNException {
        assert summarizeKind != SVNStatusType.STATUS_NORMAL || propChanged;

        if (this.reversed) {
            if (summarizeKind == SVNStatusType.STATUS_ADDED) {
                summarizeKind = SVNStatusType.STATUS_DELETED;
            } else if (summarizeKind == SVNStatusType.STATUS_DELETED) {
                summarizeKind = SVNStatusType.STATUS_ADDED;
            }
        }
        File relPath = SVNFileUtil.isAbsolute(path) ? SVNFileUtil.skipAncestor(baseDirectory, path): path;
        File relTarget = this.target == null ? null : (SVNFileUtil.isAbsolute(this.target) ? SVNFileUtil.skipAncestor(new File("").getAbsoluteFile(), this.target) : this.target);
        File realRelPath = relTarget == null ? relPath : SVNFileUtil.skipAncestor(relTarget, relPath);
        SVNDiffStatus diffStatus = new SVNDiffStatus(path, baseUrl == null ? null : baseUrl.appendPath(SVNFileUtil.getFilePath(relPath), false), SVNFileUtil.getFilePath(realRelPath), summarizeKind,
                (summarizeKind == SVNStatusType.STATUS_MODIFIED || summarizeKind == SVNStatusType.STATUS_NORMAL) ? propChanged : false, nodeKind);
        handler.handleDiffStatus(diffStatus);
    }

    private static enum SvnSummarizeKind {
        NORMAL, ADDED, MODIFIED, DELETED
    }
}
