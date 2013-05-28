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

package org.tmatesoft.svn.core;

/**
 * An exception class that is used to signal about the fact that errors
 * occured exactly during an authentication try. Provides the same kind 
 * of information as its base class does.
 *   
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see		SVNException
 */
public class SVNAuthenticationException extends SVNException {

    private static final long serialVersionUID = 4845L;

    /**
     * Creates a new authentication exception given detailed error 
     * information and the original cause.
     * 
     * @param errorMessage an error message
     * @param cause        an original cause
     */
    public SVNAuthenticationException(SVNErrorMessage errorMessage, Throwable cause) {
        super(errorMessage, cause != null ? cause : errorMessage.getCause());
    }

    /**
     * Creates a new authentication exception given detailed error 
     * information.
     * 
     * @param errorMessage an error message
     */
    public SVNAuthenticationException(SVNErrorMessage errorMessage) {
        super(errorMessage, errorMessage.getCause());
    }
}
