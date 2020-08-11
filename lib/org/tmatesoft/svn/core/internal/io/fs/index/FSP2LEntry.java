package org.tmatesoft.svn.core.internal.io.fs.index;

public class FSP2LEntry {
    public static final long SIZE_IN_BYTES = 6 * 8; //6x64bit fields

    private long offset;
    private long size;
    private FSP2LProtoIndex.ItemType type;
    private int checksum; //fnv-1a
    private long revision;
    private long number;

    public FSP2LEntry(long offset, long size, FSP2LProtoIndex.ItemType type, int checksum, long revision, long number) {
        this.offset = offset;
        this.size = size;
        this.type = type;
        this.checksum = checksum;
        this.revision = revision;
        this.number = number;
    }

    public long getOffset() {
        return offset;
    }

    public long getSize() {
        return size;
    }

    public FSP2LProtoIndex.ItemType getType() {
        return type;
    }

    public int getChecksum() {
        return checksum;
    }

    public long getRevision() {
        return revision;
    }

    public long getNumber() {
        return number;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setType(FSP2LProtoIndex.ItemType type) {
        this.type = type;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    public void setNumber(long number) {
        this.number = number;
    }
}
