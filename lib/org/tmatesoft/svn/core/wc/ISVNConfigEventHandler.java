package org.tmatesoft.svn.core.wc;

import org.tmatesoft.svn.core.internal.wc.SVNCompositeConfigFile;

public interface ISVNConfigEventHandler {
    void onLoad(SVNCompositeConfigFile configFile, SVNCompositeConfigFile serversFile);
}
