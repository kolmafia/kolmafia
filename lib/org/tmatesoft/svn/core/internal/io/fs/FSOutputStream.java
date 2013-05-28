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

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.delta.SVNDeltaCombiner;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
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
    private boolean myIsCompress;
    private FSWriteLock myTxnLock;

    private FSOutputStream(FSRevisionNode revNode, CountingOutputStream targetFileOS, File targetFile, InputStream source, long deltaStart, 
            long repSize, long repOffset, FSTransactionRoot txnRoot, boolean compress, FSWriteLock txnLock) throws SVNException {
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

        myIsCompress = compress;
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

    public static OutputStream createStream(FSRevisionNode revNode, FSTransactionRoot txnRoot, OutputStream dstStream, boolean compress) throws SVNException {
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
            
            File targetFile = txnRoot.getTransactionProtoRevFile();
            offset = targetFile.length();
            targetOS = SVNFileUtil.openFileForWriting(targetFile, true);
            CountingOutputStream revWriter = new CountingOutputStream(targetOS, offset);

            FSRepresentation baseRep = revNode.chooseDeltaBase(txnRoot.getOwner());
            sourceStream = FSInputStream.createDeltaStream(new SVNDeltaCombiner(), baseRep, txnRoot.getOwner());
            String header;

            if (baseRep != null) {
                header = FSRepresentation.REP_DELTA + " " + baseRep.getRevision() + " " + baseRep.getOffset() + " " + baseRep.getSize() + "\n";
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
                    compress, txnLock);

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
        write(new byte[] {
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
        myIsClosed = true;
        final long truncateToSize[] = new long[] {-1};
        try {
            ByteArrayInputStream target = new ByteArrayInputStream(myTextBuffer.toByteArray());
            myDeltaGenerator.sendDelta(null, mySourceStream, mySourceOffset, target, this, false);

            final FSRepresentation rep = new FSRepresentation();
            rep.setOffset(myRepOffset);

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
            final IFSRepresentationCacheManager reposCacheManager = fsfs.getRepositoryCacheManager();
            if (reposCacheManager != null) {
                try {
                    reposCacheManager.runReadTransaction(new IFSSqlJetTransaction() {
                        public void run() throws SVNException {
                            final FSRepresentation oldRep = reposCacheManager.getRepresentationByHash(rep.getSHA1HexDigest());
                            if (oldRep != null) {
                                oldRep.setUniquifier(rep.getUniquifier());
                                oldRep.setMD5HexDigest(rep.getMD5HexDigest());
                                truncateToSize[0] = myRepOffset;
                                myRevNode.setTextRepresentation(oldRep);
                            }
                        }
                    });
                } catch (SVNException e) {
                    // explicitly ignore.
                    SVNDebugLog.getDefaultLog().logError(SVNLogType.FSFS, e);
                }
            } 
            if (truncateToSize[0] < 0){
                myTargetFileOS.write("ENDREP\n".getBytes("UTF-8"));
                myRevNode.setTextRepresentation(rep);
            }
            myRevNode.setIsFreshTxnRoot(false);
            fsfs.putTxnRevisionNode(myRevNode.getId(), myRevNode);
        } catch (SVNException svne) {
            throw new IOException(svne.getMessage());
        } finally {
            closeStreams(truncateToSize[0]);
            try {
                myTxnLock.unlock();
            } catch (SVNException e) {
                //
            }
            FSWriteLock.release(myTxnLock);
        }
    }

    public void closeStreams(long truncateToSize) throws IOException {
        SVNFileUtil.closeFile(myTargetFileOS);
        SVNFileUtil.closeFile(mySourceStream);
        if (truncateToSize >= 0) {
            SVNFileUtil.truncate(myTargetFile, truncateToSize);
        }
    }

    public FSRevisionNode getRevisionNode() {
        return myRevNode;
    }

    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        mySourceOffset += diffWindow.getSourceViewLength();
        try {
            diffWindow.writeTo(myTargetFileOS, !isHeaderWritten, myIsCompress);
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
}
