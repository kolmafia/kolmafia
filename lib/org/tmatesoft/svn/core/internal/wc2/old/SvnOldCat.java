package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.wc16.SVNWCClient16;
import org.tmatesoft.svn.core.wc2.SvnCat;

public class SvnOldCat extends SvnOldRunner<SVNProperties, SvnCat> {
    @Override
    protected SVNProperties run() throws SVNException {
        
        SVNWCClient16 client = new SVNWCClient16(getOperation().getRepositoryPool(), getOperation().getOptions());

		final SVNProperties properties = new SVNProperties();

		if (getOperation().hasRemoteTargets()) {
			client.doGetFileContents(
            		getOperation().getFirstTarget().getURL(),
        			getOperation().getFirstTarget().getResolvedPegRevision(), 
        			getOperation().getRevision(),
        			getOperation().isExpandKeywords(),
					properties,
        			getOperation().getOutput());
        }
        else {
        	client.doGetFileContents(
            		getOperation().getFirstTarget().getFile(),
        			getOperation().getFirstTarget().getResolvedPegRevision(), 
        			getOperation().getRevision(),
        			getOperation().isExpandKeywords(),
					properties,
        			getOperation().getOutput());
        }
        
        return properties;
    }

}
