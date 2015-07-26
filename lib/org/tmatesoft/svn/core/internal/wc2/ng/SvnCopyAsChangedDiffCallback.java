package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;

import java.io.File;

public class SvnCopyAsChangedDiffCallback implements ISvnDiffCallback2 {

    private final ISvnDiffCallback2 delegate;

    public SvnCopyAsChangedDiffCallback(ISvnDiffCallback2 delegate) {
        this.delegate = delegate;
    }

    public void fileOpened(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, SvnDiffSource copyFromSource, boolean createDirBaton, Object dirBaton) throws SVNException {
        if (leftSource == null && copyFromSource != null) {
            assert rightSource != null;
            leftSource = copyFromSource;
            copyFromSource = null;
        }
        delegate.fileOpened(result, relPath, leftSource, rightSource, copyFromSource, createDirBaton, dirBaton);
    }

    public void fileChanged(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, File leftFile, File rightFile, SVNProperties leftProps, SVNProperties rightProps, boolean fileModified, SVNProperties propChanges) throws SVNException {
        delegate.fileChanged(result, relPath, leftSource, rightSource, leftFile, rightFile, leftProps, rightProps, fileModified, propChanges);
    }

    public void fileAdded(SvnDiffCallbackResult result, File relPath, SvnDiffSource copyFromSource, SvnDiffSource rightSource, File copyFromFile, File rightFile, SVNProperties copyFromProps, SVNProperties rightProps) throws SVNException {
        if (copyFromSource != null) {
            SVNProperties propChanges = copyFromProps.compareTo(rightProps);

            boolean same;
            if (copyFromFile != null && rightFile != null) {
                same = SVNFileUtil.compareFiles(copyFromFile, rightFile, null);
            } else {
                same = false;
            }
            delegate.fileChanged(result, relPath, copyFromSource, rightSource, copyFromFile, rightFile, copyFromProps, rightProps, !same, propChanges);
        } else {

            delegate.fileAdded(result, relPath, copyFromSource, rightSource, copyFromFile, rightFile, copyFromProps, rightProps);
        }
    }

    public void fileDeleted(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, File leftFile, SVNProperties leftProps) throws SVNException {
        delegate.fileDeleted(result, relPath, leftSource, leftFile, leftProps);
    }

    public void fileClosed(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource) throws SVNException {
        delegate.fileClosed(result, relPath, leftSource, rightSource);
    }

    public void dirOpened(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, SvnDiffSource copyFromSource, Object dirBaton) throws SVNException {
        if (leftSource == null && copyFromSource != null) {
            assert rightSource != null;
            leftSource = copyFromSource;
            copyFromSource = null;
        }
        delegate.dirOpened(result, relPath, leftSource, rightSource, copyFromSource, dirBaton);
    }

    public void dirChanged(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, SVNProperties leftProps, SVNProperties rightProps, SVNProperties propChanges, Object dirBaton) throws SVNException {
        delegate.dirChanged(result, relPath, leftSource, rightSource, leftProps, rightProps, propChanges, dirBaton);
    }

    public void dirDeleted(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SVNProperties leftProps, Object dirBaton) throws SVNException {
        delegate.dirDeleted(result, relPath, leftSource, leftProps, dirBaton);
    }

    public void dirAdded(SvnDiffCallbackResult result, File relPath, SvnDiffSource copyFromSource, SvnDiffSource rightSource, SVNProperties copyFromProps, SVNProperties rightProps, Object dirBaton) throws SVNException {
        if (copyFromSource != null) {
            SVNProperties propChanges = copyFromProps.compareTo(rightProps);
            delegate.dirChanged(result, relPath, copyFromSource, rightSource, copyFromProps, rightProps, propChanges, dirBaton);
        } else {
            delegate.dirAdded(result, relPath, copyFromSource, rightSource, copyFromProps, rightProps, dirBaton);
        }
    }

    public void dirClosed(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, Object dirBaton) throws SVNException {
        delegate.dirClosed(result, relPath, leftSource, rightSource, dirBaton);
    }

    public void nodeAbsent(SvnDiffCallbackResult result, File relPath, Object dirBaton) throws SVNException {
        delegate.nodeAbsent(result, relPath, dirBaton);
    }
}
