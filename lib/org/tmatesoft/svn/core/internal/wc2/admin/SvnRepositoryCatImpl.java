package org.tmatesoft.svn.core.internal.wc2.admin;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.tmatesoft.svn.core.wc2.admin.SvnRepositoryCat;

/**
 * Fetches file contents for the specified <code>target</code> in the given 
 * transaction. <code>path</code> must be absolute, that is it 
 * must start with <code>'/'</code>. The provided output stream 
 * is not closed within this method.
 * 
 * @param  repositoryRoot   a repository root directory path
 * @param  path             an absolute file path
 * @param  transactionName  a transaction name
 * @param  out              an output stream to write contents to
 * @throws SVNException     <ul>
 *                          <li>no repository is found at 
 *                          <code>repositoryRoot</code>
 *                          </li>
 *                          <li>if <code>path</code> is not found or
 *                          is not a file
 *                          </li>
 *                          <li>if the specified transaction is not found
 *                          </li>
 *                          </ul>
 */
public class SvnRepositoryCatImpl extends SvnRepositoryOperationRunner<Long, SvnRepositoryCat> {

    @Override
    protected Long run() throws SVNException {
        SVNLookClient lc = new SVNLookClient(getOperation().getAuthenticationManager(), getOperation().getOptions());
        lc.setEventHandler(this);
        
        lc.doCat(getOperation().getRepositoryRoot(), getOperation().getPath(), getOperation().getTransactionName(), getOperation().getOutputStream());
        
        return 1l;
    }
}
