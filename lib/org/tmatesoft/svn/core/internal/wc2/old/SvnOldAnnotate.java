package org.tmatesoft.svn.core.internal.wc2.old;

import java.io.File;
import java.util.Date;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc16.SVNLogClient16;
import org.tmatesoft.svn.core.wc.ISVNAnnotateHandler;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnAnnotate;
import org.tmatesoft.svn.core.wc2.SvnAnnotateItem;
import org.tmatesoft.svn.util.SVNLogType;

public class SvnOldAnnotate extends SvnOldRunner<SvnAnnotateItem, SvnAnnotate> implements ISVNAnnotateHandler { 
	
	@Override
    protected SvnAnnotateItem run() throws SVNException {
		if (getOperation().getEndRevision() == SVNRevision.UNDEFINED) {
			getOperation().setEndRevision(SVNRevision.BASE);
	    }
		if (getOperation().getStartRevision() == null || !getOperation().getStartRevision().isValid()) {
    		getOperation().setStartRevision(SVNRevision.create(1));
        }
        if (getOperation().getEndRevision() == null || !getOperation().getEndRevision().isValid()) {
            getOperation().setEndRevision(getOperation().getFirstTarget().getResolvedPegRevision());
        }
        if (getOperation().getStartRevision() == SVNRevision.WORKING || getOperation().getEndRevision() == SVNRevision.WORKING) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Blame of the WORKING revision is not supported");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
		
		SVNLogClient16 client = new SVNLogClient16(getOperation().getRepositoryPool(), getOperation().getOptions());
		client.setDiffOptions(getOperation().getDiffOptions());
		client.setEventHandler(getOperation().getEventHandler());
                
        if (getOperation().hasRemoteTargets()) {
        	client.doAnnotate(getOperation().getFirstTarget().getURL(), getOperation().getFirstTarget().getResolvedPegRevision(), 
        			getOperation().getStartRevision(), getOperation().getEndRevision(), getOperation().isIgnoreMimeType(), 
        			getOperation().isUseMergeHistory(), this, getOperation().getInputEncoding());   
        }
        else {
        	client.doAnnotate(getOperation().getFirstTarget().getFile(), getOperation().getFirstTarget().getResolvedPegRevision(), 
        			getOperation().getStartRevision(), getOperation().getEndRevision(), getOperation().isIgnoreMimeType(), 
        			getOperation().isUseMergeHistory(), this, getOperation().getInputEncoding());        
        }
        
        return getOperation().first();
    }
    
    public void handleLine(Date date, long revision, String author, String line, Date mergedDate, 
            long mergedRevision, String mergedAuthor, String mergedPath, int lineNumber) throws SVNException {
    	getOperation().receive(getOperation().getFirstTarget(), 
    			new SvnAnnotateItem(date, revision, author, line, mergedDate, mergedRevision, mergedAuthor, mergedPath, lineNumber)
    			);
    }
    
    public boolean handleRevision(Date date, long revision, String author, File contents) throws SVNException{
    	SvnAnnotateItem item = new SvnAnnotateItem(date, revision, author, contents);
    	getOperation().receive(getOperation().getFirstTarget(), item);
    	return item.getReturnResult();
    }
    
    public void handleLine(Date date, long revision, String author, String line) throws SVNException {
    }
    
    public void handleEOF() {
    	try {
			getOperation().receive(getOperation().getFirstTarget(), new SvnAnnotateItem(true));
		} catch (SVNException e) {
			
		}
    }
}
