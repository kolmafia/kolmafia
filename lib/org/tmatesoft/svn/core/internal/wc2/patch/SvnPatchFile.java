package org.tmatesoft.svn.core.internal.wc2.patch;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.patch.SVNPatchFileStream;
import org.tmatesoft.svn.util.SVNDebugLog;
import org.tmatesoft.svn.util.SVNLogType;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class SvnPatchFile {

    public static SvnPatchFile openReadOnly(File patchFile) throws IOException, SVNException {
        return new SvnPatchFile(SVNPatchFileStream.openReadOnly(patchFile), 0);
    }

    private SVNPatchFileStream patchFileStream;
    private long nextPatchOffset;

    public SvnPatchFile(SVNPatchFileStream patchFileStream, long nextPatchOffset) {
        this.patchFileStream = patchFileStream;
        this.nextPatchOffset = nextPatchOffset;
    }

    public SVNPatchFileStream getPatchFileStream() {
        return patchFileStream;
    }

    public long getNextPatchOffset() {
        return nextPatchOffset;
    }

    public void setNextPatchOffset(long nextPatchOffset) {
        this.nextPatchOffset = nextPatchOffset;
    }

    public void close() {
        try {
            patchFileStream.close();
        } catch (IOException e) {
            SVNDebugLog.getDefaultLog().log(SVNLogType.WC, e, Level.INFO);
        }
    }
}
