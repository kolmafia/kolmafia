package org.tmatesoft.svn.core.wc2;

import java.io.File;

/**
 * Represents callback that invokes when inconsistent EOLs are found in text files being scheduled for addition.
 * 
 * <p>
 * In other words, if a text file is scheduled for addition and an autoproperty 
 * {@link org.tmatesoft.svn.core.SVNProperty#EOL_STYLE} is set on a file that will cause an exception 
 * on files with inconsistent EOLs. In this case if the caller has provided his <code>ISvnAddParameters</code> 
 * its method <code>onInconsistentEOLs(File file)</code> will be called for that file. This method returns <code>Action</code> value. 
 * According to the return value the file may be added as-is, as 
 * binary or addition may be cancelled and an exception may be thrown indicating an error.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 * @since   1.2
 * @see SvnScheduleForAddition
 */
public interface ISvnAddParameters {
   
	/**
	 * Describes an action add operation should undertake in case of a inconsistent EOLs. 
     * This can be:
	 * <ul>
	 * <li>ADD_AS_BINARY - Rules to add a file as binary.
	 * <li>ADD_AS_IS - Rules to add a file ad is.
	 * <li>REPORT_ERROR - Rules not to add file but to report an error, i.e. throw an exception
	 * </ul>
	 * 
	 * @author TMate Software Ltd.
	 * @version 1.7
	 */
    public enum Action {
        ADD_AS_BINARY,
        ADD_AS_IS,
        REPORT_ERROR,
    }

    /**
     * Default add parameters, <code>action</code> equals to <code>Action.REPORT_ERROR</code>
     */
    ISvnAddParameters DEFAULT = new ISvnAddParameters() {
        public Action onInconsistentEOLs(File file) {
            return Action.REPORT_ERROR;
        }
    };
    
    /**
     * Receives a file with inconsistent EOLs and returns an action which should be 
     * performed against this file. It should be one of the three constant values 
     * predefined in this interface.   
     * 
     * @param  file   file path   
     * @return        action to perform on the given file
     */
    public Action onInconsistentEOLs(File file);

}
