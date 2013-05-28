package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNWCClient16;
import org.tmatesoft.svn.core.wc2.SvnMarkReplaced;

public class SvnOldMarkReplaced extends SvnOldRunner<Void, SvnMarkReplaced> {

    @Override
    protected Void run() throws SVNException {
        final SVNWCClient16 client = new SVNWCClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.doMarkReplaced(getFirstTarget());
        return null;
    }
}
