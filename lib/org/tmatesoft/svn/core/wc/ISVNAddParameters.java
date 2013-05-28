/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.wc;

import java.io.File;


/**
 * The <b>ISVNAddParameters</b> is an interface for a callback which is invoked 
 * when there are inconsistent EOLs found in text files which are being scheduled for addition.
 * 
 * <p>
 * In other words, if a text file is scheduled for addition and an autoproperty 
 * {@link org.tmatesoft.svn.core.SVNProperty#EOL_STYLE} is set on a file that will cause an exception 
 * on files with inconsistent EOLs. In this case if the caller has provided his <code>ISVNAddParameters</code> 
 * its method <code>onInconsistentEOLs(File file)</code> will be called for that file. This method returns one of
 * the three constants predefined in this interface. According to the return value the file may be added as-is, as 
 * binary or addition may be cancelled and an exception may be thrown indicating an error.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public interface ISVNAddParameters {

    /**
     * Rules to add a file as binary.
     */
    public static final Action ADD_AS_BINARY = new Action();

    /**
     * Rules to add a file ad is.
     */
    public static final Action ADD_AS_IS = new Action();
    
    /**
     * Rules not to add file but to report an error, i.e. throw an exception
     */
    public static final Action REPORT_ERROR = new Action();
    
    /**
     * Receives a file with inconsistent EOLs and returns an action which should be 
     * performed against this file. It should be one of the three constant values 
     * predefined in this interface.   
     * 
     * @param  file   file path   
     * @return        action to perform on the given file
     *
     */
    public Action onInconsistentEOLs(File file);

    /**
     * This class is simply used to define an action add 
     * operation should undertake in case of a inconsistent EOLs. 
     * 
     * @version 1.3
     * @author  TMate Software Ltd.
     * @since   1.2
     */
    public static class Action {
        private Action() {
        }
    }
}
