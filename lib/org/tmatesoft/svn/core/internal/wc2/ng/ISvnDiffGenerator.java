package org.tmatesoft.svn.core.internal.wc2.ng;

import java.io.File;
import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public interface ISvnDiffGenerator {

    void setOriginalTargets(SvnTarget originalTarget1, SvnTarget originalTarget2);

    void setAnchors(SvnTarget anchor1, SvnTarget anchor2);

    void setBaseTarget(SvnTarget baseTarget);

    void setRepositoryRoot(SvnTarget repositoryRoot);

    void setEncoding(String encoding);

    String getEncoding();

    String getGlobalEncoding();

    void setEOL(byte[] eol);

    byte[] getEOL();

    void setForceEmpty(boolean forceEmpty);

    void setForcedBinaryDiff(boolean forced);

    void setUseGitFormat(boolean useGitFormat);

    void displayDeletedDirectory(SvnTarget target, String revision1, String revision2, OutputStream outputStream) throws SVNException;

    void displayAddedDirectory(SvnTarget target, String revision1, String revision2, OutputStream outputStream) throws SVNException;

    void displayPropsChanged(SvnTarget target, String revision1, String revision2, boolean dirWasAdded, SVNProperties originalProps, SVNProperties propChanges, OutputStream outputStream) throws SVNException;

    void displayContentChanged(SvnTarget target, File leftFile, File rightFile, String revision1, String revision2, String mimeType1, String mimeType2, SvnDiffCallback.OperationKind operation, File copyFromPath, OutputStream outputStream) throws SVNException;

    boolean isForcedBinaryDiff();
}
