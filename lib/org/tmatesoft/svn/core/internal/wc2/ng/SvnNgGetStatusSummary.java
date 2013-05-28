package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SvnWcDbReader;
import org.tmatesoft.svn.core.wc2.SvnGetStatusSummary;
import org.tmatesoft.svn.core.wc2.SvnStatusSummary;

public class SvnNgGetStatusSummary extends SvnNgOperationRunner<SvnStatusSummary, SvnGetStatusSummary> {

    @Override
    protected SvnStatusSummary run(SVNWCContext context) throws SVNException {
        
        long[] minmax = SvnWcDbReader.getMinAndMaxRevisions((SVNWCDb) context.getDb(), getFirstTarget());
        boolean sparse = SvnWcDbReader.isSparseCheckout((SVNWCDb) context.getDb(), getFirstTarget());
        boolean switched = SvnWcDbReader.hasSwitchedSubtrees((SVNWCDb) context.getDb(), getFirstTarget());
        boolean modified = SvnWcDbReader.hasLocalModifications(context, getFirstTarget());
        
        SvnStatusSummary summary = new SvnStatusSummary();
        summary.setModified(modified);
        summary.setSwitched(switched);
        summary.setSparseCheckout(sparse);
        summary.setMinRevision(getOperation().isCommitted() ? minmax[2] : minmax[0]);
        summary.setMaxRevision(getOperation().isCommitted() ? minmax[3] : minmax[1]);
        
        return summary;
    }

}
