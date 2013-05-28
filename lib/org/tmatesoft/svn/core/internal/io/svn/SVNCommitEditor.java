/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */

package org.tmatesoft.svn.core.internal.io.svn;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @author TMate Software Ltd.
 * @version 1.3
 */
class SVNCommitEditor implements ISVNEditor {

    private SVNConnection myConnection;
    private SVNRepositoryImpl myRepository;
    private ISVNCommitCallback myCloseCallback;
    private Stack myDirsStack;
    private int myNextToken;
    private Map myFilesToTokens;

    public interface ISVNCommitCallback {
        public void run(SVNException error);
    }

    public SVNCommitEditor(SVNRepositoryImpl location, SVNConnection connection, ISVNCommitCallback closeCallback) {
        myRepository = location;
        myConnection = connection;
        myCloseCallback = closeCallback;
        myDirsStack = new Stack();
        myNextToken = 0;
    }

    /* do nothing */
    public void targetRevision(long revision) throws SVNException {
    }

    public void absentDir(String path) throws SVNException {
    }

    public void absentFile(String path) throws SVNException {
    }

    public void openRoot(long revision) throws SVNException {
        DirBaton rootBaton = new DirBaton(myNextToken++);
        myConnection.write("(w((n)s))", new Object[]{"open-root",
                getRevisionObject(revision), rootBaton.getToken()});
        myDirsStack.push(rootBaton);
    }

    public void deleteEntry(String path, long revision) throws SVNException {
        DirBaton parentBaton = (DirBaton) myDirsStack.peek();
        myConnection.write("(w(s(n)s))", new Object[]{"delete-entry", path,
                getRevisionObject(revision), parentBaton.getToken()});
    }

    public void addDir(String path, String copyFromPath, long copyFromRevision)
            throws SVNException {
        DirBaton parentBaton = (DirBaton) myDirsStack.peek();
        DirBaton dirBaton = new DirBaton(myNextToken++);

        if (copyFromPath != null) {
            String rootURL = myRepository.getRepositoryRoot(false).toString();
            copyFromPath = SVNPathUtil.append(rootURL, SVNEncodingUtil.uriEncode(myRepository.getRepositoryPath(copyFromPath)));
            myConnection.write("(w(sss(sn)))", new Object[]{"add-dir", path,
                    parentBaton.getToken(), dirBaton.getToken(), copyFromPath,
                    getRevisionObject(copyFromRevision)});
        } else {
            myConnection.write("(w(sss()))", new Object[]{"add-dir", path,
                    parentBaton.getToken(), dirBaton.getToken()});
        }
        myDirsStack.push(dirBaton);
    }

    public void openDir(String path, long revision) throws SVNException {
        DirBaton parentBaton = (DirBaton) myDirsStack.peek();
        DirBaton dirBaton = new DirBaton(myNextToken++);

        myConnection.write("(w(sss(n)))", new Object[]{"open-dir", path,
                parentBaton.getToken(), dirBaton.getToken(), getRevisionObject(revision)});

        myDirsStack.push(dirBaton);
    }

    public void changeDirProperty(String name, SVNPropertyValue value)
            throws SVNException {
        DirBaton dirBaton = (DirBaton) myDirsStack.peek();
        byte[] bytes = SVNPropertyValue.getPropertyAsBytes(value);
        myConnection.write("(w(ss(b)))", new Object[]{"change-dir-prop",
                dirBaton.getToken(), name, bytes});
    }

    public void closeDir() throws SVNException {
        DirBaton dirBaton = (DirBaton) myDirsStack.pop();

        myConnection.write("(w(s))",
                new Object[]{"close-dir", dirBaton.getToken()});
    }

    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        DirBaton parentBaton = (DirBaton) myDirsStack.peek();
        String fileToken = "c" + myNextToken++;

