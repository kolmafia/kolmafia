package org.tmatesoft.svn.core.internal.wc2.old;

import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc16.SVNDiffClient16;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.wc2.SvnGetMergeInfo;

public class SvnOldGetMergeInfo extends SvnOldRunner<Map<SVNURL, SVNMergeRangeList>, SvnGetMergeInfo> {

    @Override
    public boolean isApplicable(SvnGetMergeInfo operation, SvnWcGeneration wcGeneration) throws SVNException {
        return wcGeneration == SvnWcGeneration.V16;
    }

    @Override
    protected Map<SVNURL, SVNMergeRangeList> run() throws SVNException {
        SVNDiffClient16 dc = new SVNDiffClient16(getOperation().getAuthenticationManager(), getOperation().getOptions());
        if (getOperation().getFirstTarget().isURL()) {
            return dc.doGetMergedMergeInfo(getOperation().getFirstTarget().getURL(), getOperation().getFirstTarget().getResolvedPegRevision());
        }
        return dc.doGetMergedMergeInfo(getOperation().getFirstTarget().getFile(), getOperation().getFirstTarget().getResolvedPegRevision());
    }

}
