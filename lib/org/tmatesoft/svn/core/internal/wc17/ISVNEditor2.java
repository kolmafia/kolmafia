package org.tmatesoft.svn.core.internal.wc17;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.wc2.SvnChecksum;

import java.io.InputStream;
import java.util.List;

/**
 * This interface may change until together SVN Ev2 (currently it is not released yet)
 *
 * @since 1.8
 */
public interface ISVNEditor2 {

    void addDir(String path, List<String> children, SVNProperties props, long replacesRev) throws SVNException;
    void addFile(String path, SvnChecksum checksum, InputStream contents, SVNProperties props, long replacesRev) throws SVNException;
    void addSymlink(String path, String target, SVNProperties props, long replacesRev) throws SVNException;
    void addAbsent(String path, SVNNodeKind kind, long replacesRev) throws SVNException;

    void alterDir(String path, long revision, List<String> children, SVNProperties props) throws SVNException;
    void alterFile(String path, long revision, SVNProperties props, SvnChecksum checksum, InputStream newContents) throws SVNException;
    void alterSymlink(String path, long revision, SVNProperties props, String target) throws SVNException;

    void delete(String path, long revision) throws SVNException;
    void copy(String srcPath, long srcRevision, String dstPath, long replacesRev) throws SVNException;
    void move(String srcPath, long srcRevision, String dstPath, long replacesRev) throws SVNException;
    void rotate(List<String> relPaths, List<String> revisions) throws SVNException;

    void complete();
    void abort();
}