package org.tmatesoft.svn.core.wc2;

import java.io.File;

public interface ISvnPatchHandler {
    boolean singlePatch(File pathFromPatchfile, File patchPath, File rejectPath);
}
