package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;

public interface ISvnDiffCallback {
    
   
    
    public void fileOpened(SvnDiffCallbackResult result, File path, long revision) throws SVNException;
    
    public void fileChanged(SvnDiffCallbackResult result, 
            File path, File leftFile, File rightFile, long rev1, long rev2, String mimeType1, String mimeType2,
            SVNProperties propChanges, SVNProperties originalProperties) throws SVNException;
    
    public void fileAdded(SvnDiffCallbackResult result,  
            File path, File leftFile, File rightFile, long rev1, long rev2, String mimeType1, String mimeType2,
            File copyFromPath, long copyFromRevision, SVNProperties propChanges, SVNProperties originalProperties) throws SVNException;
    
    public void fileDeleted(SvnDiffCallbackResult result,  
            File path, File leftFile, File rightFile, String mimeType1, String mimeType2,
            SVNProperties originalProperties) throws SVNException;


    public void dirDeleted(SvnDiffCallbackResult result, File path) throws SVNException;
    
    public void dirOpened(SvnDiffCallbackResult result, File path, long revision) throws SVNException;
    
    public void dirAdded(SvnDiffCallbackResult result, File path, long revision, String copyFromPath, long copyFromRevision) throws SVNException;
    
    public void dirPropsChanged(SvnDiffCallbackResult result, File path, boolean isAdded, SVNProperties propChanges, SVNProperties originalProperties) throws SVNException;
    
    public void dirClosed(SvnDiffCallbackResult result, File path, boolean isAdded) throws SVNException;
}
