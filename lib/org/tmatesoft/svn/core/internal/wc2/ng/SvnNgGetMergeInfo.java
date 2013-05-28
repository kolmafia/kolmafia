package org.tmatesoft.svn.core.internal.wc2.ng;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.wc2.SvnGetMergeInfo;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnNgGetMergeInfo extends SvnNgOperationRunner<Map<SVNURL, SVNMergeRangeList>, SvnGetMergeInfo> {
    
    @Override
    public boolean isApplicable(SvnGetMergeInfo operation, SvnWcGeneration wcGeneration) throws SVNException {
        return operation.getFirstTarget().isURL() || 
                SvnOperationFactory.detectWcGeneration(operation.getFirstTarget().getFile(), true) == SvnWcGeneration.V17;
    }

    @Override
    public SvnWcGeneration getWcGeneration() {
        return SvnWcGeneration.NOT_DETECTED;
    }

    @Override
    protected Map<SVNURL, SVNMergeRangeList> run(SVNWCContext context) throws SVNException {
        SVNURL[] root = new SVNURL[1];
        Map<String, Map<String, SVNMergeRangeList>> catalog = SvnNgMergeinfoUtil.getMergeInfo(getWcContext(), getRepositoryAccess(), getOperation().getFirstTarget(), false, false, root);
        Map<String, SVNMergeRangeList> mergeinfo = null;
        if (catalog != null) {
            String relativePath;
            if (getOperation().getFirstTarget().isURL()) {
                relativePath = SVNURLUtil.getRelativeURL(root[0], getOperation().getFirstTarget().getURL(), false);
            } else {
                relativePath = SVNFileUtil.getFilePath(getWcContext().getNodeReposRelPath(getFirstTarget()));
            }
            mergeinfo = catalog.get(relativePath);
        }
        if (mergeinfo != null) {
            Map<SVNURL, SVNMergeRangeList> result = new TreeMap<SVNURL, SVNMergeRangeList>(new Comparator<SVNURL>() {
                public int compare(SVNURL o1, SVNURL o2) {
                    return o1.toString().compareTo(o2.toString());
                }
            });
            for (String path : mergeinfo.keySet()) {
                SVNURL fullURL = root[0].appendPath(path, false);
                result.put(fullURL, mergeinfo.get(path));
            }
            return result;
        }
        return null;
    }

}
