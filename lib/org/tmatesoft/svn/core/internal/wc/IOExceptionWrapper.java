package org.tmatesoft.svn.core.internal.wc;

import java.io.IOException;

import org.tmatesoft.svn.core.SVNException;


public class IOExceptionWrapper extends IOException {
    
    private static final long serialVersionUID = 4845L;
    
    private SVNException myOriginalException;
    
    public IOExceptionWrapper(SVNException cause) {
        myOriginalException = cause;
    }

    public SVNException getOriginalException() {
        return myOriginalException;
    }
    
}
