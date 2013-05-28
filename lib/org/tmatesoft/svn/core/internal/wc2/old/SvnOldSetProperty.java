package org.tmatesoft.svn.core.internal.wc2.old;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc16.SVNWCClient16;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc.ISVNPropertyHandler;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc2.SvnSetProperty;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnOldSetProperty extends SvnOldRunner<SVNPropertyData, SvnSetProperty> implements ISVNPropertyHandler {

    @Override
    protected SVNPropertyData run() throws SVNException {
        SVNWCClient16 client = new SVNWCClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        
        File path = getOperation().getFirstTarget().getFile();
        if (getOperation().getPropertyValueProvider() != null) {
            client.doSetProperty(path, SvnCodec.propertyValueProvider(getOperation().getPropertyValueProvider()), getOperation().isForce(), 
                    getOperation().getDepth(), this, getOperation().getApplicableChangelists());
        } else {
            String propName = getOperation().getPropertyName();
            SVNPropertyValue propertyValue = getOperation().getPropertyValue();
            client.doSetProperty(path, propName, propertyValue, getOperation().isForce(), 
                    getOperation().getDepth(), this, getOperation().getApplicableChangelists());
        }
        
        return getOperation().first();
    }

    public void handleProperty(File path, SVNPropertyData property) throws SVNException {
        getOperation().receive(SvnTarget.fromFile(path), property);
    }

    public void handleProperty(SVNURL url, SVNPropertyData property) throws SVNException {
    }

    public void handleProperty(long revision, SVNPropertyData property) throws SVNException {
    }

    @Override
    public boolean isApplicable(SvnSetProperty operation, SvnWcGeneration wcGeneration) throws SVNException {
        return !operation.isRevisionProperty() && super.isApplicable(operation, wcGeneration);
    }

}
