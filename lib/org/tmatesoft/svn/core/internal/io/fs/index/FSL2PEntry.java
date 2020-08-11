package org.tmatesoft.svn.core.internal.io.fs.index;

public class FSL2PEntry {
    private long offset;
    private long itemIndex;

    public FSL2PEntry(long offset, long itemIndex) {
        this.offset = offset;
        this.itemIndex = itemIndex;
    }

    public long getOffset() {
        return offset;
    }

    public long getItemIndex() {
        return itemIndex;
    }
}
