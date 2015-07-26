package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.SVNProperties;

public interface ISvnPropertiesDiffHandler {
    void handlePropertiesDiff(SVNProperties originalProperties, SVNProperties propChanges);
}
