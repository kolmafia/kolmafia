package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNProperties;

public class SvnInheritedProperties extends SvnObject {
    
    private SvnTarget target;
    private SVNProperties properties;
    
    public void setTarget(SvnTarget target) {
        this.target = target;
    }
    
    public void setProperties(SVNProperties properties) {
        this.properties = properties;
    }
    
    public SvnTarget getTarget() {
        return this.target;
    }
    
    public SVNProperties getProperties() {
        return this.properties;
    }

}
