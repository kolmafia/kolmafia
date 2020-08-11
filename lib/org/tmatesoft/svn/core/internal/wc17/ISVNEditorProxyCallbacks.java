package org.tmatesoft.svn.core.internal.wc17;

import org.tmatesoft.svn.core.SVNProperties;

import java.io.File;

public interface ISVNEditorProxyCallbacks {

    public static final ISVNEditorProxyCallbacks DUMMY = new ISVNEditorProxyCallbacks() {
        public void unlock(String path) {
        }
        public SVNProperties fetchProperties(String path, long baseRevision) {
            return null;
        }
        public File fetchBase(String path, long baseRevision) {
            return null;
        }
        public ISVNEditorExtraCallbacks getExtraCallbacks() {
            return ISVNEditorExtraCallbacks.DUMMY;
        }
    };

    void unlock(String path);

    SVNProperties fetchProperties(String path, long baseRevision);

    File fetchBase(String path, long baseRevision);

    ISVNEditorExtraCallbacks getExtraCallbacks();
}
