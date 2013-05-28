package org.tmatesoft.svn.core.internal.wc2.old;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc16.SVNWCClient16;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.wc.ISVNPropertyHandler;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc2.SvnGetProperties;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnOldGetProperties extends SvnOldRunner<SVNProperties, SvnGetProperties> implements ISVNPropertyHandler {
    
    private File currentFile;
    private SVNProperties currentProperties;
    
    @Override
    public void reset(SvnWcGeneration wcGeneration) {
        currentFile = null;
        currentProperties = null;
        super.reset(wcGeneration);
    }

    @Override
    public boolean isApplicable(SvnGetProperties operation, SvnWcGeneration wcGeneration) throws SVNException {
        return !operation.isRevisionProperties() && super.isApplicable(operation, wcGeneration);
    }

    @Override
    protected SVNProperties run() throws SVNException {        
        SVNWCClient16 client = new SVNWCClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        client.doGetProperty(
                getFirstTarget(), 
                null, 
                getOperation().getFirstTarget().getResolvedPegRevision(), 
                getOperation().getRevision(), 
                getOperation().getDepth(), 
                this, 
                getOperation().getApplicableChangelists());
        
        if (currentFile != null && currentProperties != null && !currentProperties.isEmpty()) {
            getOperation().receive(SvnTarget.fromFile(currentFile), currentProperties);
        }
        return getOperation().first();
    }
    
    public void handleProperty(File path, SVNPropertyData property) throws SVNException {
        if (currentProperties == null) {
            currentProperties = new SVNProperties();
        }
        if (currentFile == null) {
            currentFile = path;
        }
        if (!currentFile.equals(path)) {
            if (!currentProperties.isEmpty()) {
                getOperation().receive(SvnTarget.fromFile(currentFile), currentProperties);
                currentProperties.clear();
            }
            currentFile = path;
        }
        currentProperties.put(property.getName(), property.getValue());        
    }

    public void handleProperty(SVNURL url, SVNPropertyData property) throws SVNException {
    }
    public void handleProperty(long revision, SVNPropertyData property) throws SVNException {
    }

}
