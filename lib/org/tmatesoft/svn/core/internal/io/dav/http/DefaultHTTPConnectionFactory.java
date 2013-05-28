/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.io.dav.http;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;


/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class DefaultHTTPConnectionFactory implements IHTTPConnectionFactory {
    
    private File mySpoolDirectory;
    private String myHTTPCharset;
    private boolean myIsSpoolAll;
    private boolean myIsUseSendAll;
    
    public DefaultHTTPConnectionFactory(File spoolDirectory, boolean spoolAll, String httpCharset) {
        this(spoolDirectory, spoolAll, false, httpCharset);
    }

    public DefaultHTTPConnectionFactory(File spoolDirectory, boolean spoolAll, boolean useSendAllForDiff, String httpCharset) {
        mySpoolDirectory = spoolDirectory;
        myIsSpoolAll = spoolAll;
        myHTTPCharset = httpCharset;
        myIsUseSendAll = useSendAllForDiff;
    }

    public IHTTPConnection createHTTPConnection(SVNRepository repository) throws SVNException {
        String charset = myHTTPCharset != null ? myHTTPCharset : System.getProperty("svnkit.http.encoding", "UTF-8");
        File spoolLocation = mySpoolDirectory;
        if (mySpoolDirectory != null && !mySpoolDirectory.isDirectory()) {
            spoolLocation = null;
        }
        return new HTTPConnection(repository, charset, spoolLocation, myIsSpoolAll);

//        return new HttpConnection(repository, charset, spoolLocation, myIsSpoolAll);
    }

    public boolean useSendAllForDiff(SVNRepository repository) throws SVNException {
        return myIsUseSendAll;
    }

}
