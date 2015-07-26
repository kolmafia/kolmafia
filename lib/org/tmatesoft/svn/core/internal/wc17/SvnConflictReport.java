package org.tmatesoft.svn.core.internal.wc17;

import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgMergeDriver;

import java.io.File;

public class SvnConflictReport {
    private final File targetAbsPath;
    private final SvnNgMergeDriver.MergeSource conflictedRange;
    private final boolean wasLastRange;

    public SvnConflictReport(File targetAbsPath, SvnNgMergeDriver.MergeSource conflictedRange, boolean wasLastRange) {
        this.targetAbsPath = targetAbsPath;
        this.conflictedRange = conflictedRange;
        this.wasLastRange = wasLastRange;
    }

    public File getTargetAbsPath() {
        return targetAbsPath;
    }

    public SvnNgMergeDriver.MergeSource getConflictedRange() {
        return conflictedRange;
    }

    public boolean wasLastRange() {
        return wasLastRange;
    }
}
