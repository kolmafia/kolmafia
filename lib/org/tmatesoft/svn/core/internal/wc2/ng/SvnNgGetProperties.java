package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnGetProperties;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnNgGetProperties extends SvnNgOperationRunner<SVNProperties, SvnGetProperties> {
    
    @Override
    protected SVNProperties run(SVNWCContext context) throws SVNException {
        for (SvnTarget target : getOperation().getTargets()) {
            if (target.isFile()) {
                run(context, target.getFile());
            }
        }
        return getOperation().first();
    }

    protected SVNProperties run(SVNWCContext context, File target) throws SVNException {
        boolean pristine = getOperation().getRevision() == SVNRevision.COMMITTED || getOperation().getRevision() == SVNRevision.BASE;
        SVNNodeKind kind = context.readKind(target, false);
        
        if (kind == SVNNodeKind.UNKNOWN || kind == SVNNodeKind.NONE) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNVERSIONED_RESOURCE, "''{0}'' is not under version control", target);
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        if (kind == SVNNodeKind.DIR) {
            if (getOperation().getDepth() == SVNDepth.EMPTY) {
                if (!matchesChangelist(target)) {
                    return getOperation().first();
                }
                SVNProperties properties = null;
                if (pristine) {
                    properties = context.getDb().readPristineProperties(target);
                } else {
                    properties = context.getDb().readProperties(target);
                }
                if (properties != null && !properties.isEmpty()) {
                    getOperation().receive(SvnTarget.fromFile(target), properties);
                }
            } else {
                SVNWCDb db = (SVNWCDb) context.getDb();
                db.readPropertiesRecursively(
                        target, 
                        getOperation().getDepth(), 
                        false, 
                        pristine, 
                        getOperation().getApplicableChangelists(), 
                        getOperation());
            }
        } else {
            SVNProperties properties = null;
            if (pristine) {
                properties = context.getDb().readPristineProperties(target);
            } else {
                if (!context.isNodeStatusDeleted(target)) {
                    properties = context.getDb().readProperties(target);
                }
            }
            if (properties != null && !properties.isEmpty()) {
                getOperation().receive(SvnTarget.fromFile(target), properties);
            }
        }        
        return getOperation().first();
    }

    @Override
    public boolean isApplicable(SvnGetProperties operation, SvnWcGeneration wcGeneration) throws SVNException {
        return !operation.isRevisionProperties() && super.isApplicable(operation, wcGeneration);
    }

}
