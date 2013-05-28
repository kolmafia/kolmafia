package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.util.Collection;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.wc2.SvnSetChangelist;
import org.tmatesoft.svn.core.wc2.SvnTarget;


public class SvnNgSetChangelist extends SvnNgOperationRunner<Void, SvnSetChangelist> {

    @Override
    protected Void run(SVNWCContext context) throws SVNException {
        
        Collection<String> applicableChangelists = getOperation().getApplicableChangelists();
        String[] listsNames = null;
        if (applicableChangelists != null) {
            listsNames = applicableChangelists.toArray(new String[applicableChangelists.size()]);
        }
    	for (SvnTarget target : getOperation().getTargets()) {
            checkCancelled();
            
            File path = target.getFile().getAbsoluteFile();
            
            context.getDb().opSetChangelist(path, getOperation().getChangelistName(), listsNames, getOperation().getDepth(), this);
        }
        return null;
        
    }
    
    

}
