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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetStatement;
import org.tmatesoft.svn.core.internal.util.SVNDate;
import org.tmatesoft.svn.core.internal.util.SVNHashSet;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSkel;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.db.statement.SVNWCDbStatements;
import org.tmatesoft.svn.core.io.ISVNLockHandler;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSCommitter {

    private static volatile boolean ourAutoUnlock;
    
    private FSFS myFSFS;
    private FSTransactionRoot myTxnRoot;
    private FSTransactionInfo myTxn;
    private Collection<String> myLockTokens;
    private Map<String, String> myAutoUnlockPaths;
    private String myAuthor;
    
    public static synchronized void setAutoUnlock(boolean autoUnlock) {
        ourAutoUnlock = autoUnlock;
    }

    public static synchronized boolean isAutoUnlock() {
        return ourAutoUnlock;
    }

    public FSCommitter(FSFS fsfs, FSTransactionRoot txnRoot, FSTransactionInfo txn, Collection<String> lockTokens, String author) {
        myFSFS = fsfs;
        myTxnRoot = txnRoot;
        myTxn = txn;
        myLockTokens = lockTokens != null ? lockTokens : new HashSet<String>();
        myAutoUnlockPaths = new HashMap<String, String>();
        myAuthor = author;
    }
    
    public Map<String, String> getAutoUnlockPaths() {
        return myAutoUnlockPaths;
    }

    public void deleteNode(String path) throws SVNException {
        FSTransactionRoot txnRoot = getTxnRoot();
        FSParentPath parentPath = txnRoot.openPath(path, true, true);
        SVNNodeKind kind = parentPath.getRevNode().getType();
        if (parentPath.getParent() == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_ROOT_DIR,
                    "The root directory cannot be deleted");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if ((txnRoot.getTxnFlags() & FSTransactionRoot.SVN_FS_TXN_CHECK_LOCKS) != 0) {
            allowLockedOperation(myFSFS, path, myAuthor, myLockTokens, true, false);
        }

        makePathMutable(parentPath.getParent(), path);
        txnRoot.deleteEntry(parentPath.getParent().getRevNode(), parentPath.getEntryName());
        txnRoot.removeRevNodeFromCache(parentPath.getAbsPath());
        if (myFSFS.supportsMergeInfo()) {
            long mergeInfoCount = parentPath.getRevNode().getMergeInfoCount();
            if (mergeInfoCount > 0) {
                incrementMergeInfoUpTree(parentPath.getParent(), -mergeInfoCount);
            }
        }
        addChange(path, parentPath.getRevNode().getId(), FSPathChangeKind.FS_PATH_CHANGE_DELETE, false, false, SVNRepository.INVALID_REVISION, null, kind);
    }

    public void changeNodeProperty(String path, String name, SVNPropertyValue propValue) throws SVNException {
        FSRepositoryUtil.validateProperty(name, propValue);
        FSTransactionRoot txnRoot = getTxnRoot();
        FSParentPath parentPath = txnRoot.openPath(path, true, true);
        SVNNodeKind kind = parentPath.getRevNode().getType();

        if ((txnRoot.getTxnFlags() & FSTransactionRoot.SVN_FS_TXN_CHECK_LOCKS) != 0) {
            allowLockedOperation(myFSFS, path, myAuthor, myLockTokens, false, false);
        }

        makePathMutable(parentPath, path);
        SVNProperties properties = parentPath.getRevNode().getProperties(myFSFS);

        if (properties.isEmpty() && propValue == null) {
            return;
        }

        if (myFSFS.supportsMergeInfo() && name.equals(SVNProperty.MERGE_INFO)) {
            long increment = 0;
            boolean hadMergeInfo = parentPath.getRevNode().hasMergeInfo();
            if (propValue != null && !hadMergeInfo) {
                increment = 1;
            } else if (propValue == null && hadMergeInfo) {
                increment = -1;
            }
            if (increment != 0) {
                parentPath.getRevNode().setHasMergeInfo(propValue != null);
                incrementMergeInfoUpTree(parentPath, increment);
            }
        }

        if (propValue == null) {
            properties.remove(name);
        } else {
            properties.put(name, propValue);
        }

        txnRoot.setProplist(parentPath.getRevNode(), properties);
        addChange(path, parentPath.getRevNode().getId(), FSPathChangeKind.FS_PATH_CHANGE_MODIFY, false, true, SVNRepository.INVALID_REVISION, null, kind);
    }

    public void makeCopy(FSRevisionRoot fromRoot, String fromPath, String toPath, boolean preserveHistory) throws SVNException {
        FSTransactionRoot txnRoot = getTxnRoot();
        String txnId = txnRoot.getTxnID();
        FSRevisionNode fromNode = fromRoot.getRevisionNode(fromPath);

        FSParentPath toParentPath = txnRoot.openPath(toPath, false, true);
        if ((txnRoot.getTxnFlags() & FSTransactionRoot.SVN_FS_TXN_CHECK_LOCKS) != 0) {
            allowLockedOperation(myFSFS, toPath, myAuthor, myLockTokens, true, false);
        }

        if (toParentPath.getRevNode() != null && toParentPath.getRevNode().getId().equals(fromNode.getId())) {
            return;
        }

        FSPathChangeKind changeKind;
        long mergeInfoStart = 0;
        if (toParentPath.getRevNode() != null) {
            changeKind = FSPathChangeKind.FS_PATH_CHANGE_REPLACE;
            if (myFSFS.supportsMergeInfo()) {
                mergeInfoStart = toParentPath.getRevNode().getMergeInfoCount();
            }
        } else {
            changeKind = FSPathChangeKind.FS_PATH_CHANGE_ADD;
        }

        makePathMutable(toParentPath.getParent(), toPath);
        String fromCanonPath = SVNPathUtil.canonicalizeAbsolutePath(fromPath);
        copy(toParentPath.getParent().getRevNode(), toParentPath.getEntryName(), fromNode, preserveHistory, fromRoot.getRevision(), fromCanonPath, txnId);

        if (changeKind == FSPathChangeKind.FS_PATH_CHANGE_REPLACE) {
            txnRoot.removeRevNodeFromCache(toParentPath.getAbsPath());
        }

        long mergeInfoEnd = 0;
        if (myFSFS.supportsMergeInfo()) {
            mergeInfoEnd = fromNode.getMergeInfoCount();
            if (mergeInfoStart != mergeInfoEnd) {
                incrementMergeInfoUpTree(toParentPath.getParent(), mergeInfoEnd - mergeInfoStart);
            }
        }

        FSRevisionNode newNode = txnRoot.getRevisionNode(toPath);
        addChange(toPath, newNode.getId(), changeKind, false, false, fromRoot.getRevision(), fromCanonPath, fromNode.getType());
    }

    public void makeFile(String path) throws SVNException {
        SVNPathUtil.checkPathIsValid(path);
        FSTransactionRoot txnRoot = getTxnRoot();
        String txnId = txnRoot.getTxnID();
        FSParentPath parentPath = txnRoot.openPath(path, false, true);

        if (parentPath.getRevNode() != null) {
            SVNErrorManager.error(FSErrors.errorAlreadyExists(txnRoot, path, myFSFS), SVNLogType.FSFS);
        }

        if ((txnRoot.getTxnFlags() & FSTransactionRoot.SVN_FS_TXN_CHECK_LOCKS) != 0) {
            allowLockedOperation(myFSFS, path, myAuthor, myLockTokens, false, false);
        }

        makePathMutable(parentPath.getParent(), path);
        FSRevisionNode childNode = makeEntry(parentPath.getParent().getRevNode(), parentPath.getParent().getAbsPath(), parentPath.getEntryName(), false, txnId);

        txnRoot.putRevNodeToCache(parentPath.getAbsPath(), childNode);
        addChange(path, childNode.getId(), FSPathChangeKind.FS_PATH_CHANGE_ADD, false, false, SVNRepository.INVALID_REVISION, null, SVNNodeKind.FILE);
    }

    public void makeDir(String path) throws SVNException {
        SVNPathUtil.checkPathIsValid(path);
        FSTransactionRoot txnRoot = getTxnRoot();
        String txnId = txnRoot.getTxnID();
        FSParentPath parentPath = txnRoot.openPath(path, false, true);

        if (parentPath.getRevNode() != null) {
            SVNErrorManager.error(FSErrors.errorAlreadyExists(txnRoot, path, myFSFS), SVNLogType.FSFS);
        }

        if ((txnRoot.getTxnFlags() & FSTransactionRoot.SVN_FS_TXN_CHECK_LOCKS) != 0) {
            allowLockedOperation(myFSFS, path, myAuthor, myLockTokens, true, false);
        }

        makePathMutable(parentPath.getParent(), path);
        FSRevisionNode subDirNode = makeEntry(parentPath.getParent().getRevNode(), parentPath.getParent().getAbsPath(), parentPath.getEntryName(), true, txnId);

        txnRoot.putRevNodeToCache(parentPath.getAbsPath(), subDirNode);
        addChange(path, subDirNode.getId(), FSPathChangeKind.FS_PATH_CHANGE_ADD, false, false, SVNRepository.INVALID_REVISION, null, SVNNodeKind.DIR);
    }

    public FSRevisionNode makeEntry(FSRevisionNode parent, String parentPath, String entryName, boolean isDir, String txnId) throws SVNException {
        if (!SVNPathUtil.isSinglePathComponent(entryName)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_SINGLE_PATH_COMPONENT, "Attempted to create a node with an illegal name ''{0}''", entryName);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (parent.getType() != SVNNodeKind.DIR) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_DIRECTORY, "Attempted to create entry in non-directory parent");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (!parent.getId().isTxn()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_MUTABLE, "Attempted to clone child of non-mutable node");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        FSRevisionNode newRevNode = new FSRevisionNode();
        newRevNode.setType(isDir ? SVNNodeKind.DIR : SVNNodeKind.FILE);
        String createdPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(parentPath, entryName));
        newRevNode.setCreatedPath(createdPath);
        newRevNode.setCopyRootPath(parent.getCopyRootPath());
        newRevNode.setCopyRootRevision(parent.getCopyRootRevision());
        newRevNode.setCopyFromRevision(SVNRepository.INVALID_REVISION);
        newRevNode.setCopyFromPath(null);
        FSID newNodeId = createNode(newRevNode, parent.getId().getCopyID(), txnId);

        FSRevisionNode childNode = myFSFS.getRevisionNode(newNodeId);

        FSTransactionRoot txnRoot = getTxnRoot();
        txnRoot.setEntry(parent, entryName, childNode.getId(), newRevNode.getType());
        return childNode;
    }

    public void addChange(String path, FSID id, FSPathChangeKind changeKind, boolean textModified,
            boolean propsModified, long copyFromRevision, String copyFromPath, SVNNodeKind kind) throws SVNException {
        path = SVNPathUtil.canonicalizeAbsolutePath(path);
        OutputStream changesFile = null;
        try {
            FSTransactionRoot txnRoot = getTxnRoot();
            changesFile = SVNFileUtil.openFileForWriting(txnRoot.getTransactionChangesFile(), true);
            FSPathChange pathChange = new FSPathChange(path, id, changeKind, textModified, propsModified, copyFromPath, copyFromRevision, kind);
            txnRoot.writeChangeEntry(changesFile, pathChange, true);
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        } finally {
            SVNFileUtil.closeFile(changesFile);
        }
    }

    public long commitTxn(boolean runPreCommitHook, boolean runPostCommitHook, SVNErrorMessage[] postCommitHookError, StringBuffer conflictPath) throws SVNException {
        if (myFSFS.isHooksEnabled() && runPreCommitHook) {
            FSHooks.runPreCommitHook(myFSFS.getRepositoryRoot(), myTxn.getTxnId());
        }

        long newRevision = SVNRepository.INVALID_REVISION;

        while (true) {
            long youngishRev = myFSFS.getYoungestRevision();
            FSRevisionRoot youngishRoot = myFSFS.createRevisionRoot(youngishRev);

            FSRevisionNode youngishRootNode = youngishRoot.getRevisionNode("/");

            mergeChanges(myFSFS, getTxnRoot(), youngishRootNode, conflictPath);
            myTxn.setBaseRevision(youngishRev);

            FSWriteLock writeLock = FSWriteLock.getWriteLockForDB(myFSFS);
            final Collection<FSRepresentation> representations = myFSFS.getRepositoryCacheManager() != null ?
                    new ArrayList<FSRepresentation>() : null;
            synchronized (writeLock) {
                try {
                    writeLock.lock();
                    newRevision = commit(representations);
                } catch (SVNException svne) {
                    if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.FS_TXN_OUT_OF_DATE) {
                        long youngestRev = myFSFS.getYoungestRevision();
                        if (youngishRev == youngestRev) {
                            throw svne;
                        }
                        continue;
                    }
                    throw svne;
                } finally {
                    writeLock.unlock();
                    FSWriteLock.release(writeLock);
                }
            }
            // write representations here.
            if (representations != null && !representations.isEmpty()) {
                if (myFSFS.getRepositoryCacheManager() != null) {
                    try {
                        myFSFS.getRepositoryCacheManager().runWriteTransaction(new IFSSqlJetTransaction() {
                            public void run() throws SVNException {
                                for (FSRepresentation fsRepresentation : representations) {
                                    myFSFS.getRepositoryCacheManager().insert(fsRepresentation, false);
                                }
                            }
                        });
                    } catch (SVNException e) {
                        // ignore
                        SVNDebugLog.getDefaultLog().logError(SVNLogType.FSFS, e);
                    }
                }
            }
            break;
        }

        if (myFSFS.isHooksEnabled() && runPostCommitHook) {
            try {
                FSHooks.runPostCommitHook(myFSFS.getRepositoryRoot(), newRevision);
             } catch (SVNException svne) {
                 SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.REPOS_POST_COMMIT_HOOK_FAILED,
                         "Commit succeeded, but post-commit hook failed", SVNErrorMessage.TYPE_WARNING);
                 SVNErrorMessage childErr = svne.getErrorMessage();
                 childErr.setDontShowErrorCode(true);
                 errorMessage.setChildErrorMessage(childErr);

                 if (postCommitHookError != null && postCommitHookError.length > 0) {
                     postCommitHookError[0] = errorMessage;
                 } else {
                     SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
                 }
             }
        }
        return newRevision;
    }

    public void makePathMutable(FSParentPath parentPath, String errorPath) throws SVNException {
        FSTransactionRoot txnRoot = getTxnRoot();
        String txnId = txnRoot.getTxnID();

        if (parentPath.getRevNode().getId().isTxn()) {
            return;
        }
        FSRevisionNode clone = null;

        if (parentPath.getParent() != null) {
            makePathMutable(parentPath.getParent(), errorPath);
            FSID parentId = null;
            String copyId = null;

            switch (parentPath.getCopyStyle()) {
                case FSCopyInheritance.COPY_ID_INHERIT_PARENT:
                    parentId = parentPath.getParent().getRevNode().getId();
                    copyId = parentId.getCopyID();
                    break;
                case FSCopyInheritance.COPY_ID_INHERIT_NEW:
                    copyId = reserveCopyId(txnId);
                    break;
                case FSCopyInheritance.COPY_ID_INHERIT_SELF:
                    copyId = null;
                    break;
                case FSCopyInheritance.COPY_ID_INHERIT_UNKNOWN:
                default:
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "FATAL error: can not make path ''{0}'' mutable", errorPath);
                    SVNErrorManager.error(err, SVNLogType.FSFS);
            }

            String copyRootPath = parentPath.getRevNode().getCopyRootPath();
            long copyRootRevision = parentPath.getRevNode().getCopyRootRevision();

            FSRoot copyrootRoot = myFSFS.createRevisionRoot(copyRootRevision);
            FSRevisionNode copyRootNode = copyrootRoot.getRevisionNode(copyRootPath);
            FSID childId = parentPath.getRevNode().getId();
            FSID copyRootId = copyRootNode.getId();
            boolean isParentCopyRoot = false;
            if (!childId.getNodeID().equals(copyRootId.getNodeID())) {
                isParentCopyRoot = true;
            }

            String clonePath = parentPath.getParent().getAbsPath();
            clone = txnRoot.cloneChild(parentPath.getParent().getRevNode(), clonePath, parentPath.getEntryName(), copyId, isParentCopyRoot);

            txnRoot.putRevNodeToCache(parentPath.getAbsPath(), clone);
        } else {
            FSTransactionInfo txn = txnRoot.getTxn();

            if (txn.getRootID().equals(txn.getBaseID())) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN,
                                                             "FATAL error: txn ''{0}'' root id ''{1}'' matches base id ''{2}''",
                                                             new Object[] { txnId, txn.getRootID(), txn.getBaseID() });
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            clone = myFSFS.getRevisionNode(txn.getRootID());
        }

        parentPath.setRevNode(clone);
    }

    public String reserveCopyId(String txnId) throws SVNException {
        FSTransactionRoot txnRoot = getTxnRoot();
        String[] nextIds = txnRoot.readNextIDs();
        String copyId = FSRepositoryUtil.generateNextKey(nextIds[1]);
        myFSFS.writeNextIDs(txnId, nextIds[0], copyId);
        return "_" + nextIds[1];
    }

    public void incrementMergeInfoUpTree(FSParentPath parentPath, long increment) throws SVNException {
        while (parentPath != null) {
            FSTransactionRoot txnRoot = getTxnRoot();
            txnRoot.incrementMergeInfoCount(parentPath.getRevNode(), increment);
            parentPath = parentPath.getParent();
        }
    }

    private void copy(FSRevisionNode toNode, String entryName, FSRevisionNode fromNode, boolean preserveHistory,
            long fromRevision, String fromPath, String txnId) throws SVNException {
        FSID id = null;
        FSTransactionRoot txnRoot = getTxnRoot();
        if (preserveHistory) {
            FSID srcId = fromNode.getId();
            FSRevisionNode toRevNode = FSRevisionNode.dumpRevisionNode(fromNode);
            String copyId = reserveCopyId(txnId);

            toRevNode.setPredecessorId(srcId);
            if (toRevNode.getCount() != -1) {
                toRevNode.setCount(toRevNode.getCount() + 1);
            }
            String createdPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(toNode.getCreatedPath(), entryName));
            toRevNode.setCreatedPath(createdPath);
            toRevNode.setCopyFromPath(fromPath);
            toRevNode.setCopyFromRevision(fromRevision);

            toRevNode.setCopyRootPath(null);
            id = txnRoot.createSuccessor(srcId, toRevNode, copyId);
        } else {
            id = fromNode.getId();
        }

        txnRoot.setEntry(toNode, entryName, id, fromNode.getType());
    }

    private FSID createNode(FSRevisionNode revNode, String copyId, String txnId) throws SVNException {
        FSTransactionRoot txnRoot = getTxnRoot();
        String nodeId = txnRoot.getNewTxnNodeId();
        FSID id = FSID.createTxnId(nodeId, copyId, txnId);
        revNode.setId(id);
        revNode.setIsFreshTxnRoot(false);
        myFSFS.putTxnRevisionNode(id, revNode);
        return id;
    }

    private long commit(Collection<FSRepresentation> representations) throws SVNException {
        long oldRev = myFSFS.getYoungestRevision();

        if (myTxn.getBaseRevision() != oldRev) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_TXN_OUT_OF_DATE, "Transaction out of date");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        verifyLocks();

        final String startNodeId;
        final String startCopyId;
        if (myFSFS.getDBFormat() < FSFS.MIN_NO_GLOBAL_IDS_FORMAT) {
            String[] ids = myFSFS.getNextRevisionIDs();
            startNodeId = ids[0];
            startCopyId = ids[1];
        } else {
            startNodeId = null;
            startCopyId = null;
        }

        final long newRevision = oldRev + 1;
        final OutputStream protoFileOS = null;
        final FSID newRootId = null;
        final FSTransactionRoot txnRoot = getTxnRoot();
        FSWriteLock txnWriteLock = FSWriteLock.getWriteLockForTxn(myTxn.getTxnId(), myFSFS);
        synchronized (txnWriteLock) {
            try {
                // start transaction.
                txnWriteLock.lock();
                final File revisionPrototypeFile = txnRoot.getTransactionProtoRevFile();
                final long offset = revisionPrototypeFile.length();
                commit(startNodeId, startCopyId, newRevision, protoFileOS, newRootId, txnRoot, revisionPrototypeFile, offset, representations);
                File dstRevFile = myFSFS.getNewRevisionFile(newRevision);
                SVNFileUtil.rename(revisionPrototypeFile, dstRevFile);
            } finally {
               txnWriteLock.unlock();
               FSWriteLock.release(txnWriteLock);
            }
        }

        String commitTime = SVNDate.formatDate(new Date(System.currentTimeMillis()));
        SVNProperties presetRevisionProperties = myFSFS.getTransactionProperties(myTxn.getTxnId()); 
        if (presetRevisionProperties == null || !presetRevisionProperties.containsName(SVNRevisionProperty.DATE)) {
            myFSFS.setTransactionProperty(myTxn.getTxnId(), SVNRevisionProperty.DATE, SVNPropertyValue.create(commitTime));
        }

        File txnPropsFile = myFSFS.getTransactionPropertiesFile(myTxn.getTxnId());

        if (myFSFS.getDBFormat() < FSFS.MIN_PACKED_REVPROP_FORMAT ||
                newRevision >= myFSFS.getMinUnpackedRevProp()){
            File dstRevPropsFile = myFSFS.getNewRevisionPropertiesFile(newRevision);
            SVNFileUtil.rename(txnPropsFile, dstRevPropsFile);
        }
        else
        {
          /* Read the revprops, and commit them to the permenant sqlite db. */
          FSFile fsfProps = new FSFile(txnPropsFile);
          try {
              final SVNProperties revProperties = fsfProps.readProperties(false, true);
              final SVNSqlJetStatement stmt = myFSFS.getRevisionProperitesDb().getStatement(SVNWCDbStatements.FSFS_SET_REVPROP);
              try{
                  stmt.insert(new Object[] { newRevision, SVNSkel.createPropList(revProperties.asMap()).getData() } );
              } finally{
                  stmt.reset();
              }
          } finally {
              fsfProps.close();
          }
        }

        try {
            txnRoot.writeFinalCurrentFile(newRevision, startNodeId, startCopyId);
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        }
        myFSFS.setYoungestRevisionCache(newRevision);
        myFSFS.purgeTxn(myTxn.getTxnId());
        return newRevision;
    }

    private void commit(String startNodeId, String startCopyId, long newRevision, OutputStream protoFileOS, FSID newRootId, FSTransactionRoot txnRoot, File revisionPrototypeFile, long offset,
            Collection<FSRepresentation> representations) throws SVNException {
        try {
            protoFileOS = SVNFileUtil.openFileForWriting(revisionPrototypeFile, true);
            FSID rootId = FSID.createTxnId("0", "0", myTxn.getTxnId());

            CountingOutputStream revWriter = new CountingOutputStream(protoFileOS, offset);
            newRootId = txnRoot.writeFinalRevision(newRootId, revWriter, newRevision, rootId,
                    startNodeId, startCopyId, representations);
            long changedPathOffset = txnRoot.writeFinalChangedPathInfo(revWriter);

            String offsetsLine = "\n" + newRootId.getOffset() + " " + changedPathOffset + "\n";
            protoFileOS.write(offsetsLine.getBytes("UTF-8"));
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        } finally {
            SVNFileUtil.closeFile(protoFileOS);
        }

        SVNProperties txnProps = myFSFS.getTransactionProperties(myTxn.getTxnId());
        if (txnProps != null && !txnProps.isEmpty()) {
            if (txnProps.getStringValue(SVNProperty.TXN_CHECK_OUT_OF_DATENESS) != null) {
                myFSFS.setTransactionProperty(myTxn.getTxnId(), SVNProperty.TXN_CHECK_OUT_OF_DATENESS, null);
            }
            if (txnProps.getStringValue(SVNProperty.TXN_CHECK_LOCKS) != null) {
                myFSFS.setTransactionProperty(myTxn.getTxnId(), SVNProperty.TXN_CHECK_LOCKS, null);
            }
        }
    }

    public static void mergeChanges(FSFS owner, FSTransactionRoot txnRoot, FSRevisionNode sourceNode, StringBuffer conflictPath) throws SVNException {
        FSRevisionNode txnRootNode = txnRoot.getRootRevisionNode();
        FSRevisionNode ancestorNode = txnRoot.getTxnBaseRootNode();

        if (txnRootNode.getId().equals(ancestorNode.getId())) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "FATAL error: no changes in transaction to commit");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        } else {
            merge(owner, "/", txnRootNode, sourceNode, ancestorNode, txnRoot, conflictPath);
        }
    }

    private static long merge(FSFS owner, String targetPath, FSRevisionNode target, FSRevisionNode source, FSRevisionNode ancestor, FSTransactionRoot txnRoot,
            StringBuffer conflictPath) throws SVNException {
        FSID sourceId = source.getId();
        FSID targetId = target.getId();
        FSID ancestorId = ancestor.getId();
        long mergeInfoIncrement = 0;

        if (ancestorId.equals(targetId)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_GENERAL, "Bad merge; target ''{0}'' has id ''{1}'', same as ancestor", new Object[] {
                    targetPath, targetId
            });
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (ancestorId.equals(sourceId) || sourceId.equals(targetId)) {
            return mergeInfoIncrement;
        }

        if (source.getType() != SVNNodeKind.DIR || target.getType() != SVNNodeKind.DIR || ancestor.getType() != SVNNodeKind.DIR) {
            SVNErrorManager.error(FSErrors.errorConflict(targetPath, conflictPath), SVNLogType.FSFS);
        }

        if (!FSRepresentation.compareRepresentations(target.getPropsRepresentation(), ancestor.getPropsRepresentation())) {
            SVNErrorManager.error(FSErrors.errorConflict(targetPath, conflictPath), SVNLogType.FSFS);
        }
        if (!FSRepresentation.compareRepresentations(source.getPropsRepresentation(), ancestor.getPropsRepresentation())) {
            SVNErrorManager.error(FSErrors.errorConflict(targetPath, conflictPath), SVNLogType.FSFS);
        }

        Map sourceEntries = source.getDirEntries(owner);
        Map targetEntries = target.getDirEntries(owner);
        Map ancestorEntries = ancestor.getDirEntries(owner);
        Set removedEntries = new SVNHashSet();
        for (Iterator ancestorEntryNames = ancestorEntries.keySet().iterator(); ancestorEntryNames.hasNext();) {
            String ancestorEntryName = (String) ancestorEntryNames.next();
            FSEntry ancestorEntry = (FSEntry) ancestorEntries.get(ancestorEntryName);
            FSEntry sourceEntry = removedEntries.contains(ancestorEntryName) ? null : (FSEntry) sourceEntries.get(ancestorEntryName);
            FSEntry targetEntry = (FSEntry) targetEntries.get(ancestorEntryName);
            if (sourceEntry != null && ancestorEntry.getId().equals(sourceEntry.getId())) {
                /*
                 * No changes were made to this entry while the transaction was
                 * in progress, so do nothing to the target.
                 */
            } else if (targetEntry != null && ancestorEntry.getId().equals(targetEntry.getId())) {
                if (owner.supportsMergeInfo()) {
                    FSRevisionNode targetEntryNode = owner.getRevisionNode(targetEntry.getId());
                    long mergeInfoStart = targetEntryNode.getMergeInfoCount();
                    mergeInfoIncrement -= mergeInfoStart;
                }
                if (sourceEntry != null) {
                    if (owner.supportsMergeInfo()) {
                        FSRevisionNode sourceEntryNode = owner.getRevisionNode(sourceEntry.getId());
                        long mergeInfoEnd = sourceEntryNode.getMergeInfoCount();
                        mergeInfoIncrement += mergeInfoEnd;
                    }
                    txnRoot.setEntry(target, ancestorEntryName, sourceEntry.getId(), sourceEntry.getType());
                } else {
                    txnRoot.deleteEntry(target, ancestorEntryName);
                }
            } else {

                if (sourceEntry == null || targetEntry == null) {
                    SVNErrorManager.error(FSErrors.errorConflict(SVNPathUtil.getAbsolutePath(SVNPathUtil.append(targetPath, ancestorEntryName)),
                            conflictPath), SVNLogType.FSFS);
                }

                if (sourceEntry.getType() == SVNNodeKind.FILE || targetEntry.getType() == SVNNodeKind.FILE || ancestorEntry.getType() == SVNNodeKind.FILE) {
                    SVNErrorManager.error(FSErrors.errorConflict(SVNPathUtil.getAbsolutePath(SVNPathUtil.append(targetPath, ancestorEntryName)),
                            conflictPath), SVNLogType.FSFS);
                }

                if (!sourceEntry.getId().getNodeID().equals(ancestorEntry.getId().getNodeID()) ||
                        !sourceEntry.getId().getCopyID().equals(ancestorEntry.getId().getCopyID()) ||
                        !targetEntry.getId().getNodeID().equals(ancestorEntry.getId().getNodeID()) ||
                        !targetEntry.getId().getCopyID().equals(ancestorEntry.getId().getCopyID())) {
                    SVNErrorManager.error(FSErrors.errorConflict(SVNPathUtil.getAbsolutePath(SVNPathUtil.append(targetPath, ancestorEntryName)),
                            conflictPath), SVNLogType.FSFS);
                }

                FSRevisionNode sourceEntryNode = owner.getRevisionNode(sourceEntry.getId());
                FSRevisionNode targetEntryNode = owner.getRevisionNode(targetEntry.getId());
                FSRevisionNode ancestorEntryNode = owner.getRevisionNode(ancestorEntry.getId());
                String childTargetPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(targetPath, targetEntry.getName()));
                long subMergeInfoIncrement = merge(owner, childTargetPath, targetEntryNode, sourceEntryNode, ancestorEntryNode, txnRoot, conflictPath);
                if (owner.supportsMergeInfo()) {
                    mergeInfoIncrement += subMergeInfoIncrement;
                }
            }

            removedEntries.add(ancestorEntryName);
        }

        for (Iterator sourceEntryNames = sourceEntries.keySet().iterator(); sourceEntryNames.hasNext();) {
            String sourceEntryName = (String) sourceEntryNames.next();
            if (removedEntries.contains(sourceEntryName)){
                continue;
            }
            FSEntry sourceEntry = (FSEntry) sourceEntries.get(sourceEntryName);
            FSEntry targetEntry = (FSEntry) targetEntries.get(sourceEntryName);
            if (targetEntry != null) {
                SVNErrorManager.error(FSErrors.errorConflict(SVNPathUtil.getAbsolutePath(SVNPathUtil.append(targetPath, targetEntry.getName())),
                        conflictPath), SVNLogType.FSFS);
            }
            if (owner.supportsMergeInfo()) {
                FSRevisionNode sourceEntryNode = owner.getRevisionNode(sourceEntry.getId());
                long mergeInfoCount = sourceEntryNode.getMergeInfoCount();
                mergeInfoIncrement += mergeInfoCount;
            }
            txnRoot.setEntry(target, sourceEntry.getName(), sourceEntry.getId(), sourceEntry.getType());
        }
        updateAncestry(owner, sourceId, targetId);
        if (owner.supportsMergeInfo()) {
            txnRoot.incrementMergeInfoCount(target, mergeInfoIncrement);
        }
        return mergeInfoIncrement;
    }

    private static void updateAncestry(FSFS owner, FSID sourceId, FSID targetId) throws SVNException {
        if (!targetId.isTxn()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_MUTABLE, "Attempt to update ancestry of non-mutable node");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        final FSRevisionNode targetNode = owner.getRevisionNode(targetId);
        final FSRevisionNode sourceNode = owner.getRevisionNode(sourceId);
        targetNode.setPredecessorId(sourceNode.getId());
        final long sourcePredecessorCount = sourceNode.getCount();
        targetNode.setPredecessorId(sourceId);
        targetNode.setCount(sourcePredecessorCount != -1 ? sourcePredecessorCount + 1 : sourcePredecessorCount);
        
        targetNode.setIsFreshTxnRoot(false);
        owner.putTxnRevisionNode(targetId, targetNode);
    }

    private void verifyLocks() throws SVNException {
        FSTransactionRoot txnRoot = getTxnRoot();
        Map changes = txnRoot.getChangedPaths();
        Object[] changedPaths = changes.keySet().toArray();
        Arrays.sort(changedPaths);

        String lastRecursedPath = null;
        for (int i = 0; i < changedPaths.length; i++) {
            String changedPath = (String) changedPaths[i];
            boolean recurse = true;

            if (lastRecursedPath != null && SVNPathUtil.getPathAsChild(lastRecursedPath, changedPath) != null) {
                continue;
            }

            FSPathChange change = (FSPathChange) changes.get(changedPath);

            if (change.getChangeKind() == FSPathChangeKind.FS_PATH_CHANGE_MODIFY) {
                recurse = false;
            }
            allowLockedOperation(myFSFS, changedPath, myAuthor, myLockTokens, recurse, true);

            if (recurse) {
                lastRecursedPath = changedPath;
            }
        }
    }

    private FSTransactionRoot getTxnRoot() throws SVNException {
        if (myTxnRoot == null && myTxn != null) {
            myTxnRoot = myFSFS.createTransactionRoot(myTxn);
        }
        return myTxnRoot;
    }

    public void allowLockedOperation(FSFS fsfs, String path, final String username, final Collection<String> lockTokens, boolean recursive, boolean haveWriteLock) throws SVNException {
        
        path = SVNPathUtil.canonicalizeAbsolutePath(path);
        if (recursive) {
            ISVNLockHandler handler = new ISVNLockHandler() {
                public void handleLock(String path, SVNLock lock, SVNErrorMessage error) throws SVNException {
                    if (isAutoUnlock()) {
                        scheduleForAutoUnlock(username, path, lock);
                    } else {
                        verifyLock(lock, lockTokens, username);
                    }
                }
                public void handleUnlock(String path, SVNLock lock, SVNErrorMessage error) throws SVNException {
                }
            };
            fsfs.walkDigestFiles(fsfs.getDigestFileFromRepositoryPath(path), handler, haveWriteLock);
        } else {
            SVNLock lock = fsfs.getLockHelper(path, haveWriteLock);
            if (lock != null) {
                if (isAutoUnlock()) {
                    scheduleForAutoUnlock(username, path, lock);
                } else {
                    verifyLock(lock, lockTokens, username);
                }
            }
        }
    }
    
    private void scheduleForAutoUnlock(final String username, String path, SVNLock lock) {
        myAutoUnlockPaths.put(path, lock.getID());
    }

    private void verifyLock(SVNLock lock, Collection<String> lockTokens, String username) throws SVNException {
        if (username == null || "".equals(username)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_USER, "Cannot verify lock on path ''{0}''; no username available", lock.getPath());
            SVNErrorManager.error(err, SVNLogType.FSFS);
        } else if (username.compareTo(lock.getOwner()) != 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_LOCK_OWNER_MISMATCH, "User {0} does not own lock on path ''{1}'' (currently locked by {2})", new Object[] {
                    username, lock.getPath(), lock.getOwner()
            });
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        if (lockTokens.contains(lock.getID())) {
            return;
        }
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_BAD_LOCK_TOKEN, "Cannot verify lock on path ''{0}''; no matching lock-token available", lock.getPath());
        SVNErrorManager.error(err, SVNLogType.FSFS);
    }

    public static void abortTransaction(FSFS fsfs, String txnId) throws SVNException {
        File txnDir = fsfs.getTransactionDir(txnId);
        SVNFileUtil.deleteAll(txnDir, true);
        if (txnDir.exists()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "Transaction cleanup failed");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
    }

}
