package org.tmatesoft.svn.core.wc2.hooks;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;

public interface ISvnFileFilter {

    public boolean accept(File file) throws SVNException;
}
