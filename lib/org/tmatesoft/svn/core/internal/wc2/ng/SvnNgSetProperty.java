package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc2.SvnSetProperty;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnNgSetProperty extends SvnNgOperationRunner<SVNPropertyData, SvnSetProperty> {
    
    @Override
    protected SVNPropertyData run(SVNWCContext context) throws SVNException {
        SvnNgPropertiesManager.checkPropertyName(getOperation().getPropertyName(), getOperation().getPropertyValue());
        
        for (SvnTarget target : getOperation().getTargets()) {
            File localAbsPath = target.getFile();
            SVNNodeKind kind = SVNNodeKind.NONE;
            try {
                kind = getWcContext().readKind(getFirstTarget(), false);
            } catch (SVNException e) {
                if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_PATH_NOT_FOUND) {
                    notifyNonExistentPath(localAbsPath);
                } else {
                    throw e;
                }
                
            }
            if (kind == SVNNodeKind.NONE || kind == SVNNodeKind.UNKNOWN) {
                notifyNonExistentPath(localAbsPath);
            }
            File lockedPath = context.acquireWriteLock(localAbsPath, false, true);
            try {                
                SvnNgPropertiesManager.setProperty(getWcContext(), 
                        localAbsPath, 
                        getOperation().getPropertyName(), 
                        getOperation().getPropertyValue(), 
                        getOperation().getPropertyValueProvider(), 
                        getOperation().getDepth(), 
                        getOperation().isForce(), 
                        getOperation().getEventHandler(),
                        getOperation(),
                        getOperation().getApplicableChangelists());
            } finally {
                context.releaseWriteLock(lockedPath);
            }
        }
        return getOperation().first();
    }

    private void notifyNonExistentPath(File localAbsPath) throws SVNException {
        SVNEvent event = SVNEventFactory.createSVNEvent(localAbsPath, SVNNodeKind.NONE, null, -1, 
                SVNEventAction.PATH_NONEXISTENT,
                SVNEventAction.PATH_NONEXISTENT,
                null, null, -1, -1);
        if (getOperation().getEventHandler() != null) {
            getOperation().getEventHandler().handleEvent(event, -1);
        }
    }

    @Override
    public boolean isApplicable(SvnSetProperty operation, SvnWcGeneration wcGeneration) throws SVNException {
        return !operation.isRevisionProperty() && super.isApplicable(operation, wcGeneration);
    }

}
