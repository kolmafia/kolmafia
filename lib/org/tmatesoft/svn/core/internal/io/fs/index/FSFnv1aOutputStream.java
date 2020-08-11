package org.tmatesoft.svn.core.internal.io.fs.index;

import java.io.IOException;
import java.io.OutputStream;

public class FSFnv1aOutputStream extends OutputStream {

    private final OutputStream delegate;
    private final FSFnv1aInterleavedChecksumCalculator checksumCalculator;

    public FSFnv1aOutputStream(OutputStream delegate) {
        this.delegate = delegate;
        this.checksumCalculator = new FSFnv1aInterleavedChecksumCalculator();
    }

    public void write(int b) throws IOException {
        final byte[] bytes = new byte[1];
        bytes[0] = (byte) b;
        write(bytes);
    }

    public void write(byte[] data) throws IOException {
        update(data, 0, data.length);
        delegate.write(data);
    }

    public void write(byte[] data, int offset, int length) throws IOException {
        update(data, offset, length);
        delegate.write(data, offset, length);
    }

    public void flush() throws IOException {
        delegate.flush();
    }

    public void close() throws IOException {
        delegate.close();
    }

    public void resetChecksum() {
        checksumCalculator.resetChecksum();
    }

    public int finalizeChecksum() {
        return checksumCalculator.finalizeChecksum();
    }

    private void update(byte[] bytes, int offset, int length) {
        checksumCalculator.update(bytes, offset, length);
    }
}
