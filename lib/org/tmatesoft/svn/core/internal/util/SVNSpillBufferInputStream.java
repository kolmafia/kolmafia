package org.tmatesoft.svn.core.internal.util;

import java.io.IOException;
import java.io.InputStream;

public class SVNSpillBufferInputStream extends InputStream {
    private final SVNSpillBufferReader reader;
    private final byte[] byteBuffer;

    public SVNSpillBufferInputStream(SVNSpillBuffer buffer) {
        this.reader = new SVNSpillBufferReader(buffer);
        this.byteBuffer = new byte[1];
    }

    public int read() throws IOException {
        final int bytesRead = read(byteBuffer);
        if (bytesRead < 0) {
            return bytesRead;
        }
        return byteBuffer[0];
    }

    public int read(byte[] bytes) throws IOException {
        return read(bytes, 0, bytes.length);
    }

    public int read(byte[] bytes, int offset, int length) throws IOException {
        return reader.read(bytes, offset, length);
    }

    public void close() {
        reader.close();
    }
}
