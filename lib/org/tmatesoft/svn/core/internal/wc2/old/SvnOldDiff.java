package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNDiffClient16;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.ng.ISvnDiffGenerator;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffGenerator;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnNewDiffGenerator;
import org.tmatesoft.svn.core.wc.DefaultSVNDiffGenerator;
import org.tmatesoft.svn.core.wc.ISVNDiffGenerator;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnDiff;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnOldDiff extends SvnOldRunner<Void, SvnDiff> {

    @Override
    public boolean isApplicable(SvnDiff operation, SvnWcGeneration wcGeneration) throws SVNException {
        if (wcGeneration != SvnWcGeneration.V16) {
            return false;
        }
        if (operation.getSource() != null) {
            if (operation.getSource().isFile()) {
                return true;
            }
        } else {
            if (operation.getFirstSource().isFile()) {
                return true;
            }
            if (operation.getSecondSource().isFile()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Void run() throws SVNException {
        final SVNDiffClient16 diffClient = new SVNDiffClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        diffClient.setDiffGenerator(getDiffGenerator());
        diffClient.setMergeOptions(getOperation().getDiffOptions());

        final boolean peggedDiff = getOperation().getSource() != null;
        if (peggedDiff) {
            final SVNRevision startRevision = getOperation().getStartRevision() == null ? SVNRevision.UNDEFINED : getOperation().getStartRevision();
            final SVNRevision endRevision = getOperation().getEndRevision() == null ? SVNRevision.UNDEFINED : getOperation().getEndRevision();

            diffClient.doDiff(getOperation().getSource().getFile(), getOperation().getSource().getResolvedPegRevision(),
                    startRevision, endRevision,
                    getOperation().getDepth(), !getOperation().isIgnoreAncestry(), getOperation().getOutput(), getOperation().getApplicableChangelists());
        } else {
            final SVNRevision startRevision = getOperation().getFirstSource().getPegRevision() == null ? SVNRevision.UNDEFINED : getOperation().getFirstSource().getPegRevision();
            final SVNRevision endRevision = getOperation().getSecondSource().getPegRevision() == null ? SVNRevision.UNDEFINED : getOperation().getSecondSource().getPegRevision();

            if (getOperation().getFirstSource().isURL() && getOperation().getSecondSource().isFile()) {
                diffClient.doDiff(getOperation().getFirstSource().getURL(), startRevision, getOperation().getSecondSource().getFile(), endRevision,
                        getOperation().getDepth(), !getOperation().isIgnoreAncestry(), getOperation().getOutput(), getOperation().getApplicableChangelists());
            } else if (getOperation().getFirstSource().isFile() && getOperation().getSecondSource().isURL()) {
                diffClient.doDiff(getOperation().getFirstSource().getFile(), startRevision, getOperation().getSecondSource().getURL(), endRevision, getOperation().getDepth(), !getOperation().isIgnoreAncestry(), getOperation().getOutput(), getOperation().getApplicableChangelists());
            } else if (getOperation().getFirstSource().isFile() && getOperation().getSecondSource().isFile()) {
                diffClient.doDiff(getOperation().getFirstSource().getFile(), startRevision, getOperation().getSecondSource().getFile(), endRevision, getOperation().getDepth(), !getOperation().isIgnoreAncestry(), getOperation().getOutput(), getOperation().getApplicableChangelists());
            } else {
                throw new UnsupportedOperationException("URL-URL diff is not supported");
            }
        }

        return null;
    }

    private ISVNDiffGenerator getDiffGenerator() {
        ISvnDiffGenerator diffGenerator = getOperation().getDiffGenerator();

        if (diffGenerator != null) {
            if (getOperation().getRelativeToDirectory() != null) {
                if (diffGenerator instanceof SvnDiffGenerator) {
                    ((SvnDiffGenerator) diffGenerator).setRelativeToTarget(SvnTarget.fromFile(getOperation().getRelativeToDirectory()));
                } else {
                    diffGenerator.setBaseTarget(SvnTarget.fromFile(getOperation().getRelativeToDirectory()));
                }
            }
            return new SvnNewDiffGenerator(diffGenerator);
        } else {
            DefaultSVNDiffGenerator defaultSVNDiffGenerator = new DefaultSVNDiffGenerator();
            if (getOperation().getRelativeToDirectory() != null) {
                defaultSVNDiffGenerator.setBasePath(getOperation().getRelativeToDirectory());
            }
            return defaultSVNDiffGenerator;
        }
    }
}
