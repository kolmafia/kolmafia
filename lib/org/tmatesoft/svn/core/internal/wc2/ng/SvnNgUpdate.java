package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.Arrays;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUpdate;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgUpdate extends SvnNgAbstractUpdate<long[], SvnUpdate> {

    @Override
    protected long[] run(SVNWCContext context) throws SVNException {
        long[] result = new long[getOperation().getTargets().size()];
        File[] targets = new File[getOperation().getTargets().size()];
        Arrays.fill(result, SVNWCContext.INVALID_REVNUM);
        
        int j = 0;
        for (SvnTarget target : getOperation().getTargets()) {
            if (target.isURL()) {
                SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "''{0}'' is not a local path");
                SVNErrorManager.error(err, SVNLogType.WC);
            }
            targets[j++] = target.getFile();
        }
        
        for (int i = 0; i < targets.length; i++) {
            checkCancelled();
            try {
                result[i] = update(getWcContext(), targets[i], getOperation().getRevision(), getOperation().getDepth(), getOperation().isDepthIsSticky(), 
                        getOperation().isIgnoreExternals(), getOperation().isAllowUnversionedObstructions(), 
                        getOperation().isTreatAddsAsModifications(), getOperation().isMakeParents(), false, false);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() != SVNErrorCode.WC_NOT_WORKING_COPY) {
                    throw e;
                }
                handleEvent(SVNEventFactory.createSVNEvent(targets[i], SVNNodeKind.NONE, null, -1, SVNEventAction.SKIP, null, null, null));
                handleEvent(SVNEventFactory.createSVNEvent(targets[i], SVNNodeKind.NONE, null, -1, SVNEventAction.UPDATE_COMPLETED, null, null, null));
            }
        }
        sleepForTimestamp();
        return result;
    }

}
