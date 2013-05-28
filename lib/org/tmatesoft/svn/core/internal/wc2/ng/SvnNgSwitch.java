package org.tmatesoft.svn.core.internal.wc2.ng;


import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.wc2.SvnSwitch;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnNgSwitch extends SvnNgAbstractUpdate<Long, SvnSwitch> {

    @Override
    protected Long run(SVNWCContext context) throws SVNException {
        SvnTarget switchTarget = getOperation().getSwitchTarget();
        return doSwitch(getFirstTarget(), switchTarget.getURL(), getOperation().getRevision(), 
                switchTarget.getResolvedPegRevision(),
                getOperation().getDepth(), getOperation().isDepthIsSticky(), getOperation().isIgnoreExternals(),
                getOperation().isAllowUnversionedObstructions(), getOperation().isIgnoreAncestry(), getOperation().isSleepForTimestamp());
    }


}
