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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.SVNWCProperties;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSTransactionRoot extends FSRoot {

    public static final int SVN_FS_TXN_CHECK_OUT_OF_DATENESS = 0x00001;
    public static final int SVN_FS_TXN_CHECK_LOCKS = 0x00002;

    private String myTxnID;
    private int myTxnFlags;
    private File myTxnChangesFile;
    private File myTxnRevFile;
    private long myBaseRevision;
    
    public FSTransactionRoot(FSFS owner, String txnID, long baseRevision, int flags) {
        super(owner);
        myTxnID = txnID;
        myTxnFlags = flags;
        myBaseRevision = baseRevision;
    }

    public long getRevision() {
        return myBaseRevision;
    }

    public FSCopyInheritance getCopyInheritance(FSParentPath child) throws SVNException {
        if (child == null || child.getParent() == null || myTxnID == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "FATAL error: invalid txn name or child");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        FSID childID = child.getRevNode().getId();
        FSID parentID = child.getParent().getRevNode().getId();
        String childCopyID = childID.getCopyID();
        String parentCopyID = parentID.getCopyID();

        if (childID.isTxn()) {
            return new FSCopyInheritance(FSCopyInheritance.COPY_ID_INHERIT_SELF, null);
        }

        FSCopyInheritance copyInheritance = new FSCopyInheritance(FSCopyInheritance.COPY_ID_INHERIT_PARENT, null);

        if (childCopyID.compareTo("0") == 0) {
            return copyInheritance;
        }

        if (childCopyID.compareTo(parentCopyID) == 0) {
            return copyInheritance;
        }

        long copyrootRevision = child.getRevNode().getCopyRootRevision();
        String copyrootPath = child.getRevNode().getCopyRootPath();

        FSRoot copyrootRoot = getOwner().createRevisionRoot(copyrootRevision);
        FSRevisionNode copyrootNode = copyrootRoot.getRevisionNode(copyrootPath);
        FSID copyrootID = copyrootNode.getId();
        if (copyrootID.compareTo(childID) == -1) {
            return copyInheritance;
        }

        String idPath = child.getRevNode().getCreatedPath();
        if (idPath.compareTo(child.getAbsPath()) == 0) {
            copyInheritance.setStyle(FSCopyInheritance.COPY_ID_INHERIT_SELF);
            return copyInheritance;
        }

        copyInheritance.setStyle(FSCopyInheritance.COPY_ID_INHERIT_NEW);
        copyInheritance.setCopySourcePath(idPath);
        return copyInheritance;
    }

    public FSRevisionNode getRootRevisionNode() throws SVNException {
        if (myRootRevisionNode == null) {
            FSTransactionInfo txn = getTxn();
            myRootRevisionNode = getOwner().getRevisionNode(txn.getRootID());
        }
        return myRootRevisionNode;
    }

    public FSRevisionNode getTxnBaseRootNode() throws SVNException {
        FSTransactionInfo txn = getTxn();
        FSRevisionNode baseRootNode = getOwner().getRevisionNode(txn.getBaseID());
        return baseRootNode;
    }

    public FSTransactionInfo getTxn() throws SVNException {
        FSID rootID = FSID.createTxnId("0", "0", myTxnID);
        FSRevisionNode revNode = getOwner().getRevisionNode(rootID);
        FSTransactionInfo txn = new FSTransactionInfo(revNode.getId(), revNode.getPredecessorId());
        return txn;
    }

    public Map getChangedPaths() throws SVNException {
        FSFile file = getOwner().getTransactionChangesFile(myTxnID);
        try {
            return fetchAllChanges(file, false);
        } finally {
            file.close();
        }
    }

    public int getTxnFlags() {
        return myTxnFlags;
    }

    public void setTxnFlags(int txnFlags) {
        myTxnFlags = txnFlags;
    }

    public String getTxnID() {
        return myTxnID;
    }

    public SVNProperties unparseDirEntries(Map entries) {
        SVNProperties unparsedEntries = new SVNProperties();
        for (Iterator names = entries.keySet().iterator(); names.hasNext();) {
            String name = (String) names.next();
            FSEntry dirEntry = (FSEntry) entries.get(name);
            String unparsedVal = dirEntry.toString();
            unparsedEntries.put(name, unparsedVal);
        }
        return unparsedEntries;
    }

    public static FSTransactionInfo beginTransactionForCommit(long baseRevision, SVNProperties revisionProperties, FSFS owner) throws SVNException {
        List caps = new ArrayList();
        caps.add("mergeinfo");
        String author = revisionProperties.getStringValue(SVNRevisionProperty.AUTHOR);
        if (owner != null && owner.isHooksEnabled()) {
            FSHooks.runStartCommitHook(owner.getRepositoryRoot(), author, caps);
        }
        FSTransactionInfo txn = FSTransactionRoot.beginTransaction(baseRevision, FSTransactionRoot.SVN_FS_TXN_CHECK_LOCKS, owner);
        owner.changeTransactionProperties(txn.getTxnId(), revisionProperties);
        return txn;
    }

    public static FSTransactionInfo beginTransaction(long baseRevision, int flags, FSFS owner) throws SVNException {
        FSTransactionInfo txn = createTxn(baseRevision, owner);
        String commitTime = SVNDate.formatDate(new Date(System.currentTimeMillis()));
        owner.setTransactionProperty(txn.getTxnId(), SVNRevisionProperty.DATE, SVNPropertyValue.create(commitTime));

        if ((flags & SVN_FS_TXN_CHECK_OUT_OF_DATENESS) != 0) {
            owner.setTransactionProperty(txn.getTxnId(), SVNProperty.TXN_CHECK_OUT_OF_DATENESS, SVNPropertyValue.create(Boolean.TRUE.toString()));
        }

        if ((flags & FSTransactionRoot.SVN_FS_TXN_CHECK_LOCKS) != 0) {
            owner.setTransactionProperty(txn.getTxnId(), SVNProperty.TXN_CHECK_LOCKS, SVNPropertyValue.create(Boolean.TRUE.toString()));
        }

        return txn;
    }

    private static FSTransactionInfo createTxn(long baseRevision, FSFS owner) throws SVNException {
        String txnId = null;
        if (owner.getDBFormat() >= FSFS.MIN_CURRENT_TXN_FORMAT) {
            txnId = createTxnDir(baseRevision, owner);    
        } else {
            txnId = createPre15TxnDir(baseRevision, owner);
        }
        
        FSTransactionInfo txn = new FSTransactionInfo(baseRevision, txnId);
        FSRevisionRoot root = owner.createRevisionRoot(baseRevision);
        FSRevisionNode rootNode = root.getRootRevisionNode();
        owner.createNewTxnNodeRevisionFromRevision(txnId, rootNode);
        SVNFileUtil.createEmptyFile(owner.getTransactionProtoRevFile(txn.getTxnId()));
        SVNFileUtil.createEmptyFile(owner.getTransactionProtoRevLockFile(txn.getTxnId()));
        SVNFileUtil.createEmptyFile(new File(owner.getTransactionDir(txn.getTxnId()), "changes"));
        owner.writeNextIDs(txnId, "0", "0");
        return txn;
    }

    private static String createTxnDir(long revision, FSFS owner) throws SVNException {
        String txnId = owner.getAndIncrementTxnKey();
        txnId = String.valueOf(revision) + "-" + txnId;
        File parent = owner.getTransactionsParentDir();
        File txnDir = new File(parent, txnId + FSFS.TXN_PATH_EXT);
        txnDir.mkdirs();
        return txnId;
    }

    private static String createPre15TxnDir(long revision, FSFS owner) throws SVNException {
        File parent = owner.getTransactionsParentDir();
        File uniquePath = null;

        for (int i = 1; i < 99999; i++) {
            String txnId = String.valueOf(revision) + "-" + String.valueOf(i);
            uniquePath = new File(parent, txnId + FSFS.TXN_PATH_EXT);
            if (!uniquePath.exists() && uniquePath.mkdirs()) {
                return txnId;
            }
            if (!uniquePath.exists()) {
                break;
            }
        }
        
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_UNIQUE_NAMES_EXHAUSTED, 
                "Unable to create transaction directory in ''{0}'' for revision {1}", 
                new Object[] { parent, new Long(revision) });
        SVNErrorManager.error(err, SVNLogType.FSFS);
        return null;    
    }
    
    public void deleteEntry(FSRevisionNode parent, String entryName) throws SVNException {
        if (parent.getType() != SVNNodeKind.DIR) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_DIRECTORY, "Attempted to delete entry ''{0}'' from *non*-directory node", entryName);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (!parent.getId().isTxn()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_MUTABLE, "Attempted to delete entry ''{0}'' from immutable directory node", entryName);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (!SVNPathUtil.isSinglePathComponent(entryName)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_SINGLE_PATH_COMPONENT, "Attempted to delete a node with an illegal name ''{0}''", entryName);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        Map entries = parent.getDirEntries(getOwner());
        FSEntry dirEntry = (FSEntry) entries.get(entryName);

        if (dirEntry == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NO_SUCH_ENTRY, "Delete failed--directory has no entry ''{0}''", entryName);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        getOwner().getRevisionNode(dirEntry.getId());
        deleteEntryIfMutable(dirEntry.getId());
        setEntry(parent, entryName, null, SVNNodeKind.UNKNOWN);
    }

    public void incrementMergeInfoCount(FSRevisionNode node, long increment) throws SVNException {
        if (!node.getId().isTxn()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_MUTABLE, 
                    "Can''t increment mergeinfo count on *immutable* node-revision {0}", node.getId());
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        if (increment == 0) {
            return;
        }
        
        node.setMergeInfoCount(node.getMergeInfoCount() + increment);
        if (node.getMergeInfoCount() < 0) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, 
                    "Can''t increment mergeinfo count on node-revision {0} to negative value {1}",
                    new Object[] { node.getId(), new Long(node.getMergeInfoCount()) });
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        if (node.getMergeInfoCount() > 1 && node.getType() == SVNNodeKind.FILE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, 
                    "Can''t increment mergeinfo count on *file* node-revision {0} to {1} (> 1)",
                    new Object[] { node.getId(), new Long(node.getMergeInfoCount()) });
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        getOwner().putTxnRevisionNode(node.getId(), node);
    }
    
    private void deleteEntryIfMutable(FSID id) throws SVNException {
        FSRevisionNode node = getOwner().getRevisionNode(id);
        if (!node.getId().isTxn()) {
            return;
        }

        if (node.getType() == SVNNodeKind.DIR) {
            Map entries = node.getDirEntries(getOwner());
            for (Iterator names = entries.keySet().iterator(); names.hasNext();) {
                String name = (String) names.next();
                FSEntry entry = (FSEntry) entries.get(name);
                deleteEntryIfMutable(entry.getId());
            }
        }

        removeRevisionNode(id);
    }

    private void removeRevisionNode(FSID id) throws SVNException {
        FSRevisionNode node = getOwner().getRevisionNode(id);

        if (!node.getId().isTxn()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_MUTABLE, "Attempted removal of immutable node");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (node.getPropsRepresentation() != null && node.getPropsRepresentation().isTxn()) {
            SVNFileUtil.deleteFile(getTransactionRevNodePropsFile(id));
        }

        if (node.getTextRepresentation() != null && node.getTextRepresentation().isTxn() && node.getType() == SVNNodeKind.DIR) {
            SVNFileUtil.deleteFile(getTransactionRevNodeChildrenFile(id));
        }

        SVNFileUtil.deleteFile(getOwner().getTransactionRevNodeFile(id));
    }

    public void setProplist(FSRevisionNode node, SVNProperties properties) throws SVNException {
        if (!node.getId().isTxn()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_MUTABLE, "Can't set proplist on *immutable* node-revision {0}", node.getId());
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        File propsFile = getTransactionRevNodePropsFile(node.getId());
        SVNWCProperties.setProperties(properties, propsFile,
                                    SVNFileUtil.createUniqueFile(propsFile.getParentFile(), 
                                                                 ".props", ".tmp", false), 
                                    SVNWCProperties.SVN_HASH_TERMINATOR);

        if (node.getPropsRepresentation() == null || !node.getPropsRepresentation().isTxn()) {
            FSRepresentation mutableRep = new FSRepresentation();
            mutableRep.setTxnId(node.getId().getTxnID());
            node.setPropsRepresentation(mutableRep);
            node.setIsFreshTxnRoot(false);
            getOwner().putTxnRevisionNode(node.getId(), node);
        }
    }

    public FSID createSuccessor(FSID oldId, FSRevisionNode newRevNode, String copyId) throws SVNException {
        if (copyId == null) {
            copyId = oldId.getCopyID();
        }
        FSID id = FSID.createTxnId(oldId.getNodeID(), copyId, myTxnID);
        newRevNode.setId(id);
        if (newRevNode.getCopyRootPath() == null) {
            newRevNode.setCopyRootPath(newRevNode.getCreatedPath());
            newRevNode.setCopyRootRevision(newRevNode.getId().getRevision());
        }
        newRevNode.setIsFreshTxnRoot(false);
        getOwner().putTxnRevisionNode(newRevNode.getId(), newRevNode);
        return id;
    }

    public void setEntry(FSRevisionNode parentRevNode, String entryName, FSID entryId, SVNNodeKind kind) throws SVNException {
        if (parentRevNode.getType() != SVNNodeKind.DIR) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_DIRECTORY, "Attempted to set entry in non-directory node");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (!parentRevNode.getId().isTxn()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_MUTABLE, "Attempted to set entry in immutable node");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        FSRepresentation textRep = parentRevNode.getTextRepresentation();
        File childrenFile = getTransactionRevNodeChildrenFile(parentRevNode.getId());
        OutputStream dst = null;

        try {
            if (textRep == null || !textRep.isTxn()) {
                Map entries = parentRevNode.getDirEntries(getOwner());
                SVNProperties unparsedEntries = unparseDirEntries(entries);
                dst = SVNFileUtil.openFileForWriting(childrenFile);
                SVNWCProperties.setProperties(unparsedEntries, dst, SVNWCProperties.SVN_HASH_TERMINATOR);
                textRep = new FSRepresentation();
                textRep.setRevision(SVNRepository.INVALID_REVISION);
                textRep.setTxnId(myTxnID);
                String uniqueSuffix = getNewTxnNodeId();
                String uniquifier = myTxnID + '/' + uniqueSuffix;
                textRep.setUniquifier(uniquifier);
                parentRevNode.setTextRepresentation(textRep);
                parentRevNode.setIsFreshTxnRoot(false);
                getOwner().putTxnRevisionNode(parentRevNode.getId(), parentRevNode);
            } else {
                dst = SVNFileUtil.openFileForWriting(childrenFile, true);
            }
            Map dirContents = parentRevNode.getDirContents();
            if (entryId != null) {
                SVNWCProperties.appendProperty(entryName, SVNPropertyValue.create(kind + " " + entryId.toString()), dst);
                if (dirContents != null) {
                    dirContents.put(entryName, new FSEntry(entryId, kind, entryName));
                }
            } else {
                SVNWCProperties.appendPropertyDeleted(entryName, dst);
                if (dirContents != null) {
                    dirContents.remove(entryName);
                }
            }
        } finally {
            SVNFileUtil.closeFile(dst);
        }
    }

    public void writeChangeEntry(OutputStream changesFile, FSPathChange pathChange, boolean includeNodeKind) throws SVNException, IOException {
        FSPathChangeKind changeKind = pathChange.getChangeKind();
        if (!(changeKind == FSPathChangeKind.FS_PATH_CHANGE_ADD || changeKind == FSPathChangeKind.FS_PATH_CHANGE_DELETE || changeKind == FSPathChangeKind.FS_PATH_CHANGE_MODIFY
                || changeKind == FSPathChangeKind.FS_PATH_CHANGE_REPLACE || changeKind == FSPathChangeKind.FS_PATH_CHANGE_RESET)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Invalid change type");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        String changeString = changeKind.toString();
        if (includeNodeKind) {
            changeString += "-" + pathChange.getKind().toString();
        }            
        String idString = null;
        if (pathChange.getRevNodeId() != null) {
            idString = pathChange.getRevNodeId().toString();
        } else {
            idString = FSPathChangeKind.ACTION_RESET;
        }

        String output = idString + " " + changeString + " " + SVNProperty.toString(pathChange.isTextModified()) + " " + SVNProperty.toString(pathChange.arePropertiesModified()) + " "
                + pathChange.getPath() + "\n";
        changesFile.write(output.getBytes("UTF-8"));

        String copyfromPath = pathChange.getCopyPath();
        long copyfromRevision = pathChange.getCopyRevision();

        if (copyfromPath != null && copyfromRevision != SVNRepository.INVALID_REVISION) {
            String copyfromLine = copyfromRevision + " " + copyfromPath;
            changesFile.write(copyfromLine.getBytes("UTF-8"));
        }
        changesFile.write("\n".getBytes("UTF-8"));
    }

    public long writeFinalChangedPathInfo(final CountingOutputStream protoFile) throws SVNException, IOException {
        long offset = protoFile.getPosition();
        Map changedPaths = getChangedPaths();
        boolean includeNodeKind = getOwner().getDBFormat() >= FSFS.MIN_KIND_IN_CHANGED_FORMAT;

        for (Iterator paths = changedPaths.keySet().iterator(); paths.hasNext();) {
            String path = (String) paths.next();
            FSPathChange change = (FSPathChange) changedPaths.get(path);
            FSID id = change.getRevNodeId();

            if (change.getChangeKind() != FSPathChangeKind.FS_PATH_CHANGE_DELETE && !id.isTxn()) {
                FSRevisionNode revNode = getOwner().getRevisionNode(id);
                change.setRevNodeId(revNode.getId());
            }
            writeChangeEntry(protoFile, change, includeNodeKind);
        }
        return offset;
    }

    public String[] readNextIDs() throws SVNException {
        String[] ids = new String[2];
        String idsToParse = null;
        FSFile idsFile = new FSFile(getOwner().getNextIDsFile(myTxnID));

        try {
            idsToParse = idsFile.readLine(FSRepositoryUtil.MAX_KEY_SIZE * 2 + 3);
        } finally {
            idsFile.close();
        }

        int delimiterInd = idsToParse.indexOf(' ');

        if (delimiterInd == -1) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "next-ids file corrupt");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        ids[0] = idsToParse.substring(0, delimiterInd);
        ids[1] = idsToParse.substring(delimiterInd + 1);
        return ids;
    }

    public void writeFinalCurrentFile(long newRevision, String startNodeId, String startCopyId) throws SVNException, IOException {
        if (getOwner().getDBFormat() >= FSFS.MIN_NO_GLOBAL_IDS_FORMAT) {
            getOwner().writeCurrentFile(newRevision, null, null);
            return;
        }
        
        String[] txnIds = readNextIDs();
        String txnNodeId = txnIds[0];
        String txnCopyId = txnIds[1];
        String newNodeId = FSTransactionRoot.addKeys(startNodeId, txnNodeId);
        String newCopyId = FSTransactionRoot.addKeys(startCopyId, txnCopyId);
        getOwner().writeCurrentFile(newRevision, newNodeId, newCopyId);
    }

    public FSID writeFinalRevision(FSID newId, final CountingOutputStream protoFile, long revision, FSID id, 
            String startNodeId, String startCopyId, Collection<FSRepresentation> representations) throws SVNException, IOException {
        newId = null;
        if (!id.isTxn()) {
            return newId;
        }
        FSFS owner = getOwner();
        FSRevisionNode revNode = owner.getRevisionNode(id);
        if (revNode.getType() == SVNNodeKind.DIR) {
            Map namesToEntries = revNode.getDirEntries(owner);
            for (Iterator entries = namesToEntries.values().iterator(); entries.hasNext();) {
                FSEntry dirEntry = (FSEntry) entries.next();
                newId = writeFinalRevision(newId, protoFile, revision, dirEntry.getId(), 
                        startNodeId, startCopyId, representations);
                if (newId != null && newId.getRevision() == revision) {
                    dirEntry.setId(newId);
                }
            }
            if (revNode.getTextRepresentation() != null && revNode.getTextRepresentation().isTxn()) {
                SVNProperties unparsedEntries = unparseDirEntries(namesToEntries);
                FSRepresentation textRep = revNode.getTextRepresentation();
                textRep.setTxnId(null);
                textRep.setRevision(revision);
                try {
                    textRep.setOffset(protoFile.getPosition());
                    final MessageDigest checksum = MessageDigest.getInstance("MD5");
                    long size = writeHashRepresentation(unparsedEntries, protoFile, checksum);
                    String hexDigest = SVNFileUtil.toHexDigest(checksum);
                    textRep.setSize(size);
                    textRep.setMD5HexDigest(hexDigest);
                    textRep.setExpandedSize(textRep.getSize());
                } catch (NoSuchAlgorithmException nsae) {
                    SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, 
                            "MD5 implementation not found: {0}", nsae.getLocalizedMessage());
                    SVNErrorManager.error(err, nsae, SVNLogType.FSFS);
                }
            }
        } else {
            if (revNode.getTextRepresentation() != null && revNode.getTextRepresentation().isTxn()) {
                FSRepresentation textRep = revNode.getTextRepresentation();
                textRep.setTxnId(null);
                textRep.setRevision(revision);
            }
        }

        if (revNode.getPropsRepresentation() != null && revNode.getPropsRepresentation().isTxn()) {
            SVNProperties props = revNode.getProperties(owner);
            FSRepresentation propsRep = revNode.getPropsRepresentation();
            try {
                propsRep.setOffset(protoFile.getPosition());
                final MessageDigest checksum = MessageDigest.getInstance("MD5");
                long size = writeHashRepresentation(props, protoFile, checksum);
                String hexDigest = SVNFileUtil.toHexDigest(checksum);
                propsRep.setSize(size);
                propsRep.setMD5HexDigest(hexDigest);
                propsRep.setTxnId(null);
                propsRep.setRevision(revision);
                propsRep.setExpandedSize(size);
            } catch (NoSuchAlgorithmException nsae) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, 
                        "MD5 implementation not found: {0}", nsae.getLocalizedMessage());
                SVNErrorManager.error(err, nsae, SVNLogType.FSFS);
            }
        }

        long myOffset = protoFile.getPosition();
        String myNodeId = null;
        String nodeId = revNode.getId().getNodeID();

        if (nodeId.startsWith("_")) {
            if (getOwner().getDBFormat() >= FSFS.MIN_NO_GLOBAL_IDS_FORMAT) {
                myNodeId = nodeId.substring(1) + "-" + revision; 
            } else {
                myNodeId = FSTransactionRoot.addKeys(startNodeId, nodeId.substring(1));
            }
        } else {
            myNodeId = nodeId;
        }

        String myCopyId = null;
        String copyId = revNode.getId().getCopyID();

        if (copyId.startsWith("_")) {
            if (getOwner().getDBFormat() >= FSFS.MIN_NO_GLOBAL_IDS_FORMAT) {
                myCopyId = copyId.substring(1) + "-" + revision;
            } else {
                myCopyId = FSTransactionRoot.addKeys(startCopyId, copyId.substring(1));
            }
        } else {
            myCopyId = copyId;
        }

        if (revNode.getCopyRootRevision() == SVNRepository.INVALID_REVISION) {
            revNode.setCopyRootRevision(revision);
        }

        newId = FSID.createRevId(myNodeId, myCopyId, revision, myOffset);
        revNode.setId(newId);
        getOwner().writeTxnNodeRevision(protoFile, revNode);
        if (representations != null && revNode.getTextRepresentation() != null && revNode.getType() == SVNNodeKind.FILE && 
                revNode.getTextRepresentation().getRevision() == revision) {
            representations.add(revNode.getTextRepresentation());
        }
        revNode.setIsFreshTxnRoot(false);
        getOwner().putTxnRevisionNode(id, revNode);
        return newId;
    }

    public FSRevisionNode cloneChild(FSRevisionNode parent, String parentPath, String childName, String copyId, boolean isParentCopyRoot) throws SVNException {
        if (!parent.getId().isTxn()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_MUTABLE, "Attempted to clone child of non-mutable node");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (!SVNPathUtil.isSinglePathComponent(childName)) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_SINGLE_PATH_COMPONENT, "Attempted to make a child clone with an illegal name ''{0}''", childName);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        FSRevisionNode childNode = parent.getChildDirNode(childName, getOwner());
        FSID newNodeId = null;

        if (childNode.getId().isTxn()) {
            newNodeId = childNode.getId();
        } else {
            if (isParentCopyRoot) {
                childNode.setCopyRootPath(parent.getCopyRootPath());
                childNode.setCopyRootRevision(parent.getCopyRootRevision());
            }
            childNode.setCopyFromPath(null);
            childNode.setCopyFromRevision(SVNRepository.INVALID_REVISION);
            childNode.setPredecessorId(childNode.getId());
            if (childNode.getCount() != -1) {
                childNode.setCount(childNode.getCount() + 1);
            }
            childNode.setCreatedPath(SVNPathUtil.getAbsolutePath(SVNPathUtil.append(parentPath, childName)));
            newNodeId = createSuccessor(childNode.getId(), childNode, copyId);
            setEntry(parent, childName, newNodeId, childNode.getType());
        }
        return getOwner().getRevisionNode(newNodeId);
    }

    public File getTransactionRevNodePropsFile(FSID id) {
        return new File(getOwner().getTransactionDir(id.getTxnID()), FSFS.PATH_PREFIX_NODE + id.getNodeID() + "." + id.getCopyID() + FSFS.TXN_PATH_EXT_PROPS);
    }

    public File getTransactionRevNodeChildrenFile(FSID id) {
        return new File(getOwner().getTransactionDir(id.getTxnID()), FSFS.PATH_PREFIX_NODE + id.getNodeID() + "." + id.getCopyID() + FSFS.TXN_PATH_EXT_CHILDREN);
    }

    public File getTransactionProtoRevFile() {
        if (myTxnRevFile == null) {
            myTxnRevFile = getOwner().getTransactionProtoRevFile(myTxnID);
        }
        return myTxnRevFile;
    }

    public File getTransactionChangesFile() {
        if (myTxnChangesFile == null) {
            myTxnChangesFile = new File(getOwner().getTransactionDir(myTxnID), "changes");
        }
        return myTxnChangesFile;
    }

    public String getNewTxnNodeId() throws SVNException {
        String[] curIds = readNextIDs();
        String curNodeId = curIds[0];
        String curCopyId = curIds[1];
        String nextNodeId = FSRepositoryUtil.generateNextKey(curNodeId);
        getOwner().writeNextIDs(getTxnID(), nextNodeId, curCopyId);
        return "_" + curNodeId;
    }

    private long writeHashRepresentation(SVNProperties hashContents, OutputStream protoFile, MessageDigest digest) throws IOException, SVNException {
        HashRepresentationStream targetFile = new HashRepresentationStream(protoFile, digest);
        String header = FSRepresentation.REP_PLAIN + "\n";
        protoFile.write(header.getBytes("UTF-8"));
        SVNWCProperties.setProperties(hashContents, targetFile, SVNWCProperties.SVN_HASH_TERMINATOR);
        String trailer = FSRepresentation.REP_TRAILER + "\n";
        protoFile.write(trailer.getBytes("UTF-8"));
        return targetFile.mySize;
    }

    private static String addKeys(String key1, String key2) {
        int i1 = key1.length() - 1;
        int i2 = key2.length() - 1;
        int carry = 0;
        int val;
        StringBuffer result = new StringBuffer();

        while (i1 >= 0 || i2 >= 0 || carry > 0) {
            val = carry;

            if (i1 >= 0) {
                val += key1.charAt(i1) <= '9' ? key1.charAt(i1) - '0' : key1.charAt(i1) - 'a' + 10;
            }

            if (i2 >= 0) {
                val += key2.charAt(i2) <= '9' ? key2.charAt(i2) - '0' : key2.charAt(i2) - 'a' + 10;
            }

            carry = val / 36;
            val = val % 36;

            char sym = val <= 9 ? (char) ('0' + val) : (char) (val - 10 + 'a');
            result.append(sym);

            if (i1 >= 0) {
                --i1;
            }

            if (i2 >= 0) {
                --i2;
            }
        }

        return result.reverse().toString();
    }

    private static class HashRepresentationStream extends OutputStream {

        long mySize;
        MessageDigest myChecksum;
        OutputStream myProtoFile;

        public HashRepresentationStream(OutputStream protoFile, MessageDigest digest) {
            super();
            mySize = 0;
            myChecksum = digest;
            myProtoFile = protoFile;
        }

        public void write(int b) throws IOException {
            myProtoFile.write(b);
            if (myChecksum != null) {
                myChecksum.update((byte) b);
            }
            mySize++;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            myProtoFile.write(b, off, len);
            if (myChecksum != null) {
                myChecksum.update(b, off, len);
            }
            mySize += len;
        }

        public void write(byte[] b) throws IOException {
            myProtoFile.write(b);
            if (myChecksum != null) {
                myChecksum.update(b);
            }
            mySize += b.length;
        }
    }
}
