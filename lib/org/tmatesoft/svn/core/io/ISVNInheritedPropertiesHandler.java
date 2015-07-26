package org.tmatesoft.svn.core.io;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;

public interface ISVNInheritedPropertiesHandler {
    
    public void handleInheritedProperites(String inheritedFromPath, SVNProperties properties) throws SVNException;

}
