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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.delta.SVNDeltaCombiner;
import org.tmatesoft.svn.core.internal.delta.SVNDeltaCompression;
import org.tmatesoft.svn.core.internal.io.fs.index.FSLogicalAddressingIndex;
import org.tmatesoft.svn.core.internal.io.fs.index.FSP2LEntry;
import org.tmatesoft.svn.core.internal.io.fs.index.FSP2LProtoIndex;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileType;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.ISVNDeltaConsumer;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSOutputStream extends OutputStream implements ISVNDeltaConsumer {

    public static final int SVN_DELTA_WINDOW_SIZE = 102400;
    public static final int WRITE_BUFFER_SIZE = 2*SVN_DELTA_WINDOW_SIZE;

    private boolean isHeaderWritten;
    private CountingOutputStream myTargetFileOS;
    private File myTargetFile;
    private long myDeltaStart;
    private long myRepSize;
    private long myRepOffset;
    private InputStream mySourceStream;
    private SVNDeltaGenerator myDeltaGenerator;
    private FSRevisionNode myRevNode;
    private MessageDigest myMD5Digest;
    private MessageDigest mySHA1Digest;
    private FSTransactionRoot myTxnRoot;
    private long mySourceOffset;
    private ByteArrayOutputStream myTextBuffer;
    private boolean myIsClosed;
    private SVNDeltaCompression myDeltaCompression;
    private FSWriteLock myTxnLock;

    private FSOutputStream(FSRevisionNode revNode, CountingOutputStream targetFileOS, File targetFile, InputStream source, long deltaStart,
            long repSize, long repOffset, FSTransactionRoot txnRoot, SVNDeltaCompression compression, FSWriteLock txnLock) throws SVNException {
        myTxnRoot = txnRoot;
        myTargetFileOS = targetFileOS;
        myTargetFile = targetFile;
        mySourceStream = source;
        myDeltaStart = deltaStart;
        myRepSize = repSize;
        myRepOffset = repOffset;
        isHeaderWritten = false;
        myRevNode = revNode;
        mySourceOffset = 0;
        myIsClosed = false;
        myTxnLock = txnLock;
        myDeltaGenerator = new SVNDeltaGenerator(SVN_DELTA_WINDOW_SIZE);
        myTextBuffer = new ByteArrayOutputStream();

        try {
            myMD5Digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "MD5 implementation not found: {0}", nsae.getLocalizedMessage());
            SVNErrorManager.error(err, nsae, SVNLogType.FSFS);
        }
        try {
            mySHA1Digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException nsae) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, "SHA1 implementation not found: {0}", nsae.getLocalizedMessage());
            SVNErrorManager.error(err, nsae, SVNLogType.FSFS);
        }

        myDeltaCompression = compression;
    }

    private void reset(FSRevisionNode revNode, CountingOutputStream targetFileOS, File targetFile, InputStream source, long deltaStart,
            long repSize, long repOffset, FSTransactionRoot txnRoot, FSWriteLock txnLock) {
        myTxnRoot = txnRoot;
        myTargetFileOS = targetFileOS;
        myTargetFile = targetFile;
        mySourceStream = source;
        myDeltaStart = deltaStart;
        myRepSize = repSize;
        myRepOffset = repOffset;
        isHeaderWritten = false;
        myRevNode = revNode;
        mySourceOffset = 0;
        myIsClosed = false;
        myMD5Digest.reset();
        mySHA1Digest.reset();
        myTextBuffer.reset();
        myTxnLock = txnLock;
    }

    /**
     * @deprecated use {@link #createStream(FSRevisionNode, FSTransactionRoot, OutputStream, SVNDeltaCompression)} instead
     */
    @Deprecated
    public static OutputStream createStream(FSRevisionNode revNode, FSTransactionRoot txnRoot, OutputStream dstStream, boolean compress) throws SVNException {
        return createStream(revNode, txnRoot, dstStream, SVNDeltaCompression.fromLegacyCompress(compress));
    }

    /**
     * @since 1.10
     */
    public static OutputStream createStream(FSRevisionNode revNode, FSTransactionRoot txnRoot, OutputStream dstStream, SVNDeltaCompression compression) throws SVNException {
        if (revNode.getType() != SVNNodeKind.FILE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_FILE, "Attempted to set textual contents of a *non*-file node");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        if (!revNode.getId().isTxn()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.FS_NOT_MUTABLE, "Attempted to set textual contents of an immutable node");
            SVNErrorManager.error(err, SVNLogType.FSFS);
        }

        OutputStream targetOS = null;
        InputStream sourceStream = null;
        long offset = -1;
        long deltaStart = -1;
        FSWriteLock txnLock = null;
        try {
            txnLock = FSWriteLock.getWriteLockForTxn(txnRoot.getTxnID(), txnRoot.getOwner());
            txnLock.lock();

            File targetFile = txnRoot.getWritableTransactionProtoRevFile();
            offset = targetFile.length();
            targetOS = SVNFileUtil.openFileForWriting(targetFile, true);
            CountingOutputStream revWriter = new CountingOutputStream(targetOS, offset);

            FSRepresentation baseRep = revNode.chooseDeltaBase(txnRoot.getOwner());
            sourceStream = FSInputStream.createDeltaStream(new SVNDeltaCombiner(), baseRep, txnRoot.getOwner());
            String header;

            if (baseRep != null) {
                header = FSRepresentation.REP_DELTA + " " + baseRep.getRevision() + " " + baseRep.getItemIndex() + " " + baseRep.getSize() + "\n";
            } else {
                header = FSRepresentation.REP_DELTA + "\n";
            }

            revWriter.write(header.getBytes("UTF-8"));
            deltaStart = revWriter.getPosition();

            if (dstStream instanceof FSOutputStream) {
                FSOutputStream fsOS = (FSOutputStream) dstStream;
                fsOS.reset(revNode, revWriter, targetFile, sourceStream, deltaStart, 0, offset, txnRoot, txnLock);
                return dstStream;
            }

            return new FSOutputStream(revNode, revWriter, targetFile, sourceStream, deltaStart, 0, offset, txnRoot,
                    compression, txnLock);

        } catch (IOException ioe) {
            SVNFileUtil.closeFile(targetOS);
            SVNFileUtil.closeFile(sourceStream);
            txnLock.unlock();
            FSWriteLock.release(txnLock);
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        } catch (SVNException svne) {
            if (txnLock != null) {
                txnLock.unlock();
                FSWriteLock.release(txnLock);
            }
            SVNFileUtil.closeFile(targetOS);
            SVNFileUtil.closeFile(sourceStream);
            throw svne;
        }
        return null;
    }

    public void write(int b) throws IOException {
        write(new byte[]{
                (byte) (b & 0xFF)
        }, 0, 1);
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        myMD5Digest.update(b, off, len);
        mySHA1Digest.update(b, off, len);
        myRepSize += len;
        int toWrite = 0;
        while (len > 0) {
            toWrite = len;
            myTextBuffer.write(b, off, toWrite);
            if (myTextBuffer.size() >= WRITE_BUFFER_SIZE) {
                try {
                    ByteArrayInputStream target = new ByteArrayInputStream(myTextBuffer.toByteArray());
                    myDeltaGenerator.sendDelta(null, mySourceStream, mySourceOffset, target, this, false);
                } catch (SVNException svne) {
                    throw new IOException(svne.getMessage());
                }
                myTextBuffer.reset();
            }
            off += toWrite;
            len -= toWrite;
        }
    }

    public void close() throws IOException {
        if (myIsClosed) {
            return;
        }
        boolean truncateToSize = false;
        myIsClosed = true;
        try {
            ByteArrayInputStream target = new ByteArrayInputStream(myTextBuffer.toByteArray());
            myDeltaGenerator.sendDelta(null, mySourceStream, mySourceOffset, target, this, false);

            final FSRepresentation rep = new FSRepresentation();
            rep.setItemIndex(myRepOffset);

            long offset = myTargetFileOS.getPosition();

            rep.setSize(offset - myDeltaStart);
            rep.setExpandedSize(myRepSize);
            rep.setTxnId(myRevNode.getId().getTxnID());
            String uniqueSuffix = myTxnRoot.getNewTxnNodeId();
            String uniquifier = rep.getTxnId() + '/' + uniqueSuffix;
            rep.setUniquifier(uniquifier);
            rep.setRevision(SVNRepository.INVALID_REVISION);

            rep.setMD5HexDigest(SVNFileUtil.toHexDigest(myMD5Digest));
            rep.setSHA1HexDigest(SVNFileUtil.toHexDigest(mySHA1Digest));

            FSFS fsfs = myTxnRoot.getOwner();
            FSRepresentation oldRep = getSharedRepresentation(fsfs, rep, null);

            if (oldRep != null) {
                myRevNode.setTextRepresentation(oldRep);
                truncateToSize = true;
            } else {
                if (fsfs.isUseLogAddressing()) {
                    rep.setItemIndex(myTxnRoot.allocateItemIndex(myRepOffset));
                }

                myTargetFileOS.write("ENDREP\n".getBytes("UTF-8"));
                myRevNode.setTextRepresentation(rep);
            }

            myRevNode.setIsFreshTxnRoot(false);
            fsfs.putTxnRevisionNode(myRevNode.getId(), myRevNode);

            if (oldRep == null && fsfs.isUseLogAddressing()) {
                final int checksum = myTargetFileOS.finalizeChecksum();
                storeSha1RepMapping(fsfs, myRevNode.getTextRepresentation()); //store_sha1_rep_mapping
                final FSP2LEntry entry = new FSP2LEntry(myRepOffset, myTargetFileOS.getPosition() - myRepOffset, FSP2LProtoIndex.ItemType.FILE_REP, checksum, SVNRepository.INVALID_REVISION, rep.getItemIndex());
                myTxnRoot.storeP2LIndexEntry(entry);
            }
        } catch (SVNException svne) {
            throw new IOException(svne.getMessage());
        } finally {
            closeStreams();
            try {
                if (truncateToSize) {
                    SVNFileUtil.truncate(myTargetFile, myRepOffset);
                }
            } catch (IOException e) {
                SVNDebugLog.getDefaultLog().logError(SVNLogType.FSFS, e);
            }
            try {
                myTxnLock.unlock();
            } catch (SVNException e) {
                //
            }
            FSWriteLock.release(myTxnLock);
        }
    }

    public void closeStreams() throws IOException {
        SVNFileUtil.closeFile(myTargetFileOS);
        SVNFileUtil.closeFile(mySourceStream);
    }

    public FSRevisionNode getRevisionNode() {
        return myRevNode;
    }

    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        mySourceOffset += diffWindow.getSourceViewLength();
        try {
            diffWindow.writeTo(myTargetFileOS, !isHeaderWritten, myDeltaCompression);
            isHeaderWritten = true;
        } catch (IOException ioe) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, ioe.getLocalizedMessage());
            SVNErrorManager.error(err, ioe, SVNLogType.FSFS);
        }
        return SVNFileUtil.DUMMY_OUT;
    }

    public void textDeltaEnd(String path) throws SVNException {
    }

    public void applyTextDelta(String path, String baseChecksum) throws SVNException {
    }

    private FSRepresentation getSharedRepresentation(FSFS fsfs,  final FSRepresentation representation, Map<String, FSRepresentation> representationsMap) throws SVNException {
        if (!fsfs.isRepSharingAllowed()) {
            return null;
        }
        FSRepresentation oldRepresentation = null;
        if (representationsMap != null) {
            oldRepresentation = representationsMap.get(representation.getSHA1HexDigest());
        }
        if (oldRepresentation == null) {
            final IFSRepresentationCacheManager reposCacheManager = fsfs.getRepositoryCacheManager();
            if (reposCacheManager != null) {
                try {
                    reposCacheManager.runReadTransaction(new IFSSqlJetTransaction() {
                        public void run() throws SVNException {
                            final FSRepresentation oldRep = reposCacheManager.getRepresentationByHash(representation.getSHA1HexDigest());
                            if (oldRep != null) {
                                oldRep.setUniquifier(representation.getUniquifier());
                                oldRep.setMD5HexDigest(representation.getMD5HexDigest());
//                                myRevNode.setTextRepresentation(oldRep);
                            }
                        }
                    });
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.FS_CORRUPT || e.getErrorMessage().getErrorCode().getCategory() == SVNErrorCode.MALFUNC_CATEGORY) {
                        throw e;
                    }
                    // explicitly ignore.
                    SVNDebugLog.getDefaultLog().logFiner(SVNLogType.FSFS, e);
                }
                if (oldRepresentation != null) {
                    checkRepresentation(myTxnRoot.getOwner(), oldRepresentation, null);
                }
            }

        }
        if (oldRepresentation == null && representation.isTxn()) {
            File file = pathTxnSha1(fsfs, representation, representation.getTxnId());
            SVNFileType fileType = SVNFileType.getType(file);
            if (fileType == SVNFileType.FILE) {
                String representationString = SVNFileUtil.readFile(file);
                oldRepresentation = FSRepresentation.parse(representationString);
            }
        }
        if (oldRepresentation == null) {
            return null;
        }
        if (oldRepresentation.getExpandedSize() != representation.getExpandedSize() ||
                (representation.getExpandedSize() == 0 && oldRepresentation.getSize() != representation.getSize())) {
            oldRepresentation = null;
        } else {
            oldRepresentation.setMD5HexDigest(representation.getMD5HexDigest());
            oldRepresentation.setUniquifier(representation.getUniquifier());
        }
        return oldRepresentation;
    }

    private void checkRepresentation(FSFS fsfs, FSRepresentation representation, Object hint) throws SVNException {
        if (fsfs.isUseLogAddressing()) {
            final long startRevision = fsfs.getPackedBaseRevision(representation.getRevision());
            if (hint != null) {
                //TODO: this can speedup algorithm somehow
            }
            FSFile revFile = null;
            if (revFile == null || true) {
                revFile = fsfs.getPackOrRevisionFSFile(representation.getRevision());
            }
            hint = revFile;//TODO: this can speedup algorithm somehow

            final long offset = fsfs.lookupOffsetInIndex(revFile, representation.getRevision(), representation.getItemIndex());

            final FSP2LEntry entry = lookupP2LEntry(revFile, representation.getRevision(), offset);

            if (entry == null ||
                    entry.getType().getCode() < FSP2LProtoIndex.ItemType.FILE_REP.getCode() ||
                    entry.getType().getCode() > FSP2LProtoIndex.ItemType.DIR_PROPS.getCode()) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "No representation found at offset {0} for item %s in revision {1}", new Object[]{offset, representation.getItemIndex()});
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            }
        } else {
            //TODO createRepresentationState(); this assigns the "hint"
        }
    }

    private FSP2LEntry lookupP2LEntry(FSFile revFile, long revision, long offset) throws SVNException {
        boolean isCached = false;

        //TODO: cache!

        if (!isCached) {
            FSLogicalAddressingIndex index = new FSLogicalAddressingIndex(myTxnRoot.getOwner(), revFile);
            List<FSP2LEntry> entries = index.lookupP2LEntries(revision, offset, offset + 1);
            return lookupEntry(entries, offset, null);
        }
        return null;
    }

    private FSP2LEntry lookupEntry(List<FSP2LEntry> entries, long offset, Object hint) {
        if (hint != null) {
            //TODO: this can speedup algorithm somehow
        }
        int index = FSLogicalAddressingIndex.searchLowerBound(entries, offset);
        if (hint != null) {
            //TODO: this can speedup algorithm somehow
        }
        final FSP2LEntry entry = entries.get(index);
        return (FSLogicalAddressingIndex.compareEntryOffset(entry, offset) != 0) ? null : entry;
    }

    private static void storeSha1RepMapping(FSFS fsfs, FSRepresentation representation) throws SVNException {
        if (fsfs.isRepSharingAllowed() && representation != null && representation.getSHA1HexDigest() != null) {
            final File fileName = pathTxnSha1(fsfs, representation, representation.getTxnId());
            final String stringRepresentation = representation.getStringRepresentation(fsfs.getDBFormat());
            SVNFileUtil.writeToFile(fileName, stringRepresentation, "UTF-8");
        }
    }

    private static File pathTxnSha1(FSFS fsfs, FSRepresentation representation, String txnId) {
        final String checksum = representation.getSHA1HexDigest();
        final File transactionDirectory = fsfs.getTransactionDir(txnId);
        return SVNFileUtil.createFilePath(transactionDirectory, checksum);
    }
}
