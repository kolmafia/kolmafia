package org.tmatesoft.svn.core.internal.io.fs.index;

/**
 * A modification of FNV-1a algorithm. When given an input of 4k+b bytes
 * the first 4k bytes input is divided into 4 arrays:
 *   of the 1st, 5th, 9th, 13th, ... bytes
 *   of the 2nd, 6th, 10th, 14th, ... bytes
 *   of the 3rd, 7th, 11th, 15th, ... bytes
 *   of the 4th, 8th, 12th, 16th, ... bytes
 *
 *   Then 4 normal FNV-1a checksums are calculated from them.
 *   Then the checksums are written as raw a byte array and the last b bytes
 *   are appended.
 *   Finally normal FNV-1a checksum is calculated.
 */
public class FSFnv1aInterleavedChecksumCalculator {

    private static final int SCALING = 4; //number of interleaved streams
    private static final int HASH_SIZE_IN_BYTES = Integer.SIZE / 8;
    private static final long FNV1_BASE_32 = 2166136261L;
    private static final long FNV1_PRIME_32 = 0x01000193L;

    private final long[] hashes;
    private final byte[] buffer;
    private int buffered;

    public FSFnv1aInterleavedChecksumCalculator() {
        this.hashes = new long[SCALING];
        this.buffer = new byte[SCALING];
        resetChecksum();
    }

    public void update(byte[] data, int offset, int length) {
        if (buffered != 0) {
            int toCopy = SCALING - buffered;
            if (toCopy > length) {
                System.arraycopy(data, offset, buffer, buffered, length);
                buffered += length;
                return;
            }
            System.arraycopy(data, offset, buffer, buffered, toCopy);
            offset += toCopy;
            length -= toCopy;
            fnv1aInterleaved(buffer, 0, SCALING);
            buffered = 0;
        }
        int processed = fnv1aInterleaved(data, offset, length);
        if (processed != length) {
            buffered = length - processed;
            System.arraycopy(data, offset + processed, buffer, 0, length - processed);
        }
    }

    public int finalizeChecksum() {
        return finalizeChecksum(buffer, 0, buffered);
    }

    private int finalizeChecksum(byte[] data, int offset, int length) {
        final byte[] finalData = new byte[HASH_SIZE_IN_BYTES * SCALING + SCALING - 1];
        assert length < SCALING;

        for (int i = 0; i < SCALING; i++) {
            long hash = hashes[i];

            for (int j = 0; j < HASH_SIZE_IN_BYTES; j++) {
                final byte b = (byte) (hash & 0xff);
                finalData[i * HASH_SIZE_IN_BYTES + (HASH_SIZE_IN_BYTES - j - 1)] = b;
                hash = hash >> Byte.SIZE;
            }
        }
        if (length != 0) {
            System.arraycopy(data, offset, finalData, SCALING * HASH_SIZE_IN_BYTES, length);
        }

        return (int) fnv1a(FNV1_BASE_32, finalData, 0, SCALING * HASH_SIZE_IN_BYTES + length);
    }

    private int fnv1aInterleaved(byte[] data, int offset, int length) {
        int i = 0;
        for (; i + SCALING <= length; i+=SCALING) {
            hashes[0] ^= data[i + offset] & 0xffL;
            hashes[0] *= FNV1_PRIME_32;
            hashes[1] ^= data[i + offset + 1] & 0xffL;
            hashes[1] *= FNV1_PRIME_32;
            hashes[2] ^= data[i + offset + 2] & 0xffL;
            hashes[2] *= FNV1_PRIME_32;
            hashes[3] ^= data[i + offset + 3] & 0xffL;
            hashes[3] *= FNV1_PRIME_32;
        }
        return i;
    }

    private long fnv1a(long hash, byte[] data, int offset, int length) {
        for (int i = 0; i < length; i++) {
            hash ^= data[i + offset] & 0xffL;
            hash = hash * FNV1_PRIME_32;
        }
        return hash;
    }

    protected void resetChecksum() {
        for (int i = 0; i < hashes.length; i++) {
            hashes[i] = FNV1_BASE_32;
        }
        buffered = 0;
    }
}
