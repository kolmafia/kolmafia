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
package org.tmatesoft.svn.core.internal.wc17;

import java.io.File;
import java.io.OutputStream;
import java.util.LinkedList;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.wc.ISVNUpdateEditor;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbKind;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbStatus;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbBaseInfo.BaseInfoField;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.WCDbInfo.InfoField;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author TMate Software Ltd.
 */
public class SVNAmbientDepthFilterEditor17 implements ISVNUpdateEditor {

    private ISVNEditor myDelegate;
    private SVNWCContext myWcContext;
    private File myAnchor;
    private String myTarget;
    private boolean myReadBase;
    private LinkedList<DirBaton> myDirs;
    private DirBaton myCurrentDirBaton;
    private FileBaton myCurrentFileBaton;

    public SVNAmbientDepthFilterEditor17(ISVNUpdateEditor editor, SVNWCContext wcContext, File anchor, String target, boolean readBase) {
        myDelegate = editor;
        myWcContext = wcContext;
        myAnchor = anchor;
        myTarget = target;
        myReadBase = readBase;
        myDirs = new LinkedList<DirBaton>();
    }

    public static ISVNEditor wrap(SVNWCContext wcContext, File anchor, String target, ISVNUpdateEditor editor, boolean depthIsSticky) {
        if (!depthIsSticky) {
            return new SVNAmbientDepthFilterEditor17(editor, wcContext, anchor, target, true);
        }
        return editor;
    }

