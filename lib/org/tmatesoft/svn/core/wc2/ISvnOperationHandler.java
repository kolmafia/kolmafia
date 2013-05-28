package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNException;

public interface ISvnOperationHandler {

    public static final ISvnOperationHandler NOOP = new ISvnOperationHandler() {
        public void beforeOperation(SvnOperation<?> operation) {
        }
        public void afterOperationSuccess(SvnOperation<?> operation) {
        }
        public void afterOperationFailure(SvnOperation<?> operation) {
        }
    };

    /**
     * A callback that is called before each operation runs
     * @param operation operation for which the callback is called
     * @throws SVNException
     */
    void beforeOperation(SvnOperation<?> operation) throws SVNException;

    /**
     * A callback that is called after each successful operation runs
     * @param operation operation for which the callback is called
     * @throws SVNException
     */
    void afterOperationSuccess(SvnOperation<?> operation) throws SVNException;

    /**
     * A callback that is called after each unsuccessful operation runs
     * @param operation operation for which the callback is called
     */
    void afterOperationFailure(SvnOperation<?> operation);
}
