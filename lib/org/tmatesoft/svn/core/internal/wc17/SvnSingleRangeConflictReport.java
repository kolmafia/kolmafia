package org.tmatesoft.svn.core.internal.wc17;


import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgMergeDriver;

public class SvnSingleRangeConflictReport {

    private final SvnNgMergeDriver.MergeSource conflictedRange;
    private final SvnNgMergeDriver.MergeSource remainingSource;

    public SvnSingleRangeConflictReport(SvnNgMergeDriver.MergeSource conflictedRange, SvnNgMergeDriver.MergeSource remainingSource) {
        this.remainingSource = remainingSource;
        this.conflictedRange = conflictedRange;
    }

    public SvnNgMergeDriver.MergeSource getConflictedRange() {
        return conflictedRange;
    }

    public SvnNgMergeDriver.MergeSource getRemainingSource() {
        return remainingSource;
    }
}
