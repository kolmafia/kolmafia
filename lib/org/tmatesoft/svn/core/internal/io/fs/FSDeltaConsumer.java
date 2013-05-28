/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.io.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.delta.SVNDeltaCombiner;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.ISVNDeltaConsumer;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class FSDeltaConsumer implements ISVNDeltaConsumer {

    private String myBasePath;
    private FSTransactionRoot myTxnRoot;
    private FSFS myFSFS;
    private FSCommitter myCommitter;
    private SVNDeltaProcessor myDeltaProcessor;
    private FSOutputStream myTargetStream;
    private String myAuthor;
    private Collection myLockTokens;
    private SVNDeltaCombiner myDeltaCombiner;
    private boolean myIsComputeChecksum;
    private String myComputedChecksum;

    public FSDeltaConsumer(String basePath, FSTransactionRoot txnRoot, FSFS fsfs, FSCommitter committer, String author, Collection lockTokens) {
        myBasePath = basePath;
        myTxnRoot = txnRoot;
        myFSFS = fsfs;
        myCommitter = committer;
        myAuthor = author;
        myLockTokens = lockTokens != null ? lockTokens : Collections.EMPTY_LIST;
    }

    public void applyTextDelta(String path, String baseChecksum) throws SVNException {
        String fullPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(myBasePath, path));
        FSParentPath parentPath = myTxnRoot.openPath(fullPath, true, true);

        if ((myTxnRoot.getTxnFlags() & FSTransactionRoot.SVN_FS_TXN_CHECK_LOCKS) != 0) {
            myCommitter.allowLockedOperation(myFSFS, fullPath, myAuthor, myLockTokens, false, false);
        }

        myCommitter.makePathMutable(parentPath, fullPath);
        FSRevisionNode node = parentPath.getRevNode();
        if (baseChecksum != null) {
            String md5HexChecksum = node.getFileMD5Checksum();
            if (md5HexChecksum != null && !md5HexChecksum.equals(baseChecksum)) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.CHECKSUM_MISMATCH, "Base checksum mismatch on ''{0}'':\n   expected:  {1}\n     actual:  {2}\n", new Object[] {
                        path, baseChecksum, md5HexChecksum
                });
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        }

        InputStream sourceStream = null;
        OutputStream targetStream = null;
        
        int dbFormat = myFSFS.getDBFormat();
        
        try {
            sourceStream = FSInputStream.createDeltaStream(getCombiner(), node, myFSFS);
            targetStream = FSOutputStream.createStream(node, myTxnRoot, myTargetStream, dbFormat >= 2);
            if (myDeltaProcessor == null) {
                myDeltaProcessor = new SVNDeltaProcessor();
            }
            myDeltaProcessor.applyTextDelta(sourceStream, targetStream, myIsComputeChecksum);
        } catch (SVNException svne) {
            SVNFileUtil.closeFile(sourceStream);
            throw svne;
        } finally {
            myTargetStream = (FSOutputStream) targetStream;
        }

        myCommitter.addChange(fullPath, node.getId(), FSPathChangeKind.FS_PATH_CHANGE_MODIFY, true, false, SVNRepository.INVALID_REVISION, null, SVNNodeKind.FILE);
    }

    public void applyText(String path) throws SVNException {
        String fullPath = SVNPathUtil.getAbsolutePath(SVNPathUtil.append(myBasePath, path));
        FSParentPath parentPath = myTxnRoot.openPath(fullPath, true, true);

        if ((myTxnRoot.getTxnFlags() & FSTransactionRoot.SVN_FS_TXN_CHECK_LOCKS) != 0) {
            myCommitter.allowLockedOperation(myFSFS, fullPath, myAuthor, myLockTokens, false, false);
        }

        myCommitter.makePathMutable(parentPath, fullPath);
        FSRevisionNode node = parentPath.getRevNode();

        InputStream sourceStream = null;
        OutputStream targetStream = null;
        
        int dbFormat = myFSFS.getDBFormat();
        
        try {
            sourceStream = SVNFileUtil.DUMMY_IN;
            targetStream = FSOutputStream.createStream(node, myTxnRoot, myTargetStream, dbFormat >= 2);
            if (myDeltaProcessor == null) {
                myDeltaProcessor = new SVNDeltaProcessor();
            }
            myDeltaProcessor.applyTextDelta(sourceStream, targetStream, false);
        } catch (SVNException svne) {
            throw svne;
        } finally {
            myTargetStream = (FSOutputStream) targetStream;
        }

        myCommitter.addChange(fullPath, node.getId(), FSPathChangeKind.FS_PATH_CHANGE_MODIFY, true, false, SVNRepository.INVALID_REVISION, null, SVNNodeKind.FILE);
    }
    
    public OutputStream textDeltaChunk(String path, SVNDiffWindow diffWindow) throws SVNException {
        return myDeltaProcessor.textDeltaChunk(diffWindow);
    }

    public void textDeltaEnd(String path) throws SVNException {
        myComputedChecksum = myDeltaProcessor.textDeltaEnd();
    }
    
    public String getChecksum() {
        return myComputedChecksum;
    }
    
    public void close() throws SVNException {
        abort();
    }

    public void abort() throws SVNException {
        if (myTargetStream != null) {
            try {
                myTargetStream.closeStreams(-1);
            } catch (IOException e) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
                SVNErrorManager.error(err, SVNLogType.FSFS);
            }
        }
    }
    
    public void setComputeChecksum(boolean computeChecksum) {
        myIsComputeChecksum = computeChecksum;
    }
    
    private SVNDeltaCombiner getCombiner() {
        if (myDeltaCombiner == null) {
            myDeltaCombiner = new SVNDeltaCombiner();
        } else {
            myDeltaCombiner.reset();
        }
        return myDeltaCombiner;
    }
}