    public void applyTextDelta(String path, String baseChecksum) throws SVNException {
        if (myCurrentFileBaton.myIsAmbientlyExcluded) {
            return;
        }
        myDelegate.applyTextDelta(path, baseChecksum);
    }

    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        if (myCurrentFileBaton.myIsAmbientlyExcluded) {
            return SVNFileUtil.DUMMY_OUT;
        }
        return myDelegate.textDeltaChunk(path, diffWindow);
    }

    public void textDeltaEnd(String path) throws SVNException {
        if (myCurrentFileBaton.myIsAmbientlyExcluded) {
            return;
        }
        myDelegate.textDeltaEnd(path);
    }

    public void targetRevision(long revision) throws SVNException {
        myDelegate.targetRevision(revision);
    }

    public void openRoot(long revision) throws SVNException {
        myCurrentDirBaton = makeDirBaton(null, null);
        if (myCurrentDirBaton.myIsAmbientlyExcluded) {
            return;
        }

        if (myTarget == null || "".equals(myTarget)) {
            AmbientReadInfo aInfo = ambientReadInfo(myAnchor, myReadBase);
            if (aInfo.kind != SVNWCDbKind.Unknown && !aInfo.hidden) {
                myCurrentDirBaton.myAmbientDepth = aInfo.depth;
            }
        }

        myDelegate.openRoot(revision);
    }

    public void deleteEntry(String path, long revision) throws SVNException {
        if (myCurrentDirBaton.myIsAmbientlyExcluded) {
            return;
        }

        if (myCurrentDirBaton.myAmbientDepth.compareTo(SVNDepth.IMMEDIATES) < 0) {
            File abspath = SVNFileUtil.createFilePath(myAnchor, SVNWCUtils.getPathAsChild(myAnchor, SVNFileUtil.createFilePath(path)));
            AmbientReadInfo aInfo = ambientReadInfo(abspath, myReadBase);
            if (aInfo.kind == SVNWCDbKind.Unknown || aInfo.hidden) {
                return;
            }
        }

        myDelegate.deleteEntry(path, revision);
    }

    public void absentDir(String path) throws SVNException {
        if (myCurrentDirBaton.myIsAmbientlyExcluded) {
            return;
        }
        myDelegate.absentDir(path);
    }

    public void absentFile(String path) throws SVNException {
        if (myCurrentDirBaton.myIsAmbientlyExcluded) {
            return;
        }
        myDelegate.absentFile(path);
    }

    public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        DirBaton parentBaton = myCurrentDirBaton;
        myCurrentDirBaton = makeDirBaton(path, parentBaton);
        if (myCurrentDirBaton.myIsAmbientlyExcluded) {
            return;
        }

        if (path.equals(myTarget)) {
            myCurrentDirBaton.myAmbientDepth = SVNDepth.INFINITY;
        } else if (parentBaton.myAmbientDepth == SVNDepth.IMMEDIATES) {
            myCurrentDirBaton.myAmbientDepth = SVNDepth.EMPTY;
        } else {
            myCurrentDirBaton.myAmbientDepth = SVNDepth.INFINITY;
        }

        myDelegate.addDir(path, copyFromPath, copyFromRevision);
    }

    public void openDir(String path, long revision) throws SVNException {
        DirBaton parentBaton = myCurrentDirBaton;
        myCurrentDirBaton = makeDirBaton(path, parentBaton);
        if (myCurrentDirBaton.myIsAmbientlyExcluded) {
            return;
        }
        myDelegate.openDir(path, revision);
        File abspath = SVNFileUtil.createFilePath(myAnchor, SVNWCUtils.skipAncestor(myAnchor, SVNFileUtil.createFilePath(path)));
        AmbientReadInfo aInfo = ambientReadInfo(abspath, myReadBase);
        if (aInfo.kind != SVNWCDbKind.Unknown && !aInfo.hidden) {
            myCurrentDirBaton.myAmbientDepth = aInfo.depth;
        }
    }

    public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
        if (myCurrentDirBaton.myIsAmbientlyExcluded) {
            return;
        }
        myDelegate.changeDirProperty(name, value);
    }

    public void closeDir() throws SVNException {
        DirBaton closedDir = myDirs.removeLast();
        if (myDirs.isEmpty()) {
            myCurrentDirBaton = null;
        } else {
            myCurrentDirBaton = myDirs.getLast();
        }
        if (closedDir.myIsAmbientlyExcluded) {
            return;
        }
        myDelegate.closeDir();
    }

    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        myCurrentFileBaton = makeFileBaton(myCurrentDirBaton, path);
        if (myCurrentFileBaton.myIsAmbientlyExcluded) {
            return;
        }
        myDelegate.addFile(path, copyFromPath, copyFromRevision);
    }

    public void openFile(String path, long revision) throws SVNException {
        myCurrentFileBaton = makeFileBaton(myCurrentDirBaton, path);
        if (myCurrentFileBaton.myIsAmbientlyExcluded) {
            return;
        }
        myDelegate.openFile(path, revision);
    }

    public void changeFileProperty(String path, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        if (myCurrentFileBaton.myIsAmbientlyExcluded) {
            return;
        }
        myDelegate.changeFileProperty(path, propertyName, propertyValue);
    }

    public void closeFile(String path, String textChecksum) throws SVNException {
        if (myCurrentFileBaton.myIsAmbientlyExcluded) {
            return;
        }
        myDelegate.closeFile(path, textChecksum);
    }

    public SVNCommitInfo closeEdit() throws SVNException {
        return myDelegate.closeEdit();
    }

    public void abortEdit() throws SVNException {
    }

    private DirBaton makeDirBaton(String path, DirBaton parentBaton) throws SVNException {
        if (parentBaton != null && path == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "aborting in SVNAmbientDepthFilterEditor17.makeDirBation(): parentBaton != null" + " while path == null");
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }

        if (parentBaton != null && parentBaton.myIsAmbientlyExcluded) {
            myDirs.addLast(parentBaton);
            return parentBaton;
        }

        DirBaton dirBaton = new DirBaton();
        myDirs.addLast(dirBaton);

        if (parentBaton != null && parentBaton.myAmbientDepth != SVNDepth.UNKNOWN) {

            File abspath = SVNFileUtil.createFilePath(myAnchor, SVNWCUtils.getPathAsChild(myAnchor, SVNFileUtil.createFilePath(path)));

            AmbientReadInfo aInfo = ambientReadInfo(abspath, myReadBase);
            SVNWCDbStatus status = aInfo.status;
            SVNWCDbKind kind = aInfo.kind;

            boolean exclude;
            boolean exists = kind != SVNWCDbKind.Unknown;

            if (parentBaton.myAmbientDepth == SVNDepth.EMPTY || parentBaton.myAmbientDepth == SVNDepth.FILES) {
                exclude = !exists;
            } else {
                exclude = exists && (status == SVNWCDbStatus.Excluded);
            }
            if (exclude) {
                dirBaton.myIsAmbientlyExcluded = true;
                return dirBaton;
            }
        }

        dirBaton.myAmbientDepth = SVNDepth.UNKNOWN;
        return dirBaton;
    }

    private FileBaton makeFileBaton(DirBaton parentBaton, String path) throws SVNException {
        if (path == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "aborting in SVNAmbientDepthFilterEditor.makeFileBation(): path == null");
            SVNErrorManager.error(err, SVNLogType.DEFAULT);
        }

        FileBaton fileBaton = new FileBaton();
        if (parentBaton.myIsAmbientlyExcluded) {
            fileBaton.myIsAmbientlyExcluded = true;
            return fileBaton;
        }

        File abspath = SVNFileUtil.createFilePath(myAnchor, SVNWCUtils.getPathAsChild(myAnchor, SVNFileUtil.createFilePath(path)));
        AmbientReadInfo aInfo = ambientReadInfo(abspath, myReadBase);

        if (parentBaton.myAmbientDepth == SVNDepth.EMPTY) {
            if (aInfo.hidden || aInfo.kind == SVNWCDbKind.Unknown) {
                fileBaton.myIsAmbientlyExcluded = true;
            }
        }

        if (parentBaton.myAmbientDepth != SVNDepth.UNKNOWN && aInfo.status == SVNWCDbStatus.Excluded) {
            fileBaton.myIsAmbientlyExcluded = true;
        }

        return fileBaton;
    }

    private class AmbientReadInfo {

        public boolean hidden;
        public SVNWCDbStatus status;
        public SVNWCDbKind kind;
        public SVNDepth depth;
    }

    private AmbientReadInfo ambientReadInfo(File localAbspath, boolean readBase) throws SVNException {

        final AmbientReadInfo info = new AmbientReadInfo();

        try {
            if (readBase) {
                WCDbBaseInfo baseInfo = myWcContext.getDb().getBaseInfo(localAbspath, BaseInfoField.status, BaseInfoField.kind, BaseInfoField.depth);
                info.status = baseInfo.status;
                info.kind = baseInfo.kind;
                info.depth = baseInfo.depth;
            } else {
                WCDbInfo readInfo = myWcContext.getDb().readInfo(localAbspath, InfoField.status, InfoField.kind, InfoField.depth);
                info.status = readInfo.status;
                info.kind = readInfo.kind;
                info.depth = readInfo.depth;
            }
        } catch (SVNException e) {
            if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                info.status = SVNWCDbStatus.Normal;
                info.kind = SVNWCDbKind.Unknown;
                info.depth = SVNDepth.UNKNOWN;
            } else {
                throw e;
            }
        }

        info.hidden = false;
        switch (info.status) {
            case NotPresent:
            case ServerExcluded:
            case Excluded:
                info.hidden = true;
                break;
            default:
                break;
        }

        return info;
    }

    private class DirBaton {

        boolean myIsAmbientlyExcluded;
        SVNDepth myAmbientDepth;
    }

    private class FileBaton {

        boolean myIsAmbientlyExcluded;
    }

    public long getTargetRevision() {
        if (myDelegate instanceof ISVNUpdateEditor) {
            return ((ISVNUpdateEditor) myDelegate).getTargetRevision();
        }
        return SVNWCContext.INVALID_REVNUM;
    }

}
