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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.internal.delta.SVNDeltaCombiner;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNLocationEntry;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSUpdateContext {

    private File myReportFile;
    private String myTarget;
    private OutputStream myReportOS;
    private FSFile myReportIS;
    private ISVNEditor myEditor;
    private long myTargetRevision;
    private SVNDepth myDepth;
    private PathInfo myCurrentPathInfo;
    private boolean ignoreAncestry;
    private boolean sendTextDeltas;
    private String myTargetPath;
    private boolean isSwitch;
    private boolean mySendCopyFromArgs;
    private FSRoot myTargetRoot;
    private LinkedList myRootsCache;
    private FSFS myFSFS;
    private FSRepository myRepository;
    private SVNDeltaGenerator myDeltaGenerator;
    private SVNDeltaCombiner myDeltaCombiner;

    public FSUpdateContext(FSRepository repository, FSFS owner, long revision, File reportFile, 
            String target, String targetPath, boolean isSwitch, SVNDepth depth, 
            boolean ignoreAncestry, boolean textDeltas, boolean sendCopyFromArgs, 
            ISVNEditor editor) {
        myRepository = repository;
        myFSFS = owner;
        myTargetRevision = revision;
        myReportFile = reportFile;
        myTarget = target;
        myEditor = editor;
        myDepth = depth;
        this.ignoreAncestry = ignoreAncestry;
        sendTextDeltas = textDeltas;
        myTargetPath = targetPath;
        this.isSwitch = isSwitch;
        mySendCopyFromArgs = sendCopyFromArgs;
    }

    public void reset(FSRepository repository, FSFS owner, long revision, File reportFile, String target, String targetPath, boolean isSwitch, SVNDepth depth, boolean ignoreAncestry,
            boolean textDeltas, boolean sendCopyFrom, ISVNEditor editor) throws SVNException {
        dispose();
        myRepository = repository;
        myFSFS = owner;
        myTargetRevision = revision;
        myReportFile = reportFile;
        myTarget = target;
        myEditor = editor;
        myDepth = depth;
        this.ignoreAncestry = ignoreAncestry;
        sendTextDeltas = textDeltas;
        myTargetPath = targetPath;
        this.isSwitch = isSwitch;
        mySendCopyFromArgs = sendCopyFrom;
    }
    
    public void setTargetRoot(FSRoot root) {
        myTargetRoot = root;
    }

    public OutputStream getReportFileForWriting() throws SVNException {
        if (myReportOS == null) {
            myReportOS = SVNFileUtil.openFileForWriting(myReportFile);
        }
        return myReportOS;
    }

    private boolean isIgnoreAncestry() {
        return ignoreAncestry;
    }

    private boolean isSwitch() {
        return isSwitch;
    }

    private boolean isSendTextDeltas() {
        return sendTextDeltas;
    }

    private String getReportTarget() {
        return myTarget;
    }

    private String getReportTargetPath() {
        return myTargetPath;
    }

    public void dispose() throws SVNException {
        SVNFileUtil.closeFile(myReportOS);
        myReportOS = null;

        if (myReportIS != null) {
            myReportIS.close();
            myReportIS = null;
        }

        if (myReportFile != null) {
            SVNFileUtil.deleteFile(myReportFile);
            myReportFile = null;
        }

        if (myDeltaCombiner != null) {
            myDeltaCombiner.reset();
        }

        myTargetRoot = null;
        myRootsCache = null;
    }

    private ISVNEditor getEditor() {
        return myEditor;
    }

    private long getTargetRevision() {
        return myTargetRevision;
    }

    private PathInfo getNextPathInfo() throws IOException, SVNException {
        if (myReportIS == null) {
            myReportIS = new FSFile(myReportFile);
        }
        myCurrentPathInfo = myReportIS.readPathInfoFromReportFile();
        return myCurrentPathInfo;
    }

    private PathInfo getCurrentPathInfo() {
        return myCurrentPathInfo;
    }

    private FSRoot getTargetRoot() throws SVNException {
        if (myTargetRoot == null) {
            myTargetRoot = myFSFS.createRevisionRoot(myTargetRevision);
        }
        return myTargetRoot;
    }

    private LinkedList getRootsCache() {
        if (myRootsCache == null) {
            myRootsCache = new LinkedList();
        }
        return myRootsCache;
    }

    private FSRevisionRoot getSourceRoot(long revision) throws SVNException {
        LinkedList cache = getRootsCache();
        FSRevisionRoot root = null;
        int i = 0;

        for (; i < cache.size() && i < 10; i++) {
            root = (FSRevisionRoot) myRootsCache.get(i);
            if (root.getRevision() == revision) {
                if (i != 0) {
                    myRootsCache.remove(i);
                    myRootsCache.addFirst(root);
                }
                break;
            }
            root = null;
        }

        if (root == null) {
            if (i == 10) {
                myRootsCache.removeLast();
            }
            root = myFSFS.createRevisionRoot(revision);
            myRootsCache.addFirst(root);
        }

        return root;
    }

    public void drive() throws SVNException {
        OutputStream reportOS = getReportFileForWriting();
        try {
            reportOS.write('-');
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        } finally {
            SVNFileUtil.closeFile(reportOS);
            myReportOS = null;
        }

        PathInfo info = null;

        try {
            info = getNextPathInfo();
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        }

        if (info == null || !info.getPath().equals(getReportTarget()) || info.getLinkPath() != null || FSRepository.isInvalidRevision(info.getRevision())) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_BAD_REVISION_REPORT, "Invalid report for top level of working copy");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        long sourceRevision = info.getRevision();
        PathInfo lookahead = null;

        try {
            lookahead = getNextPathInfo();
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        }

        if (lookahead != null && lookahead.getPath().equals(getReportTarget())) {
            if ("".equals(getReportTarget())) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_BAD_REVISION_REPORT, "Two top-level reports with no target");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            if (lookahead.getRevision() < 0) {
                lookahead.myDepth = info.getDepth();
            }

            info = lookahead;

            try {
                getNextPathInfo();
            } catch (IOException ioe) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
                SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
            }
        }

        String fullTargetPath = getReportTargetPath();
        String fullSourcePath = myRepository != null ?
                SVNPathUtil.getAbsolutePath(SVNPathUtil.append(myRepository.getRepositoryPath(""), getReportTarget())) :
                SVNPathUtil.getAbsolutePath(getReportTarget());
        FSEntry targetEntry = fakeDirEntry(fullTargetPath, getTargetRoot());
        FSRevisionRoot srcRoot = getSourceRoot(sourceRevision);
        FSEntry sourceEntry = fakeDirEntry(fullSourcePath, srcRoot);

        if (FSRepository.isValidRevision(info.getRevision()) && info.getLinkPath() == null && sourceEntry == null) {
            fullSourcePath = null;
        }

        if ("".equals(getReportTarget()) && targetEntry == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_PATH_SYNTAX, "Target path ''{0}'' does not exist", getReportTargetPath());
            SVNErrorManager.error(err, SVNLogType.FSFS);
        } else if ("".equals(getReportTarget()) && (sourceEntry == null || sourceEntry.getType() != SVNNodeKind.DIR || 
                targetEntry.getType() != SVNNodeKind.DIR)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_PATH_SYNTAX, "Cannot replace a directory from within");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (myDeltaGenerator == null) {
            myDeltaGenerator = new SVNDeltaGenerator();
        }

        if (myDeltaCombiner == null) {
            myDeltaCombiner = new SVNDeltaCombiner();
        }

        getEditor().targetRevision(getTargetRevision());
        getEditor().openRoot(sourceRevision);

        if ("".equals(getReportTarget())) {
            diffDirs(sourceRevision, fullSourcePath, fullTargetPath, "", info.isStartEmpty(), info.getDepth(), myDepth);
        } else {
            updateEntry(sourceRevision, fullSourcePath, sourceEntry, fullTargetPath, targetEntry, getReportTarget(), info, info.getDepth(), myDepth);
        }

        getEditor().closeDir();
        getEditor().closeEdit();
    }

    private void diffDirs(long sourceRevision, String sourcePath, String targetPath, String editPath, boolean startEmpty, SVNDepth wcDepth, SVNDepth requestedDepth) throws SVNException {
        diffProplists(sourceRevision, startEmpty == true ? null : sourcePath, editPath, targetPath, null, true);
        Map sourceEntries = null;
        
        if (requestedDepth.compareTo(SVNDepth.EMPTY) > 0 || 
                requestedDepth == SVNDepth.UNKNOWN) {
            
            if (sourcePath != null && !startEmpty) {
                FSRevisionRoot sourceRoot = getSourceRoot(sourceRevision);
                FSRevisionNode sourceNode = sourceRoot.getRevisionNode(sourcePath);
                sourceEntries = new SVNHashMap(sourceNode.getDirEntries(myFSFS));
            }
            FSRevisionNode targetNode = getTargetRoot().getRevisionNode(targetPath);

            Map targetEntries = new SVNHashMap(targetNode.getDirEntries(myFSFS));

            while (true) {
                Object[] nextInfo = fetchPathInfo(editPath);
                String entryName = (String) nextInfo[0];
                if (entryName == null) {
                    break;
                }
                PathInfo pathInfo = (PathInfo) nextInfo[1];
                if (pathInfo != null && FSRepository.isInvalidRevision(pathInfo.getRevision()) && pathInfo.getDepth() != SVNDepth.EXCLUDE) {
                    if (sourceEntries != null) {
                        sourceEntries.remove(entryName);
                    }
                    continue;
                }

                String entryEditPath = SVNPathUtil.append(editPath, entryName);
                String entryTargetPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(targetPath, entryName));
                FSEntry targetEntry = (FSEntry) targetEntries.get(entryName);
                String entrySourcePath = sourcePath != null ? SVNPathUtil.getAbsolutePath(SVNPathUtil.append(sourcePath, entryName)) : null;
                FSEntry sourceEntry = sourceEntries != null ? (FSEntry) sourceEntries.get(entryName) : null;
                
                if ((pathInfo == null || pathInfo.getDepth() != SVNDepth.EXCLUDE) && (requestedDepth != SVNDepth.FILES || ((targetEntry == null || 
                        targetEntry.getType() != SVNNodeKind.DIR) && (sourceEntry == null || sourceEntry.getType() != SVNNodeKind.DIR)))) {
                    updateEntry(sourceRevision, entrySourcePath, sourceEntry, 
                                entryTargetPath, targetEntry, entryEditPath, 
                                pathInfo, pathInfo != null ? pathInfo.getDepth() : 
                                getDepthBelow(wcDepth), getDepthBelow(requestedDepth));
                }
                
                targetEntries.remove(entryName);
                if (sourceEntries != null && (pathInfo == null || pathInfo.getDepth() != SVNDepth.EXCLUDE || targetEntry != null)) {
                    sourceEntries.remove(entryName);
                }
            }

            if (sourceEntries != null) {
                FSEntry[] srcEntries = (FSEntry[]) new ArrayList(sourceEntries.values()).toArray(new FSEntry[sourceEntries.size()]);
                for (int i = 0; i < srcEntries.length; i++) {
                    FSEntry srcEntry = srcEntries[i];
                    if (targetEntries.get(srcEntry.getName()) == null) {
                        if (srcEntry.getType() == SVNNodeKind.FILE && 
                                wcDepth.compareTo(SVNDepth.FILES) < 0) {
                            continue;
                        }
                        if (srcEntry.getType() == SVNNodeKind.DIR &&
                                (wcDepth.compareTo(SVNDepth.IMMEDIATES) < 0 || 
                                        requestedDepth == SVNDepth.FILES)) {
                            continue;
                        }
                        String entryEditPath = SVNPathUtil.append(editPath, srcEntry.getName());
                        long deletedRev = getDeletedRevision(SVNPathUtil.append(targetPath, 
                                                                    srcEntry.getName()), 
                                                                    sourceRevision, getTargetRevision());
                        getEditor().deleteEntry(entryEditPath, deletedRev);
                    }
                }
            }

            for (Iterator tgts = targetEntries.values().iterator(); tgts.hasNext();) {
                FSEntry tgtEntry = (FSEntry) tgts.next();
                FSEntry srcEntry = null;
                String entrySourcePath = null;
                if (!isDepthUpgrade(wcDepth, requestedDepth, tgtEntry.getType())) {
                    if (tgtEntry.getType() == SVNNodeKind.FILE && 
                        requestedDepth == SVNDepth.UNKNOWN &&
                        wcDepth.compareTo(SVNDepth.FILES) < 0) {
                        continue;
                    }
                    if (tgtEntry.getType() == SVNNodeKind.DIR && 
                        (wcDepth.compareTo(SVNDepth.IMMEDIATES) < 0 || 
                        requestedDepth == SVNDepth.FILES)) {
                        continue;
                    }
                    srcEntry = sourceEntries != null ? (FSEntry) sourceEntries.get(tgtEntry.getName()) : null;                    
                    entrySourcePath = srcEntry != null ? SVNPathUtil.getAbsolutePath(SVNPathUtil.append(sourcePath, tgtEntry.getName())) : null;
                }

                String entryEditPath = SVNPathUtil.append(editPath, tgtEntry.getName());
                String entryTargetPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(targetPath, tgtEntry.getName()));
                updateEntry(sourceRevision, entrySourcePath, srcEntry, entryTargetPath, 
                            tgtEntry, entryEditPath, null, getDepthBelow(wcDepth), 
                            getDepthBelow(requestedDepth));
            }
        }
    }

    private boolean isDepthUpgrade(SVNDepth wcDepth, SVNDepth requestedDepth, SVNNodeKind kind) {
        if (requestedDepth == SVNDepth.UNKNOWN || 
                wcDepth == SVNDepth.IMMEDIATES) {
            return false;
        }
        int compareResult = requestedDepth.compareTo(wcDepth);
        if (compareResult <= 0) {
            return false;
        }
        
        if (kind == SVNNodeKind.FILE && wcDepth == SVNDepth.FILES) {
            return false;
        }
        if (kind == SVNNodeKind.DIR && wcDepth == SVNDepth.EMPTY &&
                requestedDepth == SVNDepth.FILES) {
            return false;
        }
        return true;
    }
    
    private void diffFiles(long sourceRevision, String sourcePath, String targetPath, String editPath, String lockToken) throws SVNException {
        diffProplists(sourceRevision, sourcePath, editPath, targetPath, lockToken, false);
        String sourceHexDigest = null;
        FSRevisionRoot sourceRoot = null;
        if (sourcePath != null) {
            sourceRoot = getSourceRoot(sourceRevision);

            boolean changed = false;
            if (isIgnoreAncestry()) {
                changed = FSRepositoryUtil.checkFilesDifferent(sourceRoot, sourcePath, getTargetRoot(), 
                        targetPath, myDeltaCombiner);
            } else {
                changed = FSRepositoryUtil.areFileContentsChanged(sourceRoot, sourcePath, getTargetRoot(), targetPath);
            }
            if (!changed) {
                return;
            }
            FSRevisionNode sourceNode = sourceRoot.getRevisionNode(sourcePath);
            sourceHexDigest = sourceNode.getFileMD5Checksum();
        }
        
        FSRepositoryUtil.sendTextDelta(getEditor(), editPath, sourcePath, sourceHexDigest, 
                sourceRoot, targetPath, getTargetRoot(), isSendTextDeltas(), myDeltaCombiner, 
                myDeltaGenerator, myFSFS);
    }

    private void updateEntry(long sourceRevision, String sourcePath, FSEntry sourceEntry, 
            String targetPath, FSEntry targetEntry, String editPath, PathInfo pathInfo, 
            SVNDepth wcDepth, SVNDepth requestedDepth) throws SVNException {
        if (pathInfo != null && pathInfo.getLinkPath() != null && !isSwitch()) {
            targetPath = pathInfo.getLinkPath();
            targetEntry = fakeDirEntry(targetPath, getTargetRoot());
        }

        if (pathInfo != null && FSRepository.isInvalidRevision(pathInfo.getRevision())) {
            sourcePath = null;
            sourceEntry = null;
        } else if (pathInfo != null && sourcePath != null) {
            sourcePath = pathInfo.getLinkPath() != null ? pathInfo.getLinkPath() : sourcePath;
            sourceRevision = pathInfo.getRevision();
            FSRevisionRoot srcRoot = getSourceRoot(sourceRevision);
            sourceEntry = fakeDirEntry(sourcePath, srcRoot);
        }

        if (sourcePath != null && sourceEntry == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, "Working copy path ''{0}'' does not exist in repository", editPath);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        boolean related = false;
        if (sourceEntry != null && targetEntry != null && sourceEntry.getType() == targetEntry.getType()) {
            int distance = sourceEntry.getId().compareTo(targetEntry.getId());
            if (distance == 0 && !PathInfo.isRelevant(getCurrentPathInfo(), editPath) && 
                            (requestedDepth.compareTo(wcDepth) <= 0 || targetEntry.getType() == SVNNodeKind.FILE)) {
                if (pathInfo == null) {
                    return;
                }
                if (!pathInfo.isStartEmpty()) {
                    if (pathInfo.getLockToken() == null) {
                        return;
                    }
                    SVNLock exitsingLock = myFSFS.getLock(targetPath, false, false);
                    if (exitsingLock != null && exitsingLock.getID().equals(pathInfo.getLockToken())) {
                        return;
                    }
                }
            }
            related = distance != -1 || isIgnoreAncestry();
        }

        if (sourceEntry != null && !related) {
            long deletedRev = getDeletedRevision(targetPath, sourceRevision, getTargetRevision());
            getEditor().deleteEntry(editPath, deletedRev);
            sourcePath = null;
        }

        if (targetEntry == null) {
            skipPathInfo(editPath);
            return;
        }

        if (targetEntry.getType() == SVNNodeKind.DIR) {
            if (related) {
                getEditor().openDir(editPath, sourceRevision);
            } else {
                getEditor().addDir(editPath, null, SVNRepository.INVALID_REVISION);
            }
            diffDirs(sourceRevision, sourcePath, targetPath, editPath, pathInfo != null ? pathInfo.isStartEmpty() : false, wcDepth, requestedDepth);
            getEditor().closeDir();
        } else {
            if (related) {
                getEditor().openFile(editPath, sourceRevision);
                diffFiles(sourceRevision, sourcePath, targetPath, editPath, pathInfo != null ? pathInfo.getLockToken() : null);
            } else {
                SVNLocationEntry copyFromFile = addFileSmartly(editPath, targetPath);
                String copyFromPath = copyFromFile.getPath();
                long copyFromRevision = copyFromFile.getRevision();
                if (copyFromPath == null) {
                    diffFiles(sourceRevision, sourcePath, targetPath, editPath, 
                            pathInfo != null ? pathInfo.getLockToken() : null);
                } else {
                    diffFiles(copyFromRevision, copyFromPath, targetPath, editPath, 
                            pathInfo != null ? pathInfo.getLockToken() : null);
                }
            }
            FSRevisionNode targetNode = getTargetRoot().getRevisionNode(targetPath);
            String targetHexDigest = targetNode.getFileMD5Checksum();
            getEditor().closeFile(editPath, targetHexDigest);
        }
    }

    private long getDeletedRevision(String targetPath, long sourceRevision, long targetRevision) throws SVNException {
        if (isTransactionTarget()) {
            return getTargetRevision();
        }
        return myFSFS.getDeletedRevision(targetPath, sourceRevision, targetRevision);
    }
    
    private boolean isTransactionTarget() throws SVNException {
        return getTargetRoot() instanceof FSTransactionRoot;
    }

    private SVNLocationEntry addFileSmartly(String editPath, String originalPath) throws SVNException {
        String copyFromPath = null;
        long copyFromRevision = SVNRepository.INVALID_REVISION;
        if (mySendCopyFromArgs) {
            if (!isTransactionTarget()) {
                FSClosestCopy closestCopy = ((FSRevisionRoot) getTargetRoot()).getClosestCopy(originalPath);
                if (closestCopy != null) {
                    FSRevisionRoot closestCopyRoot = closestCopy.getRevisionRoot();
                    String closestCopyPath = closestCopy.getPath();
                    if (originalPath.equals(closestCopyPath)) {
                        FSRevisionNode closestCopyFromNode = closestCopyRoot.getRevisionNode(
                                closestCopyPath);
                        copyFromPath = closestCopyFromNode.getCopyFromPath();
                        copyFromRevision = closestCopyFromNode.getCopyFromRevision();
                    }
                }
            } else if (isTransactionTarget()) {
                FSTransactionRoot txn = (FSTransactionRoot) getTargetRoot();
                FSPathChange change = (FSPathChange) txn.getChangedPaths().get(originalPath);
                if (change != null) {
                    copyFromPath = change.getCopyPath();
                    copyFromRevision = change.getCopyRevision();
                }
            }
        }
        myEditor.addFile(editPath, copyFromPath, copyFromRevision);
        return new SVNLocationEntry(copyFromRevision, copyFromPath);
    }
    
    private Map computeMetaProperties(long revision) throws SVNException {
        Map metaProperties = new SVNHashMap();
        if (FSRepository.isValidRevision(revision)) {
            SVNProperties entryProps = myFSFS.compoundMetaProperties(revision);
            metaProperties.put(SVNProperty.COMMITTED_REVISION, entryProps.getSVNPropertyValue(SVNProperty.COMMITTED_REVISION));
            metaProperties.put(SVNProperty.COMMITTED_DATE, entryProps.getSVNPropertyValue(SVNProperty.COMMITTED_DATE));
            metaProperties.put(SVNProperty.LAST_AUTHOR, entryProps.getSVNPropertyValue(SVNProperty.LAST_AUTHOR));
            metaProperties.put(SVNProperty.UUID, entryProps.getSVNPropertyValue(SVNProperty.UUID));
        } else if (!FSRepository.isValidRevision(revision) && isTransactionTarget()) {
            FSTransactionRoot txnRoot = (FSTransactionRoot) getTargetRoot();
            SVNProperties txnProperties = myFSFS.getTransactionProperties(txnRoot.getTxnID());
            metaProperties.put(SVNProperty.COMMITTED_REVISION, SVNPropertyValue.create(Long.toString(getTargetRevision())));
            metaProperties.put(SVNProperty.COMMITTED_DATE, txnProperties.getSVNPropertyValue(SVNRevisionProperty.DATE));
            metaProperties.put(SVNProperty.LAST_AUTHOR, txnProperties.getSVNPropertyValue(SVNRevisionProperty.AUTHOR));
            metaProperties.put(SVNProperty.UUID, SVNPropertyValue.create(myFSFS.getUUID()));
        }  else {
            metaProperties = null;
        }
        return metaProperties;
    }
    
    private void diffProplists(long sourceRevision, String sourcePath, String editPath, String targetPath, String lockToken, boolean isDir) throws SVNException {
        FSRevisionNode targetNode = getTargetRoot().getRevisionNode(targetPath);
        long createdRevision = targetNode.getCreatedRevision();
        Map metaProperties = computeMetaProperties(createdRevision);
        if (metaProperties != null) {
            SVNPropertyValue committedRevision = (SVNPropertyValue) metaProperties.get(SVNProperty.COMMITTED_REVISION);
            changeProperty(editPath, SVNProperty.COMMITTED_REVISION, committedRevision, isDir);
            SVNPropertyValue committedDate = (SVNPropertyValue) metaProperties.get(SVNProperty.COMMITTED_DATE);
            if (committedDate != null || sourcePath != null) {
                changeProperty(editPath, SVNProperty.COMMITTED_DATE, committedDate, isDir);
            }
            SVNPropertyValue lastAuthor = (SVNPropertyValue) metaProperties.get(SVNProperty.LAST_AUTHOR);
            if (lastAuthor != null || sourcePath != null) {
                changeProperty(editPath, SVNProperty.LAST_AUTHOR, lastAuthor, isDir);
            }
            SVNPropertyValue uuid = (SVNPropertyValue) metaProperties.get(SVNProperty.UUID);
            if (uuid != null || sourcePath != null) {
                changeProperty(editPath, SVNProperty.UUID, uuid, isDir);
            }
        } 
        
        if (lockToken != null) {
            SVNLock lock = myFSFS.getLockHelper(targetPath, false);
            if (lock == null || !lockToken.equals(lock.getID())) {
                changeProperty(editPath, SVNProperty.LOCK_TOKEN,  null, isDir);
            }
        }

        SVNProperties sourceProps = null;
        if (sourcePath != null) {
            FSRevisionRoot sourceRoot = getSourceRoot(sourceRevision);
            FSRevisionNode sourceNode = sourceRoot.getRevisionNode(sourcePath);
            boolean propsChanged = !FSRepositoryUtil.arePropertiesEqual(sourceNode, targetNode);
            if (!propsChanged) {
                return;
            }
            sourceProps = sourceNode.getProperties(myFSFS);
        } else {
            sourceProps = new SVNProperties();
        }

        SVNProperties targetProps = targetNode.getProperties(myFSFS);
        SVNProperties propsDiffs = FSRepositoryUtil.getPropsDiffs(sourceProps, targetProps);
        Object[] names = propsDiffs.nameSet().toArray();
        for (int i = 0; i < names.length; i++) {
            String propName = (String) names[i];
            changeProperty(editPath, propName, propsDiffs.getSVNPropertyValue(propName), isDir);
        }
    }

    private SVNDepth getDepthBelow(SVNDepth depth) {
        return depth == SVNDepth.IMMEDIATES ? SVNDepth.EMPTY : depth;
    }
    
    private Object[] fetchPathInfo(String prefix) throws SVNException {
        Object[] result = new Object[2];
        PathInfo pathInfo = getCurrentPathInfo();
        if (!PathInfo.isRelevant(pathInfo, prefix)) {
            result[0] = null;
            result[1] = null;
        } else {
            String relPath = "".equals(prefix) ? pathInfo.getPath() : pathInfo.getPath().substring(prefix.length() + 1);
            if (relPath.indexOf('/') != -1) {
                result[0] = relPath.substring(0, relPath.indexOf('/'));
                result[1] = null;
            } else {
                result[0] = relPath;
                result[1] = pathInfo;
                try {
                    getNextPathInfo();
                } catch (IOException ioe) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
                    SVNErrorManager.error(err, SVNLogType.FSFS);
                }
            }
        }
        return result;
    }

    private void changeProperty(String path, String name, SVNPropertyValue value, boolean isDir) throws SVNException {
        if (isDir) {
            getEditor().changeDirProperty(name, value);
        } else {
            getEditor().changeFileProperty(path, name, value);
        }
    }

    private FSEntry fakeDirEntry(String reposPath, FSRoot root) throws SVNException {
        if (root.checkNodeKind(reposPath) == SVNNodeKind.NONE) {
            return null;
        }

        FSRevisionNode node = root.getRevisionNode(reposPath);
        FSEntry dirEntry = new FSEntry(node.getId(), node.getType(), SVNPathUtil.tail(node.getCreatedPath()));
        return dirEntry;
    }

    private void skipPathInfo(String prefix) throws SVNException {
        while (PathInfo.isRelevant(getCurrentPathInfo(), prefix)) {
            try {
                getNextPathInfo();
            } catch (IOException ioe) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        }
    }

    private void writeSingleString(String s, OutputStream out) throws IOException {
        if (s != null) {
            byte[] b = s.getBytes("UTF-8");
            out.write('+');
            out.write(String.valueOf(b.length).getBytes("UTF-8"));
            out.write(':');
            out.write(b);
        } else {
            out.write('-');
        }
    }
    
    public void writePathInfoToReportFile(String path, String linkPath, String lockToken, long revision, boolean startEmpty, SVNDepth depth) throws SVNException {
        if (depth == null || depth == SVNDepth.UNKNOWN) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_BAD_ARGS, "Unsupported report depth ''{0}''", depth != null ? depth.getName() : "null");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        String anchorRelativePath = SVNPathUtil.append(getReportTarget(), path);
        String revisionRep = FSRepository.isValidRevision(revision) ? "+" + String.valueOf(revision) + ":" : "-";
        String depthRep = "-";//infinity by default
        if (depth == SVNDepth.EXCLUDE || depth == SVNDepth.EMPTY || depth == SVNDepth.FILES || depth == SVNDepth.IMMEDIATES) {
            depthRep = "+" + getDepthLetter(depth);
        } 
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeSingleString(anchorRelativePath, baos);
            writeSingleString(linkPath, baos);
            baos.write(revisionRep.getBytes("UTF-8"));
            baos.write(depthRep.getBytes("UTF-8"));
            baos.write(startEmpty ? '+' : '-');
            writeSingleString(lockToken, baos);
            OutputStream reportOS = getReportFileForWriting();
            reportOS.write(baos.toByteArray());
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        }
    }

    public String getDepthLetter(SVNDepth depth) throws SVNException {
        if (depth == SVNDepth.EXCLUDE) {
            return "X";
        }
        if (depth == SVNDepth.EMPTY) {
            return "E";
        }
        if (depth == SVNDepth.FILES) {
            return "F";
        }
        if (depth == SVNDepth.IMMEDIATES) {
            return "M";
        }
        
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.REPOS_BAD_ARGS, "Unsupported report depth ''{0}''", SVNDepth.asString(depth));
        SVNErrorManager.error(err, SVNLogType.FSFS);
        return null;
    }
}
