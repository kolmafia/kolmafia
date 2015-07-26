package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;

import java.io.File;

public class SvnReverseOrderDiffCallback implements ISvnDiffCallback2 {

    private final ISvnDiffCallback2 delegate;
    private final String prefixRelPath;

    public SvnReverseOrderDiffCallback(ISvnDiffCallback2 delegate, String prefixRelPath) {
        this.delegate = delegate;
        this.prefixRelPath = prefixRelPath;
    }

    public void fileOpened(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, SvnDiffSource copyFromSource, boolean createDirBaton, Object dirBaton) throws SVNException {
        if (prefixRelPath != null) {
            relPath = SVNFileUtil.createFilePath(prefixRelPath, SVNFileUtil.getFilePath(relPath));
        }
        delegate.fileOpened(result, relPath, rightSource, leftSource, null, createDirBaton, dirBaton);
    }

    public void fileChanged(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, File leftFile, File rightFile, SVNProperties leftProps, SVNProperties rightProps, boolean fileModified, SVNProperties propChanges) throws SVNException {
        if (prefixRelPath != null) {
            relPath = SVNFileUtil.createFilePath(prefixRelPath, SVNFileUtil.getFilePath(relPath));
        }
        SVNProperties reversedPropChanges = null;
        if (propChanges != null) {
            assert leftProps != null && rightProps != null;
            reversedPropChanges = rightProps.compareTo(leftProps);
        }
        delegate.fileChanged(result, relPath, rightSource, leftSource, rightFile, leftFile, rightProps, leftProps, fileModified, reversedPropChanges);
    }

    public void fileAdded(SvnDiffCallbackResult result, File relPath, SvnDiffSource copyFromSource, SvnDiffSource rightSource, File copyFromFile, File rightFile, SVNProperties copyFromProps, SVNProperties rightProps) throws SVNException {
        if (prefixRelPath != null) {
            relPath = SVNFileUtil.createFilePath(prefixRelPath, SVNFileUtil.getFilePath(relPath));
        }
        delegate.fileDeleted(result, relPath, rightSource, rightFile, rightProps);
    }

    public void fileDeleted(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, File leftFile, SVNProperties leftProps) throws SVNException {
        if (prefixRelPath != null) {
            relPath = SVNFileUtil.createFilePath(prefixRelPath, SVNFileUtil.getFilePath(relPath));
        }
        delegate.fileAdded(result, relPath, null, leftSource, null, leftFile, null, leftProps);
    }

    public void fileClosed(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource) throws SVNException {
        if (prefixRelPath != null) {
            relPath = SVNFileUtil.createFilePath(prefixRelPath, SVNFileUtil.getFilePath(relPath));
        }
        delegate.fileClosed(result, relPath, rightSource, leftSource);
    }

    public void dirOpened(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, SvnDiffSource copyFromSource, Object dirBaton) throws SVNException {
        if (prefixRelPath != null) {
            relPath = SVNFileUtil.createFilePath(prefixRelPath, SVNFileUtil.getFilePath(relPath));
        }
        delegate.dirOpened(result, relPath, rightSource, leftSource, null, dirBaton);
    }

    public void dirChanged(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, SVNProperties leftProps, SVNProperties rightProps, SVNProperties propChanges, Object dirBaton) throws SVNException {
        if (prefixRelPath != null) {
            relPath = SVNFileUtil.createFilePath(prefixRelPath, SVNFileUtil.getFilePath(relPath));
        }
        SVNProperties reversedPropChanges = null;
        if (propChanges != null) {
            assert leftProps != null && rightProps != null;
            reversedPropChanges = rightProps.compareTo(leftProps);
        }
        delegate.dirChanged(result, relPath, rightSource, leftSource, rightProps, leftProps, reversedPropChanges, dirBaton);
    }

    public void dirDeleted(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SVNProperties leftProps, Object dirBaton) throws SVNException {
        if (prefixRelPath != null) {
            relPath = SVNFileUtil.createFilePath(prefixRelPath, SVNFileUtil.getFilePath(relPath));
        }
        delegate.dirAdded(result, relPath, null, leftSource, null, leftProps, dirBaton);
    }

    public void dirAdded(SvnDiffCallbackResult result, File relPath, SvnDiffSource copyFromSource, SvnDiffSource rightSource, SVNProperties copyFromProps, SVNProperties rightProps, Object dirBaton) throws SVNException {
        if (prefixRelPath != null) {
            relPath = SVNFileUtil.createFilePath(prefixRelPath, SVNFileUtil.getFilePath(relPath));
        }
        delegate.dirDeleted(result, relPath, rightSource, rightProps, dirBaton);
    }

    public void dirClosed(SvnDiffCallbackResult result, File relPath, SvnDiffSource leftSource, SvnDiffSource rightSource, Object dirBaton) throws SVNException {
        if (prefixRelPath != null) {
            relPath = SVNFileUtil.createFilePath(prefixRelPath, SVNFileUtil.getFilePath(relPath));
        }
        delegate.dirClosed(result, relPath, rightSource, leftSource, dirBaton);
    }

    public void nodeAbsent(SvnDiffCallbackResult result, File relPath, Object dirBaton) throws SVNException {
        if (prefixRelPath != null) {
            relPath = SVNFileUtil.createFilePath(prefixRelPath, SVNFileUtil.getFilePath(relPath));
        }
        delegate.nodeAbsent(result, relPath, dirBaton);
    }
}
