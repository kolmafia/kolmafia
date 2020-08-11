package org.tmatesoft.svn.core.internal.io.fs.index;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.fs.FSFS;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;

public class FSP2LProtoIndex {

    private static final long MAX_OFFSET = Integer.MAX_VALUE;
    private static final String FILENAME = "index.p2l";

    public static FSP2LProtoIndex open(FSFS fsfs, String txnId, boolean append) throws SVNException {
        try {
            final RandomAccessFile randomAccessFile = new RandomAccessFile(getIndexPath(fsfs, txnId), "rw");
            if (append) {
                randomAccessFile.seek(randomAccessFile.length());
            }
            return new FSP2LProtoIndex(randomAccessFile);
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

    public FSP2LProtoIndex(RandomAccessFile file) {
        this.file = file;
    }

    public void close() {
        try {
            file.close();
        } catch (IOException e) {
            SVNDebugLog.getDefaultLog().log(SVNLogType.FSFS, e, Level.INFO);
        }
    }

    public FSP2LEntry readEntry() throws SVNException {
        final long offset = readOffset();
        final long size = readOffset();
        final int type = readInt();
        final long fnv1Checksum = readLong();
        final long revision = readLong();
        final long itemNumber = readLong();

        final boolean eof = offset < 0 || size < 0 || type < 0 || fnv1Checksum < 0 || revision < 0 || itemNumber < 0;
        if (!eof) {
            if (revision > 0 && revision - 1 > Long.MAX_VALUE) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_OVERFLOW, "Revision 0x{0} too large, max = 0x{1}", Long.toHexString(revision), Long.toHexString(MAX_OFFSET));
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            }
            final long itemRevision = revision == 0 ? SVNRepository.INVALID_REVISION : (revision -1);
            return new FSP2LEntry(offset, size, ItemType.fromCode(type), (int)fnv1Checksum, itemRevision, itemNumber);
        }
        return null;
    }

    public void writeEntry(FSP2LEntry entry) throws SVNException {
        assert entry.getOffset() >= 0;
        assert entry.getSize() >= 0;
        assert (entry.getRevision() >= 0 || entry.getRevision() == SVNRepository.INVALID_REVISION);

        final long revision = entry.getRevision() == SVNRepository.INVALID_REVISION ? 0 : entry.getRevision() + 1;
        try {
            FSRepositoryUtil.writeLongLittleEndian(file, entry.getOffset());
            FSRepositoryUtil.writeLongLittleEndian(file, entry.getSize());
            FSRepositoryUtil.writeLongLittleEndian(file, entry.getType().getCode());
            FSRepositoryUtil.writeLongLittleEndian(file, (long)(entry.getChecksum() & 0x00000000ffffffffL));
            FSRepositoryUtil.writeLongLittleEndian(file, revision);
            FSRepositoryUtil.writeLongLittleEndian(file, entry.getNumber());
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
    }

    public long readNextOffset() throws SVNException {
        try {
            long offset = file.getFilePointer();
            if (offset == 0) {
                return 0;
            } else {
                offset -= FSP2LEntry.SIZE_IN_BYTES;
                file.seek(offset);
                final FSP2LEntry entry = readEntry();
                return entry.getOffset() + entry.getSize();
            }
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        return -1;
    }

    private int readInt() throws SVNException {
        final long value = readLong();

        if (value >= 0) {
            if (value > Integer.MAX_VALUE) {
                final SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_OVERFLOW, "UINT32 0x{0} too large, max = 0x{1}", Long.toHexString(value), Long.toHexString(MAX_OFFSET));
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            }
        }
        return (int) value;
    }

    private long readOffset() throws SVNException {
        final long offset = readLong();

        if (offset >= 0) {
            if (offset > MAX_OFFSET) {
                final SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_OVERFLOW, "File offset 0x{0} too large, max = 0x{1}", Long.toHexString(offset), Long.toHexString(MAX_OFFSET));
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            }
        }
        return offset;
    }

    private long readLong() throws SVNException {
        try {
            return FSRepositoryUtil.readLongLittleEndian(file);
        } catch (EOFException e) {
            return -1;
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        return -1;
    }

    public static enum ItemType {
        UNUSED(0), FILE_REP(1), DIR_REP(2), FILE_PROPS(3), DIR_PROPS(4), NODEREV(5), CHANGES(6);

        public static ItemType fromCode(int code) {
            final ItemType[] values = values();
            for (ItemType itemType : values) {
                if (itemType.getCode() == code) {
                    return itemType;
                }
            }
            return null;
        }

        private final int code;

        private ItemType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
