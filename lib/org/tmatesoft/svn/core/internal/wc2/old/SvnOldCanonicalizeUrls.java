package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNUpdateClient16;
import org.tmatesoft.svn.core.wc2.SvnCanonicalizeUrls;

public class SvnOldCanonicalizeUrls extends SvnOldRunner<Void, SvnCanonicalizeUrls> {

    @Override
    protected Void run() throws SVNException {
        SVNUpdateClient16 client = new SVNUpdateClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setIgnoreExternals(getOperation().isIgnoreExternals());
        client.setEventHandler(getOperation().getEventHandler());
        
        client.doCanonicalizeURLs(getOperation().getFirstTarget().getFile(), getOperation().isOmitDefaultPort(), getOperation().getDepth().isRecursive());
        
        return null;
    }

}
