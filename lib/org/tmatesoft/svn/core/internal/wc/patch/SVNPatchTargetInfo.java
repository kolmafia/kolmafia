package org.tmatesoft.svn.core.internal.wc.patch;

import java.io.File;

public class SVNPatchTargetInfo {

    private final File localAbsPath;
    private final boolean deleted;

    public SVNPatchTargetInfo(File localAbsPath, boolean deleted) {
        this.localAbsPath = localAbsPath;
        this.deleted = deleted;
    }

    public File getLocalAbsPath() {
        return localAbsPath;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
