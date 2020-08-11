package org.tmatesoft.svn.core.internal.io.fs.index;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.fs.FSFS;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.*;
import java.util.logging.Level;

public class FSL2PProtoIndex implements Closeable {

    private static final long MAX_OFFSET = Integer.MAX_VALUE;
    private static final String FILENAME = "index.l2p";

    public static FSL2PProtoIndex open(FSFS fsfs, String txnId, boolean append) throws SVNException {
        try {
            final RandomAccessFile randomAccessFile = new RandomAccessFile(getIndexPath(fsfs, txnId), "rw");
            if (append) {
                randomAccessFile.seek(randomAccessFile.length());
            }
            return new FSL2PProtoIndex(randomAccessFile);
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        return null;
    }

    public static File getIndexPath(FSFS fsfs, String txnId) {
        final File transactionDir = fsfs.getTransactionDir(txnId);
        return SVNFileUtil.createFilePath(transactionDir, FILENAME);
    }

    private final RandomAccessFile file;

    public FSL2PProtoIndex(RandomAccessFile file) {
        this.file = file;
    }

    public void close() {
        try {
            file.close();
        } catch (IOException e) {
            SVNDebugLog.getDefaultLog().log(SVNLogType.FSFS, e, Level.INFO);
        }
    }

    public long getOffsetByItemIndex(long itemIndex) throws SVNException {
        try {
            if (file.length() % 16 != 0) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION);
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            }
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        final long[] entryOffset = new long[1];
        final long[] entryItemIndex = new long[1];

        readEntry(entryOffset, entryItemIndex);

        if (entryItemIndex[0] == itemIndex) {
            return entryOffset[0] - 1;
        }
        return -1;
    }

    public FSL2PEntry readEntry() throws SVNException {
        final long[] entryOffset = new long[1];
        final long[] entryItemIndex = new long[1];

        readEntry(entryOffset, entryItemIndex);

        if (entryOffset[0] < 0 || entryItemIndex[0] < 0) {
            return null;
        }
        return new FSL2PEntry(entryOffset[0], entryItemIndex[0]);
    }

    private void readEntry(long[] entryOffset, long[] entryItemIndex) throws SVNException {
        try {
            entryOffset[0] = FSRepositoryUtil.readLongLittleEndian(file);
            entryItemIndex[0] = FSRepositoryUtil.readLongLittleEndian(file);
        } catch (EOFException e) {
            entryOffset[0] = -1;
            entryItemIndex[0] = -1;
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
    }

    public void addEntry(long offset, long itemIndex) throws SVNException {
        try {
            assert offset >= -1;

            FSRepositoryUtil.writeLongLittleEndian(file, offset + 1);
            FSRepositoryUtil.writeLongLittleEndian(file, itemIndex);
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
    }
}
