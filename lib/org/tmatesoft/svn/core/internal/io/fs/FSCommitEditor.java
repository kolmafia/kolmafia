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
package org.tmatesoft.svn.core.internal.io.fs;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.ISVNDeltaConsumer;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSCommitEditor implements ISVNEditor {

    private Map<String, String> myPathsToLockTokens;
    private Collection<String> myLockTokens;
    private String myBasePath;
    private FSTransactionInfo myTxn;
    private FSTransactionRoot myTxnRoot;
    private boolean isTxnOwner;
    private FSFS myFSFS;
    private FSRepository myRepository;
    private Stack<DirBaton> myDirsStack;
    private FSDeltaConsumer myDeltaConsumer;
    private SVNProperties myCurrentFileProps;
    private String myCurrentFilePath;
    private FSCommitter myCommitter;
    private SVNProperties myRevProps;
    private String myAuthor;
    
    public FSCommitEditor(String path, String logMessage, String userName, Map<String, String> lockTokens, boolean keepLocks, FSTransactionInfo txn, FSFS owner, FSRepository repository) {
        this(path, lockTokens, keepLocks, txn, owner, repository, null);
        myRevProps = new SVNProperties();
        if (userName != null) {
            myAuthor = userName;
            myRevProps.put(SVNRevisionProperty.AUTHOR, userName);
        }
        if (logMessage != null) {
            myRevProps.put(SVNRevisionProperty.LOG, logMessage);
        }
    }

    public FSCommitEditor(String path, Map<String, String> lockTokens, boolean keepLocks, FSTransactionInfo txn, FSFS owner, FSRepository repository, SVNProperties revProps) {
        myPathsToLockTokens = !keepLocks ? lockTokens : null;
        myLockTokens = lockTokens != null ? lockTokens.values() : new HashSet<String>();
        myBasePath = path;
        myTxn = txn;
        isTxnOwner = txn == null;
        myRepository = repository;
        myFSFS = owner;
        myDirsStack = new Stack<DirBaton>();
        myRevProps = revProps != null ? revProps : new SVNProperties();
    }

    public void targetRevision(long revision) throws SVNException {
        // does nothing
    }

    public void openRoot(long revision) throws SVNException {
        long youngestRev = myFSFS.getYoungestRevision();

        if (isTxnOwner) {
            myTxn = FSTransactionRoot.beginTransactionForCommit(youngestRev, myRevProps, myFSFS);
        } else {
            myFSFS.changeTransactionProperties(myTxn.getTxnId(), myRevProps);
        }
        myTxnRoot = myFSFS.createTransactionRoot(myTxn);
        myCommitter = new FSCommitter(myFSFS, myTxnRoot, myTxn, myLockTokens, getAuthor());
        DirBaton dirBaton = new DirBaton(revision, myBasePath, false);
        myDirsStack.push(dirBaton);
    }

    private String getAuthor() {
        if (myAuthor == null) {
            myAuthor = myRevProps.getStringValue(SVNRevisionProperty.AUTHOR);
        }
        return myAuthor;
    }

    public void openDir(String path, long revision) throws SVNException {
        DirBaton parentBaton = (DirBaton) myDirsStack.peek();
        String fullPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(myBasePath, path));
        SVNNodeKind kind = myTxnRoot.checkNodeKind(fullPath);
        if (kind == SVNNodeKind.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_DIRECTORY, "Path ''{0}'' not present", path);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        DirBaton dirBaton = new DirBaton(revision, fullPath, parentBaton.isCopied());
        myDirsStack.push(dirBaton);
    }

    public void deleteEntry(String path, long revision) throws SVNException {
        String fullPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(myBasePath, path));
        SVNNodeKind kind = myTxnRoot.checkNodeKind(fullPath);

        if (kind == SVNNodeKind.NONE) {
            SVNErrorManager.error(FSErrors.errorOutOfDate(fullPath, kind), SVNLogType.FSFS);
        }

        FSRevisionNode existingNode = myTxnRoot.getRevisionNode(fullPath);
        long createdRev = existingNode.getCreatedRevision();
        if (FSRepository.isValidRevision(revision) && revision < createdRev) {
            SVNErrorManager.error(FSErrors.errorOutOfDate(fullPath, kind), SVNLogType.FSFS);
        }
        myCommitter.deleteNode(fullPath);
    }

    public void absentDir(String path) throws SVNException {
        // does nothing
    }

    public void absentFile(String path) throws SVNException {
        // does nothing
    }

    public void addDir(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        DirBaton parentBaton = (DirBaton) myDirsStack.peek();
        String fullPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(myBasePath, path));
        boolean isCopied = false;
        if (copyFromPath != null && FSRepository.isInvalidRevision(copyFromRevision)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_GENERAL, "Got source path but no source revision for ''{0}''", fullPath);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        } else if (copyFromPath != null) {
            SVNNodeKind kind = myTxnRoot.checkNodeKind(fullPath);
            if (kind != SVNNodeKind.NONE && !parentBaton.isCopied()) {
                SVNErrorManager.error(FSErrors.errorOutOfDate(fullPath, kind), SVNLogType.FSFS);
            }
            copyFromPath = myRepository.getRepositoryPath(copyFromPath);

            FSRevisionRoot copyRoot = myFSFS.createRevisionRoot(copyFromRevision);
            myCommitter.makeCopy(copyRoot, copyFromPath, fullPath, true);
            isCopied = true;
        } else {
            myCommitter.makeDir(fullPath);
        }

        DirBaton dirBaton = new DirBaton(SVNRepository.INVALID_REVISION, fullPath, isCopied);
        myDirsStack.push(dirBaton);
    }

    public void changeDirProperty(String name, SVNPropertyValue value) throws SVNException {
        DirBaton dirBaton = (DirBaton) myDirsStack.peek();
        if (FSRepository.isValidRevision(dirBaton.getBaseRevision())) {
            FSRevisionNode existingNode = myTxnRoot.getRevisionNode(dirBaton.getPath());
            long createdRev = existingNode.getCreatedRevision();
            if (dirBaton.getBaseRevision() < createdRev) {
                SVNErrorManager.error(FSErrors.errorOutOfDate(dirBaton.getPath(), SVNNodeKind.DIR), SVNLogType.FSFS);
            }
        }
        myCommitter.changeNodeProperty(dirBaton.getPath(), name, value);
    }

    private void changeNodeProperties(String path, SVNProperties propNamesToValues) throws SVNException {
        FSParentPath parentPath = null;
        SVNNodeKind kind = null;
        SVNProperties properties = null;
        boolean done = false;
        boolean haveRealChanges = false;
        for (Iterator<String> propNames = propNamesToValues.nameSet().iterator(); propNames.hasNext();) {
            String propName = (String)propNames.next();
            SVNPropertyValue propValue = propNamesToValues.getSVNPropertyValue(propName);

            FSRepositoryUtil.validateProperty(propName, propValue);

            if (!done) {
                parentPath = myTxnRoot.openPath(path, true, true);
                kind = parentPath.getRevNode().getType();

                if ((myTxnRoot.getTxnFlags() & FSTransactionRoot.SVN_FS_TXN_CHECK_LOCKS) != 0) {
                    myCommitter.allowLockedOperation(myFSFS, path, getAuthor(), myLockTokens, false, false);
                }

                myCommitter.makePathMutable(parentPath, path);
                properties = parentPath.getRevNode().getProperties(myFSFS);
                
                done = true;
            }

            if (properties.isEmpty() && propValue == null) {
                continue;
            }

            if (myFSFS.supportsMergeInfo() && propName.equals(SVNProperty.MERGE_INFO)) {
                long increment = 0;
                boolean hadMergeInfo = parentPath.getRevNode().hasMergeInfo(); 
                if (propValue != null && !hadMergeInfo) {
                    increment = 1;
                } else if (propValue == null && hadMergeInfo) {
                    increment = -1;
                }
                if (increment != 0) {
                    parentPath.getRevNode().setHasMergeInfo(propValue != null);
                    myCommitter.incrementMergeInfoUpTree(parentPath, increment);
                }
            }

            if (propValue == null) {
                properties.remove(propName);
            } else {
                properties.put(propName, propValue);
            }
            
            if (!haveRealChanges) {
                haveRealChanges = true;
            }
        }

        if (haveRealChanges) {
            myTxnRoot.setProplist(parentPath.getRevNode(), properties);
            myCommitter.addChange(path, parentPath.getRevNode().getId(), FSPathChangeKind.FS_PATH_CHANGE_MODIFY, false, true, SVNRepository.INVALID_REVISION, null, kind);
        }
    }
    
    public void closeDir() throws SVNException {
        flushPendingProperties();
        myDirsStack.pop();
    }

    public void addFile(String path, String copyFromPath, long copyFromRevision) throws SVNException {
        DirBaton parentBaton = (DirBaton) myDirsStack.peek();
        String fullPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(myBasePath, path));
        if (copyFromPath != null && FSRepository.isInvalidRevision(copyFromRevision)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_GENERAL, "Got source path but no source revision for ''{0}''", fullPath);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        } else if (copyFromPath != null) {
            SVNNodeKind kind = myTxnRoot.checkNodeKind(fullPath);
            if (kind != SVNNodeKind.NONE && !parentBaton.isCopied()) {
                SVNErrorManager.error(FSErrors.errorOutOfDate(fullPath, kind), SVNLogType.FSFS);
            }
            copyFromPath = myRepository.getRepositoryPath(copyFromPath);

            FSRevisionRoot copyRoot = myFSFS.createRevisionRoot(copyFromRevision);
            myCommitter.makeCopy(copyRoot, copyFromPath, fullPath, true);
        } else {
            myCommitter.makeFile(fullPath);
        }
    }

    public void openFile(String path, long revision) throws SVNException {
        String fullPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(myBasePath, path));
        FSRevisionNode revNode = myTxnRoot.getRevisionNode(fullPath);

        if (FSRepository.isValidRevision(revision) && revision < revNode.getCreatedRevision()) {
            SVNErrorManager.error(FSErrors.errorOutOfDate(fullPath, SVNNodeKind.FILE), SVNLogType.FSFS);
        }
    }

    public void applyTextDelta(String path, String baseChecksum) throws SVNException {
        flushPendingProperties();
        ISVNDeltaConsumer fsfsConsumer = getDeltaConsumer();
        fsfsConsumer.applyTextDelta(path, baseChecksum);
    }

    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        ISVNDeltaConsumer fsfsConsumer = getDeltaConsumer();
        return fsfsConsumer.textDeltaChunk(path, diffWindow);
    }

    public void textDeltaEnd(String path) throws SVNException {
        ISVNDeltaConsumer fsfsConsumer = getDeltaConsumer();
        fsfsConsumer.textDeltaEnd(path);
    }

    private FSDeltaConsumer getDeltaConsumer() {
        if (myDeltaConsumer == null) {
            myDeltaConsumer = new FSDeltaConsumer(myBasePath, myTxnRoot, myFSFS, myCommitter, getAuthor(), myLockTokens);
        }
        return myDeltaConsumer;
    }

    public void changeFileProperty(String path, String name, SVNPropertyValue value) throws SVNException {
        String fullPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(myBasePath, path));
        SVNProperties props = getFilePropertiesStorage();
        if (!fullPath.equals(myCurrentFilePath)) {
            if (myCurrentFilePath != null) {
                changeNodeProperties(myCurrentFilePath, props);
                props.clear();
            }
            myCurrentFilePath = fullPath;
        }
        props.put(name, value);
    }

    private SVNProperties getFilePropertiesStorage() {
        if (myCurrentFileProps == null) {
            myCurrentFileProps = new SVNProperties();
        }
        return myCurrentFileProps;
    }
    
    private void flushPendingProperties() throws SVNException {
        if (myCurrentFilePath != null) {
            SVNProperties props = getFilePropertiesStorage();
            changeNodeProperties(myCurrentFilePath, props);
            props.clear();
            myCurrentFilePath = null;
        }
    }
    
    public void closeFile(String path, String textChecksum) throws SVNException {
        flushPendingProperties();
        
        if (textChecksum != null) {
            String fullPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(myBasePath, path));
            FSRevisionNode revNode = myTxnRoot.getRevisionNode(fullPath);
            FSRepresentation txtRep = revNode.getTextRepresentation();
            if (txtRep != null && !textChecksum.equals(txtRep.getMD5HexDigest())) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CHECKSUM_MISMATCH,
                        "Checksum mismatch for resulting fulltext\n({0}):\n   expected checksum:  {1}\n   actual checksum:    {2}\n",
                        new Object[] { fullPath, textChecksum, txtRep.getMD5HexDigest() });
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        }
    }

    public SVNCommitInfo closeEdit() throws SVNException {
        try {
            if (myTxn == null) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_BAD_ARGS, "No valid transaction supplied to closeEdit()");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
    
            long committedRev = -1;
            if (myDeltaConsumer != null) {
                myDeltaConsumer.close();
            }
            
            SVNErrorMessage[] errorMessage = new SVNErrorMessage[1];
            committedRev = myCommitter.commitTxn(true, true, errorMessage, null);
                
            SVNProperties revProps = myFSFS.getRevisionProperties(committedRev);
            String dateProp = revProps.getStringValue(SVNRevisionProperty.DATE);
            Date datestamp = dateProp != null ? SVNDate.parseDateString(dateProp) : null;
            
            SVNErrorMessage err = errorMessage[0];
            if (err != null && err.getErrorCode() == SVNErrorCode.REPOS_POST_COMMIT_HOOK_FAILED) {
                String message = err.getChildErrorMessage() != null ? err.getChildErrorMessage().getFullMessage() : err.getFullMessage();
                err = SVNErrorMessage.create(SVNErrorCode.REPOS_POST_COMMIT_HOOK_FAILED, message, 
                        SVNErrorMessage.TYPE_WARNING);
            }
            SVNCommitInfo info = new SVNCommitInfo(committedRev, getAuthor(), datestamp, err);
            releaseLocks();
            return info;
        } finally {
            myRepository.closeRepository();
        }
    }

    private void releaseLocks() {
        releaseLocks(myPathsToLockTokens, false, true);
        final Map<String, String> autoUnlockPaths = myCommitter.getAutoUnlockPaths();
        releaseLocks(autoUnlockPaths, true, false);
    }

    private void releaseLocks(Map<String, String> pathsToLockTokens, boolean breakLocks, boolean runHooks) {
        if (pathsToLockTokens == null) {
            return;
        }
        for (Iterator<String> paths = pathsToLockTokens.keySet().iterator(); paths.hasNext();) {
            String path = (String) paths.next();
            String token = (String) pathsToLockTokens.get(path);
            String absPath = !path.startsWith("/") ? SVNPathUtil.getAbsolutePath(SVNPathUtil.append(myBasePath, path)) : path;

            try {
                myFSFS.unlockPath(absPath, token, getAuthor(), breakLocks, runHooks);
            } catch (SVNException svne) {
                // ignore exceptions
            }
        }
    }

    public void abortEdit() throws SVNException {
        if (myDeltaConsumer != null) {
            myDeltaConsumer.abort();
        }
        try {
            if (myTxn == null || !isTxnOwner) {
                return;
            }
            FSCommitter.abortTransaction(myFSFS, myTxn.getTxnId());
        } finally {
            myRepository.closeRepository();
            myTxn = null;
            myTxnRoot = null;
        }
    }

    private static class DirBaton {

        private long myBaseRevision;

        private String myPath;

        private boolean isCopied;

        public DirBaton(long revision, String path, boolean copied) {
            myBaseRevision = revision;
            myPath = path;
            isCopied = copied;
        }

        public boolean isCopied() {
            return isCopied;
        }

        public long getBaseRevision() {
            return myBaseRevision;
        }

        public String getPath() {
            return myPath;
        }
    }

}
