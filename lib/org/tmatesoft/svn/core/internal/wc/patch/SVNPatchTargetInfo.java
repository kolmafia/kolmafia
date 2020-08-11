package org.tmatesoft.svn.core.internal.wc.patch;

import java.io.File;

public class SVNPatchTargetInfo {

    private final File localAbsPath;
    private final boolean added;
    private final boolean deleted;

    public SVNPatchTargetInfo(File localAbsPath, boolean added, boolean deleted) {
        this.localAbsPath = localAbsPath;
        this.added = added;
        this.deleted = deleted;
    }

    public File getLocalAbsPath() {
        return localAbsPath;
    }

    public boolean isAdded() {
        return added;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
