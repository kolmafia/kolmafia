package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.admin.ISVNAdminAreaFactorySelector;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminAreaFactory;
import org.tmatesoft.svn.core.internal.wc16.SVNUpdateClient16;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc2.SvnCheckout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SvnOldCheckout extends SvnOldRunner<Long, SvnCheckout> {
    @Override
    public boolean isApplicable(SvnCheckout operation, SvnWcGeneration wcGeneration) throws SVNException {
        final int targetWorkingCopyFormat = operation.getTargetWorkingCopyFormat();
        if (targetWorkingCopyFormat > 0) {
            return targetWorkingCopyFormat < SVNWCContext.WC_NG_VERSION;
        }
        return super.isApplicable(operation, wcGeneration);
    }

    @Override
    protected Long run() throws SVNException {
        final int targetWorkingCopyFormat = getOperation().getTargetWorkingCopyFormat();
        final ISVNAdminAreaFactorySelector oldSelector = SVNAdminAreaFactory.getSelector();
        try {
        if (targetWorkingCopyFormat > 0) {
            SVNAdminAreaFactory.setSelector(new ISVNAdminAreaFactorySelector() {
                public Collection getEnabledFactories(File path, Collection factories, boolean writeAccess) throws SVNException {
                    List<SVNAdminAreaFactory> adminAreaFactories = new ArrayList<SVNAdminAreaFactory>(factories);
                    int index = findFactoryByFormat(adminAreaFactories, targetWorkingCopyFormat);
                    if (index > 0) {
                        //move the factory to the start of the list
                        SVNAdminAreaFactory adminAreaFactory = adminAreaFactories.get(index);
                        adminAreaFactories.remove(index);
                        adminAreaFactories.add(0, adminAreaFactory);
                    }
                    return adminAreaFactories;
                }

                private int findFactoryByFormat(List<SVNAdminAreaFactory> factories, int workingCopyFormat) {
                    for (int i = 0; i < factories.size(); i++) {
                        final SVNAdminAreaFactory factory = factories.get(i);
                        if (factory.getSupportedVersion() == workingCopyFormat) {
                            return i;
                        }
                    }
                    return -1;
                }
            });
        }

        SVNUpdateClient16 client = new SVNUpdateClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        
        client.setIgnoreExternals(getOperation().isIgnoreExternals());
        client.setUpdateLocksOnDemand(getOperation().isUpdateLocksOnDemand());
        client.setEventHandler(getOperation().getEventHandler());
        client.setExternalsHandler(SvnCodec.externalsHandler(getOperation().getExternalsHandler()));

        return client.doCheckout(getOperation().getSource().getURL(), 
                getFirstTarget(), 
                getOperation().getSource().getResolvedPegRevision(), 
                getOperation().getRevision(), 
                getOperation().getDepth(), 
                getOperation().isAllowUnversionedObstructions());
        } finally {
            if (targetWorkingCopyFormat > 0) {
                SVNAdminAreaFactory.setSelector(oldSelector);
            }
        }
    }
}
