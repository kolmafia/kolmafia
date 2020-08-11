package org.tmatesoft.svn.core.internal.util;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class SVNSpillBuffer implements Closeable {

    private final int blockSize;
    private final long maxSize;
    private final boolean deleteOnClose;
    private final boolean spillAllContents;
    private final File dirPath;
    private long memorySize;
    private long spillSize;
    private File fileName;
    private RandomAccessFile spill;
    private MemoryBlock head;
    private MemoryBlock tail;
    private MemoryBlock outForReading;
    private MemoryBlock available;
    private long spillStart;

    public SVNSpillBuffer(int blockSize, long maxSize) {
        this(blockSize, maxSize, true, false, null);
    }

    public SVNSpillBuffer(int blockSize, long maxSize, boolean deleteOnClose, boolean spillAllContents, File dirPath) {
        this.blockSize = blockSize;
        this.maxSize = maxSize;
        this.deleteOnClose = deleteOnClose;
        this.spillAllContents = spillAllContents;
        this.dirPath = dirPath;
    }

    public long getSize() {
        return this.memorySize + this.spillSize;
    }

    public long getMemorySize() {
        return this.memorySize;
    }

    public File getFileName() {
        return fileName;
    }

    public RandomAccessFile getSpill() {
        return spill;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public SVNSpillBufferInputStream createInputStream() {
        return new SVNSpillBufferInputStream(this);
    }

    public void write(byte[] data, int offset, int length) throws SVNException {
        try {
            MemoryBlock block = null;
            if (spill == null && (memorySize + length) > maxSize) {
                File dirPath = this.dirPath;
                if (dirPath == null) {
                    dirPath = SVNFileUtil.createFilePath(System.getProperty("java.io.tmpdir"));
                }
                fileName = SVNFileUtil.createUniqueFile(dirPath, "svn", ".tmp", false);
                spill = SVNFileUtil.openRAFileForWriting(fileName, false);
                assert spill != null;

                if (spillAllContents) {
                    block = head;
                    while (block != null) {
                        spill.write(block.data, 0, block.length);
                        block = block.next;
                    }
                    spillStart = memorySize;
                }
            }
            if (spill != null) {
                spill.seek(spill.length());
                spill.write(data, offset, length);
                spillSize += length;
                return;
            }
            while (length > 0) {
                if (tail == null || tail.length == blockSize) {
                    block = getBuffer();
                    block.length = 0;
                    block.next = null;
                } else {
                    block = tail;
                }
                int amt = blockSize - block.length;
                if (amt > length) {
                    amt = length;
                }
                System.arraycopy(data, offset, block.data, block.length, amt);
                block.length += amt;
                offset += amt;
                length -= amt;
                memorySize += amt;
                if (tail == null) {
                    head = block;
                    tail = block;
                } else if (block != tail) {
                    tail.next = block;
                    tail = block;
                }
            }
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
    }

    public void close() {
        SVNFileUtil.closeFile(spill);
        spill = null;
        if (deleteOnClose) {
            try {
                if (fileName != null) {
                    SVNFileUtil.deleteFile(fileName);
                    fileName = null;
                }
            } catch (SVNException e) {
                //ignore
            }
        }
    }

    public MemoryBlock read() throws IOException {
        maybeSeek();
        MemoryBlock block = readData();
        if (block == null) {
            return null;
        } else {
            if (outForReading != null) {
                returnBuffer(outForReading);
            }
            outForReading = block;
            return block;
        }
    }

    private boolean maybeSeek() throws IOException {
        if (head == null && spill != null) {
            spill.seek(spillStart);
            return true;
        } else {
            return false;
        }
    }

    private MemoryBlock readData() throws IOException {
        final MemoryBlock block;
        if (head != null) {
            block = head;
            if (tail == block) {
                head = tail = null;
            } else {
                head = block.next;
            }
            memorySize -= block.length;
            return block;
        }

        if (spill == null) {
            block = null;
            return block;
        }
        block = getBuffer();
        if (spillSize < blockSize) {
            block.length = (int) spillSize;
        } else {
            block.length = blockSize;
        }
        block.next = null;
        try {
            spill.read(block.data, 0, block.length);
        } catch (IOException e) {
            returnBuffer(block);
            throw e;
        }
        spillStart += block.length;

        if ((spillSize -= block.length) == 0) {
            close();
            spillStart = 0;
        }
        return block;
    }

    private MemoryBlock getBuffer() {
        MemoryBlock block = outForReading;
        if (block != null) {
            outForReading = null;
            return block;
        }
        if (available == null) {
            block = new MemoryBlock(new byte[blockSize], blockSize);
            return block;
        }

        block = available;
        available = block.next;
        return block;
    }

    private void returnBuffer(MemoryBlock block) {
        block.next = available;
        available = block;
    }

    static class MemoryBlock {
        protected final byte[] data;
        protected int length;
        protected MemoryBlock next;

        public MemoryBlock(byte[] data, int length) {
            this.data = data;
            this.length = length;
        }
    }

}
