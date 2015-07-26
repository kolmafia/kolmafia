package org.tmatesoft.svn.core.internal.io.fs.revprop;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class SVNFSFSPackedRevPropsManifest {

    public static SVNFSFSPackedRevPropsManifest fromFile(File manifestFile, long revision, long maxFilesPerDirectory) throws SVNException {
        final long firstRevision = revision < maxFilesPerDirectory ? 1 : revision - revision % maxFilesPerDirectory;
        return fromString(firstRevision, SVNFileUtil.readFile(manifestFile));
    }

    public static SVNFSFSPackedRevPropsManifest fromString(long firstRevision, String manifestString) throws SVNException {
        final List<String> packNames = new ArrayList<String>();

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new StringReader(manifestString));
            while (true) {
                final String packName = bufferedReader.readLine();
                if (packName == null) {
                    break;
                }
                packNames.add(packName);
            }
            return new SVNFSFSPackedRevPropsManifest(firstRevision, packNames);
        } catch (IOException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT);
            SVNErrorManager.error(errorMessage, e, SVNLogType.FSFS);
        } finally {
            SVNFileUtil.closeFile(bufferedReader);
        }
        return null;
    }

    private final long firstRevision;
    private final List<String> packNames;

    private SVNFSFSPackedRevPropsManifest(long firstRevision, List<String> packNames) {
        this.firstRevision = firstRevision;
        this.packNames = packNames;
    }

    public long getFirstRevision() {
        return firstRevision;
    }

    public int getRevisionsCount() {
        return packNames.size();
    }

    public String updatePackName(long firstRevision, int revisionsCount) throws SVNException {
        int start = (int) (firstRevision - this.firstRevision);
        int end = start + revisionsCount;

        final String oldName = packNames.get(start);
        final int pos = oldName.indexOf('.');
        if (pos < 0) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Packed file '{{0}}' misses a tag", oldName);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }
        long tag = -1;
        try {
            tag = Long.valueOf(oldName.substring(pos + ".".length()));
        } catch (NumberFormatException e) {
            SVNErrorMessage errorMessage = SVNErrorMessage.create(SVNErrorCode.FS_CORRUPT, "Packed file '{{0}}' misses a tag", oldName);
            SVNErrorManager.error(errorMessage, SVNLogType.FSFS);
        }

        tag++;
        final String newName = firstRevision + "." + tag;

        for (int index = start; index < end; index++) {
            packNames.set(index, newName);
        }

        return newName;
    }

    public String getPackName(long revision) {
        final int revisionIndex = (int) (revision - getFirstRevision());
        return packNames.get(revisionIndex);
    }

    public String toString() {
        return asString();
    }

    public String asString() {
        final StringBuilder stringBuilder = new StringBuilder();
        for (String packName : packNames) {
            stringBuilder.append(packName).append('\n');
        }
        return stringBuilder.toString();
    }

    public static class Builder {

        private long firstRevision;
        private final List<String> packNames;

        public Builder() {
            this.firstRevision = -1;
            this.packNames = new ArrayList<String>();
        }

        public SVNFSFSPackedRevPropsManifest build() {
            return new SVNFSFSPackedRevPropsManifest(firstRevision, packNames);
        }

        public void setFirstRevision(long firstRevision) {
            this.firstRevision = firstRevision;
        }

        public void addPackName(String packName) {
            packNames.add(packName);
        }
    }
}
