package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNWCClient16;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc.ISVNInfoHandler;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc2.SvnGetInfo;
import org.tmatesoft.svn.core.wc2.SvnInfo;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnOldGetInfo extends SvnOldRunner<SvnInfo, SvnGetInfo> implements ISVNInfoHandler {
    
    @Override
    protected SvnInfo run() throws SVNException {        
        SVNWCClient16 client = new SVNWCClient16(getOperation().getRepositoryPool(), getOperation().getOptions());        
        client.setEventHandler(getOperation().getEventHandler());
        client.doInfo(getFirstTarget(), 
                getOperation().getFirstTarget().getResolvedPegRevision(), 
                getOperation().getRevision(), 
                getOperation().getDepth(), 
                getOperation().getApplicableChangelists(), 
                this);
        
        return getOperation().first();
    }

    public void handleInfo(SVNInfo info) throws SVNException {
        SvnInfo info2 = SvnCodec.info(info);
        getOperation().receive(SvnTarget.fromFile(info.getFile()), info2);
    }

}
