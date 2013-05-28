package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.SVNExternalsStore;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.wc2.SvnCanonicalizeUrls;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnNgCanonicalizeUrls extends SvnNgOperationRunner<Void, SvnCanonicalizeUrls> {

    @Override
    protected Void run(SVNWCContext context) throws SVNException {
        File lockRootPath = null;
        SVNExternalsStore store = getOperation().isIgnoreExternals() ? null : new SVNExternalsStore();
        try {
            lockRootPath = context.acquireWriteLock(getFirstTarget(), false, true);
            context.canonicalizeURLs(getFirstTarget(), store, getOperation().isOmitDefaultPort());
        } finally {
            if (lockRootPath != null) {
                context.releaseWriteLock(lockRootPath);
            }

        }

        if (!getOperation().isIgnoreExternals()) {
            for (File path : store.getNewExternals().keySet()) {
                String externalPropertyValue = store.getNewExternals().get(path);
                SVNExternal[] externals = SVNExternal.parseExternals(path, externalPropertyValue);
                for (int i = 0; i < externals.length; i++) {
                    File externalPath = SVNFileUtil.createFilePath(path, externals[i].getPath());
                    if (externalPath.isDirectory()) {
                        SvnCanonicalizeUrls canonicalize = getOperation().getOperationFactory().createCanonicalizeUrls();
                        canonicalize.setSingleTarget(SvnTarget.fromFile(externalPath));
                        canonicalize.setOmitDefaultPort(getOperation().isOmitDefaultPort());
                        canonicalize.run();
                    }
                }
            }
        }
        return null;
    }

}
