package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;

public class SvnDiffSource {

    private File reposRelPath;
    private long revision;

    public SvnDiffSource(long revision) {
        this.revision = revision;
    }

    public void setReposRelPath(File reposRelPath) {
        this.reposRelPath = reposRelPath;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    public File getReposRelPath() {
        return reposRelPath;
    }

    public long getRevision() {
        return revision;
    }
}
