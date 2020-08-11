package org.tmatesoft.svn.core.internal.util;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.Closeable;
import java.io.IOException;

public class SVNSpillBufferReader implements Closeable {

    private final SVNSpillBuffer buffer;

    private byte[] saveBuffer;
    private int savePointer;
    private int savePosition;
    private int saveLength;

    private byte[] sbBuffer;
    private int sbPointer;
    private int sbLength;

    public SVNSpillBufferReader(SVNSpillBuffer buffer) {
        this.buffer = buffer;
    }

    public int read(byte[] data, int offset, int length) throws IOException {
        if (length == 0) {
            throw new IllegalArgumentException();
        }

        int amt = 0;

        while (length > 0) {
            int copyAmt;

            if (saveLength > 0) {
                if (length < saveLength) {
                    copyAmt = length;
                } else {
                    copyAmt = saveLength;
                }
                System.arraycopy(saveBuffer, savePointer + savePosition, data, offset, copyAmt);
                savePointer += copyAmt;
                saveLength -= copyAmt;
            } else {
                if (sbLength == 0) {
                    final SVNSpillBuffer.MemoryBlock block = buffer.read();

                    if (block == null) {
                        sbBuffer = null;
                        sbPointer = 0;
                        sbLength = 0;

                        sbLength = 0;
                        return amt == 0 ? -1 : amt;
                    } else {
                        sbBuffer = block.data;
                        sbPointer = 0;
                        sbLength = block.length;
                    }
                }
                if (length < sbLength) {
                    copyAmt = length;
                } else {
                    copyAmt = sbLength;
                }
                System.arraycopy(sbBuffer, sbPointer, data, offset, copyAmt);
                sbPointer += copyAmt;
                sbLength -= copyAmt;
            }
            offset += copyAmt;
            length -= copyAmt;
            amt += copyAmt;
        }
        return amt == 0 ? -1 : amt;
    }

    public char readChar() throws SVNException {
        final byte[] bytes = new byte[1];
        try {
            final int read = read(bytes, 0, 1);
            if (read < 0) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.STREAM_UNEXPECTED_EOF);
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            }
            return (char) bytes[0];
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        return (char) -1;
    }

    public void write(byte[] data, int offset, int length) {
        if (sbLength > 0) {
            if (saveBuffer == null) {
                saveBuffer = new byte[buffer.getBlockSize()];
                savePointer = 0;
            }
            System.arraycopy(sbBuffer, sbPointer, saveBuffer, savePointer, sbLength);
            saveLength = sbLength;
            savePosition = 0;
            sbLength = 0;
        }
    }

    public void close() {
        buffer.close();
    }
}
