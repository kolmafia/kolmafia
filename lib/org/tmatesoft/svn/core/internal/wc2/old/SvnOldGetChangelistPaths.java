package org.tmatesoft.svn.core.internal.wc2.old;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc16.SVNChangelistClient16;
import org.tmatesoft.svn.core.wc.ISVNChangelistHandler;
import org.tmatesoft.svn.core.wc2.SvnGetChangelistPaths;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnOldGetChangelistPaths extends SvnOldRunner<String, SvnGetChangelistPaths> implements ISVNChangelistHandler{

    @Override
    protected String run() throws SVNException {
        
    	SVNChangelistClient16 client = new SVNChangelistClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
        client.setEventHandler(getOperation().getEventHandler());
        
        Collection<File> targets = new ArrayList<File>();
        for (SvnTarget target : getOperation().getTargets()) {
        	targets.add(target.getFile());
        }
        client.doGetChangeListPaths(getOperation().getApplicableChangelists(), targets, getOperation().getDepth(), this);
                
        return getOperation().first();
    }
    
    public void handle(File path, String changelistName) {
    	try {
    		getOperation().receive(SvnTarget.fromFile(path), changelistName);
    	} catch (SVNException e) {}
    }

}
