package org.tmatesoft.svn.core.internal.wc2.patch;

import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffCallback;

import java.util.List;

public class SvnPropertiesPatch {

    private String name;
    private List<SvnDiffHunk> hunks;
    private SvnDiffCallback.OperationKind operation;

    public SvnPropertiesPatch(String name, List<SvnDiffHunk> hunks, SvnDiffCallback.OperationKind operation) {
        this.name = name;
        this.hunks = hunks;
        this.operation = operation;
    }

    public List<SvnDiffHunk> getHunks() {
        return hunks;
    }

    public SvnDiffCallback.OperationKind getOperation() {
        return operation;
    }

    public void addHunk(SvnDiffHunk hunk) {
        hunks.add(hunk);
    }
}
