package org.tmatesoft.svn.core.internal.wc2.old;

import org.tmatesoft.svn.core.internal.wc2.SvnLocalOperationRunner;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.wc2.SvnOperation;

public abstract class SvnOldRunner<V, T extends SvnOperation<V>> extends SvnLocalOperationRunner<V, T> {
    
    public SvnWcGeneration getWcGeneration() {
        return SvnWcGeneration.V16;
    }

}
