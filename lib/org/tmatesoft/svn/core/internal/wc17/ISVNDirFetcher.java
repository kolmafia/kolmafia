package org.tmatesoft.svn.core.internal.wc17;

import java.io.File;
import java.util.Map;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

public interface ISVNDirFetcher {
    public Map<String, SVNDirEntry> fetchEntries(SVNURL repositoryRoot, File reposRelPath) throws SVNException;
}
