package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositorySetUUID extends SvnRepositoryOperation<Long> {
    
    private String uuid;
    
    public SvnRepositorySetUUID(SvnOperationFactory factory) {
        super(factory);
    }

    public String getUUID() {
		return uuid;
	}

	public void setUUID(String uuid) {
		this.uuid = uuid;
	}
}
