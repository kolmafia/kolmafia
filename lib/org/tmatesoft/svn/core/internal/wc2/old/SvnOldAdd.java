package org.tmatesoft.svn.core.internal.wc2.old;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNWCClient16;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc2.SvnScheduleForAddition;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnOldAdd extends SvnOldRunner<Void, SvnScheduleForAddition> {

    @Override
    protected Void run() throws SVNException {
        
        SVNWCClient16 client = new SVNWCClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        
        File[] paths = new File[getOperation().getTargets().size()];
        int i = 0;
        for (SvnTarget target : getOperation().getTargets()) {
            paths[i++] = target.getFile();
        }
        
        client.setAddParameters(SvnCodec.addParameters(getOperation().getAddParameters()));
        client.doAdd(paths, 
                getOperation().isForce(), 
                getOperation().isMkDir(), 
                false, 
                getOperation().getDepth(), 
                false, 
                getOperation().isIncludeIgnored(), 
                getOperation().isAddParents());

        return null;
    }

}
