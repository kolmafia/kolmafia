package org.tmatesoft.svn.core.wc2;

import java.util.Collection;

import org.tmatesoft.svn.core.SVNURL;

/**
 * Returns a collection of potential merge sources (expressed as full
 * repository {@link SVNURL URLs}) for working copy <code>target</code> at
 * <code>target</code>'s <code>pegRevision</code>.
 *  
 * <p/>
 * {@link #run()} potential merge sources for <code>url</code>.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnSuggestMergeSources extends SvnOperation<Collection<SVNURL>> {

    protected SvnSuggestMergeSources(SvnOperationFactory factory) {
        super(factory);
    }

    /**
     * Gets whether the operation changes working copy
     * @return <code>true</code> if the operation changes the working copy, otherwise <code>false</code>
     */
    @Override
    public boolean isChangesWorkingCopy() {
        return false;
    }
}
