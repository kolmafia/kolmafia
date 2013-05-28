package org.tmatesoft.svn.core.wc2.admin;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

public class SvnRepositoryCreate extends SvnRepositoryOperation<SVNURL> {
    
    private String uuid;
    private boolean enableRevisionProperties;
    private boolean force;
    private boolean pre14Compatible;
    private boolean pre15Compatible;
    private boolean pre16Compatible;
    private boolean pre17Compatible;
    private boolean with17Compatible;
        
    public SvnRepositoryCreate(SvnOperationFactory factory) {
        super(factory);
    }
    
    public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public boolean isEnableRevisionProperties() {
		return enableRevisionProperties;
	}

	public void setEnableRevisionProperties(boolean enableRevisionProperties) {
		this.enableRevisionProperties = enableRevisionProperties;
	}

	public boolean isForce() {
		return force;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public boolean isPre15Compatible() {
		return pre15Compatible;
	}

	public void setPre15Compatible(boolean pre15Compatible) {
		this.pre15Compatible = pre15Compatible;
	}

	public boolean isPre16Compatible() {
		return pre16Compatible;
	}

	public void setPre16Compatible(boolean pre16Compatible) {
		this.pre16Compatible = pre16Compatible;
	}

	public boolean isPre17Compatible() {
		return pre17Compatible;
	}

	public void setPre17Compatible(boolean pre17Compatible) {
		this.pre17Compatible = pre17Compatible;
	}

	public boolean isWith17Compatible() {
		return with17Compatible;
	}

	public void setWith17Compatible(boolean with17Compatible) {
		this.with17Compatible = with17Compatible;
	}

	public boolean isPre14Compatible() {
		return pre14Compatible;
	}

	public void setPre14Compatible(boolean pre14Compatible) {
		this.pre14Compatible = pre14Compatible;
	}
}
