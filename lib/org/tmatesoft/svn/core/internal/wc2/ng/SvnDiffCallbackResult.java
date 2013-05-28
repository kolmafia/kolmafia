package org.tmatesoft.svn.core.internal.wc2.ng;

import org.tmatesoft.svn.core.wc.SVNStatusType;

public class
        SvnDiffCallbackResult {
    
    public SVNStatusType contentState;
    public SVNStatusType propState;
    public boolean skip;
    public boolean skipChildren;
    public boolean treeConflicted;
    
    public SvnDiffCallbackResult reset() {
        contentState = null;
        propState = null;
        skip = false;
        skipChildren = false;
        treeConflicted = false;
        
        return this;
    }

}
