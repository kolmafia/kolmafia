package org.tmatesoft.svn.core.io;

/**
 * Mix-in interface to be optionally implemented by ISVNLockHandler
 * When supported by the server, lock timeout would be requested during lock operation.
 */
public interface ISVNLockTimeoutProvider {

    /**
     * Returns lock timeout
     *
     * @param repositoryPath path to the file about to be locked relative to repository root
     * @param path path to the file about to be locked relative to SVNRepository location
     * @param comment lock comment
     * @param force true if lock is forced
     * @return lock timeout duration in seconds or 0 for infinite timeout
     */
    long getLockTimeout(String repositoryPath, String path, String comment, boolean force);
}