        if (copyFromPath != null) {
            String host = myRepository.getRepositoryRoot(false).toString();
            copyFromPath = SVNPathUtil.append(host, SVNEncodingUtil.uriEncode(myRepository.getRepositoryPath(copyFromPath)));
            myConnection.write("(w(sss(sn)))", new Object[]{"add-file", path,
                    parentBaton.getToken(), fileToken, copyFromPath,
                    getRevisionObject(copyFromRevision)});
        } else {
            myConnection.write("(w(sss()))", new Object[]{"add-file", path,
                    parentBaton.getToken(), fileToken});
        }
        if (myFilesToTokens == null) {
            myFilesToTokens = new SVNHashMap();
        }
        myFilesToTokens.put(path, fileToken);
    }

    public void openFile(String path, long revision) throws SVNException {
        DirBaton parentBaton = (DirBaton) myDirsStack.peek();
        String fileToken = "c" + myNextToken++;

        myConnection.write("(w(sss(n)))", new Object[]{"open-file", path,
                parentBaton.getToken(), fileToken, getRevisionObject(revision)});
        if (myFilesToTokens == null) {
            myFilesToTokens = new SVNHashMap();
        }
        myFilesToTokens.put(path, fileToken);
    }

    public void applyTextDelta(String path, String baseChecksum) throws SVNException {
        String fileToken = (String) myFilesToTokens.get(path);
        myDiffWindowCount = 0;
        myConnection.write("(w(s(s)))", new Object[]{"apply-textdelta", fileToken, baseChecksum});
    }

    private int myDiffWindowCount = 0;
    private boolean myIsAborted;

    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        String fileToken = (String) myFilesToTokens.get(path);

        try {
            diffWindow.writeTo(myConnection.getDeltaStream(fileToken), myDiffWindowCount == 0, myConnection.isSVNDiff1());
            myDiffWindowCount++;
            return SVNFileUtil.DUMMY_OUT;
        } catch (IOException e) {
            SVNErrorManager.error(SVNErrorMessage.create(SVNErrorCode.RA_SVN_IO_ERROR, e.getMessage()), e, SVNLogType.NETWORK);
        }
        return null;
    }

    public void textDeltaEnd(String path) throws SVNException {
        String fileToken = (String) myFilesToTokens.get(path);
        myDiffWindowCount = 0;
        myConnection.write("(w(s))", new Object[]{"textdelta-end", fileToken});
    }

    public void changeFileProperty(String path, String name, SVNPropertyValue value) throws SVNException {
        String fileToken = (String) myFilesToTokens.get(path);
        byte[] bytes = SVNPropertyValue.getPropertyAsBytes(value);
        myConnection.write("(w(ss(b)))", new Object[]{"change-file-prop", fileToken, name, bytes});
    }

    public void closeFile(String path, String textChecksum) throws SVNException {
        String fileToken = (String) myFilesToTokens.remove(path);
        myDiffWindowCount = 0;
        myConnection.write("(w(s(s)))", new Object[]{"close-file", fileToken, textChecksum});
    }

    public SVNCommitInfo closeEdit() throws SVNException {
        SVNException e = null;
        try {
            myConnection.write("(w())", new Object[]{"close-edit"});
            myConnection.read("", null, true);

            myRepository.authenticate();

            List items = myConnection.readTuple("r(?s)(?s)?(?s)", true);
            long revision = SVNReader.getLong(items, 0);
            Date date = SVNReader.getDate(items, 1);
            String author = SVNReader.getString(items, 2);
            String errorMessage = SVNReader.getString(items, 3);
            SVNErrorMessage err = errorMessage == null || errorMessage.length() == 0 ? null : SVNErrorMessage.create(SVNErrorCode.REPOS_POST_COMMIT_HOOK_FAILED, errorMessage, SVNErrorMessage.TYPE_WARNING);
            return new SVNCommitInfo(revision, author, date, err);
        } catch (SVNException exception) {
            e = exception;
            try {
                myConnection.write("(w())", new Object[]{"abort-edit"});
            } catch (SVNException e1) {
            }
            throw exception;
        } finally {
            myCloseCallback.run(e);
            myCloseCallback = null;
        }
    }

    public void abortEdit() throws SVNException {
        if (myIsAborted || myCloseCallback == null) {
            return;
        }
        myIsAborted = true;
        SVNException error = null;
        try {
            myConnection.write("(w())", new Object[]{"abort-edit"});
            myConnection.read("", null, true);
        } catch (SVNException e) {
            error = e;
            throw e;
        } finally {
            myCloseCallback.run(error);
            myCloseCallback = null;
        }
    }

    private static Long getRevisionObject(long rev) {
        return rev >= 0 ? new Long(rev) : null;
    }

    private final static class DirBaton {

        private String myToken;

        public DirBaton(int token) {
            myToken = "d" + token;
        }

        public String getToken() {
            return myToken;
        }
    }
}
