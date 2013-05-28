package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.wc2.SvnResolve;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgResolve extends SvnNgOperationRunner<Void, SvnResolve>  {

    @Override
    protected Void run(SVNWCContext context) throws SVNException {
    	if (getOperation().getFirstTarget().isURL()) {
        	SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET,
        			"'{0}' is not a local path", getOperation().getFirstTarget().getURL());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        context.resolvedConflict(getFirstTarget(), getOperation().getDepth(), true, null, true, getOperation().getConflictChoice());
    	return null;
    }
    
   
 
}
