package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc16.SVNUpdateClient16;
import org.tmatesoft.svn.core.wc2.SvnRelocate;

public class SvnOldRelocate extends SvnOldRunner<SVNURL, SvnRelocate> {

    @Override
    protected SVNURL run() throws SVNException {        
        SVNUpdateClient16 client = new SVNUpdateClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setIgnoreExternals(getOperation().isIgnoreExternals());
        client.setEventHandler(getOperation().getEventHandler());

        client.doRelocate(getFirstTarget(), getOperation().getFromUrl(), getOperation().getToUrl(), getOperation().isRecursive());
        return getOperation().getToUrl();
    }
}
