package org.tmatesoft.svn.core.internal.io.fs.index;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.fs.FSFS;
import org.tmatesoft.svn.core.internal.io.fs.FSID;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.*;
import java.util.Arrays;
import java.util.logging.Level;

public class FSTransactionItemIndex implements Closeable {

    private static final String FILENAME = "itemidx";

    public static FSTransactionItemIndex open(FSFS fsfs, String txnId) throws SVNException {
        try {
            return new FSTransactionItemIndex(new RandomAccessFile(getIndexPath(fsfs, txnId), "rw"));
        } catch (FileNotFoundException e) {
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

    private FSTransactionItemIndex(RandomAccessFile file) {
        this.file = file;
    }

    public void close() {
        try {
            file.close();
        } catch (IOException e) {
            SVNDebugLog.getDefaultLog().log(SVNLogType.FSFS, e, Level.INFO);
        }
    }

    public long allocateItemIndex(long offset) throws SVNException {
        final byte[] numberBytes = new byte[21];

        long itemIndex = 0;
        try {
            final int bytesRead = file.read(numberBytes);
            if (bytesRead < 0) {
                itemIndex = FSID.ITEM_FIRST_USER;
            } else {
                final int position = arrayIndexOf(numberBytes, (byte) 0);
                final String numberString;
                if (position >= 0) {
                    numberString = new String(numberBytes, 0, position);
                } else {
                    numberString = new String(numberBytes);
                }
                try {
                    itemIndex = Long.parseLong(numberString);
                } catch (NumberFormatException e) {
                    itemIndex = FSID.ITEM_FIRST_USER;
                }
            }
        } catch (EOFException e) {
            itemIndex = FSID.ITEM_FIRST_USER;
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        try {
            file.seek(0);
            final String incrementedItemIndexString = String.valueOf(itemIndex + 1);
            final byte[] incrementedItemIndexBytes = incrementedItemIndexString.getBytes();

            file.write(incrementedItemIndexBytes);
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        return itemIndex;
    }

    private static int arrayIndexOf(byte[] array, byte value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }
}
