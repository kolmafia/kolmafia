package org.tmatesoft.svn.core.wc2.hooks;

import java.io.File;
import java.util.Map;

import org.tmatesoft.svn.core.wc2.SvnList;

/**
 * Implementing this interface allows to providing file list for directory.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 * @see SvnList
 */
public interface ISvnFileListHook {
    
	/**
	 * Returns <code>Map</code> of file names with the corresponding <code>File</code> objects
	 * containing files in <code>parent</code>
	 * 
	 * @param parent parent directory name
	 * @return map of all files in directory with their names
	 */
    public Map<String, File> listFiles(File parent);

}
