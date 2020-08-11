package org.tmatesoft.svn.core.internal.wc17;

public interface ISVNEditorExtraCallbacks {
    public static final ISVNEditorExtraCallbacks DUMMY = new ISVNEditorExtraCallbacks() {
        public void startEdit(long revision) {
        }
        public void targetRevision(long revision) {
        }
    };

    void startEdit(long revision);
    void targetRevision(long revision);
}
