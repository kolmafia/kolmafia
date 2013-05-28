package org.tmatesoft.svn.core.internal.wc2.ng;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.io.SVNLocationEntry;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnSuggestMergeSources;

public class SvnNgSuggestMergeSources extends SvnNgOperationRunner<Collection<SVNURL>, SvnSuggestMergeSources> {

    @Override
    public boolean isApplicable(SvnSuggestMergeSources operation, SvnWcGeneration wcGeneration) throws SVNException {
        return operation.getFirstTarget().isURL() 
                || SvnOperationFactory.detectWcGeneration(operation.getFirstTarget().getFile(), true) == SvnWcGeneration.V17;
    }

    @Override
    public SvnWcGeneration getWcGeneration() {
        return SvnWcGeneration.NOT_DETECTED;
    }

    @Override
    protected Collection<SVNURL> run(SVNWCContext context) throws SVNException {
        SVNURL[] root = new SVNURL[1];
        Map<String, Map<String, SVNMergeRangeList>> mergeInfoCatalog = SvnNgMergeinfoUtil.getMergeInfo(context, getRepositoryAccess(), getOperation().getFirstTarget(), false, false, root);
        Map<String, SVNMergeRangeList> mergeInfo = null;
        List<SVNURL> suggestions = new ArrayList<SVNURL>();
        
        if (mergeInfoCatalog != null && !mergeInfoCatalog.isEmpty()) {
            mergeInfo = mergeInfoCatalog.get(mergeInfoCatalog.keySet().iterator().next());
        }
        SVNLocationEntry copySource = getRepositoryAccess().getCopySource(getOperation().getFirstTarget(), getOperation().getFirstTarget().getPegRevision());
        if (copySource != null && copySource.getPath() != null) {
            suggestions.add(root[0].appendPath(copySource.getPath(), false));
        }
        if (mergeInfo != null) {
            for (String path : mergeInfo.keySet()) {
                SVNURL url = root[0].appendPath(path, false);
                if (!suggestions.contains(url)) {
                    suggestions.add(url);
                }
            }
        }
        return suggestions;
    }

}
