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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.delta.SVNDeltaCombiner;
import org.tmatesoft.svn.core.internal.io.fs.index.*;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.util.SVNSpillBuffer;
import org.tmatesoft.svn.core.internal.util.SVNSpillBufferInputStream;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc.admin.SVNChecksumOutputStream;
import org.tmatesoft.svn.core.internal.wc.admin.SVNTranslator;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public abstract class FSRoot {

    private RevisionCache myRevNodesCache;
    private FSFS myFSFS;
    protected FSRevisionNode myRootRevisionNode;

    protected FSRoot(FSFS owner) {
        myFSFS = owner;
    }

    public FSFS getOwner() {
        return myFSFS;
    }

    public FSRevisionNode getRevisionNode(String path) throws SVNException {
        path = SVNPathUtil.canonicalizeAbsolutePath(path);
        FSRevisionNode node = fetchRevNodeFromCache(path);
        if (node == null) {
            FSParentPath parentPath = openPath(path, true, false);
            node = parentPath.getRevNode();
        }
        return node;
    }

    public abstract long getRevision();

    public abstract FSRevisionNode getRootRevisionNode() throws SVNException;

    public abstract Map getChangedPaths() throws SVNException;

    public abstract FSCopyInheritance getCopyInheritance(FSParentPath child) throws SVNException;

    public FSParentPath openPath(String path, boolean lastEntryMustExist, boolean storeParents) throws SVNException {
        if (path == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FOUND, "null path is not supported");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        
        String canonPath = SVNPathUtil.canonicalizeAbsolutePath(path);
        FSRevisionNode here = getRootRevisionNode();
        String pathSoFar = "/";

        FSParentPath parentPath = new FSParentPath(here, null, null);
        parentPath.setCopyStyle(FSCopyInheritance.COPY_ID_INHERIT_SELF);

        // skip the leading '/'
        String rest = canonPath.substring(1);

        while (true) {
            String entry = SVNPathUtil.head(rest);
            String next = SVNPathUtil.removeHead(rest);
            pathSoFar = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(pathSoFar, entry));
            FSRevisionNode child = null;
            if (entry == null || "".equals(entry)) {
                child = here;
            } else {
                FSRevisionNode cachedRevNode = fetchRevNodeFromCache(pathSoFar);
                if (cachedRevNode != null) {
                    child = cachedRevNode;
                } else {
                    try {
                        child = here.getChildDirNode(entry, getOwner());
                    } catch (SVNException svne) {
                        if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND) {
                            if (!lastEntryMustExist && (next == null || "".equals(next))) {
                                return new FSParentPath(null, entry, parentPath);
                            }
                            SVNErrorManager.error(FSErrors.errorNotFound(this, path), svne, SVNLogType.FSFS);
                        }
                        throw svne;
                    }
                }

                parentPath.setParentPath(child, entry, storeParents ? new FSParentPath(parentPath) : null);

                if (storeParents) {
                    FSCopyInheritance copyInheritance = getCopyInheritance(parentPath);
                    if (copyInheritance != null) {
                        parentPath.setCopyStyle(copyInheritance.getStyle());
                        parentPath.setCopySourcePath(copyInheritance.getCopySourcePath());
                    }
                }

                if (cachedRevNode == null) {
                    putRevNodeToCache(pathSoFar, child);
                }
            }
            if (next == null || "".equals(next)) {
                break;
            }

            if (child.getType() != SVNNodeKind.DIR) {
                SVNErrorMessage err = FSErrors.errorNotDirectory(pathSoFar, getOwner());
                SVNErrorManager.error(err.wrap("Failure opening ''{0}''", path), SVNLogType.FSFS);
            }
            rest = next;
            here = child;
        }
        return parentPath;
    }

    public SVNNodeKind checkNodeKind(String path) throws SVNException {
        FSRevisionNode revNode = null;
        try {
            revNode = getRevisionNode(path);
        } catch (SVNException svne) {
            if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_FOUND ||
                    svne.getErrorMessage().getErrorCode() == SVNErrorCode.FS_NOT_DIRECTORY) {
                return SVNNodeKind.NONE;
            }
            throw svne;
        }
        return revNode.getType();
    }

    public void putRevNodeToCache(String path, FSRevisionNode node) throws SVNException {
        if (!path.startsWith("/")) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "Invalid path ''{0}''", path);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        if (myRevNodesCache == null) {
            myRevNodesCache = new RevisionCache(100);
        }
        myRevNodesCache.put(path, node);
    }

    public void removeRevNodeFromCache(String path) throws SVNException {
        if (!path.startsWith("/")) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "Invalid path ''{0}''", path);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        if (myRevNodesCache == null) {
            return;
        }
        myRevNodesCache.delete(path);
    }

    protected FSRevisionNode fetchRevNodeFromCache(String path) throws SVNException {
        if (myRevNodesCache == null) {
            return null;
        }
        if (!path.startsWith("/")) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNKNOWN, "Invalid path ''{0}''", path);
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }
        return (FSRevisionNode) myRevNodesCache.fetch(path);
    }

    private void foldChange(Map mapChanges, FSPathChange change) throws SVNException {
        if (change == null) {
            return;
        }
        mapChanges = mapChanges != null ? mapChanges : new SVNHashMap();
        FSPathChange newChange = null;
        String copyfromPath = null;
        long copyfromRevision = SVNRepository.INVALID_REVISION;

        FSPathChange oldChange = (FSPathChange) mapChanges.get(change.getPath());
        if (oldChange != null) {
            copyfromPath = oldChange.getCopyPath();
            copyfromRevision = oldChange.getCopyRevision();

            if ((change.getRevNodeId() == null) && (FSPathChangeKind.FS_PATH_CHANGE_RESET != change.getChangeKind())) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Missing required node revision ID");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            if ((change.getRevNodeId() != null) && (!oldChange.getRevNodeId().equals(change.getRevNodeId())) && (oldChange.getChangeKind() != FSPathChangeKind.FS_PATH_CHANGE_DELETE)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Invalid change ordering: new node revision ID without delete");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            if (FSPathChangeKind.FS_PATH_CHANGE_DELETE == oldChange.getChangeKind()
                    && !(FSPathChangeKind.FS_PATH_CHANGE_REPLACE == change.getChangeKind() || FSPathChangeKind.FS_PATH_CHANGE_RESET == change.getChangeKind() || FSPathChangeKind.FS_PATH_CHANGE_ADD == change
                            .getChangeKind())) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Invalid change ordering: non-add change on deleted path");
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
            if (FSPathChangeKind.FS_PATH_CHANGE_MODIFY == change.getChangeKind()) {
                if (change.isTextModified()) {
                    oldChange.setTextModified(true);
                }
                if (change.arePropertiesModified()) {
                    oldChange.setPropertiesModified(true);
                }
            } else if (FSPathChangeKind.FS_PATH_CHANGE_ADD == change.getChangeKind() || FSPathChangeKind.FS_PATH_CHANGE_REPLACE == change.getChangeKind()) {
                oldChange.setChangeKind(FSPathChangeKind.FS_PATH_CHANGE_REPLACE);
                oldChange.setRevNodeId(change.getRevNodeId());
                oldChange.setTextModified(change.isTextModified());
                oldChange.setPropertiesModified(change.arePropertiesModified());
                if (change.getCopyPath() != null) {
                    copyfromPath = change.getCopyPath();
                    copyfromRevision = change.getCopyRevision();
                }
            } else if (FSPathChangeKind.FS_PATH_CHANGE_DELETE == change.getChangeKind()) {
                if (FSPathChangeKind.FS_PATH_CHANGE_ADD == oldChange.getChangeKind()) {
                    oldChange = null;
                    mapChanges.remove(change.getPath());
                } else {
                    oldChange.setChangeKind(FSPathChangeKind.FS_PATH_CHANGE_DELETE);
                    oldChange.setPropertiesModified(change.arePropertiesModified());
                    oldChange.setTextModified(change.isTextModified());
                }

                copyfromPath = null;
                copyfromRevision = SVNRepository.INVALID_REVISION;

            } else if (FSPathChangeKind.FS_PATH_CHANGE_RESET == change.getChangeKind()) {
                oldChange = null;
                copyfromPath = null;
                copyfromRevision = SVNRepository.INVALID_REVISION;
                mapChanges.remove(change.getPath());
            }

            newChange = oldChange;
        } else {
            copyfromPath = change.getCopyPath();
            copyfromRevision = change.getCopyRevision();
            newChange = change;
        }

        if (newChange != null) {
            newChange.setCopyPath(copyfromPath);
            newChange.setCopyRevision(copyfromRevision);

            final SVNNodeKind nodeKind = change.getKind();
            if (nodeKind != null && nodeKind != SVNNodeKind.UNKNOWN) {
                newChange.setNodeKind(nodeKind);
            }
            mapChanges.put(change.getPath(), newChange);
        }
    }

    protected Map fetchAllChanges(FSFile changesFile, boolean prefolded) throws SVNException {
        Map changedPaths = new SVNHashMap();
        FSPathChange change = readChange(changesFile);
        while (change != null) {
            foldChange(changedPaths, change);
            if ((FSPathChangeKind.FS_PATH_CHANGE_DELETE == change.getChangeKind() || FSPathChangeKind.FS_PATH_CHANGE_REPLACE == change.getChangeKind()) && !prefolded) {
                for (Iterator curIter = changedPaths.keySet().iterator(); curIter.hasNext();) {
                    String hashKeyPath = (String) curIter.next();
                    if (change.getPath().equals(hashKeyPath)) {
                        continue;
                    }
                    if (SVNPathUtil.getPathAsChild(change.getPath(), hashKeyPath) != null) {
                        curIter.remove();
                    }
                }
            }
            change = readChange(changesFile);
        }
        return changedPaths;
    }

    public Map detectChanged() throws SVNException {
        Map changes = getChangedPaths();
        if (changes.size() == 0) {
            return changes;
        }

        for (Iterator paths = changes.keySet().iterator(); paths.hasNext();) {
            String changedPath = (String) paths.next();
            FSPathChange change = (FSPathChange) changes.get(changedPath);
            if (change.getChangeKind() == FSPathChangeKind.FS_PATH_CHANGE_RESET) {
                paths.remove();
            }
        }
        return changes;
    }

    private FSPathChange readChange(FSFile raReader) throws SVNException {
        String changeLine = null;
        try {
            changeLine = raReader.readLine(4096);
        } catch (SVNException svne) {
            if (svne.getErrorMessage().getErrorCode() == SVNErrorCode.STREAM_UNEXPECTED_EOF) {
                return null;
            }
            throw svne;
        }
        if (changeLine == null || changeLine.length() == 0) {
            return null;
        }
        String copyfromLine = raReader.readLine(4096);
        return FSPathChange.fromString(changeLine, copyfromLine);
    }

    public InputStream getFileStreamForPath(SVNDeltaCombiner combiner, String path) throws SVNException {
        FSRevisionNode fileNode = getRevisionNode(path);
        return FSInputStream.createDeltaStream(combiner, fileNode, getOwner());
    }

    public long getFileSize(String path) throws SVNException {
        FSRevisionNode fileNode = getRevisionNode(path);
        return fileNode.getFileLength();
    }

    public boolean isUseLogAddressing() {
        return getOwner().isUseLogAddressing();
    }

    public void writeIndexData(CountingOutputStream protoFile, long newRevision, String txnId) throws SVNException {
        final long l2pOffset = protoFile.getPosition();
        final String l2pChecksum = writeL2PIndex(protoFile, newRevision, txnId);
        final long p2lOffset = protoFile.getPosition();
        final String p2lChecksum = writeP2LIndex(protoFile, newRevision, txnId);

        final StringBuilder footerBuilder = new StringBuilder();
        footerBuilder.append(l2pOffset);
        footerBuilder.append(' ');
        footerBuilder.append(l2pChecksum);
        footerBuilder.append(' ');
        footerBuilder.append(p2lOffset);
        footerBuilder.append(' ');
        footerBuilder.append(p2lChecksum);
        final String footerString = footerBuilder.toString();

        try {
            final byte[] footer = footerString.getBytes("UTF-8");
            protoFile.write(footer);
            protoFile.write(footer.length);
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
    }

    private String writeL2PIndex(CountingOutputStream protoFile, long revision, String txnId) throws SVNException {
        SVNSpillBuffer spillBuffer = null;
        final List<Long> pageCounts = new ArrayList<Long>(16);
        final List<Long> pageSizes = new ArrayList<Long>(16);
        final List<Integer> entryCounts = new ArrayList<Integer>(16);
        final List<Long> entries = new ArrayList<Long>(16);
        final byte[] buffer = new byte[10]; //just a buffer in order not to allocate it for every writeEncoded() call
        long lastPageCount = 0;

        final long l2pPageSize = myFSFS.getL2PPageSize();
        if (l2pPageSize > Integer.MAX_VALUE) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_OVERFLOW, "L2P index page size  {0} exceeds current limit of 2G entries", String.valueOf(l2pPageSize));
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        FSL2PProtoIndex index = null;
        try {
            index = FSL2PProtoIndex.open(myFSFS, txnId, false);
            assert index != null;

            spillBuffer = new SVNSpillBuffer(0x10000, 0x1000000);

            for (int entryIndex = 0; ; entryIndex++) {
                final FSL2PEntry entry = index.readEntry();

                if ((entry == null) || (entryIndex > 0 && entry.getOffset() == 0)) {
                    int entryCount = 0;
                    for (int i = 0; i < entries.size(); i += entryCount) {
                        long lastBufferSize = spillBuffer.getSize();
                        entryCount = (myFSFS.getL2PPageSize() < entries.size() - i) ? (int) myFSFS.getL2PPageSize() : (entries.size() - i);

                        encodeL2PPage(buffer, entries, i, i + entryCount, spillBuffer);

                        entryCounts.add(entryCount);
                        pageSizes.add(spillBuffer.getSize() - lastBufferSize);
                    }
                    pageCounts.add(pageSizes.size() - lastPageCount);
                    lastPageCount = pageSizes.size();
                } else {
                    if (entry.getItemIndex() > Integer.MAX_VALUE) {
                        final SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_OVERFLOW, "Item index %s too large " + "in l2p proto index for revision %ld", String.valueOf(entry.getItemIndex()), revision + pageCounts.size());
                        SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
                    }
                    final long idx = entry.getItemIndex();
                    while (idx >= entries.size()) {
                        entries.add((long) 0);
                    }
                    entries.set((int) idx, entry.getOffset());
                }
                if (entry == null) {
                    break;
                }
            }

            final SVNChecksumOutputStream checksumOutputStream = new SVNChecksumOutputStream(protoFile, SVNChecksumOutputStream.MD5_ALGORITHM, false);

            try {
                checksumOutputStream.write(FSLogicalAddressingIndex.L2P_STREAM_PREFIX.getBytes("UTF-8"));

                writeEncoded(checksumOutputStream, buffer, revision);
                writeEncoded(checksumOutputStream, buffer, myFSFS.getL2PPageSize());
                writeEncoded(checksumOutputStream, buffer, pageCounts.size());
                writeEncoded(checksumOutputStream, buffer, pageSizes.size());

                for (long pageCount : pageCounts) {
                    writeEncoded(checksumOutputStream, buffer, pageCount);
                }

                for (int i = 0, pageSizesSize = pageSizes.size(); i < pageSizesSize; i++) {
                    final Long pageSize = pageSizes.get(i);
                    writeEncoded(checksumOutputStream, buffer, pageSize);
                    final int entryCount = entryCounts.get(i);
                    writeEncoded(checksumOutputStream, buffer, entryCount);
                }

                //here we should copy spillBuffer to the stream (checksumOutputStream)
                final SVNSpillBufferInputStream inputStream = spillBuffer.createInputStream();
                try {
                    SVNTranslator.copy(inputStream, checksumOutputStream);

                    checksumOutputStream.flush();
                    checksumOutputStream.close();
                    return checksumOutputStream.getDigest();
                } finally {
                    SVNFileUtil.closeFile(inputStream);
                }
            } catch (IOException e) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            } finally {
                if (spillBuffer != null) {
                    spillBuffer.close();
                }
            }
        } finally {
            if (index != null) {
                index.close();
            }
        }
        return null;
    }

    private String writeP2LIndex(CountingOutputStream protoFile, long revision, String txnId) throws SVNException {
        SVNSpillBuffer spillBuffer = null;
        spillBuffer = new SVNSpillBuffer(0x10000, 0x1000000);
        final byte[] buffer = new byte[10]; //just a buffer in order not to allocate it for every writeEncoded() call

        FSP2LProtoIndex index = null;
        try {
            index = FSP2LProtoIndex.open(myFSFS, txnId, false);
            assert index != null;

            long fileSize = 0;
            long lastEntryEnd = 0;
            long pageSize = myFSFS.getP2LPageSize();
            long lastRevision = revision;
            long lastCompound = 0;
            long entryEnd;
            long lastPageEnd = 0;
            long lastBufferSize = 0;
            boolean eof = false;

            List<Long> tableSizes = new ArrayList<Long>();

            while (true) {
                boolean newPage = spillBuffer.getSize() == 0;
                FSP2LEntry entry = index.readEntry();
                long revDiff;
                long compound;
                long compoundDiff;

                if (entry == null) {
                    fileSize = lastEntryEnd;

                    final long entryOffset = lastEntryEnd;
                    final long entrySize = FSRepositoryUtil.align(entryOffset, pageSize) - entryOffset;
                    final FSP2LProtoIndex.ItemType entryType = FSP2LProtoIndex.ItemType.UNUSED;
                    final int entryChecksum = 0;
                    final long entryRevision = lastRevision;
                    final long entryNumber = 0;

                    entry = new FSP2LEntry(entryOffset, entrySize, entryType, entryChecksum, entryRevision, entryNumber);
                    eof = true;
                } else {
                    if (entry.getRevision() == SVNRepository.INVALID_REVISION) {
                        entry.setRevision(revision);
                    }
                }
                entryEnd = entry.getOffset() + entry.getSize();
                while (entryEnd - lastPageEnd > pageSize) {
                    long bufferSize = spillBuffer.getSize();
                    tableSizes.add(bufferSize - lastBufferSize);

                    lastBufferSize = bufferSize;
                    lastPageEnd += pageSize;
                    newPage = true;
                }

                if (newPage) {
                    spillBuffer.write(buffer, 0, FSRepositoryUtil.encodeUnsignedInt(buffer, entry.getOffset()));
                    lastRevision = revision;
                    lastCompound = 0;
                }
                spillBuffer.write(buffer, 0, FSRepositoryUtil.encodeUnsignedInt(buffer, entry.getSize()));

                revDiff = entry.getRevision() - lastRevision;
                lastRevision = entry.getRevision();

                compound = entry.getNumber() * 8 + entry.getType().getCode();
                compoundDiff = compound - lastCompound;
                lastCompound = compound;

                spillBuffer.write(buffer, 0, FSRepositoryUtil.encodeInt(buffer, compoundDiff));
                spillBuffer.write(buffer, 0, FSRepositoryUtil.encodeInt(buffer, revDiff));
                spillBuffer.write(buffer, 0, FSRepositoryUtil.encodeUnsignedInt(buffer, entry.getChecksum()));

                lastEntryEnd = entryEnd;
                if (eof) {
                    break;
                }
            }

            tableSizes.add(spillBuffer.getSize() - lastBufferSize);

            final SVNChecksumOutputStream checksumOutputStream = new SVNChecksumOutputStream(protoFile, SVNChecksumOutputStream.MD5_ALGORITHM, false);

            try {
                checksumOutputStream.write(FSLogicalAddressingIndex.P2L_STREAM_PREFIX.getBytes("UTF-8"));

                writeEncoded(checksumOutputStream, buffer, revision);
                writeEncoded(checksumOutputStream, buffer, fileSize);
                writeEncoded(checksumOutputStream, buffer, pageSize);
                writeEncoded(checksumOutputStream, buffer, tableSizes.size());

                for (Long tableSize : tableSizes) {
                    writeEncoded(checksumOutputStream, buffer, tableSize);
                }
                //here we should copy spillBuffer to the stream (checksumOutputStream)
                final SVNSpillBufferInputStream inputStream = spillBuffer.createInputStream();
                try {
                    SVNTranslator.copy(inputStream, checksumOutputStream);

                    checksumOutputStream.flush();
                    checksumOutputStream.close();
                    return checksumOutputStream.getDigest();
                } finally {
                    SVNFileUtil.closeFile(inputStream);
                }
            } catch (IOException e) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            } finally {
                if (spillBuffer != null) {
                    spillBuffer.close();
                }
            }
        } finally {
            if (index != null) {
                index.close();
            }
        }
        return null;
    }

    private void encodeL2PPage(byte[] encoded, List<Long> entries, int start, int end, SVNSpillBuffer spillBuffer) throws SVNException {
        long lastValue = 0;
        for (int i = start; i < end; i++) {
            long diff = entries.get(i) - lastValue;
            lastValue = entries.get(i);

            final int length = FSRepositoryUtil.encodeInt(encoded, diff);
            spillBuffer.write(encoded, 0, length);
        }
    }

    private static void writeEncoded(OutputStream outputStream, byte[] bytes, long value) throws IOException {
        final int length = FSRepositoryUtil.encodeUnsignedInt(bytes, value);
        outputStream.write(bytes, 0, length);
    }

    private static final class RevisionCache {

        private LinkedList myKeys;
        private Map myCache;
        private int mySizeLimit;

        public RevisionCache(int limit) {
            mySizeLimit = limit;
            myKeys = new LinkedList();
            myCache = new TreeMap();
        }

        public void put(Object key, Object value) {
            if (mySizeLimit <= 0) {
                return;
            }
            if (myKeys.size() == mySizeLimit) {
                Object cachedKey = myKeys.removeLast();
                myCache.remove(cachedKey);
            }
            myKeys.addFirst(key);
            myCache.put(key, value);
        }

        public void delete(Object key) {
            myKeys.remove(key);
            myCache.remove(key);
        }

        public Object fetch(Object key) {
            int ind = myKeys.indexOf(key);
            if (ind != -1) {
                if (ind != 0) {
                    Object cachedKey = myKeys.remove(ind);
                    myKeys.addFirst(cachedKey);
                }
                return myCache.get(key);
            }
            return null;
        }
    }

}
