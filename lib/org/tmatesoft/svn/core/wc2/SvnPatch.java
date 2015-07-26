package org.tmatesoft.svn.core.wc2;

import java.io.File;

public class SvnPatch extends SvnOperation<Void> {

    private File patchFile;
    private boolean dryRun;
    private int stripCount;
    private boolean reverse;
    private boolean ignoreWhitespace;
    private boolean removeTempFiles;

    protected SvnPatch(SvnOperationFactory factory) {
        super(factory);
    }

    public File getPatchFile() {
        return patchFile;
    }

    public void setPatchFile(File patchFile) {
        this.patchFile = patchFile;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public int getStripCount() {
        return stripCount;
    }

    public void setStripCount(int stripCount) {
        this.stripCount = stripCount;
    }

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    public boolean isIgnoreWhitespace() {
        return ignoreWhitespace;
    }

    public void setIgnoreWhitespace(boolean ignoreWhitespace) {
        this.ignoreWhitespace = ignoreWhitespace;
    }

    public boolean isRemoveTempFiles() {
        return removeTempFiles;
    }

    public void setRemoveTempFiles(boolean removeTempFiles) {
        this.removeTempFiles = removeTempFiles;
    }
}
