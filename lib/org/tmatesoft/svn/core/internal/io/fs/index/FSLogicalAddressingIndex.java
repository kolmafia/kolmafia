package org.tmatesoft.svn.core.internal.io.fs.index;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.io.fs.FSFS;
import org.tmatesoft.svn.core.internal.io.fs.FSFile;
import org.tmatesoft.svn.core.internal.io.fs.FSID;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FSLogicalAddressingIndex {

    public static final String L2P_STREAM_PREFIX = "L2P-INDEX\n";
    public static final String P2L_STREAM_PREFIX = "P2L-INDEX\n";

    private FSFile myFile;
    private long startRevision;
    private FSFS fsfs;

    public FSLogicalAddressingIndex(FSFS fsfs, FSFile myFile) {
        this.fsfs = fsfs;
        this.myFile = myFile;
        this.startRevision = -1;
    }

    public long getStartRevision(long revision) {
        if (startRevision == -1) {
            this.startRevision = (fsfs.isPackedRevision(revision)) ?
                    (revision - (revision % fsfs.getMaxFilesPerDirectory())) : revision;
        }
        return startRevision;
    }

    public long getOffsetByItemIndex(long revision, long itemIndex) throws SVNException {
        L2PPageInfo pageInfo = getL2PPageInfo(revision, itemIndex);
        //TODO: cache the result

        final boolean isPackedRevision = fsfs.isPackedRevision(revision);
        final boolean isCached = false;
        long offset;
        if (!isCached) {
            final long lastRevision = pageInfo.getFirstRevision() + (isPackedRevision ? fsfs.getMaxFilesPerDirectory() : 1);
            final PageTableEntry entry = pageInfo.getEntry();
//            final long maxOffset = align(entry.offset + entry.size, fsfs.getBlockSize());
//            final long minOffset = maxOffset - fsfs.getBlockSize();

            final Page page = getL2PPage(entry);
            //TODO: cache!
            offset = getL2PPageEntry(page, pageInfo.getPageOffset(), itemIndex, revision);
        } else {
            //TODO: set this to the cached value
            offset = -1;
        }
        return offset;
    }

    public long getItemIndexByOffset(long offset) {
        return -1;
    }

    public List<FSP2LEntry> lookupP2LEntries(long revision, long blockStart, long blockEnd) throws SVNException {
        final boolean isCached = false;

        List<FSP2LEntry> entries = new ArrayList<FSP2LEntry>();
        P2LPageInfo pageInfo = getP2LKeys(revision, blockStart);

        if (!isCached) {
            long originalPageStart = pageInfo.getPageStart();
            int leakingBucket = 4;
            P2LPageInfo prefetchInfo = pageInfo;

            long maxOffset = FSRepositoryUtil.align(pageInfo.getNextOffset(), fsfs.getBlockSize());
            long minOffset = FSRepositoryUtil.align(pageInfo.getStartOffset(), fsfs.getBlockSize()) - fsfs.getBlockSize();

            //TODO: block read?

            final List<FSP2LEntry> pageEntries = getP2LPage(
                    pageInfo.getFirstRevision(),
                    pageInfo.getStartOffset(),
                    pageInfo.getNextOffset(),
                    pageInfo.getPageStart(),
                    pageInfo.getPageSize());
            if (pageEntries.size() > 0) {
                FSP2LEntry entry = pageEntries.get(pageEntries.size() - 1);
                if (entry.getOffset() + entry.getSize() > pageInfo.getPageSize() * pageInfo.getPageCount()) {
                    SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_OVERFLOW, "Last P2L index entry extends beyond the last page in revision {0}", revision);
                    SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
                }
            }
            //cache_set()
            appendP2LEntries(entries, pageEntries, blockStart, blockEnd);
            //TODO: block read again?

        }

        assert entries.size() > 0;

        if (pageInfo.getPageNumber() + 1 >= pageInfo.getPageCount()) {
            FSP2LEntry entry = entries.get(entries.size() - 1);
            long entryEnd = entry.getOffset() + entry.getSize();
            if (entryEnd < blockEnd) {
                if (entry.getType() == FSP2LProtoIndex.ItemType.UNUSED) {
                    entry.setSize(blockEnd - entry.getOffset());
                } else {
                    entry = new FSP2LEntry(entryEnd, blockEnd - entryEnd, FSP2LProtoIndex.ItemType.UNUSED, 0, SVNRepository.INVALID_REVISION, FSID.ITEM_INDEX_UNUSED);
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    protected P2LPageInfo getP2LKeys(long revision, long offset) throws SVNException {
        final P2LPageInfo pageInfo = getP2LPageInfo(revision, offset);

        if (pageInfo.getPageCount() <= pageInfo.getPageNumber()) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_OVERFLOW, "Offset {0} too large in revision {1}", new Object[]{offset, revision});
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        return pageInfo;

    }

    private P2LPageInfo getP2LPageInfo(long revision, long offset) throws SVNException {
        boolean isCached = false;

        if (isCached) {
            //TODO return cached value here
            return null;
        }
        final P2LIndexHeader header = getP2LHeader();
        return createPageInfo(header, revision, offset);
    }

    private P2LIndexHeader getP2LHeader() throws SVNException {
        boolean isCached = false;

        if (isCached) {
            //TODO return cached value here
            return null;
        }

        final FSPackedNumbersStream packedNumbersStream = autoOpenP2LIndex();
        packedNumbersStream.seek(0);

        final long firstRevision = packedNumbersStream.read();
        if (firstRevision != startRevision) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "Index rev / pack file revision numbers do not match");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        final long fileSize = packedNumbersStream.read();
        if (fileSize != myFile.getL2POffset()) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "Index offset and rev / pack file size do not match");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        final long pageSize = packedNumbersStream.read();
        if (pageSize == 0 || (pageSize & (pageSize - 1)) != 0) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "P2L index page size is not a power of two");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        final long pageCount = packedNumbersStream.read();
        if (pageCount != (fileSize - 1)/pageSize + 1) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "P2L page count does not match rev / pack file size");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        final long[] offsets = new long[(int) (pageCount + 1)];
        offsets[0] = 0;

        for (int i = 0; i < pageCount; i++) {
            final long value = packedNumbersStream.read();
            offsets[i+1] = offsets[i] + value;
        }

        final long offset = packedNumbersStream.position();
        for (int i = 0; i <= pageCount; i++) {
            offsets[i] += offset;
        }

        //TODO: save to cache here

        final P2LIndexHeader p2LIndexHeader = new P2LIndexHeader(firstRevision, pageSize, pageCount, fileSize, offsets);
        return p2LIndexHeader;
    }

    private P2LPageInfo createPageInfo(P2LIndexHeader header, long revision, long offset) {
        final P2LPageInfo pageInfo = new P2LPageInfo();
        if (offset / header.getPageSize() < header.getPageCount()) {
            pageInfo.setPageNumber(offset / header.getPageSize());
            pageInfo.setStartOffset(header.getOffsets()[((int) pageInfo.getPageNumber())]);
            pageInfo.setNextOffset(header.getOffsets()[((int) (pageInfo.getPageNumber() + 1))]);
            pageInfo.setPageSize(header.getPageSize());
        } else {
            pageInfo.setPageNumber(header.getPageCount());
            pageInfo.setStartOffset(header.getOffsets()[((int) pageInfo.getPageNumber())]);
            pageInfo.setNextOffset(header.getOffsets()[((int) pageInfo.getPageNumber())]);
            pageInfo.setPageSize(0);
        }
        pageInfo.setFirstRevision(header.getFirstRevision());
        pageInfo.setPageStart(header.getPageSize() * pageInfo.getPageNumber());
        pageInfo.setPageCount(header.getPageCount());
        return pageInfo;
    }

    private void appendP2LEntries(List<FSP2LEntry> entries, List<FSP2LEntry> pageEntries, long blockStart, long blockEnd) {
        FSP2LEntry entry;
        int idx = searchLowerBound(pageEntries, blockStart);

        if (idx > 0) {
            entry = pageEntries.get(idx - 1);
            if (entry.getOffset() + entry.getSize() > blockStart) {
                idx--;
            }
        }

        for (; idx < pageEntries.size(); idx++) {
            entry = pageEntries.get(idx);
            if (entry.getOffset() >= blockEnd) {
                break;
            }
            entries.add(entry);
        }
    }

    public static int searchLowerBound(List<FSP2LEntry> list, long key) {
        int lower = 0;
        int upper = list.size() - 1;

        while (lower <= upper) {
            int attempt = lower + (upper - lower) / 2;
            int cmp = compareEntryOffset(list.get(attempt), key);

            if (cmp < 0) {
                lower = attempt + 1;
            } else {
                upper = attempt - 1;
            }
        }
        assert lower == upper + 1;
        return lower;
    }

    public static int compareEntryOffset(FSP2LEntry entry, long offset) {
        long diff = entry.getOffset() - offset;
        return diff < 0 ? -1 : (diff == 0 ? 0 : 1);
    }

    private List<FSP2LEntry> getP2LPage(long startRevision, long startOffset, long nextOffset, long pageStart, long pageSize) throws SVNException {
        final List<FSP2LEntry> result = new ArrayList<FSP2LEntry>();
        final FSPackedNumbersStream packedNumbersStream = autoOpenP2LIndex();
        packedNumbersStream.seek(startOffset);

        long[] itemOffset = new long[] {packedNumbersStream.read()};
        long[] lastRevision = new long[] {startRevision};
        long[] lastCompound = new long[] {0};

        if (startOffset == nextOffset) {
            readEntryToList(packedNumbersStream, itemOffset, lastRevision, lastCompound, result);
        } else {
            long offset;
            do {
                readEntryToList(packedNumbersStream, itemOffset, lastRevision, lastCompound, result);
                offset = packedNumbersStream.read();
            } while (offset < nextOffset);

            if (offset == nextOffset) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "P2L page description overlaps with next page description");
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            }

            if (itemOffset[0] < pageStart + pageSize) {
                itemOffset[0] = packedNumbersStream.read();
                lastRevision[0] = startRevision;
                lastCompound[0] = 0;
                readEntryToList(packedNumbersStream, itemOffset, lastRevision, lastCompound, result);
            }
        }
        return result;
    }

    private void readEntryToList(FSPackedNumbersStream packedNumbersStream, long[] itemOffset, long[] lastRevision, long[] lastCompound, List<FSP2LEntry> result) throws SVNException {
        long entryOffset = itemOffset[0];
        long entrySize = packedNumbersStream.read();
        lastCompound[0] += packedNumbersStream.readSigned();
        FSP2LProtoIndex.ItemType entryType = FSP2LProtoIndex.ItemType.fromCode((int) (lastCompound[0] & 7));
        long entryNumber = lastCompound[0] / 8;

        if (entryType.getCode() > FSP2LProtoIndex.ItemType.CHANGES.getCode()) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "Invalid item type in P2L index");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        if (entryType == FSP2LProtoIndex.ItemType.CHANGES && entryNumber != FSID.ITEM_INDEX_CHANGES) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "Changed path list must have item number 1");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        lastRevision[0] += packedNumbersStream.readSigned();
        long entryRevision = lastRevision[0];

        long entryChecksum = packedNumbersStream.read();

        if (entryChecksum > Integer.MAX_VALUE) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "Invalid FNV1 checksum in P2L index");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        if (entryType == FSP2LProtoIndex.ItemType.UNUSED) {
            if (entryNumber != FSID.ITEM_INDEX_UNUSED || entryChecksum != 0) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "Empty regions must have item number 0 and checksum 0");
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            }
        }
        FSP2LEntry entry = new FSP2LEntry(entryOffset, entrySize, entryType, (int) entryChecksum, entryRevision, entryNumber);
        result.add(entry);
        itemOffset[0] += entry.getSize();
    }

    private long getL2PPageEntry(Page page, long pageOffset, long itemIndex, long revision) throws SVNException {
        if (page.getEntryCount() <= pageOffset) {
            final SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_OVERFLOW, "Item index {0}" + " too large in revision {1}", itemIndex, revision);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        final long[] offsets = page.getOffsets();
        return offsets[((int) pageOffset)];
    }

    private Page getL2PPage(PageTableEntry tableEntry) throws SVNException {
        final long entryCount = tableEntry.entryCount;
        long lastValue = 0;
        final long[] offsets = new long[(int) entryCount];

        final FSPackedNumbersStream packedNumbersStream = autoOpenL2PIndex();
        packedNumbersStream.seek(tableEntry.offset);

        for (int i = 0; i < entryCount; i++) {
            final long value = packedNumbersStream.readSigned();
            lastValue += value;
            offsets[i] = lastValue - 1;
        }
        if (packedNumbersStream.position() != tableEntry.offset + tableEntry.size) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "L2P actual page size does not match page table value");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        return new Page(entryCount, offsets);
    }

    private L2PPageInfo getL2PPageInfo(long revision, long itemIndex) throws SVNException {
        //TODO: cache the result
        final L2PIndexHeader header = getL2PHeaderBody(revision);
        return createPageInfo(header, revision, itemIndex);
    }

    private L2PPageInfo createPageInfo(L2PIndexHeader header, long revision, long itemIndex) throws SVNException {
        long relativeRevision = revision - header.getFirstRevision();
        if (relativeRevision >= header.getRevisionCount()) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_REVISION, "Revision %ld not covered by item index", revision);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        final PageTableEntry[] pageTable = header.getPageTable();
        final long[] pageTableIndex = header.getPageTableIndex();

        L2PPageInfo pageInfo = new L2PPageInfo();
        pageInfo.setRevision(revision);
        pageInfo.setItemIndex(itemIndex);
        pageInfo.setFirstRevision(header.getFirstRevision());

        if (itemIndex < header.getPageSize()) {
            pageInfo.setPageOffset((int) itemIndex);
            pageInfo.setPageNumber(0);
            pageInfo.setEntry(pageTable[((int) pageTableIndex[((int) relativeRevision)])]);
        } else {
            long maxItemIndex = header.getPageSize()*(pageTableIndex[((int) (relativeRevision + 1))] - pageTableIndex[((int) relativeRevision)]);

            if (itemIndex >= maxItemIndex) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_OVERFLOW, "Item index {0} exceeds l2p limit " +
                        "of {1} for revision {2}", itemIndex, maxItemIndex, revision);
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            }

            pageInfo.setPageOffset((int) (itemIndex % header.getPageSize()));
            pageInfo.setPageNumber((int) (itemIndex / header.getPageSize()));
            pageInfo.setEntry(pageTable[((int) (pageTableIndex[((int) relativeRevision)] + pageInfo.getPageNumber()))]);
        }
        return pageInfo;
    }

    private L2PIndexHeader getL2PHeaderBody(long revision) throws SVNException {
        FSPackedNumbersStream packedNumbersStream = autoOpenL2PIndex();
        packedNumbersStream.seek(0);
        final long firstRevision = packedNumbersStream.read();

        if (firstRevision != getStartRevision(revision)) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "Index rev / pack file revision numbers do not match");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        final long pageSize = packedNumbersStream.read();
        if (pageSize == 0 || ((pageSize & (pageSize - 1)) != 0)) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "L2P index page size is not a power of two");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        final long revisionCount = packedNumbersStream.read();
        if (revisionCount != 1 && revisionCount != fsfs.getMaxFilesPerDirectory()) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "Invalid number of revisions in L2P index");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        final long pageCount = packedNumbersStream.read();
        if (pageCount < revisionCount) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "Fewer L2P index pages than revisions");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        if (pageCount > (myFile.getP2LOffset() - myFile.getL2POffset()) / 2) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "L2P index page count implausibly large");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        final long nextRevision = firstRevision + revisionCount;
        if (firstRevision > revision || nextRevision <= revision) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "Corrupt L2P index for r{0} only covers r{1}:{2}", revision, firstRevision, nextRevision);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        PageTableEntry[] pageTable = new PageTableEntry[(int) pageCount];
        long[] pageTableIndex = new long[(int) (revisionCount + 1)];
        pageTableIndex[0] = 0;

        long index = 0;
        for (int i = 0; i < revisionCount; i++) {
            final long value = packedNumbersStream.read();
            if (value == 0) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "Revision with no L2P index pages");
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            }

            index += value;

            if (index > pageCount) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "L2P page table exceeded");
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            }
            pageTableIndex[i+1] = index;
        }

        if (index != pageCount) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "Revisions do not cover the full L2P index page table");
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        for (int page = 0; page < pageCount; page++) {
            long value = packedNumbersStream.read();
            if (value == 0) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "Empty L2P index page");
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            }

            pageTable[page] = new PageTableEntry();
            pageTable[page].size = value;
            value = packedNumbersStream.read();
            if (value > pageSize) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION, "Page exceeds L2P index page size");
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            }

            pageTable[page].entryCount = value;
        }

        long offset = packedNumbersStream.position();
        for (int page = 0; page < pageCount; page++) {
            pageTable[page].offset = offset;
            offset += pageTable[page].size;
        }

        L2PIndexHeader indexHeader = new L2PIndexHeader(firstRevision, revisionCount, pageSize, pageTableIndex, pageTable);
        return indexHeader;
    }

    private FSPackedNumbersStream autoOpenP2LIndex() throws SVNException {
        myFile.ensureFooterLoaded();
        FSPackedNumbersStream packedNumbersStream = packedStreamOpen(P2L_STREAM_PREFIX);
        return packedNumbersStream;
    }

    private FSPackedNumbersStream autoOpenL2PIndex() throws SVNException {
        myFile.ensureFooterLoaded();
        FSPackedNumbersStream packedNumbersStream = packedStreamOpen(L2P_STREAM_PREFIX);
        return packedNumbersStream;
    }

    private FSPackedNumbersStream packedStreamOpen(String prefix) throws SVNException {
        myFile.seek(myFile.getL2POffset());
        final int length = prefix.length();
        final byte[] headerBytes = new byte[length];
        try {
            final int bytesRead = myFile.read(headerBytes, 0, length);
            if (bytesRead != length) {
                SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION);
                SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
            }
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.IO_ERROR, e);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        if (!Arrays.equals(headerBytes, prefix.getBytes())) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_INDEX_CORRUPTION,
                    "Index stream header prefix mismatch.\n" +
                            "  expected: {0}" +
                            "  found: {1}", new Object[]{prefix, new String(headerBytes)});
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        return new FSPackedNumbersStream(myFile);
    }

    private static class L2PIndexHeader {
        private long firstRevision;
        private long revisionCount;
        private long pageSize;
        private long[] pageTableIndex;
        private PageTableEntry[] pageTable;

        public L2PIndexHeader(long firstRevision, long revisionCount, long pageSize, long[] pageTableIndex, PageTableEntry[] pageTable) {
            this.firstRevision = firstRevision;
            this.revisionCount = revisionCount;
            this.pageSize = pageSize;
            this.pageTableIndex = pageTableIndex;
            this.pageTable = pageTable;
        }

        public long getFirstRevision() {
            return firstRevision;
        }

        public long getRevisionCount() {
            return revisionCount;
        }

        public long getPageSize() {
            return pageSize;
        }

        public long[] getPageTableIndex() {
            return pageTableIndex;
        }

        public PageTableEntry[] getPageTable() {
            return pageTable;
        }
    }

    private static class PageTableEntry {
        private long offset;
        private long entryCount;
        private long size;
    }

    private static class L2PPageInfo {
        private long revision;
        private long itemIndex;
        private PageTableEntry entry;
        private int pageNumber;
        private int pageOffset;
        private long firstRevision;

        public long getRevision() {
            return revision;
        }

        public void setRevision(long revision) {
            this.revision = revision;
        }

        public long getItemIndex() {
            return itemIndex;
        }

        public void setItemIndex(long itemIndex) {
            this.itemIndex = itemIndex;
        }

        public PageTableEntry getEntry() {
            return entry;
        }

        public void setEntry(PageTableEntry entry) {
            this.entry = entry;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(int pageNumber) {
            this.pageNumber = pageNumber;
        }

        public int getPageOffset() {
            return pageOffset;
        }

        public void setPageOffset(int pageOffset) {
            this.pageOffset = pageOffset;
        }

        public long getFirstRevision() {
            return firstRevision;
        }

        public void setFirstRevision(long firstRevision) {
            this.firstRevision = firstRevision;
        }
    }

    private static class P2LPageInfo {
        private long revision;
        private long offset;
        private long pageNumber;
        private long firstRevision;
        private long startOffset;
        private long nextOffset;
        private long pageStart;
        private long pageCount;
        private long pageSize;

        public long getRevision() {
            return revision;
        }

        public void setRevision(long revision) {
            this.revision = revision;
        }

        public long getOffset() {
            return offset;
        }

        public void setOffset(long offset) {
            this.offset = offset;
        }

        public long getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(long pageNumber) {
            this.pageNumber = pageNumber;
        }

        public long getFirstRevision() {
            return firstRevision;
        }

        public void setFirstRevision(long firstRevision) {
            this.firstRevision = firstRevision;
        }

        public long getStartOffset() {
            return startOffset;
        }

        public void setStartOffset(long startOffset) {
            this.startOffset = startOffset;
        }

        public long getNextOffset() {
            return nextOffset;
        }

        public void setNextOffset(long nextOffset) {
            this.nextOffset = nextOffset;
        }

        public long getPageStart() {
            return pageStart;
        }

        public void setPageStart(long pageStart) {
            this.pageStart = pageStart;
        }

        public long getPageCount() {
            return pageCount;
        }

        public void setPageCount(long pageCount) {
            this.pageCount = pageCount;
        }

        public long getPageSize() {
            return pageSize;
        }

        public void setPageSize(long pageSize) {
            this.pageSize = pageSize;
        }
    }

    private static class P2LIndexHeader {
        private long firstRevision;
        private long pageSize;
        private long pageCount;
        private long fileSize;
        private long[] offsets;

        public P2LIndexHeader(long firstRevision, long pageSize, long pageCount, long fileSize, long[] offsets) {
            this.firstRevision = firstRevision;
            this.pageSize = pageSize;
            this.pageCount = pageCount;
            this.fileSize = fileSize;
            this.offsets = offsets;
        }

        public long getFirstRevision() {
            return firstRevision;
        }

        public long getPageSize() {
            return pageSize;
        }

        public long getPageCount() {
            return pageCount;
        }

        public long getFileSize() {
            return fileSize;
        }

        public long[] getOffsets() {
            return offsets;
        }
    }

    private static class Page {

        long entryCount;
        long[] offsets;

        public Page(long entryCount, long[] offsets) {
            this.entryCount = entryCount;
            this.offsets = offsets;
        }

        public long getEntryCount() {
            return entryCount;
        }

        public long[] getOffsets() {
            return offsets;
        }
    }
}
