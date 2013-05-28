package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.wc2.SvnUpgrade;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgUpgrade extends SvnNgOperationRunner<SvnWcGeneration, SvnUpgrade> {

    @Override
    protected SvnWcGeneration run(SVNWCContext context) throws SVNException {
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.WC_INVALID_OP_ON_CWD, "Can''t upgrade ''{0}'' as it is not a pre-1.7 working copy directory", 
        		getOperation().getFirstTarget().getFile().getAbsolutePath());
		SVNErrorManager.error(err, SVNLogType.WC);
		return SvnWcGeneration.V17;
    }

}
