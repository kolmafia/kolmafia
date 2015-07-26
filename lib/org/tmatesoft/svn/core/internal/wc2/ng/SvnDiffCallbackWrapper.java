package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.File;

public class SvnDiffCallbackWrapper implements ISvnDiffCallback2 {

    private final ISvnDiffCallback callback;
    private final boolean deleteDirs;
    private final File anchorAbsPath;

    public SvnDiffCallbackWrapper(ISvnDiffCallback callback, boolean deleteDirs, File anchorAbsPath) {
        this.callback = callback;
        this.deleteDirs = deleteDirs;
        this.anchorAbsPath = anchorAbsPath;
    }

    public void fileOpened(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, SvnDiffSource copyFromSource, boolean createDirBaton, Object dirBaton) throws SVNException {
        if (leftSource != null) {
            callback.fileOpened(result, getAbsPath(relPath), rightSource != null ? rightSource.getRevision() : (leftSource != null ? leftSource.getRevision() : SVNRepository.INVALID_REVISION));
        }
        result.newBaton = null;
    }

    public void fileChanged(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, File leftFile, File rightFile, SVNProperties leftProps, SVNProperties rightProps, boolean fileModified, SVNProperties propChanges) throws SVNException {
        assert leftSource != null && rightSource != null;

        callback.fileChanged(result, getAbsPath(relPath), fileModified ? leftFile : null, fileModified ? rightFile : null, leftSource.getRevision(), rightSource.getRevision(), leftProps == null ? null : leftProps.getStringValue(SVNProperty.MIME_TYPE), rightProps == null ? null : rightProps.getStringValue(SVNProperty.MIME_TYPE), propChanges, leftProps);
    }

    public void fileAdded(SvnDiffCallbackResult result, File relPath, SvnDiffSource copyFromSource, SvnDiffSource rightSource, File copyFromFile, File rightFile, SVNProperties copyFromProps, SVNProperties rightProps) throws SVNException {
        if (copyFromProps == null) {
            copyFromProps = new SVNProperties();
        }
        SVNProperties propChanges = copyFromProps.compareTo(rightProps);

        callback.fileAdded(result, getAbsPath(relPath), copyFromSource != null ? copyFromFile : null, rightFile, 0, rightSource.getRevision(), copyFromProps != null ? copyFromProps.getStringValue(SVNProperty.MIME_TYPE) : null, rightProps != null ? rightProps.getStringValue(SVNProperty.MIME_TYPE) : null, copyFromSource != null ? copyFromSource.getReposRelPath() : null, copyFromSource != null ? copyFromSource.getRevision() : SVNRepository.INVALID_REVISION, propChanges, copyFromProps);
    }

    public void fileDeleted(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, File leftFile, SVNProperties leftProps) throws SVNException {
        callback.fileDeleted(result, getAbsPath(relPath), leftFile, null, leftProps == null ? null : leftProps.getStringValue(SVNProperty.MIME_TYPE), null, leftProps);
    }

    public void fileClosed(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource) throws SVNException {
    }

    public void dirOpened(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, SvnDiffSource copyFromSource, Object dirBaton) throws SVNException {
        assert (leftSource != null || rightSource != null);
        assert (copyFromSource == null || rightSource != null);

        if (leftSource != null) {
            callback.dirOpened(result, getAbsPath(relPath), rightSource != null ? rightSource.getRevision() :
                    (leftSource != null ? leftSource.getRevision() : SVNRepository.INVALID_REVISION));
            if (rightSource == null && !deleteDirs) {
                result.skipChildren = true;
            }
        } else {
            callback.dirAdded(result, getAbsPath(relPath), rightSource.getRevision(), copyFromSource != null ? SVNFileUtil.getFilePath(copyFromSource.getReposRelPath()) : null, copyFromSource != null ? copyFromSource.getRevision() : SVNRepository.INVALID_REVISION);
        }
        result.newBaton = null;
    }

    public void dirChanged(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, SVNProperties leftProps, SVNProperties rightProps, SVNProperties propChanges, Object dirBaton) throws SVNException {
        assert leftSource != null && rightSource != null;

        callback.dirPropsChanged(result, getAbsPath(relPath), false, propChanges, leftProps);
        dirClosed(result, getAbsPath(relPath), leftSource, rightSource, dirBaton);
    }

    public void dirDeleted(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SVNProperties leftProps, Object dirBaton) throws SVNException {
        callback.dirDeleted(result, getAbsPath(relPath));
    }

    public void dirAdded(SvnDiffCallbackResult result, File relPath, SvnDiffSource copyFromSource, SvnDiffSource rightSource, SVNProperties copyFromProps, SVNProperties rightProps, Object dirBaton) throws SVNException {
        SVNProperties pristineProps = copyFromProps;
        if (rightProps != null && rightProps.size() > 0) {
            if (pristineProps == null) {
                pristineProps = new SVNProperties();
            }
            SVNProperties propChanges = pristineProps.compareTo(rightProps);
            callback.dirPropsChanged(result, getAbsPath(relPath), true, propChanges, pristineProps);
        }
        callback.dirClosed(result, getAbsPath(relPath), true);
    }

    public void dirClosed(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, Object dirBaton) throws SVNException {
        callback.dirClosed(result, getAbsPath(relPath), false);
    }

    public void nodeAbsent(SvnDiffCallbackResult result, File relPath, Object dirBaton) throws SVNException {
    }

    private File getAbsPath(File relPath) {
        return SVNFileUtil.createFilePath(anchorAbsPath, relPath);
    }
}
