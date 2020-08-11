package org.tmatesoft.svn.core.internal.util;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class BufferedRandomAccessFile {

    private final RandomAccessFile randomAccessFile;
    private final byte[] buffer;
    private int bufferSize;
    private long bufferPointer;
    private long filePointer;

    public BufferedRandomAccessFile(RandomAccessFile randomAccessFile) {
        this(randomAccessFile, 32768);
    }

    public BufferedRandomAccessFile(RandomAccessFile randomAccessFile, int bufferCapacity) {
        this.randomAccessFile = randomAccessFile;
        this.buffer = new byte[bufferCapacity];
        this.bufferSize = 0;
        this.bufferPointer = 0;
        this.filePointer = 0;
    }

    public void loadPosition() throws IOException {
        this.filePointer = randomAccessFile.getFilePointer();
    }

    public void savePosition() throws IOException {
        randomAccessFile.seek(this.filePointer);
    }

    public FileDescriptor getFD() throws IOException {
        return randomAccessFile.getFD();
    }

    public FileChannel getChannel() {
        return randomAccessFile.getChannel();
    }

    public int read() throws IOException {
        final long filePointer = getFilePointer();
        if (filePointer >= bufferPointer &&
                filePointer < bufferPointer + bufferSize) {
            final int bufferOffset = (int) (filePointer - bufferPointer);
            seek(filePointer + 1);
            return buffer[bufferOffset] & 0xff;
        } else {
            bufferSize = 0; //invalidate buffer
            bufferPointer = filePointer;
            randomAccessFile.seek(getFilePointer());
            final int bytesRead = randomAccessFile.read(buffer);
            if (bytesRead < 0) {
                return -1;
            }
            seek(filePointer + 1);
            bufferSize = bytesRead;
            return buffer[0] & 0xff;
        }
    }

    public int read(byte[] b, int off, int len) throws IOException {
        final long filePointer = getFilePointer();
        long left = Math.max(filePointer, bufferPointer);
        long right = Math.min(filePointer + left, bufferPointer + bufferSize);

        if (left < right) {
            //buffer overlaps with data to read
            if (filePointer < left) {
                randomAccessFile.seek(getFilePointer());
                final int bytesRead = randomAccessFile.read(b, off, (int) (left - filePointer));
                if (bytesRead < 0) {
                    //strange situation but return -1:
                    return bytesRead;
                }
            }
            System.arraycopy(buffer, (int)(left - bufferPointer),
                    b, off + (int)(left - filePointer),
                    (int) (right - left));
            if (filePointer + len > right) {
                randomAccessFile.seek(right);
                final int bytesRead = randomAccessFile.read(buffer, 0, buffer.length);
                if (bytesRead < 0) {
                    bufferSize = 0;
                    seek(right);
                    return (int) (right - filePointer);
                } else {
                    bufferSize = bytesRead;
                    final int bytesToCopy = (int) Math.min(bytesRead, filePointer + len - right);
                    System.arraycopy(buffer, 0,
                            b, off + (int)(right - filePointer),
                            bytesToCopy);
                    seek(right + bytesToCopy);
                    return (int) (right - filePointer + bytesToCopy);
                }
            } else {
                seek(right);
                return (int) (right - filePointer);
            }
        } else {
            randomAccessFile.seek(getFilePointer());
            final int bytesRead = randomAccessFile.read(b, off, len);
            if (bytesRead >= 0) {
                seek(getFilePointer() + bytesRead);
            }
            return bytesRead;
        }
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public long getFilePointer() throws IOException {
        return this.filePointer;
    }

    public void seek(long pos) throws IOException {
        this.filePointer = pos;
    }
}
