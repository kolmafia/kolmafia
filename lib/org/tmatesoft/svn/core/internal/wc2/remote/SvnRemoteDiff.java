package org.tmatesoft.svn.core.internal.wc2.remote;

import java.util.Collection;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNDiffClient16;
import org.tmatesoft.svn.core.internal.wc2.SvnRemoteOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNewDiffGenerator;
import org.tmatesoft.svn.core.wc.DefaultSVNDiffGenerator;
import org.tmatesoft.svn.core.wc.ISVNDiffGenerator;
import org.tmatesoft.svn.core.wc2.SvnDiff;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnRemoteDiff extends SvnRemoteOperationRunner<Void, SvnDiff> {

    @Override
    public boolean isApplicable(SvnDiff operation, SvnWcGeneration wcGeneration) throws SVNException {
//        final Collection<SvnTarget> targets = operation.getTargets();
//        for (SvnTarget target : targets) {
//            if (!target.isURL()) {
//                return false;
//            }
//        }
//
        return false;
    }

    @Override
    protected Void run() throws SVNException {
        assert getOperation().getTargets().size() == 1 || getOperation().getTargets().size() == 2;

        final SVNDiffClient16 diffClient = new SVNDiffClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        diffClient.setDiffGenerator(getDiffGenerator());
        diffClient.setMergeOptions(getOperation().getDiffOptions());

        final Collection<SvnTarget> targets = getOperation().getTargets();
        if (getOperation().getSource() != null) {
            diffClient.doDiff(getOperation().getSource().getURL(),
                    getOperation().getSource().getResolvedPegRevision(),
                    getOperation().getStartRevision(),
                    getOperation().getEndRevision(),
                    getOperation().getDepth(),
                    !getOperation().isIgnoreAncestry(),
                    getOperation().getOutput());
        } else {
            diffClient.doDiff(getOperation().getFirstSource().getURL(), getOperation().getFirstSource().getPegRevision(),
                    getOperation().getSecondSource().getURL(), getOperation().getSecondSource().getPegRevision(),
                    getOperation().getDepth(),
                    !getOperation().isIgnoreAncestry(),
                    getOperation().getOutput());
        }

        return null;
    }

    private ISVNDiffGenerator getDiffGenerator() {
        ISVNDiffGenerator generator = getOperation().getDiffGenerator() != null ? new SvnNewDiffGenerator(getOperation().getDiffGenerator()) : new DefaultSVNDiffGenerator();
        generator.setDiffAdded(true);
        return generator;
    }
}
