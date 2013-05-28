package org.tmatesoft.svn.core.wc2;

import java.io.File;

/**
 * Interface describes the parameters defining behavior for the commit operation 
 * that touches still versioned files or directories that are somehow missing.  
 * 
 * <p>
 * To bring your commit parameters into usage, simply pass them to 
 * a committer object, for example, to 
 * {@link SvnCommit#setCommitParameters(ISvnCommitParameters) SvnCommit}. 
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public interface ISvnCommitParameters {
    
	/**
	 * Describes an instruction that operation should take if it meets unversioned or missing item.
	 * This can be:
	 * <ul>
	 * <li>DELETE - operation should force a deletion of the item. Although the item may be not 
     * scheduled for deletion (only missing in filesystem) it will 
     * be deleted from version control.
	 * <li>ERROR - Commit should fail and error should be reported.
	 * <li>SKIP - Item should not be committed.
	 * </ul>
	 * 
	 * @author TMate Software Ltd.
	 * @version 1.7
	 */
	public enum Action {
        DELETE,
        ERROR,
        SKIP,
    }
    
    public Action onMissingFile(File file);

    /**
     * Returns the action a commit operation should undertake 
     * if there's a missing directory under commit scope that is not 
     * however scheduled for deletion.    
     * 
     * @param  file a missing directory
     * @return      an action that must be one of 
     *              the constants defined in the interface 
     */
    public Action onMissingDirectory(File file);
    
    /**
     * Instructs whether to remove the local <code>directory</code> after commit or not.
     *    
     * @param directory  working copy directory
     * @return           <code>true</code> if directory should be deleted after commit
     */
    public boolean onDirectoryDeletion(File directory);
    
    /**
     * Instructs whether to remove the local <code>file</code> after commit or not.
     * 
     * @param file  working copy file 
     * @return      <code>true</code> if file should be deleted after commit
     */
    public boolean onFileDeletion(File file);
}
