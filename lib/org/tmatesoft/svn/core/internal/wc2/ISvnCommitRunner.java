package org.tmatesoft.svn.core.internal.wc2;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc2.SvnCommit;
import org.tmatesoft.svn.core.wc2.SvnCommitPacket;

public interface ISvnCommitRunner {
    
    public SvnCommitPacket collectCommitItems(SvnCommit operation) throws SVNException;

    public void disposeCommitPacket(Object lockingContext, boolean disposeParentContext) throws SVNException;
    
    public Object splitLockingContext(Object lockingContext, SvnCommitPacket newPacket);

}
