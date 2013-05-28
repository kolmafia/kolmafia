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
package org.tmatesoft.svn.core.internal.io.dav.http;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public interface IHTTPConnectionFactory {
    
    public IHTTPConnectionFactory DEFAULT = new IHTTPConnectionFactory() {

        public IHTTPConnection createHTTPConnection(SVNRepository repository) throws SVNException {
            String charset = System.getProperty("svnkit.http.encoding", "UTF-8");
            return new HTTPConnection(repository, charset, null, false);
//            return new HttpConnection(repository, charset, null, false);
        }

        public boolean useSendAllForDiff(SVNRepository repository) throws SVNException {
            return false;
        }
        
    };
    
    public IHTTPConnection createHTTPConnection(SVNRepository repository) throws SVNException;
    
    public boolean useSendAllForDiff(SVNRepository repository) throws SVNException;

}
