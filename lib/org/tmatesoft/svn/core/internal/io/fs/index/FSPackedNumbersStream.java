package org.tmatesoft.svn.core.internal.io.fs.index;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.fs.FSFile;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.IOException;

public class FSPackedNumbersStream {

    private static final int MAX_BYTES_PER_NUMER = 4;
    private static final int NUMBERS_BUFFER_SIZE = 1024;
    private static final int BYTES_BUFFER_SIZE = MAX_BYTES_PER_NUMER * NUMBERS_BUFFER_SIZE;
    private final byte[] bytes;
    private final long[] numbers;
    private final int[] numbersLengths;
    private final FSFile input;
    private int bytesStart;
    private int bytesLength;
    private int numbersStart;
    private int numbersLength;
    private boolean eof;
    private long inputStartPosition;
    private long prefetchedBytesCount;

    public FSPackedNumbersStream(FSFile input) {
        this.input = input;
        this.bytes = new byte[BYTES_BUFFER_SIZE];
        this.numbers = new long[NUMBERS_BUFFER_SIZE];
        this.numbersLengths = new int[NUMBERS_BUFFER_SIZE];
        this.bytesStart = 0;
        this.bytesLength = 0;
        this.numbersStart = 0;
        this.numbersLength = 0;
        this.eof = false;
        this.inputStartPosition = input.position();
        this.prefetchedBytesCount = 0;
    }

    public boolean isEof() {
        return eof;
    }

    public long readSigned() throws SVNException {
        return decodeSigned(read());
    }

    private long decodeSigned(long value) {
        return ((value % 2 != 0) ? (-1 - (value / 2)) : (value / 2));
    }

    public long read() throws SVNException {
        try {
            if (numbersLength == 0) {
                numbersStart = 0;
                if (!eof) {
                    this.eof = readNextNumbersBlock();
                }
            }
            final int length = numbersLengths[numbersStart];
            final long value = numbers[numbersStart];
            numbersStart++;
            numbersLength--;
            if (numbersLength == 0 || numbersStart == numbers.length) {
                numbersStart = 0;
            }
            prefetchedBytesCount -= length;
            return value;
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        return -1;
    }

    private boolean readNextNumbersBlock() throws IOException {
        boolean eof = false;
        int bytesToUnpack;
        do {
            int numbersToRead = numbers.length - (numbersStart + numbersLength);
            int bytesToRead = Math.min(numbersToRead * 4, bytes.length - (bytesStart + bytesLength));
            int bytesRead = input.read(bytes, bytesStart + bytesLength, bytesToRead);
            if (bytesRead < 0) {
                bytesRead = 0;
                eof = true;
            }
            prefetchedBytesCount += bytesRead;
            bytesLength += bytesRead;
            bytesToUnpack = bytesLength;

            while (bytesToUnpack > 0 && ((bytes[bytesToUnpack - 1] & 0x80) != 0)) {
                bytesToUnpack--;
            }
        } while (!eof && bytesToUnpack == 0);

        int i = 0;
        while (i < bytesToUnpack) {
            if (numbers.length == numbersLength) {
                break;
            }
            byte b = bytes[i];
            long value = 0;
            int length = 0;
            if ((b & 0x80) == 0) {
                bytesStart++;
                bytesLength--;
                i++;
                value = b;
                length = 1;
            } else {
                int j = i;

                while (bytes[j] < 0) {
                    j++;
                }
                int bytesProcessed = j - i + 1;
                if (bytesProcessed > 9) {
                    throw new RuntimeException("Packed number exceeds capacity of signed long");
                }

                value = bytes[j];
                while (j > i) {
                    j--;
                    value <<= 7;
                    value += (bytes[j] & 0x7f);
                }

                length = bytesProcessed;
                i += bytesProcessed;
                bytesStart += bytesProcessed;
                bytesLength -= bytesProcessed;
            }
            int p = numbersStart + numbersLength;// == numbers.length
            if (p >= numbers.length) {
                p -= numbers.length;
            }
            numbers[p] = value;
            numbersLengths[p] = length;
            numbersLength++;

        }
        //move bytes[i] --> bytes[0]
        for (int k = 0; k < bytesLength; k++) {
            bytes[k] = bytes[k + i];
        }
        bytesStart = 0;
        return eof && numbersLength <= 0;
    }

    public void seek(long offset) {
        //TODO: optimize if the buffer contains data to read
        numbersStart = 0;
        numbersLength = 0;
        bytesStart = 0;
        bytesLength = 0;
        prefetchedBytesCount = 0;
        input.seek(inputStartPosition + offset);
    }

    public long position() {
        return input.position() - inputStartPosition - prefetchedBytesCount;
    }
}
