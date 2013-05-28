package org.tmatesoft.svn.core.internal.wc2.old;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNChangelistClient16;
import org.tmatesoft.svn.core.wc2.SvnSetChangelist;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnOldSetChangelist extends SvnOldRunner<Void, SvnSetChangelist> {

    @Override
    protected Void run() throws SVNException {
        
    	SVNChangelistClient16 client = new SVNChangelistClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        
        File[] paths = new File[getOperation().getTargets().size()];
        int i = 0;
        for (SvnTarget target : getOperation().getTargets()) {
            paths[i++] = target.getFile();
        }
        
        Collection<String> applicableChangelists = getOperation().getApplicableChangelists();
        if (applicableChangelists == null) {
            applicableChangelists = Collections.emptyList();
        }
        if (getOperation().isRemove()) {
        	client.doRemoveFromChangelist(paths, getOperation().getDepth(), applicableChangelists.toArray(new String[applicableChangelists.size()]));

        } else {
        	client.doAddToChangelist(paths, 
        			getOperation().getDepth(), 
                    getOperation().getChangelistName(), 
                    applicableChangelists.toArray(new String[applicableChangelists.size()]));

        }
        
        return null;
    }

}
